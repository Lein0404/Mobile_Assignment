package com.example.foodieheal.Chef.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChefPortalAppointmentDao {

    @Query("SELECT * FROM chef_portal_appointments WHERE chefId = :chefId ORDER BY date ASC")
    fun getAppointmentsForChefFlow(chefId: String): Flow<List<ChefPortalAppointmentEntity>>

    @Query("SELECT * FROM chef_portal_appointments WHERE chefId = :chefId ORDER BY date ASC")
    suspend fun getAppointmentsForChef(chefId: String): List<ChefPortalAppointmentEntity>

    @Query("SELECT * FROM chef_portal_appointments WHERE appointmentId = :appointmentId LIMIT 1")
    suspend fun getAppointmentById(appointmentId: String): ChefPortalAppointmentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppointments(appointments: List<ChefPortalAppointmentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppointment(appointment: ChefPortalAppointmentEntity)

    @Query("UPDATE chef_portal_appointments SET status = :status, rejectReason = :rejectReason WHERE appointmentId = :appointmentId")
    suspend fun updateAppointmentStatus(
        appointmentId: String,
        status: String,
        rejectReason: String? = null
    )

    @Query("DELETE FROM chef_portal_appointments WHERE appointmentId = :appointmentId")
    suspend fun deleteAppointment(appointmentId: String)

    @Query("DELETE FROM chef_portal_appointments WHERE chefId = :chefId")
    suspend fun clearAppointmentsForChef(chefId: String)

    @Query("DELETE FROM chef_portal_appointments")
    suspend fun clearAllAppointments()
}

@Dao
interface ChefPortalUserDao {

    @Query("SELECT * FROM chef_portal_users WHERE id IN (:userIds)")
    fun getUsersByIdsFlow(userIds: List<String>): Flow<List<ChefPortalUserEntity>>

    @Query("SELECT * FROM chef_portal_users WHERE id IN (:userIds)")
    suspend fun getUsersByIds(userIds: List<String>): List<ChefPortalUserEntity>

    @Query("SELECT * FROM chef_portal_users WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: String): ChefPortalUserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<ChefPortalUserEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: ChefPortalUserEntity)

    @Query("DELETE FROM chef_portal_users WHERE id = :userId")
    suspend fun deleteUser(userId: String)

    @Query("DELETE FROM chef_portal_users")
    suspend fun clearAllUsers()
}

@Dao
interface ChefProfileDao {

    @Query("SELECT * FROM chef_portal_profiles WHERE chefId = :chefId OR customId = :chefId LIMIT 1")
    fun getChefProfileFlow(chefId: String): Flow<ChefProfileEntity?>

    @Query("SELECT * FROM chef_portal_profiles WHERE chefId = :chefId OR customId = :chefId LIMIT 1")
    suspend fun getChefProfile(chefId: String): ChefProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChefProfile(profile: ChefProfileEntity)

    @Query("DELETE FROM chef_portal_profiles WHERE chefId = :chefId OR customId = :chefId")
    suspend fun clearChefProfile(chefId: String)

    @Query("DELETE FROM chef_portal_profiles")
    suspend fun clearAllChefProfiles()
}
