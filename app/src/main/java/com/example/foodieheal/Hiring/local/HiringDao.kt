package com.example.foodieheal.hiring.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChefDao {

    @Query("SELECT * FROM hiring_chefs WHERE status = 'approved' ORDER BY averagerating DESC")
    fun getAllChefsFlow(): Flow<List<ChefEntity>>

    @Query("SELECT * FROM hiring_chefs WHERE status = 'approved' ORDER BY averagerating DESC")
    suspend fun getAllChefs(): List<ChefEntity>

    @Query("SELECT * FROM hiring_chefs WHERE chefId = :chefId OR id = :chefId LIMIT 1")
    suspend fun getChefById(chefId: String): ChefEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChefs(chefs: List<ChefEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChef(chef: ChefEntity)

    @Query("DELETE FROM hiring_chefs")
    suspend fun clearChefs()
}

@Dao
interface AppointmentDao {

    @Query("SELECT * FROM hiring_appointments WHERE userId = :userId ORDER BY date DESC")
    fun getAppointmentsForUserFlow(userId: String): Flow<List<AppointmentEntity>>

    @Query("SELECT * FROM hiring_appointments WHERE userId = :userId ORDER BY date DESC")
    suspend fun getAppointmentsForUser(userId: String): List<AppointmentEntity>

    @Query("SELECT * FROM hiring_appointments WHERE chefId = :chefId ORDER BY date ASC")
    fun getAppointmentsForChefFlow(chefId: String): Flow<List<AppointmentEntity>>

    @Query("SELECT * FROM hiring_appointments WHERE chefId = :chefId ORDER BY date ASC")
    suspend fun getAppointmentsForChef(chefId: String): List<AppointmentEntity>

    @Query("SELECT * FROM hiring_appointments WHERE appointmentId = :appointmentId LIMIT 1")
    suspend fun getAppointmentById(appointmentId: String): AppointmentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppointments(appointments: List<AppointmentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppointment(appointment: AppointmentEntity)

    @Query("UPDATE hiring_appointments SET status = :status WHERE appointmentId = :appointmentId")
    suspend fun updateAppointmentStatus(appointmentId: String, status: String)

    @Query("DELETE FROM hiring_appointments WHERE appointmentId = :appointmentId")
    suspend fun deleteAppointment(appointmentId: String)

    @Query("DELETE FROM hiring_appointments")
    suspend fun clearAppointments()
}

@Dao
interface ChefBookmarkDao {

    @Query("SELECT * FROM hiring_chef_bookmarks WHERE userId = :userId")
    fun getBookmarksForUserFlow(userId: String): Flow<List<ChefBookmarkEntity>>

    @Query("SELECT * FROM hiring_chef_bookmarks WHERE userId = :userId")
    suspend fun getBookmarksForUser(userId: String): List<ChefBookmarkEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM hiring_chef_bookmarks WHERE userId = :userId AND chefId = :chefId)")
    suspend fun isBookmarked(userId: String, chefId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmarks(bookmarks: List<ChefBookmarkEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: ChefBookmarkEntity)

    @Query("DELETE FROM hiring_chef_bookmarks WHERE userId = :userId AND chefId = :chefId")
    suspend fun deleteBookmark(userId: String, chefId: String)

    @Query("DELETE FROM hiring_chef_bookmarks WHERE userId = :userId")
    suspend fun clearBookmarksForUser(userId: String)
}

@Dao
interface ChefReviewDao {

    @Query("SELECT * FROM hiring_reviews WHERE chefId = :chefId ORDER BY rating DESC")
    fun getReviewsForChefFlow(chefId: String): Flow<List<ChefReviewEntity>>

    @Query("SELECT * FROM hiring_reviews WHERE chefId = :chefId ORDER BY rating DESC")
    suspend fun getReviewsForChef(chefId: String): List<ChefReviewEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReviews(reviews: List<ChefReviewEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: ChefReviewEntity)

    @Query("DELETE FROM hiring_reviews WHERE chefId = :chefId")
    suspend fun clearReviewsForChef(chefId: String)
}
