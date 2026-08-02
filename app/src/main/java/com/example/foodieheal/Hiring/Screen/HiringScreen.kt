package com.example.foodieheal.Hiring.Screen

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.foodieheal.Hiring.ViewModel.BookmarkViewModel
import com.example.foodieheal.Hiring.ViewModel.HiringViewModel
import com.example.foodieheal.ViewModel.AuthViewModel
import com.example.mobileassignmentloginpart.Model.Chef
import com.example.foodieheal.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HiringScreen(
    HiringViewModel: HiringViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel(),
    bookmarkViewModel: BookmarkViewModel = viewModel(), // Added BookmarkViewModel instance
    onChefClick: (Chef) -> Unit
) {
    val currentUser = authViewModel.currentUser
    val currentUserId = currentUser?.id.orEmpty()
    val chefs = HiringViewModel.chefList
    val isLoading = HiringViewModel.isProcessing
    val errorMessage = HiringViewModel.errorMessage

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Popular", "Appointment", "Bookmarks")

    // Fetch all chefs on initial screen launch
    LaunchedEffect(Unit) {
        if (chefs.isEmpty()) {
            HiringViewModel.fetchAllChefs()
        }
    }

    // Automatically fetch bookmarked chefs whenever switching to Tab 2 (Bookmarks)
    LaunchedEffect(selectedTabIndex, currentUserId) {
        if (selectedTabIndex == 2 && currentUserId.isNotEmpty()) {
            bookmarkViewModel.fetchBookmarkedChefs(currentUserId)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F8F8))
    ) {
        // Top Bar & Tabs Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
        ) {
            Column(
                modifier = Modifier.statusBarsPadding()
            ) {
                Text(
                    text = "Hiring",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 12.dp)
                )

                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color.Transparent,
                    contentColor = Color.White,
                    indicator = { tabPositions ->
                        if (selectedTabIndex < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                height = 3.dp,
                                color = Color.White
                            )
                        }
                    },
                    divider = {}
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 15.sp,
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedTabIndex == index) Color.White else Color.White.copy(alpha = 0.8f)
                                )
                            }
                        )
                    }
                }
            }
        }

        // Tab Content Area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                !errorMessage.isNullOrEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { HiringViewModel.fetchAllChefs() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Retry")
                        }
                    }
                }
                else -> {
                    when (selectedTabIndex) {
                        // 0 -> Popular Chefs Tab
                        0 -> {
                            if (chefs.isNotEmpty()) {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    item(span = { GridItemSpan(2) }) {
                                        Text(
                                            text = "Chef",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                    }

                                    items(chefs, key = { it.chefId.ifEmpty { it.id } }) { chef ->

                                        val chefId = chef.chefId.ifEmpty { chef.id }
                                        val isBookmarked = bookmarkViewModel.isChefBookmarked(chefId)
                                        ChefHireItem(
                                            chef = chef,
                                            onClick = { onChefClick(chef) }
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = "No chef profiles found.",
                                    modifier = Modifier.align(Alignment.Center),
                                    color = Color.Gray
                                )
                            }
                        }

                        // 1 -> Appointment Tab (Placeholder)
                        1 -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No Appointments Yet",
                                    color = Color.Gray,
                                    fontSize = 16.sp
                                )
                            }
                        }

                        // 2 -> Bookmarks Tab
                        2 -> {
                            val bookmarkedChefs = bookmarkViewModel.bookmarkedChefsList

                            if (bookmarkedChefs.isNotEmpty()) {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    item(span = { GridItemSpan(2) }) {
                                        Text(
                                            text = "Bookmarked Chefs (${bookmarkedChefs.size})",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                    }

                                    items(
                                        items = bookmarkedChefs,
                                        key = { it.chefId.ifEmpty { it.id } }
                                    ) { chef ->
                                        val chefId = chef.chefId.ifEmpty { chef.id }
                                        val isBookmarked = bookmarkViewModel.isChefBookmarked(chefId)
                                        ChefHireItem(
                                            chef = chef,
                                            onClick = { onChefClick(chef) }
                                        )
                                    }
                                }
                            } else {
                                Column(
                                    modifier = Modifier.align(Alignment.Center),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.bookmark),
                                        contentDescription = "No Bookmarks",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "No Bookmarked Chefs",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.Gray
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Chefs you bookmark will appear here.",
                                        fontSize = 12.sp,
                                        color = Color.Gray.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChefHireItem(
    chef: Chef,
    onClick : () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
            .clickable{onClick()}
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Image
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                if (chef.profilePictureUrl.isNullOrEmpty()) {
                    Text(
                        text = chef.name?.take(1)?.uppercase() ?: "C",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    AsyncImage(
                        model = chef.profilePictureUrl,
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Chef Name
            Text(
                text = chef.name.ifEmpty { "Unknown Chef" },
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Experience & Location
            val expText = "${chef.experience ?: 0} yrs exp"
            val locationText = chef.state?.takeIf { it.isNotBlank() }?.let { "$it" } ?: ""
            Text(
                text = expText,
                fontSize = 11.sp,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = locationText,
                fontSize = 11.sp,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Rating and Pricing Row at the bottom
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val rating = chef.averagerating
                val ratingDisplay = if (rating != null && rating > 0.0) "★ %.1f".format(rating) else "★ N/A"

                Text(
                    text = ratingDisplay,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                chef.Pricing?.let { price ->
                    Text(
                        text = "$${price.toInt()}/hr",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )
                }
            }
        }
    }
}

private val sampleChef = Chef(
    chefId = "chef_123",
    id = "1",
    name = "Gordon Ramsay",
    email = "gordon@kitchen.com",
    phoneNumber = "0234567 8900",
    description = "Passionate executive chef with over 15 years of culinary experience in high-end fine dining and custom home catering.",
    Pricing = 120.0,
    experience = 15,
    averagerating = 4.9,
    gender = "Male",
    state = "California",
    postcode = "90210",
    address = "123 Culinary Way",
    status = "approved",
    profilePictureUrl = "null",
    age = 50
)

//@Preview(showBackground = true)
//@Composable
//fun HiringChefDetailsPreview() {
//    MaterialTheme {
//        HiringChefDetails(
//            chef = sampleChef,
//            onBackClick = {},
//            onBookClick = {}
//        )
//    }
//}
