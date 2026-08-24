package com.example.foodieheal.hiring.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.foodieheal.R
import com.example.foodieheal.hiring.model.UserAppointmentsUiState
import com.example.foodieheal.hiring.viewmodel.BookmarkViewModel
import com.example.foodieheal.hiring.viewmodel.ChefListViewModel
import com.example.foodieheal.hiring.viewmodel.UserAppointmentViewModel
import com.example.foodieheal.model.Appointment
import com.example.foodieheal.ui.components.AppointmentStatusBadge
import com.example.foodieheal.viewmodel.AuthViewModel
import com.example.mobileassignmentloginpart.Model.Chef
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HiringScreen(
    chefListViewModel: ChefListViewModel = viewModel(),
    userAppointmentViewModel: UserAppointmentViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel(),
    bookmarkViewModel: BookmarkViewModel = viewModel(),
    onChefClick: (Chef) -> Unit,
    onAppointmentClick: (Appointment) -> Unit
) {
    val currentUser = authViewModel.currentUser
    val currentUserId = currentUser?.id.orEmpty()
    val chefs by chefListViewModel.chefList.collectAsStateWithLifecycle()
    val isLoading by chefListViewModel.isProcessing.collectAsStateWithLifecycle()
    val errorMessage by chefListViewModel.errorMessage.collectAsStateWithLifecycle()
    val isNetworkAvailable by chefListViewModel.isNetworkAvailable.collectAsStateWithLifecycle()

    var selectedSortOrder by rememberSaveable { mutableStateOf(RateSortOrder.NONE) }
    var selectedStateFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedAgeRangeFilter by rememberSaveable { mutableStateOf<AgeRange?>(null) }
    var showFilterBottomSheet by remember { mutableStateOf(false) }

    val availableStates = remember(chefs) {
        chefs.mapNotNull { it.state }.filter { it.isNotBlank() }.distinct().sorted()
    }

    // Filter Chef List
    val filteredChefs = remember(chefs, selectedSortOrder, selectedStateFilter, selectedAgeRangeFilter) {
        chefs.filter { chef ->
            // Filter by State
            val matchesState = selectedStateFilter == null || chef.state.equals(selectedStateFilter, ignoreCase = true)

            // Filter by Age
            val chefAge = chef.age ?: 0
            val matchesAge = selectedAgeRangeFilter == null || when (selectedAgeRangeFilter) {
                AgeRange.YOUNG -> chefAge in 18..30
                AgeRange.MID -> chefAge in 31..45
                AgeRange.SENIOR -> chefAge > 45
                else -> true
            }

            matchesState && matchesAge
        }.let { list ->
            // Sort by Rate
            when (selectedSortOrder) {
                RateSortOrder.ASCENDING -> list.sortedBy { it.averagerating ?: 0.0 }
                RateSortOrder.DESCENDING -> list.sortedByDescending { it.averagerating ?: 0.0 }
                RateSortOrder.NONE -> list
            }
        }
    }

    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf(
        stringResource(R.string.tab_popular),
        stringResource(R.string.tab_appointment),
        stringResource(R.string.tab_bookmarks)
    )

    // Fetch all chefs on initial screen launch
    LaunchedEffect(Unit) {
        if (chefs.isEmpty()) {
            chefListViewModel.fetchAllChefs()
        }
    }

    // Automatically fetch data whenever switching tabs
    LaunchedEffect(selectedTabIndex, currentUserId) {
        if (currentUserId.isNotEmpty()) {
            when (selectedTabIndex) {
                1 -> userAppointmentViewModel.fetchAppointmentsForCurrentUser()
                2 -> bookmarkViewModel.fetchBookmarkedChefs(currentUserId)
            }
        }
    }

    val isFilterActive = selectedSortOrder != RateSortOrder.NONE ||
            selectedStateFilter != null ||
            selectedAgeRangeFilter != null

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
                    text = stringResource(R.string.title_hiring),
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 12.dp)
                )

                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    indicator = { tabPositions ->
                        if (selectedTabIndex < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                height = 3.dp,
                                color = MaterialTheme.colorScheme.onPrimary
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
                                    color = if (selectedTabIndex == index) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                                    }
                                )
                            }
                        )
                    }
                }
            }
        }

        // Offline Mode Banner
        if (!isNetworkAvailable) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.wifi_off),
                        contentDescription = stringResource(R.string.desc_no_network),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Offline Mode: Showing cached data",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
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
                            Text(text = errorMessage.orEmpty(), color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { chefListViewModel.fetchAllChefs() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text(stringResource(R.string.btn_retry))
                            }
                        }
                    } else if (chefs.isNotEmpty()) {
                        if (filteredChefs.isEmpty()) {
                            Column(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No chefs match the selected filters",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 14.sp
                                )
                                TextButton(
                                    onClick = {
                                        selectedSortOrder = RateSortOrder.NONE
                                        selectedStateFilter = null
                                        selectedAgeRangeFilter = null
                                    }
                                ) {
                                    Text("Reset Filters")
                                }
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Header item with Title and Filter Icon Button
                                item(span = { GridItemSpan(2) }) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = stringResource(R.string.header_chef),
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )

                                        IconButton(onClick = { showFilterBottomSheet = true }) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.filter),
                                                contentDescription = "Filter Chefs",
                                                tint = if (isFilterActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }

                                items(filteredChefs, key = { it.chefId.ifEmpty { it.id } }) { chef ->
                                    ChefHireItem(
                                        chef = chef,
                                        onClick = { onChefClick(chef) }
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.empty_no_chefs_found),
                            modifier = Modifier.align(Alignment.Center),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 1 -> Appointment Tab
                1 -> {
                    val state by userAppointmentViewModel.userAppointmentsState.collectAsState()

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
                                    Button(onClick = { userAppointmentViewModel.fetchAppointmentsForCurrentUser() }) {
                                        Text(stringResource(R.string.btn_retry))
                                    }
                                }
                            }
                        }

                        is UserAppointmentsUiState.Success -> {
                            val activeAppointments = remember(currentState.appointments) {
                                currentState.appointments.filter { appointment ->
                                    val status = appointment.Status.orEmpty().lowercase(Locale.US)
                                    status == "pending" || status == "confirmed" || status == "unpaid"
                                }
                            }

                            if (activeAppointments.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_clock),
                                            contentDescription = stringResource(R.string.no_appointments),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = stringResource(R.string.empty_no_appointments_found),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                            text = stringResource(R.string.header_my_bookings, activeAppointments.size),
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onBackground,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                    }

                                    items(
                                        items = activeAppointments,
                                        key = { it.AppointmentID ?: it.hashCode().toString() }
                                    ) { appointment ->
                                        val chefUser = currentState.usersMap[appointment.chefId]
                                        val chefName = chefUser?.name ?: stringResource(R.string.default_chef_name)
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
                                    text = stringResource(R.string.header_bookmarked_chefs, bookmarkedChefs.size),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }

                            items(
                                items = bookmarkedChefs,
                                key = { it.chefId.ifEmpty { it.id } }
                            ) { chef ->
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
                                contentDescription = stringResource(R.string.no_bookmarks),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.empty_no_bookmarked_chefs),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.empty_bookmarked_chefs_sub),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
    if (showFilterBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterBottomSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            FilterBottomSheetContent(
                sortOrder = selectedSortOrder,
                onSortChange = { selectedSortOrder = it },
                availableStates = availableStates,
                selectedState = selectedStateFilter,
                onStateChange = { selectedStateFilter = it },
                selectedAgeRange = selectedAgeRangeFilter,
                onAgeRangeChange = { selectedAgeRangeFilter = it },
                onResetFilters = {
                    selectedSortOrder = RateSortOrder.NONE
                    selectedStateFilter = null
                    selectedAgeRangeFilter = null
                },
                onApply = { showFilterBottomSheet = false }
            )
        }
    }
}

