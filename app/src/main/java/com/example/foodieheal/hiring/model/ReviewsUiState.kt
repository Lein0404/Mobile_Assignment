package com.example.foodieheal.hiring.model


sealed interface ReviewsUiState {
    data object Loading : ReviewsUiState
    data class Success(val reviews: List<ReviewWithUser>) : ReviewsUiState
    data class Error(val message: String) : ReviewsUiState
}
