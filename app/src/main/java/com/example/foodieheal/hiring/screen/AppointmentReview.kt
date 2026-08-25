package com.example.foodieheal.hiring.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.foodieheal.R
import com.example.foodieheal.hiring.viewmodel.AppointmentBookingViewModel
import com.example.foodieheal.User.viewModel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentReviewScreen(
    viewModel: AppointmentBookingViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel(),
    onBackClick: () -> Unit,
    onFinalConfirm: () -> Unit
) {
    val context = LocalContext.current

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedChef by viewModel.selectedChef.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val isNetworkAvailable by viewModel.isNetworkAvailable.collectAsStateWithLifecycle()

    var showConfirmationDialog by remember { mutableStateOf(false) }

    val chefId = selectedChef?.let { it.chefId.ifEmpty { it.id } }.orEmpty()
    val chefPicture = selectedChef?.profilePictureUrl.orEmpty()
    val chefName = selectedChef?.name ?: stringResource(R.string.default_selected_chef_name)
    val appointmentTime = uiState.appointmentTime.ifBlank { stringResource(R.string.not_selected) }
    val address = uiState.address.ifBlank { stringResource(R.string.not_provided) }

    val location = listOf(uiState.postcode, uiState.state)
        .filter { it.isNotBlank() }
        .joinToString(", ")
        .ifBlank { stringResource(R.string.not_provided) }

    val servingSize = uiState.servingSize.ifBlank { "0" }
    val healthPref = uiState.healthPreference.ifBlank { stringResource(R.string.none_selected) }
    val description = uiState.description.ifBlank { stringResource(R.string.none_selected) }
    val hourlyRate = selectedChef?.Pricing ?: 0.0

    val currentUserId = authViewModel.currentUser?.id.orEmpty()

    val selectedDateString = selectedDate.toString()
    val timeRange = uiState.appointmentTime.split(" - ")
    val startTime = timeRange.getOrNull(0).orEmpty()
    val endTime = timeRange.getOrNull(1).orEmpty()

    val totalPrice = viewModel.calculateTotalPrice(hourlyRate, uiState.appointmentTime)

    val processingToastMsg = stringResource(R.string.toast_processing_booking)
    val successToastMsg = stringResource(R.string.toast_booking_success)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.review_booking),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrowback),
                            contentDescription = stringResource(R.string.back_button),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
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
                            text = "Offline Mode: Internet required to confirm booking",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (chefPicture.isNotBlank()) {
                            AsyncImage(
                                model = chefPicture,
                                contentDescription = stringResource(R.string.profile_picture),
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.ic_outline_account_circle),
                                contentDescription = stringResource(R.string.default_profile),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.label_selected_chef),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = chefName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.currency_hourly_rate_format, hourlyRate),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            ReviewSectionCard(title = stringResource(R.string.section_date_and_time)) {
                ReviewDetailItem(
                    painter = painterResource(R.drawable.ic_planner),
                    label = stringResource(R.string.label_date),
                    value = selectedDateString
                )
                ReviewDetailItem(
                    painter = painterResource(R.drawable.ic_clock),
                    label = stringResource(R.string.label_time_slot),
                    value = "$startTime - $endTime"
                )
            }

            ReviewSectionCard(title = stringResource(R.string.section_location_and_event)) {
                ReviewDetailItem(
                    painter = painterResource(R.drawable.location),
                    label = stringResource(R.string.label_address),
                    value = "$address, $location"
                )
                ReviewDetailItem(
                    painter = painterResource(R.drawable.serving_size),
                    label = stringResource(R.string.label_serving_size),
                    value = stringResource(R.string.serving_pax_format, servingSize)
                )
                ReviewDetailItem(
                    painter = painterResource(R.drawable.health_preference),
                    label = stringResource(R.string.label_health_preference),
                    value = healthPref
                )
                if (description.isNotBlank() && description != stringResource(R.string.none_selected)) {
                    ReviewDetailItem(
                        painter = painterResource(id = R.drawable.note),
                        label = stringResource(R.string.label_notes_special_requests),
                        value = description
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.section_payment_breakdown),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    PriceSummaryRow(
                        label = stringResource(R.string.label_hourly_rate),
                        value = stringResource(R.string.currency_rm_format, hourlyRate)
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.label_total_price),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.currency_rm_format, totalPrice),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Confirm & Book Button
            Button(
                onClick = { showConfirmationDialog = true },
                enabled = isNetworkAvailable && !uiState.isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = stringResource(R.string.confirm_and_book),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

    // Confirmation Dialog
    if (showConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmationDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.confirm_booking),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.confirm_booking_message),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    BookingDetailRow(
                        label = stringResource(R.string.label_chef),
                        value = chefName
                    )
                    BookingDetailRow(
                        label = stringResource(R.string.label_date),
                        value = selectedDateString
                    )
                    BookingDetailRow(
                        label = stringResource(R.string.label_time),
                        value = appointmentTime
                    )
                    BookingDetailRow(
                        label = stringResource(R.string.label_total_price),
                        value = stringResource(R.string.currency_rm_format, totalPrice),
                        isHighlight = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmationDialog = false
                        Toast.makeText(context, processingToastMsg, Toast.LENGTH_SHORT).show()

                        viewModel.createAppointment(
                            userId = currentUserId,
                            chefId = chefId,
                            selectedDate = selectedDateString,
                            startTime = startTime,
                            endTime = endTime,
                            totalPrice = totalPrice,
                            onSuccess = {
                                Toast.makeText(context, successToastMsg, Toast.LENGTH_LONG).show()
                                onFinalConfirm()
                            },
                            onError = { errorMsg ->
                                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        text = stringResource(R.string.confirm_and_pay),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showConfirmationDialog = false },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
private fun ReviewSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            content()
        }
    }
}

@Composable
private fun ReviewDetailItem(
    painter: Painter,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            painter = painter,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(20.dp)
                .padding(top = 2.dp)
        )
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun PriceSummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun BookingDetailRow(
    label: String,
    value: String,
    isHighlight: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Medium,
            color = if (isHighlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}
