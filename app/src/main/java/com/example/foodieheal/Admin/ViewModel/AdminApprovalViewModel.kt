package com.example.foodieheal.Admin.ViewModel1

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodieheal.SupabaseClient
import com.example.foodieheal.Chef.model.Chef
import com.example.foodieheal.Chef.notification.ChefEmailNotificationService
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch

class AdminApprovalViewModel : ViewModel() {

    var allChefs by mutableStateOf<List<Chef>>(emptyList())
        private set

    var pendingChefs by mutableStateOf<List<Chef>>(emptyList())
        private set

    var selectedStatusTab by mutableStateOf(0) // 0: All, 1: Pending, 2: Approved, 3: Rejected

    var selectedChef by mutableStateOf<Chef?>(null)
        private set

    var searchQuery by mutableStateOf("")
        private set

    var isRefreshing by mutableStateOf(false)
        private set

    var showLogoutDialog by mutableStateOf(false)

    val pendingCount: Int
        get() = allChefs.count { it.status.equals("Pending", ignoreCase = true) }

    val approvedCount: Int
        get() = allChefs.count { it.status.equals("Approved", ignoreCase = true) }

    val rejectedCount: Int
        get() = allChefs.count { it.status.equals("Rejected", ignoreCase = true) }

    val totalCount: Int
        get() = allChefs.size

    val displayedChefs: List<Chef>
        get() {
            val statusFiltered = when (selectedStatusTab) {
                0 -> allChefs
                1 -> allChefs.filter { it.status.equals("Pending", ignoreCase = true) }
                2 -> allChefs.filter { it.status.equals("Approved", ignoreCase = true) }
                3 -> allChefs.filter { it.status.equals("Rejected", ignoreCase = true) }
                else -> allChefs
            }
            return if (searchQuery.isBlank()) {
                statusFiltered
            } else {
                statusFiltered.filter {
                    it.name.contains(searchQuery, ignoreCase = true) ||
                    it.email.contains(searchQuery, ignoreCase = true) ||
                    it.description.contains(searchQuery, ignoreCase = true) ||
                    it.chefId.contains(searchQuery, ignoreCase = true)
                }
            }
        }

    fun onSearchQueryChanged(query: String) {
        searchQuery = query
    }

    fun onStatusTabSelected(tab: Int) {
        selectedStatusTab = tab
    }

    fun onShowLogoutDialog(show: Boolean) {
        showLogoutDialog = show
    }

    fun refreshChefs() {
        viewModelScope.launch {
            isRefreshing = true
            try {
                val client = SupabaseClient.client
                val list = client.postgrest.from("Chef").select().decodeList<Chef>()
                allChefs = list
                pendingChefs = list.filter { it.status.equals("Pending", ignoreCase = true) }
            } catch (e: Exception) {
                Log.e("AdminApproval", "Error refreshing chefs: ${e.localizedMessage}", e)
            } finally {
                isRefreshing = false
            }
        }
    }

    fun loadAllChefs() {
        viewModelScope.launch {
            try {
                val client = SupabaseClient.client
                val list = client.postgrest.from("Chef").select().decodeList<Chef>()
                allChefs = list
                pendingChefs = list.filter { it.status.equals("Pending", ignoreCase = true) }
            } catch (e: Exception) {
                Log.e("AdminApproval", "Error loading chefs: ${e.localizedMessage}", e)
            }
        }
    }

    fun loadPendingChefs() {
        loadAllChefs()
    }

    fun loadChefDetail(chefId: String) {
        viewModelScope.launch {
            try {
                selectedChef = SupabaseClient.client
                    .postgrest
                    .from("Chef")
                    .select {
                        filter {

                            eq("chefId", chefId)
                        }
                    }
                    .decodeSingle<Chef>()
            } catch (e: Exception) {
                Log.e(
                    "AdminApproval",
                    e.message ?: "Error loading chef"
                )
            }
        }
    }

    fun updateChefStatus(
        chefId: String,
        status: String,
        chefEmail: String = "",
        chefName: String = "",
        rejectionReason: String = ""
    ) {
        viewModelScope.launch {
            try {
                SupabaseClient.client
                    .postgrest
                    .from("Chef")
                    .update(
                        {
                            set(
                                "Status",
                                status
                            )
                        }
                    ) {
                        filter {
                            eq(
                                "chefId",
                                chefId
                            )
                        }
                    }
                loadAllChefs()

                // Dispatch email safely in viewModelScope so it survives back-navigation
                if (chefEmail.isNotBlank()) {
                    if (status.equals("Approved", ignoreCase = true)) {
                        ChefEmailNotificationService.sendChefApprovalEmail(
                            toEmail = chefEmail,
                            chefName = chefName
                        )
                    } else if (status.equals("Rejected", ignoreCase = true)) {
                        ChefEmailNotificationService.sendChefRejectionEmail(
                            toEmail = chefEmail,
                            chefName = chefName,
                            rejectionReason = rejectionReason
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(
                    "AdminApproval",
                    e.message ?: "Update failed"
                )
            }
        }
    }
}