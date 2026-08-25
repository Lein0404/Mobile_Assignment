package com.example.foodieheal.Admin

import android.app.Application
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil3.compose.SubcomposeAsyncImage
import com.example.foodieheal.Admin.ViewModel.AdminIngredientRequestViewModel
import com.example.foodieheal.Admin.ViewModel.AdminViewModelFactory
import com.example.foodieheal.R
import com.example.foodieheal.model.Status
import com.example.foodieheal.navigation.Screen
import com.example.foodieheal.ui.components.ImagePlaceholder
import com.example.foodieheal.ui.components.PrimaryButton
import com.example.foodieheal.ui.components.StatusBadge
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminIngredientDetailScreen(
    navController: NavController,
    requestId: String
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val viewModel: AdminIngredientRequestViewModel = viewModel(
        factory = AdminViewModelFactory(application)
    )
    val requestItem by viewModel.requestDetail.collectAsState()
    val actionUiState by viewModel.uiState.collectAsState()
    val isLoading = actionUiState.isLoading
    val isRefreshing = actionUiState.isRefreshing
    
    RequestConflictDialog(
        isDeleted = actionUiState.isDeletedByUser,
        isProcessed = actionUiState.isAlreadyProcessed,
        onDeletedConfirm = {
            navController.popBackStack()
        },
        onProcessedConfirm = {
            // Already on detail screen, just refresh or close dialog
            viewModel.fetchRequestDetail(requestId)
        }
    )

    LaunchedEffect(requestId) {
        viewModel.fetchRequestDetail(requestId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.admin_detail_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() })
                    {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrowback),
                            contentDescription = stringResource(R.string.back)
                        )
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
    ) { paddingValues ->
        if (isLoading && !isRefreshing) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.refreshRequestDetail(requestId) },
                modifier = Modifier.fillMaxSize().padding(paddingValues)
            ) {
                requestItem?.let { info ->
                    val request = info.request
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                                .verticalScroll(rememberScrollState())
                        ) {
                            // 1. Image
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(250.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!request.ingredientImage.isNullOrEmpty()) {
                                    SubcomposeAsyncImage(
                                        model = request.ingredientImage,
                                        contentDescription = request.ingredientName,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                        loading = { CircularProgressIndicator(modifier = Modifier.scale(0.2f)) },
                                        error = { ImagePlaceholder() }
                                    )
                                } else {
                                    ImagePlaceholder()
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(dimensionResource(id = R.dimen.padding_l))
                            ) {
                                // 2. Title & Category
                                Text(
                                    text = request.ingredientName,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = request.ingredientCategory?.categoryName ?: stringResource(R.string.none),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.padding_xsm)))
                                StatusBadge(status = request.requestStatus)

                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = dimensionResource(id = R.dimen.padding_l)),
                                    thickness = 1.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                // 3. Description
                                Text(stringResource(R.string.ingredient_detail_description_label), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.padding_smd)))
                                Text(
                                    text = request.ingredientDesc.ifEmpty { stringResource(R.string.admin_detail_no_description) },
                                    color = MaterialTheme.colorScheme.onBackground,
                                )

                                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.padding_xl)))

                                // 4. Calorie Information
                                Text(stringResource(R.string.calorie_information), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.padding_smd)))
                                Text(
                                    text = info.calorieSummary.ifEmpty { stringResource(R.string.admin_detail_no_calorie_info) },
                                    color = MaterialTheme.colorScheme.onBackground
                                )

                                // 5. Rejected Reason (if applicable)
                                if (request.requestStatus == Status.REJECTED) {
                                    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.padding_xl)))
                                    Text(stringResource(R.string.ingredient_detail_rejected_reason_label), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.padding_smd)))
                                    Text(
                                        text = request.rejectedReason ?: stringResource(R.string.ingredient_detail_unspecified),
                                        color = MaterialTheme.colorScheme.onBackground,
                                    )
                                }

                                if (request.requestStatus == Status.APPROVED && !request.adminNote.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.padding_xl)))
                                    Text(stringResource(R.string.ingredient_detail_admin_notes_label), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.padding_smd)))
                                    Text(
                                        text = request.adminNote,
                                        color = MaterialTheme.colorScheme.onBackground,
                                    )
                                }

                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = dimensionResource(id = R.dimen.padding_l)),
                                    thickness = 1.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                // 6. Request Information
                                Text(stringResource(R.string.admin_detail_request_info_header), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.padding_smd)))
                                RequestInfoRow(
                                    label = stringResource(R.string.admin_detail_user_id),
                                    value = info.requesterCustomId
                                )
                                RequestInfoRow(
                                    label = stringResource(R.string.admin_detail_user_name),
                                    value = info.requesterName
                                )
                                RequestInfoRow(
                                    label = stringResource(R.string.admin_detail_created_date),
                                    value = formatDisplayDateTime(request.datetimeCreated)
                                )

                                Spacer(modifier = Modifier.height(120.dp))
                            }
                        }

                        // 7. Admin Action Buttons (for Pending)
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = dimensionResource(id = R.dimen.padding_l)),
                            verticalArrangement = Arrangement.Bottom, // push the button down to the bottom
                            horizontalAlignment = Alignment.CenterHorizontally
                        ){
                            if (request.requestStatus == Status.PENDING && actionUiState.isNetworkAvailable) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(dimensionResource(id = R.dimen.padding_l)),
                                    horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.padding_l))
                                ) {
                                    Button(
                                        onClick = { viewModel.onShowRejectDialog(true) },
                                        modifier = Modifier.weight(0.45f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.error
                                        ),
                                        shape = RoundedCornerShape(dimensionResource(id = R.dimen.corner_radius_sm))
                                    ) {
                                        Text(
                                            text = stringResource(R.string.reject),
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onError
                                        )
                                    }
                                    PrimaryButton(
                                        modifier = Modifier.weight(0.55f),
                                        onClick = {
                                            navController.navigate(Screen.AdminIngredientReview.createRoute(requestId))
                                        },
                                        textID = R.string.review_approve
                                    )
                                }
                            }
                        }
                    }
                } ?:
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.admin_detail_request_not_found))
                }
            }
        }
    }

    if (actionUiState.showRejectDialog) {
        RejectRequestDialog(
            reason = actionUiState.rejectReason,
            onReasonChange = { viewModel.onRejectReasonChange(it) },
            error = actionUiState.rejectReasonError,
            onDismiss = { viewModel.onShowRejectDialog(false) },
            onConfirm = { reason ->
                viewModel.rejectRequest(requestId, reason) {
                    Toast.makeText(context, R.string.admin_detail_toast_rejected, Toast.LENGTH_SHORT).show()
                    viewModel.onShowRejectDialog(false)
                    navController.popBackStack()
                }
            }
        )
    }
}

