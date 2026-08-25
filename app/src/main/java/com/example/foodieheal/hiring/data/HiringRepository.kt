package com.example.foodieheal.hiring.data

import android.util.Log
import com.example.foodieheal.Payment.data.payment
import com.example.foodieheal.SupabaseClient.client
import com.example.foodieheal.hiring.local.AppointmentDao
import com.example.foodieheal.hiring.local.ChefDao
import com.example.foodieheal.hiring.local.ChefReviewDao
import com.example.foodieheal.hiring.local.toEntity
import com.example.foodieheal.hiring.model.Appointment
import com.example.foodieheal.hiring.model.ReviewWithUser
import com.example.foodieheal.User.Model.User
import com.example.foodieheal.Chef.model.Chef
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.Locale
import java.util.UUID

class HiringRepository(
    private val chefDao: ChefDao? = null,
    private val appointmentDao: AppointmentDao? = null,
    private val reviewDao: ChefReviewDao? = null
) {

    fun getCurrentUserId(): String? {
        return client.auth.currentUserOrNull()?.id
    }

    suspend fun fetchAllChefs(): List<Chef> = withContext(Dispatchers.IO) {
        try {
            val chefs = client.from("Chef")
                .select { filter { ilike("Status", "approved") } }
                .decodeList<Chef>()

            val ratedAppointments = try {
                client.from("Appointment")
                    .select { filter { gt("rating", 0) } }
                    .decodeList<Appointment>()
            } catch (e: Exception) {
                Log.e("HiringRepository", "Could not fetch rated appointments: ${e.localizedMessage}", e)
                emptyList()
            }

            val processedChefs = chefs.filter { (it.Pricing ?: -1.0) >= 0.0 }
                .map { chef ->
                    val idToMatch = chef.chefId.ifEmpty { chef.id }
                    val chefRatings = ratedAppointments
                        .filter { appt ->
                            (appt.chefId.isNotBlank() && (appt.chefId == chef.chefId || appt.chefId == chef.id)) ||
                            (idToMatch.isNotBlank() && appt.chefId == idToMatch)
                        }
                        .mapNotNull { it.rating }

                    val avgRating = if (chefRatings.isNotEmpty()) {
                        (chefRatings.average() * 10).toInt() / 10.0
                    } else {
                        chef.averagerating
                    }

                    chef.copy(averagerating = avgRating)
                }
                .sortedByDescending { it.averagerating ?: 0.0 }

            // Cache into Room
            chefDao?.let { dao ->
                dao.clearChefs()
                dao.insertChefs(processedChefs.map { it.toEntity() })
            }

            processedChefs
        } catch (e: Exception) {
            Log.e("HiringRepository", "Network error fetching chefs, falling back to local database", e)
            chefDao?.getAllChefs()?.map { it.toDomain() } ?: emptyList()
        }
    }

    suspend fun fetchAppointmentsForUser(userId: String): List<Appointment> = withContext(Dispatchers.IO) {
        try {
            val appointments = client.from("Appointment")
                .select { filter { eq("userId", userId) } }
                .decodeList<Appointment>()

            // Cache into Room
            appointmentDao?.insertAppointments(appointments.map { it.toEntity() })

            appointments
        } catch (e: Exception) {
            Log.e("HiringRepository", "Network error fetching user appointments, falling back to local database", e)
            appointmentDao?.getAppointmentsForUser(userId)?.map { it.toDomain() } ?: emptyList()
        }
    }

    suspend fun fetchChefsMapForAppointments(appointments: List<Appointment>): Map<String, User> = withContext(Dispatchers.IO) {
        val chefIds = appointments.mapNotNull { it.chefId }.distinct().filter { it.isNotBlank() }
        if (chefIds.isEmpty()) return@withContext emptyMap()

        try {
            val chefRecords = client.from("Chef")
                .select { filter { isIn("chefId", chefIds) } }
                .decodeList<Chef>()

            chefRecords.associate { chef ->
                val key = chef.chefId.ifEmpty { chef.id }
                key to User(
                    id = key,
                    name = chef.name,
                    profilePicUrl = chef.profilePictureUrl.orEmpty()
                )
            }
        } catch (e: Exception) {
            Log.e("HiringRepository", "Error fetching chef details map for appointments", e)
            emptyMap()
        }
    }

    suspend fun fetchChefAppointments(chefId: String): Pair<List<Appointment>, User?> = withContext(Dispatchers.IO) {
        try {
            val appointments = client.from("Appointment")
                .select { filter { eq("chefId", chefId) } }
                .decodeList<Appointment>()

            val user = client.from("users")
                .select { filter { eq("id", chefId) } }
                .decodeSingleOrNull<User>()

            // Cache appointments in Room
            appointmentDao?.insertAppointments(appointments.map { it.toEntity() })

            Pair(appointments, user)
        } catch (e: Exception) {
            Log.e("HiringRepository", "Network error fetching chef appointments, falling back to local database", e)
            val cachedAppointments = appointmentDao?.getAppointmentsForChef(chefId)?.map { it.toDomain() } ?: emptyList()
            Pair(cachedAppointments, null)
        }
    }

    suspend fun createAppointment(appointment: Appointment) = withContext(Dispatchers.IO) {
        val generatedAppointmentId = appointment.AppointmentID?.ifBlank { null } ?: UUID.randomUUID().toString()
        val generatedPaymentId = appointment.PaymentId?.ifBlank { null } ?: UUID.randomUUID().toString()

        val initialAppointment = appointment.copy(
            AppointmentID = generatedAppointmentId,
            PaymentId = null,
            Status = appointment.Status.ifBlank { "Pending" }
        )
        client.from("Appointment").insert(initialAppointment)

        val paymentRecord = payment(
            paymentId = generatedPaymentId,
            transactionId = null,
            appointmentId = generatedAppointmentId,
            userId = initialAppointment.userId,
            totalAmount = initialAppointment.Total_Price,
            paymentMethod = null,
            paymentMethodId = null,
            status = "Pending"
        )
        client.from("Payment").insert(paymentRecord)

        client.from("Appointment").update(
            {
                set("PaymentId", generatedPaymentId)
            }
        ) {
            filter {
                eq("AppointmentID", generatedAppointmentId)
            }
        }

        val finalAppointment = initialAppointment.copy(PaymentId = generatedPaymentId)
        appointmentDao?.insertAppointment(finalAppointment.toEntity())
    }

    suspend fun updateAppointmentStatus(appointmentId: String, status: String) = withContext(Dispatchers.IO) {
        val updateData = buildJsonObject { put("Status", status) }
        client.from("Appointment").update(updateData) {
            filter { eq("AppointmentID", appointmentId) }
        }
        appointmentDao?.updateAppointmentStatus(appointmentId, status)
    }

    suspend fun cancelAppointment(appointmentId: String) = withContext(Dispatchers.IO) {
        val currentAppt = try {
            client.from("Appointment")
                .select { filter { eq("AppointmentID", appointmentId) } }
                .decodeSingleOrNull<Appointment>()
        } catch (e: Exception) {
            appointmentDao?.getAppointmentById(appointmentId)?.toDomain()
        }

        val originalStatus = currentAppt?.Status.orEmpty().trim().lowercase(Locale.US)
        val linkedPaymentId = currentAppt?.PaymentId
        val targetPaymentStatus = if (originalStatus == "confirmed") "Refunded" else "Cancelled"

        updateAppointmentStatus(appointmentId, "Cancelled")

        try {
            if (!linkedPaymentId.isNullOrBlank()) {
                client.from("Payment").update({ set("status", targetPaymentStatus) }) {
                    filter { eq("paymentId", linkedPaymentId) }
                }
            } else {
                client.from("Payment").update({ set("status", targetPaymentStatus) }) {
                    filter { eq("appointmentID", appointmentId) }
                }
            }
        } catch (e: Exception) {
            Log.e("HiringRepository", "Error updating payment status on cancel: ${e.localizedMessage}", e)
        }
    }

    suspend fun rescheduleAppointment(
        appointmentId: String,
        newDate: String,
        newStartTime: String,
        newEndTime: String,
        newAddress: String,
        newPostcode: String,
        newState: String,
        newServingSize: Int,
        newDescription: String,
        newTotalPrice: Double = 0.0
    ) = withContext(Dispatchers.IO) {

        val currentAppt = try {
            client.from("Appointment")
                .select { filter { eq("AppointmentID", appointmentId) } }
                .decodeSingleOrNull<Appointment>()
        } catch (e: Exception) {
            appointmentDao?.getAppointmentById(appointmentId)?.toDomain()
        }

        val originalStatus = currentAppt?.Status.orEmpty().trim().lowercase(Locale.US)
        val oldPaymentId = currentAppt?.PaymentId
        val finalPrice = if (newTotalPrice > 0.0) newTotalPrice else (currentAppt?.Total_Price ?: 0.0)

        var newPaymentId: String? = oldPaymentId

        if (originalStatus == "confirmed") {
            // Appointment was already paid, refund previous payment
            try {
                if (!oldPaymentId.isNullOrBlank()) {
                    client.from("Payment").update({ set("status", "Refunded") }) {
                        filter { eq("paymentId", oldPaymentId) }
                    }
                } else {
                    client.from("Payment").update({ set("status", "Refunded") }) {
                        filter { eq("appointmentID", appointmentId) }
                    }
                }

                // TODO: Future Wallet Feature - Credit the refunded amount (currentAppt.Total_Price) back to user's wallet balance
                // like walletRepository.creditRefund(userId = currentAppt.userId, amount = currentAppt.Total_Price, appointmentId = appointmentId)

                // Create a new pending payment record for the rescheduled appointment
                val generatedPaymentId = UUID.randomUUID().toString()
                newPaymentId = generatedPaymentId

                val newPayment = payment(
                    paymentId = generatedPaymentId,
                    transactionId = null,
                    appointmentId = appointmentId,
                    userId = currentAppt?.userId.orEmpty(),
                    totalAmount = finalPrice,
                    paymentMethod = null,
                    paymentMethodId = null,
                    status = "Pending"
                )
                client.from("Payment").insert(newPayment)
            } catch (e: Exception) {
                Log.e("HiringRepository", "Error processing refund/payment on reschedule: ${e.localizedMessage}", e)
            }
        } else {
            // update the existing payment with new total price
            try {
                if (!oldPaymentId.isNullOrBlank()) {
                    client.from("Payment").update({
                        set("totalAmount", finalPrice)
                        set("status", "Pending")
                    }) {
                        filter { eq("paymentId", oldPaymentId) }
                    }
                } else {
                    client.from("Payment").update({
                        set("totalAmount", finalPrice)
                        set("status", "Pending")
                    }) {
                        filter { eq("appointmentID", appointmentId) }
                    }
                }
            } catch (e: Exception) {
                Log.e("HiringRepository", "Error updating pending payment on reschedule: ${e.localizedMessage}", e)
            }
        }

        val updateData = buildJsonObject {
            put("Date", newDate)
            put("Start_Time", newStartTime)
            put("End_Time", newEndTime)
            put("Address", newAddress)
            put("Postcode", newPostcode)
            put("State", newState)
            put("Serving_Size", newServingSize)
            put("Note", newDescription)
            put("Status", "Pending")
            put("Total_Price", finalPrice)
            if (!newPaymentId.isNullOrBlank()) {
                put("PaymentId", newPaymentId)
            }
        }

        client.from("Appointment").update(updateData) {
            filter { eq("AppointmentID", appointmentId) }
        }

        appointmentDao?.updateAppointmentStatus(appointmentId, "Pending")
    }

    suspend fun fetchChefReviews(chefId: String): List<ReviewWithUser> = withContext(Dispatchers.IO) {
        try {
            val appointments = client.from("Appointment")
                .select {
                    filter {
                        eq("chefId", chefId)
                        gt("rating", 0)
                    }
                }
                .decodeList<Appointment>()
                .filter { !it.Comment.isNullOrBlank() || (it.rating ?: 0) > 0 }

            if (appointments.isEmpty()) return@withContext emptyList()

            val userIds = appointments.map { it.userId }.filter { it.isNotBlank() }.distinct()
            val usersMap = if (userIds.isNotEmpty()) {
                client.from("users")
                    .select { filter { isIn("id", userIds) } }
                    .decodeList<User>()
                    .associateBy { it.id }
            } else {
                emptyMap()
            }

            val reviews = appointments.map { appointment ->
                val user = usersMap[appointment.userId]
                ReviewWithUser(
                    appointment = appointment,
                    userName = user?.name?.ifBlank { null } ?: "Customer"
                )
            }

            // Cache reviews into Room
            reviewDao?.let { dao ->
                dao.clearReviewsForChef(chefId)
                dao.insertReviews(reviews.map { it.toEntity() })
            }

            reviews
        } catch (e: Exception) {
            Log.e("HiringRepository", "Network error fetching chef reviews, falling back to local database", e)
            reviewDao?.getReviewsForChef(chefId)?.map { it.toDomain() } ?: emptyList()
        }
    }

    suspend fun submitReview(
        appointmentId: String,
        rating: Int,
        comment: String
    ) = withContext(Dispatchers.IO) {
        val updateData = buildJsonObject {
            put("rating", rating)
            put("Comment", comment)
        }
        client.from("Appointment").update(updateData) {
            filter { eq("AppointmentID", appointmentId) }
        }

        try {
            val appt = client.from("Appointment")
                .select { filter { eq("AppointmentID", appointmentId) } }
                .decodeSingleOrNull<Appointment>()

            val targetChefId = appt?.chefId
            if (!targetChefId.isNullOrBlank()) {
                val chefReviews = client.from("Appointment")
                    .select {
                        filter {
                            eq("chefId", targetChefId)
                            gt("rating", 0)
                        }
                    }
                    .decodeList<Appointment>()

                val allRatings = chefReviews.mapNotNull { it.rating }
                val newAvg = if (allRatings.isNotEmpty()) {
                    (allRatings.average() * 10).toInt() / 10.0
                } else {
                    rating.toDouble()
                }

                client.from("Chef").update(buildJsonObject { put("averagerating", newAvg) }) {
                    filter {
                        or {
                            eq("chefId", targetChefId)
                            eq("id", targetChefId)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("HiringRepository", "Error updating chef average rating: ${e.localizedMessage}", e)
        }
    }
}
