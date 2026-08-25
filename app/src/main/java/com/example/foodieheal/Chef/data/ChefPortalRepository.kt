package com.example.foodieheal.Chef.data

import android.util.Log
import com.example.foodieheal.Chef.local.ChefPortalAppointmentDao
import com.example.foodieheal.Chef.local.ChefPortalUserDao
import com.example.foodieheal.Chef.local.ChefProfileDao
import com.example.foodieheal.Chef.local.toDomain
import com.example.foodieheal.Chef.local.toEntity
import com.example.foodieheal.SupabaseClient.client
import com.example.foodieheal.hiring.model.Appointment
import com.example.foodieheal.User.Model.User
import com.example.foodieheal.Chef.model.Chef
import com.example.foodieheal.Recipe.Repo.RecipeRepository
import com.example.foodieheal.hiring.model.AppointmentRecipe
import com.example.foodieheal.hiring.model.AppointmentRecipeWithDetails
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChefPortalRepository(
    private val appointmentDao: ChefPortalAppointmentDao,
    private val userDao: ChefPortalUserDao,
    private val profileDao: ChefProfileDao,
    private val recipeRepository: RecipeRepository = RecipeRepository(client)
) {

    fun getCurrentUserId(): String? {
        return client.auth.currentUserOrNull()?.id
    }

    suspend fun fetchAppointmentsForChef(chefId: String): List<Appointment> = withContext(Dispatchers.IO) {
        try {
            val remote = client.from("Appointment")
                .select {
                    filter {
                        eq("chefId", chefId)
                    }
                }
                .decodeList<Appointment>()

            appointmentDao.insertAppointments(remote.map { it.toEntity() })
            remote
        } catch (e: Exception) {
            // error -> return cached data from Room
            Log.e(TAG, "Error fetching appointments from network, falling back to Room DB", e)
            appointmentDao.getAppointmentsForChef(chefId).map { it.toDomain() }
        }
    }

    suspend fun fetchUsersForAppointments(appointments: List<Appointment>): Map<String, User> = withContext(Dispatchers.IO) {
        val userIds = appointments.map { it.userId }.distinct().filter { it.isNotBlank() }
        if (userIds.isEmpty()) return@withContext emptyMap()

        try {
            val remote = client.from("users")
                .select {
                    filter {
                        isIn("id", userIds)
                    }
                }
                .decodeList<User>()

            userDao.insertUsers(remote.map { it.toEntity() })
            remote.associateBy { it.id ?: "" }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching users from network, falling back to Room DB", e)
            val cached = userDao.getUsersByIds(userIds)
            cached.associate { it.id to it.toDomain() }
        }
    }

    suspend fun updateAppointmentStatus(
        appointmentId: String,
        newStatus: String,
        rejectionReason: String? = null
    ) = withContext(Dispatchers.IO) {
        try {
            val updateData = buildMap<String, String> {
                put("Status", newStatus)
                if (!rejectionReason.isNullOrBlank()) {
                    put("Reject_Reason", rejectionReason)
                }
            }

            client.from("Appointment")
                .update(updateData) {
                    filter {
                        eq("AppointmentID", appointmentId)
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Network error updating appointment status in Supabase", e)
        }

        // Update local Room database
        appointmentDao.updateAppointmentStatus(
            appointmentId = appointmentId,
            status = newStatus,
            rejectReason = rejectionReason
        )
    }

    suspend fun fetchAppointmentRecipes(appointmentId: String): List<AppointmentRecipeWithDetails> = withContext(Dispatchers.IO) {
        if (appointmentId.isBlank()) return@withContext emptyList()
        try {
            val appointmentRecipes = client.from("appointment_recipe")
                .select {
                    filter {
                        eq("appointmentId", appointmentId)
                    }
                }
                .decodeList<AppointmentRecipe>()

            if (appointmentRecipes.isEmpty()) return@withContext emptyList()

            val recipeIds = appointmentRecipes.map { it.recipeId }.distinct()
            val fetchedRecipes = recipeRepository.getRecipesByIds(recipeIds).getOrDefault(emptyList())
            val recipesMap = fetchedRecipes.associateBy { it.recipe_id ?: "" }

            appointmentRecipes.map { apptRecipe ->
                AppointmentRecipeWithDetails(
                    id = apptRecipe.id,
                    appointmentId = apptRecipe.appointmentId,
                    recipeId = apptRecipe.recipeId,
                    service_count = apptRecipe.service_count,
                    custom_note = apptRecipe.custom_note,
                    recipe = recipesMap[apptRecipe.recipeId]
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching appointment recipes: ${e.localizedMessage}", e)
            emptyList()
        }
    }

    suspend fun cacheChefProfile(chef: Chef) = withContext(Dispatchers.IO) {
        profileDao.insertChefProfile(chef.toEntity())
    }

    suspend fun getCachedChefProfile(chefId: String): Chef? = withContext(Dispatchers.IO) {
        profileDao.getChefProfile(chefId)?.toDomain()
    }

    companion object {
        private const val TAG = "ChefPortalRepository"
    }
}
