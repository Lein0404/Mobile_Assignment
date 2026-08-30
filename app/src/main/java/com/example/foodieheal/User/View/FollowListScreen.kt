package com.example.foodieheal.User.View

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.foodieheal.R
import com.example.foodieheal.User.Model.User
import com.example.foodieheal.User.viewModel.FollowViewModel
import com.example.foodieheal.Recipe.viewModel.RecipeViewModel
import com.example.foodieheal.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowListScreen(
    navController: NavController,
    userId: String,
    type: String, // "followers" or "following"
    followViewModel: FollowViewModel = viewModel(),
    recipeViewModel: RecipeViewModel
) {
    val users = remember { mutableStateListOf<User>() }
    var isFetchingUsers by remember { mutableStateOf(false) }
    var hasLoadedAtLeastOnce by remember { mutableStateOf(false) }

    LaunchedEffect(userId, type) {
        if (type == "followers") {
            followViewModel.fetchFollowers(userId)
        } else {
            followViewModel.fetchFollowing(userId)
        }
    }

    val followData = if (type == "followers") followViewModel.followersList else followViewModel.followingList

    LaunchedEffect(followData) {
        if (followData.isNotEmpty()) {
            val ids = if (type == "followers") {
                followData.filter { it.status == "ACCEPTED" }.mapNotNull { it.followerId }
            } else {
                followData.filter { it.status == "ACCEPTED" }.mapNotNull { it.followingId }
            }
            
            if (ids.isNotEmpty()) {
                isFetchingUsers = true
                val repo = com.example.foodieheal.Recipe.Repo.RecipeRepository()
                repo.getUsersByCustomIds(ids).onSuccess { result ->
                    users.clear()
                    users.addAll(result)
                    isFetchingUsers = false
                    hasLoadedAtLeastOnce = true
                }.onFailure {
                    isFetchingUsers = false
                    hasLoadedAtLeastOnce = true
                }
            } else {
                users.clear()
                isFetchingUsers = false
                hasLoadedAtLeastOnce = true
            }
        } else {
            users.clear()
            // If the viewmodel has finished its fetch and followData is still empty, then we stop loading
            if (!followViewModel.isLoadingFollowList) {
                isFetchingUsers = false
                hasLoadedAtLeastOnce = true
            }
        }
    }

    // 🌟 Correct Loading Logic: Show spinner until the viewmodel finishes AND the user profile fetch completes
    val showLoading = followViewModel.isLoadingFollowList || isFetchingUsers || !hasLoadedAtLeastOnce

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (type == "followers") "Followers" else "Following", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(painterResource(id = R.drawable.ic_arrowback), "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        if (showLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (users.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No users found", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(users) { user ->
                    UserListItem(user = user) {
                        navController.navigate(Screen.Profile.createRoute(user.customId))
                    }
                }
            }
        }
    }
}

@Composable
fun UserListItem(user: User, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!user.profilePicUrl.isNullOrBlank()) {
            AsyncImage(
                model = user.profilePicUrl,
                contentDescription = null,
                modifier = Modifier.size(50.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier.size(50.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = user.name?.take(1)?.uppercase() ?: "?", fontWeight = FontWeight.Bold)
            }
        }
        
        Column(modifier = Modifier.padding(start = 16.dp), verticalArrangement = Arrangement.Center) {
            Text(text = user.name ?: "Unknown User", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}
