package com.example.foodieheal.Hiring.ViewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodieheal.model.Appointment
import com.example.foodieheal.model.ReviewWithUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ReviewsUiState {
    object Loading : ReviewsUiState
    data class Success(val reviews: List<ReviewWithUser>) : ReviewsUiState
    data class Error(val message: String) : ReviewsUiState
}

class ReviewViewModel(
    private val repository: HiringRepository = HiringRepository()
) : ViewModel() {

    private val _reviewsState = MutableStateFlow<ReviewsUiState>(ReviewsUiState.Loading)
    val reviewsState: StateFlow<ReviewsUiState> = _reviewsState.asStateFlow()

    fun fetchChefReviews(chefId: String) {
        if (chefId.isBlank()) return

        viewModelScope.launch {
            _reviewsState.value = ReviewsUiState.Loading
            try {
                // Use the new fetchChefReviews method added to HiringRepository
                val reviews = repository.fetchChefReviews(chefId)
                _reviewsState.value = ReviewsUiState.Success(reviews)
            } catch (e: Exception) {
                _reviewsState.value = ReviewsUiState.Error(
                    e.localizedMessage ?: "Failed to load reviews"
                )
            }
        }
    }

    fun submitReview(
        appointmentId: String,
        rating: Int,
        comment: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                repository.submitReview(appointmentId, rating, comment)
                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Failed to submit review")
            }
        }
    }
}