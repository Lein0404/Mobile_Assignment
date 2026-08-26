package com.example.foodieheal.hiring.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodieheal.R
import com.example.foodieheal.hiring.viewmodel.BookmarkViewModel
import com.example.foodieheal.hiring.viewmodel.ChefListViewModel
import com.example.foodieheal.hiring.viewmodel.UserAppointmentViewModel
import com.example.foodieheal.hiring.model.Appointment
import com.example.foodieheal.User.viewModel.AuthViewModel
import com.example.foodieheal.Chef.model.Chef

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

    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf(
        stringResource(R.string.tab_popular),
        stringResource(R.string.tab_appointment),
        stringResource(R.string.tab_bookmarks)
    )

    val context = androidx.compose.ui.platform.LocalContext.current

    // Fetch all chefs on initial screen launch & ensure cleanup service is active
    LaunchedEffect(Unit) {
        try {
            val cleanupIntent = android.content.Intent(context, com.example.foodieheal.hiring.local.HiringCacheCleanupService::class.java)
            context.startService(cleanupIntent)
        } catch (e: Exception) {
            android.util.Log.e("HiringScreen", "Failed to start HiringCacheCleanupService", e)
        }
        chefListViewModel.fetchAllChefs()
    }

    // Automatically fetch data whenever switching tabs
    LaunchedEffect(selectedTabIndex, currentUserId) {
        if (currentUserId.isNotEmpty()) {
            when (selectedTabIndex) {
                0 -> chefListViewModel.fetchAllChefs()
                1 -> userAppointmentViewModel.fetchAppointmentsForCurrentUser()
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
                                    fontSize = 13.sp,
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
                        text = stringResource(R.string.hiring_offline_mode),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Tab Content Area (Modular Tab Composables)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            when (selectedTabIndex) {
                0 -> PopularChefsTabContent(
                    chefs = chefs,
                    isLoading = isLoading,
                    errorMessage = errorMessage,
                    onRetry = { chefListViewModel.fetchAllChefs(forceRefresh = true) },
                    onRefresh = { chefListViewModel.fetchAllChefs(forceRefresh = true) },
                    onChefClick = onChefClick
                )
                1 -> UserAppointmentsTabContent(
                    viewModel = userAppointmentViewModel,
                    onAppointmentClick = onAppointmentClick,
                    onRefresh = { userAppointmentViewModel.fetchAppointmentsForCurrentUser(forceRefresh = true) }
                )
                2 -> BookmarkedChefsTabContent(
                    viewModel = bookmarkViewModel,
                    onChefClick = onChefClick,
                    onRefresh = {
                        if (currentUserId.isNotEmpty()) {
                            bookmarkViewModel.fetchBookmarkedChefs(currentUserId, forceRefresh = true)
                        }
                    }
                )
            }
        }
    }
}
