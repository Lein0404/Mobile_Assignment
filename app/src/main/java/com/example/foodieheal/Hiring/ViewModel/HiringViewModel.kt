package com.example.foodieheal.Hiring.ViewModel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodieheal.SupabaseClient.client
import com.example.mobileassignmentloginpart.Model.Chef
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch

class HiringViewModel : ViewModel() {

    var chefList by mutableStateOf<List<Chef>>(emptyList())
        private set

    var isProcessing by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var selectedChef by mutableStateOf<Chef?>(null)
        private set

    fun selectChef(chef: Chef) {
        selectedChef = chef
    }

    fun clearSelectedChef() {
        selectedChef = null
    }

    fun fetchAllChefs() {
        viewModelScope.launch {
            isProcessing = true
            errorMessage = null

            try {
                // Fetch approved chefs from Supabase
                val chefs = client.postgrest["Chef"]
                    .select {
                        filter {
                            ilike("Status", "approved")
                        }
                    }
                    .decodeList<Chef>()

                Log.d("SupabaseChef", "Successfully loaded ${chefs.size} chefs.")
                chefList = chefs
            } catch (e: Exception) {
                Log.e("SupabaseChef", "Error decoding chefs list", e)
                errorMessage = e.message ?: "Failed to fetch chef profiles"
            } finally {
                isProcessing = false
            }
        }
    }
}