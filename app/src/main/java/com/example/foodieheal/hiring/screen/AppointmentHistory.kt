package com.example.foodieheal.hiring.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.foodieheal.R
import com.example.foodieheal.hiring.model.UserAppointmentsUiState
import com.example.foodieheal.hiring.viewmodel.UserAppointmentViewModel
import com.example.foodieheal.hiring.model.Appointment
import com.example.foodieheal.ui.components.AppointmentStatusBadge
import com.example.foodieheal.ui.components.formatToAmPm
import kotlinx.coroutines.launch
import java.util.Locale

enum class AppointmentFilterOption(val displayNameRes: Int) {
    ALL(R.string.filter_all_statuses),
    PENDING(R.string.status_pending),
    UNPAID(R.string.status_unpaid),
    CONFIRMED(R.string.status_confirmed),
    COMPLETED(R.string.status_completed),
    CANCELLED(R.string.status_cancelled),
    REJECTED(R.string.status_rejected)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentHistoryScreen(
    viewModel: UserAppointmentViewModel,
    onBackClick: () -> Unit,
    onAppointmentClick: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var appointmentToDelete by remember { mutableStateOf<Appointment?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadDeletedAppointments(context)
        viewModel.fetchAppointmentsForCurrentUser()
    }

    val appointmentsState by viewModel.userAppointmentsState.collectAsState()
    val deletedAppointmentIds by viewModel.deletedAppointmentIds.collectAsState()

    // Active filter state
    var selectedFilter by remember { mutableStateOf(AppointmentFilterOption.ALL) }
    var showFilterSheet by remember { mutableStateOf(false) }

    val successState = appointmentsState as? UserAppointmentsUiState.Success
    val allAppointments = successState?.appointments.orEmpty()
    val usersMap = successState?.usersMap.orEmpty()

    // Filter appointments based on selection and soft deletion
    val visibleAppointments = remember(allAppointments, deletedAppointmentIds) {
        allAppointments.filter { it.AppointmentID !in deletedAppointmentIds }
    }

    val filteredAppointments = remember(visibleAppointments, selectedFilter) {
        val filtered = when (selectedFilter) {
            AppointmentFilterOption.ALL -> visibleAppointments
            AppointmentFilterOption.PENDING -> visibleAppointments.filter { it.Status.equals("pending", ignoreCase = true) }
            AppointmentFilterOption.UNPAID -> visibleAppointments.filter { it.Status.equals("unpaid", ignoreCase = true) }
            AppointmentFilterOption.CONFIRMED -> visibleAppointments.filter { it.Status.equals("confirmed", ignoreCase = true) }
            AppointmentFilterOption.COMPLETED -> visibleAppointments.filter { it.Status.equals("completed", ignoreCase = true) }
            AppointmentFilterOption.CANCELLED -> visibleAppointments.filter { it.Status.equals("cancelled", ignoreCase = true) }
            AppointmentFilterOption.REJECTED -> visibleAppointments.filter { it.Status.equals("rejected", ignoreCase = true) }
        }
        filtered.sortedByDescending { it.created_at }
    }

    val isRefreshing = appointmentsState is UserAppointmentsUiState.Loading

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.appointment_history_title),
                            fontWeight = FontWeight.Bold
                        )
                        if (selectedFilter != AppointmentFilterOption.ALL) {
                            Text(
                                text = stringResource(R.string.filter_prefix, stringResource(selectedFilter.displayNameRes)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrowback),
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showFilterSheet = true }) {
                        BadgedBox(
                            badge = {
                                if (selectedFilter != AppointmentFilterOption.ALL) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(8.dp)
                                    )
                                }
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.filter),
                                contentDescription = stringResource(R.string.filter_by_status),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.fetchAppointmentsForCurrentUser() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (val state = appointmentsState) {
                is UserAppointmentsUiState.Loading -> {

                    if (allAppointments.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }

                is UserAppointmentsUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = state.message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Button(onClick = { viewModel.fetchAppointmentsForCurrentUser() }) {
                                Text(stringResource(R.string.retry_action))
                            }
                        }
                    }
                }

                is UserAppointmentsUiState.Success -> {
                    if (filteredAppointments.isEmpty()) {
                        EmptyHistoryState(filterName = stringResource(selectedFilter.displayNameRes))
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                items = filteredAppointments,
                                key = { it.AppointmentID ?: it.hashCode().toString() }
                            ) { appointment ->
                                val chefUser = usersMap[appointment.chefId]

                                AppointmentHistoryCard(
                                    appointment = appointment,
                                    chefName = chefUser?.name ?: stringResource(R.string.private_chef_name),
                                    chefPicture = chefUser?.profilePicUrl,
                                    onClick = {
                                        val id = appointment.AppointmentID.orEmpty()
                                        if (id.isNotEmpty()) {
                                            onAppointmentClick(id)
                                        }
                                    },
                                    onDeleteClick = {
                                        appointmentToDelete = appointment
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Filter Bottom Sheet Dialog
    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.filter_by_status),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                AppointmentFilterOption.entries.forEach { option ->
                    val isSelected = selectedFilter == option

                    Surface(
                        onClick = {
                            selectedFilter = option
                            showFilterSheet = false
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(option.displayNameRes),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                            if (isSelected) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_check),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (appointmentToDelete != null) {
        val target = appointmentToDelete!!
        AlertDialog(
            onDismissRequest = { appointmentToDelete = null },
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_delete),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.remove_from_history),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(stringResource(R.string.confirm_remove_history_msg, target.Status.lowercase()))
            },
            confirmButton = {
                val removedMessage = stringResource(R.string.appointment_removed_snackbar)
                val undoLabel = stringResource(R.string.undo)
                Button(
                    onClick = {
                        val id = target.AppointmentID.orEmpty()
                        if (id.isNotEmpty()) {
                            viewModel.softDeleteAppointment(context, id)
                            coroutineScope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = removedMessage,
                                    actionLabel = undoLabel,
                                    duration = SnackbarDuration.Short
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    viewModel.restoreAppointment(context, id)
                                }
                            }
                        }
                        appointmentToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { appointmentToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
fun AppointmentHistoryCard(
    appointment: Appointment,
    chefName: String,
    chefPicture: String?,
    onClick: () -> Unit,
    onDeleteClick: (() -> Unit)? = null
) {
    val statusLower = appointment.Status.lowercase().trim()
    val isDeletable = statusLower == "completed" || statusLower == "rejected" || statusLower == "cancelled" || statusLower == "cancel"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row: Chef Info + Status & Delete Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    AsyncImage(
                        model = chefPicture,
                        contentDescription = stringResource(R.string.chef_picture),
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentScale = ContentScale.Crop,
                        error = painterResource(R.drawable.ic_outline_account_circle),
                        placeholder = painterResource(R.drawable.ic_outline_account_circle)
                    )
                    Column {
                        Text(
                            text = chefName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_star),
                                contentDescription = stringResource(R.string.label_rating),
                                modifier = Modifier.size(14.dp),
                                tint = Color(0xFFFFC107)
                            )
                            Text(
                                text = stringResource(
                                    R.string.rating_format_prefix,
                                    appointment.rating ?: stringResource(R.string.not_available)
                                ),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AppointmentStatusBadge(status = appointment.Status)

                    if (isDeletable && onDeleteClick != null) {
                        IconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_delete),
                                contentDescription = stringResource(R.string.delete_action),
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter =  painterResource(R.drawable.ic_calendar),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = appointment.Date.orEmpty(),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter =  painterResource(R.drawable.ic_clock),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    val startTimeAmPm = formatToAmPm(appointment.Start_Time)
                    val endTimeAmPm = formatToAmPm(appointment.End_Time)
                    Text(
                        text = if (startTimeAmPm.isNotBlank() && endTimeAmPm.isNotBlank()) {
                            stringResource(R.string.time_range_format, startTimeAmPm, endTimeAmPm)
                        } else {
                            stringResource(R.string.time_not_set)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.label_total_price),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = String.format(Locale.US, "RM %.2f", appointment.Total_Price ?: 0.0),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun EmptyHistoryState(filterName: String) {
    val allStatusesLabel = stringResource(R.string.filter_all_statuses)
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_calendar),
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            Text(
                text = if (filterName == allStatusesLabel) {
                    stringResource(R.string.no_appointments_found)
                } else {
                    stringResource(R.string.no_appointments_found_filter, filterName)
                },
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}