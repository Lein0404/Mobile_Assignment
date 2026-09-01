package com.example.foodieheal.Admin

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import com.example.foodieheal.Admin.ViewModel.AdminIngredientRequestViewModel
import com.example.foodieheal.Admin.ViewModel.AdminViewModelFactory
import com.example.foodieheal.R
import com.example.foodieheal.Cloudinary.CloudinaryUploadViewModel
import com.example.foodieheal.ingredients.shared.IngredientFormBody
import com.example.foodieheal.navigation.Screen
import com.example.foodieheal.ui.components.PrimaryButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminIngredientRequestFormScreen(
    navController: NavController,
    requestId: String,
    cloudinaryViewModel: CloudinaryUploadViewModel = viewModel()
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val viewModel: AdminIngredientRequestViewModel = viewModel(
        factory = AdminViewModelFactory(application)
    )
    
    val formState by viewModel.formState.collectAsState()
    val availableUnits by viewModel.availableUnits.collectAsState()
    val requestDetail by viewModel.requestDetail.collectAsState()
    val actionUiState by viewModel.uiState.collectAsState()
    val isLoading = actionUiState.isLoading
    val scope = rememberCoroutineScope()

    RequestConflictDialog(
        isDeleted = actionUiState.isDeletedByUser,
        isProcessed = actionUiState.isAlreadyProcessed,
        onDeletedConfirm = {
            // Return to the list screen correctly
            navController.popBackStack(Screen.AdminChefScreen.route, false)
        },
        onProcessedConfirm = {
            // Return to the detail screen to see the updated status
            navController.popBackStack()
        }
    )

    val errorMessage = formState.errorMessage

    // Android 13+ Notification Permission Launcher
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val isGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!isGranted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(requestId) {
        viewModel.populateFormForReview(requestId)
    }

    LaunchedEffect(formState.imageUrl) {
        formState.imageUrl?.let {
            cloudinaryViewModel.setExistingImageUrl(it)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.admin_review_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrowback),
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
        ) {
            if (!actionUiState.isNetworkAvailable) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            painter = painterResource(R.drawable.wifi_off),
                            contentDescription = null,
                            modifier = Modifier.size(dimensionResource(R.dimen.icon_xlarge_size)),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.padding_md)))
                        Text(
                            text = stringResource(R.string.admin_add_offline_message),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.padding_xxl))
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(dimensionResource(id = R.dimen.padding_l)),
                ) {
                    IngredientFormBody(
                        cloudinaryViewModel = cloudinaryViewModel,
                        ingredientName = formState.ingredientName,
                        category = formState.category,
                        description = formState.description,
                        unitRows = formState.unitRows,
                        availableUnits = availableUnits,
                        nameError = formState.nameError,
                        nameErrorArg = formState.nameErrorArg,
                        categoryError = formState.categoryError,
                        descriptionError = formState.descriptionError,
                        unitRowsError = formState.unitRowsError,
                        categoryPlaceholder = stringResource(R.string.admin_add_category_placeholder),
                        onNameChange = { viewModel.updateFormName(it) },
                        onCategoryChange = { viewModel.updateFormCategory(it) },
                        onDescriptionChange = { viewModel.updateFormDescription(it) },
                        onUnitRowUpdate = { index, unit, cal -> viewModel.updateUnitRow(index, unit, cal) },
                        onUnitRowRemove = { viewModel.removeUnitRow(it) },
                        onAddUnitRow = { viewModel.addUnitRow() },
                    ) {
                        // Screen-specific bottom content
                        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.padding_xl)))
                        val validateErrorToastMsg = stringResource(R.string.admin_review_error_validate)
                        PrimaryButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                if (viewModel.validateForm()) {
                                    viewModel.onShowApproveDialog(true)
                                } else {
                                    Toast.makeText(context, validateErrorToastMsg, Toast.LENGTH_SHORT).show()
                                }
                            },
                            textID = R.string.admin_approve_ingredient_request,
                            enabled = !formState.isSubmitting && requestDetail != null && !isLoading
                        )
                        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.padding_xxl)))
                    }
                }
            }

            if (formState.isSubmitting || (isLoading && requestDetail == null)) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        .clickable(enabled = false) {}, // Prevent interaction
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }

    val requestApprovedMsg = stringResource(R.string.admin_request_approved)
    if (actionUiState.showApproveDialog) {
        ApproveRequestDialog(
            adminNote = actionUiState.adminNote,
            onAdminNoteChange = { viewModel.onAdminNoteChange(it) },
            onDismiss = { viewModel.onShowApproveDialog(false) },
            onConfirm = { adminNote ->
                viewModel.onShowApproveDialog(false)
                scope.launch {
                    val imageUrl = if (cloudinaryViewModel.uiState.value.selectedImageUri != null) {
                        cloudinaryViewModel.uploadImage(context)
                    } else {
                        cloudinaryViewModel.uiState.value.uploadedImageUrl.ifEmpty { null }
                    }

                    viewModel.approveRequest(
                        imageUrl = imageUrl,
                        adminNote = if (adminNote.isBlank()) null else adminNote,
                        onComplete = {
                            Toast.makeText(context, requestApprovedMsg, Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        }
                    )
                }
            }
        )
    }

    if (actionUiState.showErrorDialog && errorMessage != null) {
        AlertDialog(
            onDismissRequest = { 
                viewModel.onShowErrorDialog(false)
                viewModel.clearError()
            },
            title = { Text(
                text = stringResource(R.string.admin_approval_failed_dialog_title),
                fontWeight = FontWeight.Bold)
            },
            text = { 
                Text(
                    text = stringResource(errorMessage),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = { 
                    viewModel.onShowErrorDialog(false)
                    viewModel.clearError()
                }) {
                    Text(
                        text = stringResource(R.string.ok),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        )
    }
}

@Composable
fun ApproveRequestDialog(
    adminNote: String,
    onAdminNoteChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.admin_approve_request_dialog_title), fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.padding_l))
            ) {
                Text(
                    text = stringResource(R.string.admin_approve_request_dialog_warning),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                Text(stringResource(R.string.admin_approve_request_dialog_note))
                OutlinedTextField(
                    value = adminNote,
                    onValueChange = onAdminNoteChange,
                    placeholder = { Text(stringResource(R.string.admin_approve_request_dialog_placeholder)) },
                    modifier = Modifier.fillMaxWidth().height(120.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(adminNote)
            }) {
                Text(
                    text = stringResource(R.string.approve),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.cancel),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
            text = { Text(stringResource(R.string.admin_detail_conflict_processed_title)) },
            confirmButton = {
                TextButton(onClick = onProcessedConfirm) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }
}
