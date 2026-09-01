package com.example.foodieheal.hiring.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.foodieheal.hiring.model.Appointment
import com.example.foodieheal.hiring.model.ChefBookmark
import com.example.foodieheal.hiring.model.ReviewWithUser
import com.example.foodieheal.Chef.model.Chef
import com.example.foodieheal.Recipe.Model.Recipe
import com.example.foodieheal.hiring.model.AppointmentRecipeWithDetails
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Entity(tableName = "hiring_chefs")
data class ChefEntity(
    @PrimaryKey
    val chefId: String,
    val id: String,
    val name: String,
    val gender: String,
    val age: Int,
    val phoneNumber: String,
    val email: String,
    val address: String,
    val state: String,
    val postcode: String,
    val experience: Int,
    val description: String,
    val profilePictureUrl: String?,
    val averagerating: Double?,
    val pricing: Double?,
    val status: String,
    val createdAt: String? = null,
    val availabilityHours: String? = null
) {
    fun toDomain(): Chef {
        return Chef(
            chefId = chefId,
            id = id,
            name = name,
            gender = gender,
            age = age,
            phoneNumber = phoneNumber,
            email = email,
            address = address,
            state = state,
            postcode = postcode,
            experience = experience,
            description = description,
            profilePictureUrl = profilePictureUrl,
            averagerating = averagerating,
            Pricing = pricing,
            status = status,
            created_at = createdAt,
            availability_hours = availabilityHours?.let {
                try {
                    kotlinx.serialization.json.Json.parseToJsonElement(it)
                } catch (e: Exception) {
                    null
                }
            }
        )
    }
}

fun Chef.toEntity(): ChefEntity {
    return ChefEntity(
        chefId = this.chefId.ifEmpty { this.id },
        id = this.id.ifEmpty { this.chefId },
        name = this.name,
        gender = this.gender,
        age = this.age,
        phoneNumber = this.phoneNumber,
        email = this.email,
        address = this.address,
        state = this.state,
        postcode = this.postcode,
        experience = this.experience,
        description = this.description,
        profilePictureUrl = this.profilePictureUrl,
        averagerating = this.averagerating,
        pricing = this.Pricing,
        status = this.status,
        createdAt = this.created_at,
        availabilityHours = this.availability_hours?.toString()
    )
}

@Entity(tableName = "hiring_appointments")
data class AppointmentEntity(
    @PrimaryKey
    val appointmentId: String,
    val createdAt: String?,
    val date: String,
    val startTime: String,
    val endTime: String,
    val address: String,
    val postcode: String,
    val state: String,
    val note: String,
    val servingSize: Int,
    val healthPreference: String,
    val totalPrice: Double,
    val status: String,
    val rating: Int?,
    val comment: String?,
    val rejectReason: String?,
    val chefId: String,
    val userId: String,
    val paymentId: String? = null
) {
    fun toDomain(): Appointment {
        return Appointment(
            AppointmentID = appointmentId,
            created_at = createdAt,
            Date = date,
            Start_Time = startTime,
            End_Time = endTime,
            Address = address,
            Postcode = postcode,
            State = state,
            Note = note,
            Serving_Size = servingSize,
            Health_Preference = healthPreference,
            Total_Price = totalPrice,
            Status = status,
            rating = rating,
            Comment = comment,
            Reject_Reason = rejectReason,
            chefId = chefId,
            userId = userId,
            PaymentId = paymentId
        )
    }
}

fun Appointment.toEntity(): AppointmentEntity {
    return AppointmentEntity(
        appointmentId = this.AppointmentID.orEmpty().ifEmpty { java.util.UUID.randomUUID().toString() },
        createdAt = this.created_at,
        date = this.Date,
        startTime = this.Start_Time,
        endTime = this.End_Time,
        address = this.Address,
        postcode = this.Postcode,
        state = this.State,
        note = this.Note,
        servingSize = this.Serving_Size,
        healthPreference = this.Health_Preference,
        totalPrice = this.Total_Price,
        status = this.Status,
        rating = this.rating,
        comment = this.Comment,
        rejectReason = this.Reject_Reason,
        chefId = this.chefId,
        userId = this.userId,
        paymentId = this.PaymentId
    )
}

