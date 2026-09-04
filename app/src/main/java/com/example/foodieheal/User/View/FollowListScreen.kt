package com.example.foodieheal.User.View

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.foodieheal.R
import com.example.foodieheal.Recipe.Repo.RecipeRepository
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

    val snackbarHostState = remember { SnackbarHostState() }

    val errorMessage = stringResource(R.string.error_failed_refresh_list)

    LaunchedEffect(Unit) {
        followViewModel.followEvents.collect { event ->
            if (event is FollowViewModel.FollowEvent.Error) {
                snackbarHostState.showSnackbar(errorMessage)
            }
        }
    }

    LaunchedEffect(userId, type) {
        if (type == "followers") {
            followViewModel.fetchFollowers(userId)
        } else {
            followViewModel.fetchFollowing(userId)
        }
    }

    val followData = if (type == "followers") followViewModel.followersList else followViewModel.followingList

    LaunchedEffect(followData, followViewModel.isLoadingFollowList) {
        if (!followViewModel.isLoadingFollowList) {
            if (followData.isNotEmpty()) {
                val ids = if (type == "followers") {
                    followData.filter { it.status == "ACCEPTED" }.mapNotNull { it.followerId }
                } else {
                    followData.filter { it.status == "ACCEPTED" }.mapNotNull { it.followingId }
                }
                
                if (ids.isNotEmpty()) {
                    isFetchingUsers = true
                    val repo = RecipeRepository()
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
                isFetchingUsers = false
                hasLoadedAtLeastOnce = true
            }
        }
    }

    // Show spinner until the viewmodel finishes AND the user profile fetch completes
    val showLoading = followViewModel.isLoadingFollowList || isFetchingUsers || !hasLoadedAtLeastOnce

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (type == "followers") stringResource(R.string.profile_followers) else stringResource(R.string.profile_following), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { 
                        // Safety check to prevent spam-clicks from causing navigation crashes or "blank screens"
                        val currentRoute = navController.currentDestination?.route
                        if (currentRoute?.contains(Screen.FollowList.route.substringBefore("/{")) == true) {
                            navController.popBackStack() 
                        }
                    }) {
                        Icon(painterResource(id = R.drawable.ic_arrowback), stringResource(R.string.back))
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
                Text(stringResource(R.string.no_users_found), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(users) { user ->
                    UserListItem(user = user) {
                        navController.navigate(Screen.Profile.createRoute(user.id ?: ""))
                    }
                }
            }
        }
    }
}

@Composable
fun UserListItem(user: User, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(50.dp),
                shape = CircleShape,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                if (!user.profilePicUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = user.profilePicUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = user.name?.take(1)?.uppercase() ?: "?", fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            Column(modifier = Modifier.padding(start = 16.dp), verticalArrangement = Arrangement.Center) {
                Text(text = user.name ?: stringResource(R.string.unknown_user), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}
