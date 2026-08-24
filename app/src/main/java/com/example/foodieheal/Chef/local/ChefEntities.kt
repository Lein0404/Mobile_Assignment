package com.example.foodieheal.Chef.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.foodieheal.model.Appointment
import com.example.foodieheal.model.User
import com.example.mobileassignmentloginpart.Model.Chef

@Entity(tableName = "chef_portal_appointments")
data class ChefPortalAppointmentEntity(
    @PrimaryKey
    val appointmentId: String,
    val createdAt: String? = null,
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
    val rating: Int? = null,
    val comment: String? = null,
    val rejectReason: String? = null,
    val chefId: String,
    val userId: String
) {
    fun toDomainModel(): Appointment {
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
            userId = userId
        )
    }

    companion object {
        fun fromDomainModel(domain: Appointment): ChefPortalAppointmentEntity {
            return ChefPortalAppointmentEntity(
                appointmentId = domain.AppointmentID.orEmpty().ifEmpty { "${domain.chefId}_${domain.userId}_${domain.Date}_${domain.Start_Time}" },
                createdAt = domain.created_at,
                date = domain.Date,
                startTime = domain.Start_Time,
                endTime = domain.End_Time,
                address = domain.Address.orEmpty(),
                postcode = domain.Postcode.orEmpty(),
                state = domain.State.orEmpty(),
                note = domain.Note.orEmpty(),
                servingSize = domain.Serving_Size ?: 1,
                healthPreference = domain.Health_Preference.orEmpty(),
                totalPrice = domain.Total_Price ?: 0.0,
                status = domain.Status,
                rating = domain.rating,
                comment = domain.Comment,
                rejectReason = domain.Reject_Reason,
                chefId = domain.chefId.orEmpty(),
                userId = domain.userId.orEmpty()
            )
        }
    }
}

@Entity(tableName = "chef_portal_users")
data class ChefPortalUserEntity(
    @PrimaryKey
    val id: String,
    val customId: String? = null,
    val name: String,
    val email: String,
    val phone: String,
    val profilePicUrl: String? = null,
    val description: String? = null
) {
    fun toDomainModel(): User {
        return User(
            id = id,
            customId = customId,
            name = name,
            email = email,
            profilePicUrl = profilePicUrl,
            description = description
        )
    }

    companion object {
        fun fromDomainModel(domain: User): ChefPortalUserEntity {
            return ChefPortalUserEntity(
                id = domain.id.orEmpty(),
                customId = domain.customId,
                name = domain.name.orEmpty(),
                email = domain.email.orEmpty(),
                phone = "",
                profilePicUrl = domain.profilePicUrl,
                description = domain.description
            )
        }
    }
}

@Entity(tableName = "chef_portal_profiles")
data class ChefProfileEntity(
    @PrimaryKey
    val chefId: String,
    val customId: String,
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
    val pricing: Double?,
    val averagerating: Double?,
    val profilePictureUrl: String?,
    val status: String
) {
    fun toDomainModel(): Chef {
        return Chef(
            id = customId,
            chefId = chefId,
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
            Pricing = pricing,
            averagerating = averagerating,
            profilePictureUrl = profilePictureUrl,
            status = status
        )
    }

    companion object {
        fun fromDomainModel(domain: Chef): ChefProfileEntity {
            return ChefProfileEntity(
                chefId = domain.chefId.ifEmpty { domain.id },
                customId = domain.id,
                name = domain.name,
                gender = domain.gender,
                age = domain.age ?: 0,
                phoneNumber = domain.phoneNumber,
                email = domain.email,
                address = domain.address,
                state = domain.state,
                postcode = domain.postcode,
                experience = domain.experience ?: 0,
                description = domain.description,
                pricing = domain.Pricing,
                averagerating = domain.averagerating,
                profilePictureUrl = domain.profilePictureUrl,
                status = domain.status
            )
        }
    }
}

fun Appointment.toEntity(): ChefPortalAppointmentEntity = ChefPortalAppointmentEntity.fromDomainModel(this)
fun ChefPortalAppointmentEntity.toDomain(): Appointment = this.toDomainModel()

fun User.toEntity(): ChefPortalUserEntity = ChefPortalUserEntity.fromDomainModel(this)
fun ChefPortalUserEntity.toDomain(): User = this.toDomainModel()

fun Chef.toEntity(): ChefProfileEntity = ChefProfileEntity.fromDomainModel(this)
fun ChefProfileEntity.toDomain(): Chef = this.toDomainModel()

