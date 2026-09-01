package com.example.foodieheal.User.View

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.example.foodieheal.meal_planner.screen.OfflinePlaceholder
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.foodieheal.R
import com.example.foodieheal.User.Model.Follow
import com.example.foodieheal.User.Model.User
import com.example.foodieheal.User.viewModel.AuthViewModel
import com.example.foodieheal.User.viewModel.FollowViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowRequestsScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    followViewModel: FollowViewModel = viewModel()
) {
    val user = authViewModel.currentUser
    val requests = followViewModel.followersList.filter { it.status == "PENDING" }
    val requestUsers = remember { mutableStateMapOf<String, User>() }
    var isFetchingRequesterProfiles by remember { mutableStateOf(false) }
    var isInitialLoadComplete by remember { mutableStateOf(false) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        followViewModel.followEvents.collect { event ->
            val message = when(event) {
                FollowViewModel.FollowEvent.RequestAccepted -> context.getString(R.string.follow_request_accepted)
                FollowViewModel.FollowEvent.RequestRejected -> context.getString(R.string.follow_request_rejected)
                FollowViewModel.FollowEvent.NoInternet -> context.getString(R.string.desc_connect_internet_follow)
                else -> ""
            }
            if (message.isNotEmpty()) {
                snackbarHostState.showSnackbar(message)
            }
        }
    }

    LaunchedEffect(user?.customId) {
        user?.customId?.let { followViewModel.fetchFollowers(it) }
    }

    LaunchedEffect(requests) {
        if (requests.isNotEmpty()) {
            val ids = requests.mapNotNull { it.followerId }.distinct()
            if (ids.isNotEmpty()) {
                isFetchingRequesterProfiles = true
                val repo = com.example.foodieheal.Recipe.Repo.RecipeRepository()
                repo.getUsersByCustomIds(ids).onSuccess { result ->
                    result.forEach { u ->
                        u.customId?.let { requestUsers[it] = u }
                    }
                    isFetchingRequesterProfiles = false
                    isInitialLoadComplete = true
                }.onFailure {
                    isFetchingRequesterProfiles = false
                    isInitialLoadComplete = true
                }
            } else {
                isFetchingRequesterProfiles = false
                isInitialLoadComplete = true
            }
        } else {
            // If requests are empty, we check if the VM is still loading the relationship list
            if (!followViewModel.isLoadingFollowList) {
                isInitialLoadComplete = true
            }
        }
    }

    val showLoading = followViewModel.isLoadingFollowList || isFetchingRequesterProfiles || !isInitialLoadComplete

    Scaffold(
        snackbarHost = { 
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.profile_follow_requests), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(painterResource(id = R.drawable.ic_arrowback), stringResource(R.string.back_button))
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
        if (!followViewModel.isNetworkAvailable) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                OfflinePlaceholder(message = stringResource(R.string.desc_connect_internet_follow_request))
            }
        } else if (showLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (requests.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.follow_requests_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(requests) { request ->
                    val requester = request.followerId?.let { requestUsers[it] }
                    RequestItem(
                        request = request,
                        requester = requester,
                        onAccept = {
                            user?.customId?.let { myId ->
                                request.followerId?.let { fid ->
                                    followViewModel.acceptRequest(myId, fid)
                                }
                            }
                        },
                        onReject = {
                            user?.customId?.let { myId ->
                                request.followerId?.let { fid ->
                                    followViewModel.rejectRequest(myId, fid)
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun RequestItem(
    request: Follow,
    requester: User?,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top // 🌟 Align to top for multi-line content
        ) {
            if (!requester?.profilePicUrl.isNullOrBlank()) {
                AsyncImage(
                    model = requester.profilePicUrl,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = requester?.name?.take(1)?.uppercase() ?: request.followerId?.take(1)?.uppercase() ?: "?",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text(
                    text = requester?.name ?: "${stringResource(R.string.user)} ${request.followerId}",
                    fontWeight = FontWeight.Bold, 
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.follow_requests_wants_to_follow), 
                    fontSize = 12.sp, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onAccept,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        modifier = Modifier.height(32.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(stringResource(R.string.follow_requests_accept), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))

                    TextButton(
                        onClick = onReject,
                        modifier = Modifier.height(32.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(stringResource(R.string.follow_requests_reject), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