@Entity(tableName = "hiring_chef_bookmarks")
data class ChefBookmarkEntity(
    @PrimaryKey
    val bookmarkId: String,
    val userId: String,
    val chefId: String,
    val createdAt: String?
) {
    fun toDomain(): ChefBookmark {
        return ChefBookmark(
            id = bookmarkId,
            userId = userId,
            chefId = chefId,
            createdAt = createdAt
        )
    }
}

fun ChefBookmark.toEntity(): ChefBookmarkEntity {
    return ChefBookmarkEntity(
        bookmarkId = this.id ?: "${this.userId}_${this.chefId}",
        userId = this.userId,
        chefId = this.chefId,
        createdAt = this.createdAt
    )
}

@Entity(tableName = "hiring_reviews")
data class ChefReviewEntity(
    @PrimaryKey
    val appointmentId: String,
    val chefId: String,
    val userId: String,
    val userName: String,
    val rating: Int?,
    val comment: String?,
    val date: String?,
    val createdAt: String?
) {
    fun toDomain(): ReviewWithUser {
        val appointment = Appointment(
            AppointmentID = appointmentId,
            created_at = createdAt,
            Date = date.orEmpty(),
            Start_Time = "",
            End_Time = "",
            Address = "",
            Postcode = "",
            State = "",
            Note = "",
            Serving_Size = 0,
            Health_Preference = "",
            Total_Price = 0.0,
            Status = "Completed",
            rating = rating,
            Comment = comment,
            Reject_Reason = null,
            chefId = chefId,
            userId = userId
        )
        return ReviewWithUser(
            appointment = appointment,
            userName = userName
        )
    }
}

fun ReviewWithUser.toEntity(): ChefReviewEntity {
    return ChefReviewEntity(
        appointmentId = this.appointment.AppointmentID.orEmpty().ifEmpty { java.util.UUID.randomUUID().toString() },
        chefId = this.appointment.chefId,
        userId = this.appointment.userId,
        userName = this.userName,
        rating = this.appointment.rating,
        comment = this.appointment.Comment,
        date = this.appointment.Date,
        createdAt = this.appointment.created_at
    )
}

private val recipeJsonParser = Json { ignoreUnknownKeys = true }

@Entity(tableName = "hiring_appointment_recipes")
data class AppointmentRecipeEntity(
    @PrimaryKey
    val id: String,
    val appointmentId: String,
    val recipeId: String,
    val serviceCount: Double,
    val customNote: String?,
    val chefProvideIngredient: Boolean,
    val recipeJson: String? = null
) {
    fun toDomain(): AppointmentRecipeWithDetails {
        val decodedRecipe = recipeJson?.let {
            try {
                recipeJsonParser.decodeFromString<Recipe>(it)
            } catch (e: Exception) {
                null
            }
        }
        return AppointmentRecipeWithDetails(
            id = id,
            appointmentId = appointmentId,
            recipeId = recipeId,
            service_count = serviceCount,
            custom_note = customNote,
            chef_provide_ingredient = chefProvideIngredient,
            recipe = decodedRecipe
        )
    }
}

fun AppointmentRecipeWithDetails.toRecipeEntity(): AppointmentRecipeEntity {
    val json = recipe?.let {
        try {
            recipeJsonParser.encodeToString(it)
        } catch (e: Exception) {
            null
        }
    }
    return AppointmentRecipeEntity(
        id = this.id?.ifBlank { null } ?: "${appointmentId}_${recipeId}",
        appointmentId = this.appointmentId,
        recipeId = this.recipeId,
        serviceCount = this.service_count,
        customNote = this.custom_note,
        chefProvideIngredient = this.chef_provide_ingredient,
        recipeJson = json
    )
}
