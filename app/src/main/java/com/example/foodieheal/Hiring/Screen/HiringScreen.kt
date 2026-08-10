package com.example.foodieheal.Hiring.Screen

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.example.foodieheal.Chef.AppointmentCard
import com.example.foodieheal.Chef.ViewModel.AppointmentsUiState
import com.example.foodieheal.Chef.ViewModel.ChefPortalViewModel
import com.example.foodieheal.Hiring.ViewModel.BookmarkViewModel
import com.example.foodieheal.Hiring.ViewModel.HiringViewModel
import com.example.foodieheal.Hiring.ViewModel.UserAppointmentsUiState
import com.example.foodieheal.viewmodel.AuthViewModel
import com.example.mobileassignmentloginpart.Model.Chef
import com.example.foodieheal.R
import com.example.foodieheal.model.Appointment
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HiringScreen(
    hiringViewModel: HiringViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel(),
    bookmarkViewModel: BookmarkViewModel = viewModel(),
    onChefClick: (Chef) -> Unit,
    onAppointmentClick: (Appointment) -> Unit
) {
    val currentUser = authViewModel.currentUser
    val currentUserId = currentUser?.id.orEmpty()
    val chefs = hiringViewModel.chefList
    val isLoading = hiringViewModel.isProcessing
    val errorMessage = hiringViewModel.errorMessage

    val appointmentState by hiringViewModel.userAppointmentsState.collectAsState()

    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("Popular", "Appointment", "Bookmarks")

    // Fetch all chefs on initial screen launch
    LaunchedEffect(Unit) {
        if (chefs.isEmpty()) {
            hiringViewModel.fetchAllChefs()
        }
    }

    // Automatically fetch bookmarked chefs whenever switching to Tab 2 (Bookmarks)
    LaunchedEffect(selectedTabIndex, currentUserId) {
        if (currentUserId.isNotEmpty()) {
            when (selectedTabIndex) {
                1 -> hiringViewModel.fetchAppointmentsForCurrentUser()
                2 -> bookmarkViewModel.fetchBookmarkedChefs(currentUserId)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
                                    fontSize = 12.sp,
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
            when (selectedTabIndex) {
                // 0 -> Popular Chefs Tab
                0 -> {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else if (!errorMessage.isNullOrEmpty()) {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { hiringViewModel.fetchAllChefs() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Retry")
                            }
                        }
                    } else if (chefs.isNotEmpty()) {
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
                                    color = MaterialTheme.colorScheme.onBackground,
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

                // 1 -> Appointment Tab
                1 -> {
                    val state by hiringViewModel.userAppointmentsState.collectAsState()

                    when (val currentState = state) {
                        is UserAppointmentsUiState.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        }

                        is UserAppointmentsUiState.Error -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = currentState.message,
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(onClick = { hiringViewModel.fetchAppointmentsForCurrentUser() }) {
                                        Text("Retry")
                                    }
                                }
                            }
                        }

                        is UserAppointmentsUiState.Success -> {
                            if (currentState.appointments.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_clock),
                                            contentDescription = "No Appointments",
                                            tint = Color.Gray,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "No Appointments Found",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    item {
                                        Text(
                                            text = "My Bookings (${currentState.appointments.size})",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onBackground,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                    }

                                    items(currentState.appointments) { appointment ->

                                        val chefUser = currentState.usersMap[appointment.chefId]
                                        val chefName = chefUser?.name ?: "Chef"
                                        val chefPicture = chefUser?.profilePicUrl ?: ""

                                        UserAppointmentCard(
                                            appointment = appointment,
                                            chefName = chefName,
                                            chefPicture = chefPicture,
                                            onClick = { onAppointmentClick(appointment) }
                                        )
                                    }
                                }
                            }
                        }
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

@Composable
fun ChefHireItem(
    chef: Chef,
    onClick : () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_star), // 👈 Your star drawable
                        contentDescription = "Rating Star",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )

                    Text(
                        text = if (rating != null && rating > 0.0) "%.1f".format(rating) else "N/A",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

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

@Composable
fun UserAppointmentCard(
    appointment: Appointment,
    chefName: String,
    chefPicture: String?,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // --- 1. Chef Profile Header ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Chef Avatar
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    if (!chefPicture.isNullOrBlank()) {
                        AsyncImage(
                            model = chefPicture,
                            contentDescription = "$chefName Profile Picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                           painter = painterResource(R.drawable.ic_outline_account_circle),
                            contentDescription = "Chef Placeholder",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Chef Name & Role
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = chefName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Private Chef",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Appointment Status Badge
                AppointmentStatusBadge(status = appointment.Status)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // --- 2. Appointment Schedule & Price Details ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Schedule Info
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                           painter = painterResource(R.drawable.ic_planner),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = appointment.Date,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_clock),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (appointment.Start_Time.isNotBlank() && appointment.End_Time.isNotBlank()) {
                                "${appointment.Start_Time} - ${appointment.End_Time}"
                            } else {
                                "Time no set"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Total Price
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Total Price",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format(Locale.US, "RM %.2f", appointment.Total_Price),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

// Helper composable for Status Badge
@Composable
private fun AppointmentStatusBadge(status: String) {
    val (backgroundColor, textColor) = when (status.lowercase()) {
        "confirmed" -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
        "cancelled" -> Color(0xFFFFEBEE) to Color(0xFFC62828)
        "pending" -> Color(0xFFFFF3E0) to Color(0xFFE65100)
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor
    ) {
        Text(
            text = status.ifBlank { "Confirmed" }.replaceFirstChar { it.uppercase() },
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
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
