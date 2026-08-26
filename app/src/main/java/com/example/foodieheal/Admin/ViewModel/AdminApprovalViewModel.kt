package com.example.foodieheal.Admin.ViewModel1

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodieheal.SupabaseClient
import com.example.foodieheal.Chef.model.Chef
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch

class AdminApprovalViewModel : ViewModel() {

    var pendingChefs by mutableStateOf<List<Chef>>(emptyList())
        private set

    var selectedChef by mutableStateOf<Chef?>(null)
        private set

    fun loadPendingChefs() {
        viewModelScope.launch {

            val client = SupabaseClient.client

            pendingChefs = client
                .postgrest
                .from("Chef")
                .select {
                    filter {
                        eq("Status", "Pending")
                    }
                }
                .decodeList()
        }
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
        status: String
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
            } catch (e: Exception) {

                Log.e(
                    "AdminApproval",
                    e.message ?: "Update failed"
                )
            }
        }
    }
}