@Composable
fun ChefHireItem(
    chef: Chef,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                    .background(MaterialTheme.colorScheme.tertiary),
                contentAlignment = Alignment.Center
            ) {
                if (chef.profilePictureUrl.isNullOrEmpty()) {
                    Text(
                        text = chef.name?.take(1)?.uppercase() ?: stringResource(R.string.default_initial_chef),
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onTertiary,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    AsyncImage(
                        model = chef.profilePictureUrl,
                        contentDescription = stringResource(R.string.profile_picture),
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
                text = chef.name.ifEmpty { stringResource(R.string.unknown_chef) },
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Experience & Location
            val expText = stringResource(R.string.experience_years_short, chef.experience ?: 0)
            val locationText = chef.state?.takeIf { it.isNotBlank() }.orEmpty()

            Text(
                text = expText,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = locationText,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        painter = painterResource(R.drawable.ic_star),
                        contentDescription = stringResource(R.string.rating_star),
                        tint = Color(0xFFFFB300), // Gold Star Color
                        modifier = Modifier.size(14.dp)
                    )

                    Text(
                        text = if (rating != null && rating > 0.0) String.format(Locale.US, "%.1f", rating) else stringResource(R.string.not_available),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                chef.Pricing?.let { price ->
                    Text(
                        text = stringResource(R.string.rate_per_hour, price.toInt()),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
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
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
                        .background(MaterialTheme.colorScheme.tertiary),
                    contentAlignment = Alignment.Center
                ) {
                    if (!chefPicture.isNullOrBlank()) {
                        AsyncImage(
                            model = chefPicture,
                            contentDescription = stringResource(R.string.chef_profile_picture_format, chefName),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.ic_outline_account_circle),
                            contentDescription = stringResource(R.string.chef_placeholder),
                            tint = MaterialTheme.colorScheme.onTertiary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = chefName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.role_private_chef),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Appointment Status Badge
                AppointmentStatusBadge(status = appointment.Status)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

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
                                stringResource(R.string.time_range_format, appointment.Start_Time, appointment.End_Time)
                            } else {
                                stringResource(R.string.time_not_set)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Total Price
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.label_total_price),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format(Locale.US, stringResource(R.string.price_currency_format), appointment.Total_Price),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

enum class RateSortOrder { NONE, ASCENDING, DESCENDING }
enum class AgeRange(val label: String) {
    YOUNG("18 - 30"),
    MID("31 - 45"),
    SENIOR("45+")
}

@Composable
fun FilterBottomSheetContent(
    sortOrder: RateSortOrder,
    onSortChange: (RateSortOrder) -> Unit,
    availableStates: List<String>,
    selectedState: String?,
    onStateChange: (String?) -> Unit,
    selectedAgeRange: AgeRange?,
    onAgeRangeChange: (AgeRange?) -> Unit,
    onResetFilters: () -> Unit,
    onApply: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Filter Chefs",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onResetFilters) {
                Text("Reset All")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sort by Rate Section
        Text(
            text = "Rating",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = sortOrder == RateSortOrder.ASCENDING,
                onClick = {
                    onSortChange(if (sortOrder == RateSortOrder.ASCENDING) RateSortOrder.NONE else RateSortOrder.ASCENDING)
                },
                label = { Text("Low to High") }
            )
            FilterChip(
                selected = sortOrder == RateSortOrder.DESCENDING,
                onClick = {
                    onSortChange(if (sortOrder == RateSortOrder.DESCENDING) RateSortOrder.NONE else RateSortOrder.DESCENDING)
                },
                label = { Text("High to Low") }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Age Filter Section
        Text(
            text = "Chef Age",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AgeRange.entries.forEach { range ->
                FilterChip(
                    selected = selectedAgeRange == range,
                    onClick = {
                        onAgeRangeChange(if (selectedAgeRange == range) null else range)
                    },
                    label = { Text(range.label) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // State Filter Section
        if (availableStates.isNotEmpty()) {
            Text(
                text = "State",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                availableStates.forEach { state ->
                    FilterChip(
                        selected = selectedState.equals(state, ignoreCase = true),
                        onClick = {
                            onStateChange(if (selectedState.equals(state, ignoreCase = true)) null else state)
                        },
                        label = { Text(state) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Apply Button
        Button(
            onClick = onApply,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Apply Filters")
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
