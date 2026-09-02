package com.example.foodieheal.User.viewModel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodieheal.User.Model.Follow
import com.example.foodieheal.User.Repo.FollowRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import com.example.foodieheal.MainActivity
import com.example.foodieheal.User.Repo.UserRepository
import com.example.foodieheal.meal_planner.viewModel.NetworkMonitor

class FollowViewModel(private val repository: FollowRepository = FollowRepository()) : ViewModel() {

    var followStatus by mutableStateOf<String?>(null) // null, PENDING, ACCEPTED
        private set

    var followingList by mutableStateOf<List<Follow>>(emptyList())
        private set

    var followersList by mutableStateOf<List<Follow>>(emptyList())
        private set

    var followerCount by mutableIntStateOf(0)
        private set

    var followingCount by mutableIntStateOf(0)
        private set

    var isLoadingFollowCounts by mutableStateOf(false)
        private set

    var isLoadingFollowList by mutableStateOf(false)
        private set

    var isNetworkAvailable by mutableStateOf(true)
        private set

    private val networkMonitor = MainActivity.appContext?.let { NetworkMonitor(it) }

    private val _followEvents = MutableSharedFlow<FollowEvent>()
    val followEvents = _followEvents.asSharedFlow()

    init {
        observeNetworkStatus()
    }

    private fun observeNetworkStatus() {
        viewModelScope.launch {
            networkMonitor?.isConnected?.collect { connected ->
                isNetworkAvailable = connected
            }
        }
    }

    fun fetchFollowStatus(myId: String, targetId: String) {
        viewModelScope.launch {
            followStatus = repository.getFollowStatus(myId, targetId)
        }
    }

    fun toggleFollow(myId: String, targetId: String) {
        if (!isNetworkAvailable) {
            viewModelScope.launch { _followEvents.emit(FollowEvent.NoInternet) }
            return
        }
        viewModelScope.launch {
            when (followStatus) {
                null -> {
                    repository.sendFollowRequest(myId, targetId)
                    followStatus = "PENDING"
                    _followEvents.emit(FollowEvent.RequestSent)
                }
                "PENDING" -> {
                    repository.cancelFollowRequest(myId, targetId)
                    followStatus = null
                    _followEvents.emit(FollowEvent.RequestCancelled)
                }
                "ACCEPTED" -> {
                    repository.unfollowUser(myId, targetId)
                    followStatus = null
                    _followEvents.emit(FollowEvent.Unfollowed)
                }
            }
        }
    }

    fun acceptRequest(myId: String, followerId: String) {
        if (!isNetworkAvailable) {
            viewModelScope.launch { _followEvents.emit(FollowEvent.NoInternet) }
            return
        }
        viewModelScope.launch {
            repository.acceptFollowRequest(followerId, myId)
            
            // Refresh counts after accepting
            val userRepo = UserRepository()
            userRepo.getUserByCustomId(myId) // This is just to trigger any DB logic if needed, 
            
            fetchFollowers(myId)
            _followEvents.emit(FollowEvent.RequestAccepted)
        }
    }

    fun rejectRequest(myId: String, followerId: String) {
        if (!isNetworkAvailable) {
            viewModelScope.launch { _followEvents.emit(FollowEvent.NoInternet) }
            return
        }
        viewModelScope.launch {
            repository.cancelFollowRequest(followerId, myId)
            fetchFollowers(myId)
            _followEvents.emit(FollowEvent.RequestRejected)
        }
    }

    fun fetchFollowing(userId: String) {
        viewModelScope.launch {
            try {
                isLoadingFollowList = true
                followingList = repository.getFollowing(userId)
                followingCount = followingList.filter { it.status == "ACCEPTED" }.size
            } catch (e: Exception) {
                // If it fails, we keep existing list or emit error
                _followEvents.emit(FollowEvent.Error)
            } finally {
                isLoadingFollowList = false
            }
        }
    }

    fun fetchFollowers(userId: String) {
        viewModelScope.launch {
            try {
                isLoadingFollowList = true
                followersList = repository.getFollowers(userId)
                followerCount = followersList.filter { it.status == "ACCEPTED" }.size
            } catch (e: Exception) {
                _followEvents.emit(FollowEvent.Error)
            } finally {
                isLoadingFollowList = false
            }
        }
    }

    fun fetchFollowCounts(userId: String) {
        viewModelScope.launch {
            try {
                isLoadingFollowCounts = true
                // Fetch followers
                repository.getFollowers(userId).let { list ->
                    followersList = list
                    followerCount = list.filter { it.status == "ACCEPTED" }.size
                }
                // Fetch following
                repository.getFollowing(userId).let { list ->
                    followingList = list
                    followingCount = list.filter { it.status == "ACCEPTED" }.size
                }
            } catch (e: Exception) {
                // Fail silently for counts to not disrupt UI
            } finally {
                isLoadingFollowCounts = false
            }
        }
    }

    sealed class FollowEvent {
        object RequestSent : FollowEvent()
        object RequestCancelled : FollowEvent()
        object Unfollowed : FollowEvent()
        object RequestAccepted : FollowEvent()
        object RequestRejected : FollowEvent()
        object NoInternet : FollowEvent()
        object Error : FollowEvent()
    }
}
