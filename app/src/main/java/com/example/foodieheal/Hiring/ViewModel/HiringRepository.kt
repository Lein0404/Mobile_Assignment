package com.example.foodieheal.Hiring.ViewModel

import com.example.foodieheal.SupabaseClient.client
import com.example.foodieheal.model.Appointment
import com.example.foodieheal.model.ReviewWithUser
import com.example.foodieheal.model.User
import com.example.mobileassignmentloginpart.Model.Chef
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class HiringRepository {

    fun getCurrentUserId(): String? {
        return client.auth.currentUserOrNull()?.id
    }

    suspend fun fetchAllChefs(): List<Chef> = withContext(Dispatchers.IO) {
        val chefs = client.postgrest["Chef"]
            .select { filter { ilike("Status", "approved") } }
            .decodeList<Chef>()

        val ratedAppointments = client.postgrest["Appointment"]
            .select { filter { gt("rating", 0) } }
            .decodeList<Appointment>()

        chefs.filter { (it.Pricing ?: -1.0) >= 0.0 }
            .map { chef ->
                val idToMatch = chef.chefId.ifEmpty { chef.id }
                val chefRatings = ratedAppointments
                    .filter { it.chefId == idToMatch }
                    .mapNotNull { it.rating }

                val avgRating = if (chefRatings.isNotEmpty()) {
                    (chefRatings.average() * 10).toInt() / 10.0
                } else null

                chef.copy(averagerating = avgRating)
            }
            .sortedByDescending { it.averagerating ?: 0.0 }
    }

    suspend fun fetchAppointmentsForUser(userId: String): List<Appointment> = withContext(Dispatchers.IO) {
        client.from("Appointment")
            .select { filter { eq("userId", userId) } }
            .decodeList<Appointment>()
    }

    suspend fun fetchChefsMapForAppointments(appointments: List<Appointment>): Map<String, User> = withContext(Dispatchers.IO) {
        val chefIds = appointments.mapNotNull { it.chefId }.distinct().filter { it.isNotBlank() }
        if (chefIds.isEmpty()) return@withContext emptyMap()

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
    }

    suspend fun fetchChefAppointments(chefId: String): Pair<List<Appointment>, User?> = withContext(Dispatchers.IO) {
        val appointments = client.from("Appointment")
            .select { filter { eq("chefId", chefId) } }
            .decodeList<Appointment>()

        val user = client.from("users")
            .select { filter { eq("id", chefId) } }
            .decodeSingleOrNull<User>()

        Pair(appointments, user)
    }

    suspend fun createAppointment(appointment: Appointment) = withContext(Dispatchers.IO) {
        client.from("Appointment").insert(appointment)
    }

    suspend fun updateAppointmentStatus(appointmentId: String, status: String) = withContext(Dispatchers.IO) {
        val updateData = buildJsonObject { put("Status", status) }
        client.from("Appointment").update(updateData) {
            filter { eq("AppointmentID", appointmentId) }
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
        newDescription: String
    ) = withContext(Dispatchers.IO) {
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
        }
        client.from("Appointment").update(updateData) {
            filter { eq("AppointmentID", appointmentId) }
        }
    }

    suspend fun fetchChefReviews(chefId: String): List<ReviewWithUser> = withContext(Dispatchers.IO) {
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

        appointments.map { appointment ->
            val user = usersMap[appointment.userId]
            ReviewWithUser(
                appointment = appointment,
                userName = user?.name?.ifBlank { null } ?: "Customer"
            )
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
    }
}