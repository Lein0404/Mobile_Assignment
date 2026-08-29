package com.example.foodieheal.User.View

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.example.foodieheal.meal_planner.screen.OfflinePlaceholder
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.foodieheal.R
import com.example.foodieheal.User.Model.Follow
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
                title = { Text("Follow Requests", fontWeight = FontWeight.Bold) },
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
        if (!followViewModel.isNetworkAvailable) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                OfflinePlaceholder(message = stringResource(R.string.desc_connect_internet_follow_request))
            }
        } else if (requests.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No pending follow requests", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(requests) { request ->
                    RequestItem(
                        request = request,
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
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Placeholder for avatar (in a real app, you'd fetch the user info)
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = request.followerId?.take(1)?.uppercase() ?: "?", fontWeight = FontWeight.Bold)
            }
            
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text(text = "User ${request.followerId}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(text = "wants to follow you", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = onReject,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Reject", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onAccept,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("Accept", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
