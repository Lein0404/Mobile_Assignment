package com.example.foodieheal.Hiring.Screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import coil.compose.AsyncImage
import com.example.foodieheal.Hiring.ViewModel.BookmarkViewModel
import com.example.foodieheal.R
import com.example.foodieheal.viewmodel.AuthViewModel
import com.example.mobileassignmentloginpart.Model.Chef

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HiringChefDetails(
    chef: Chef,
    userId: String,
    viewModel: BookmarkViewModel = viewModel(),
    onBackClick: () -> Unit,
    onHireClick: (Chef) -> Unit
) {

    val AuthviewModel: AuthViewModel = viewModel()
    val user = AuthviewModel.currentUser
    val currentChefId = chef.chefId.ifEmpty { chef.id }
    val isBookmarked = viewModel.isChefBookmarked(currentChefId)

    LaunchedEffect(userId) {
        if (userId.isNotBlank()) {
            viewModel.fetchBookmarkedChefs(userId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Chef Profile",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrowback),
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
            )
        },
        bottomBar = {
            Surface(
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.background
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Rate", fontSize = 12.sp, color = Color.Gray)
                        Text(
                            text = chef.Pricing?.let { "$${it.toInt()}/hr" } ?: "N/A",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Button(
                        onClick = { onHireClick(chef) },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("Hire This Chef", fontSize = 16.sp)
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8F8F8))
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Profile Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Profile Image can use in every module like this
                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (chef.profilePictureUrl.isNullOrEmpty()) {
                            Text(
                                text = chef.name?.take(1)?.uppercase() ?: "C",
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.align(Alignment.Center)
                                                    .size(150.dp)
                                                    .clip(CircleShape)
                            )
                        } else {
                            AsyncImage(
                                model = chef.profilePictureUrl,
                                contentDescription = "Profile Picture",
                                modifier = Modifier.align(Alignment.Center)
                                                    .size(150.dp)
                                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }

                        //Bookmark button
                        IconButton(
                            onClick = {
                                viewModel.onBookmarkToggled(
                                    userId = userId,
                                    chefId = currentChefId
                                )
                            },
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Icon(
                                painter = painterResource(
                                    if (isBookmarked) R.drawable.bookmark_fill
                                    else R.drawable.bookmark
                                ),
                                contentDescription = "Bookmark Chef",
                                tint = if (isBookmarked) Color(0xFFE65127) else Color.Gray,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = chef.name.ifBlank { "Unknown Chef" },
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )

                    val location = listOfNotNull(chef.state, chef.postcode)
                        .filter { it.isNotBlank() }
                        .joinToString(", ")

                    if (location.isNotEmpty()) {
                        Text(
                            text = location,
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DetailChip(
                            label = "Rating",
                            value = chef.averagerating?.let { "★ %.1f".format(it) } ?: "★ N/A"
                        )
                        DetailChip(
                            label = "Experience",
                            value = "${chef.experience ?: 0} Years"
                        )
                        DetailChip(
                            label = "Gender",
                            value = chef.gender?.capitalized() ?: "N/A"
                        )
                    }
                }
            }

            // About Section
            if (!chef.description.isNullOrBlank()) {
                DetailSectionCard(title = "About Chef") {
                    Text(
                        text = chef.description,
                        fontSize = 14.sp,
                        color = Color.DarkGray,
                        lineHeight = 20.sp
                    )
                }
            }

            // Contact Info Section
            DetailSectionCard(title = "Contact Information") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoRow(label = "Phone", value = chef.phoneNumber ?: "N/A")
                    InfoRow(label = "Email", value = chef.email.ifBlank { "N/A" })
                    InfoRow(label = "Address", value = chef.address?.ifBlank { "N/A" } ?: "N/A")
                }
            }
        }
    }
}

@Composable
private fun DetailChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
        Text(text = label, fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable
private fun DetailSectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            content()
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 13.sp, color = Color.Gray)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.Black)
    }
}

private fun String.capitalized(): String = replaceFirstChar { it.uppercase() }