fun formatDisplayDateTime(isoDateTime: String?): String {
    if (isoDateTime.isNullOrEmpty()) return "N/A"
    return try {
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
        val zonedDateTime = ZonedDateTime.parse(isoDateTime)
            .withZoneSameInstant(ZoneId.of("UTC+8"))
        "${zonedDateTime.format(formatter)}"
    } catch (_: Exception) {
        isoDateTime
    }
}

@Composable
fun RejectRequestDialog(
    reason: String,
    onReasonChange: (String) -> Unit,
    error: Int?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reject Request", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(stringResource(R.string.admin_detail_reject_prompt))
                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.padding_l)))
                OutlinedTextField(
                    value = reason,
                    onValueChange = onReasonChange,
                    placeholder = { Text(stringResource(R.string.admin_detail_reject_placeholder)) },
                    modifier = Modifier.fillMaxWidth().height(240.dp),
                    isError = error != null,
                    supportingText = {
                        error?.let { errorResId ->
                            Text(
                                text = stringResource(errorResId),
                                color = MaterialTheme.colorScheme.error)
                        }
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(reason)
            }) {
                Text(stringResource(R.string.admin_detail_reject_title), color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

@Composable
private fun RequestConflictDialog(
    isDeleted: Boolean,
    isProcessed: Boolean,
    onDeletedConfirm: () -> Unit,
    onProcessedConfirm: () -> Unit
) {
    if (isDeleted) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text(stringResource(R.string.admin_detail_conflict_deleted_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.admin_detail_conflict_deleted_text)) },
            confirmButton = {
                TextButton(onClick = onDeletedConfirm) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    } else if (isProcessed) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text(stringResource(R.string.admin_detail_conflict_processed_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.admin_detail_conflict_processed_text)) },
            confirmButton = {
                TextButton(onClick = onProcessedConfirm) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }
}

@Composable
private fun RequestInfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_xxsm)),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.6f)
        )
    }
}