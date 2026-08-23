package com.example.foodieheal.Admin

import android.app.Application
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.example.foodieheal.Cloudinary.CloudinaryUploadScreen
import com.example.foodieheal.Cloudinary.CloudinaryUploadViewModel
import com.example.foodieheal.ingredients.model.IngredientCategory
import com.example.foodieheal.ingredients.view.UnitRow
import com.example.foodieheal.navigation.Screen
import com.example.foodieheal.ui.components.CommonInputField
import com.example.foodieheal.ui.components.PrimaryButton
import com.kanyidev.searchable_dropdown.LargeSearchableDropdownMenu

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
    var showErrorDialog by remember { mutableStateOf(false) }
    var showApproveDialog by remember { mutableStateOf(false) }

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            showErrorDialog = true
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
        containerColor = Color(0xFFF8F8F8)
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
                            modifier = Modifier.size(70.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.admin_add_offline_message),
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                        .imePadding(),
                ) {
                    // 1. Cloudinary Upload
                    CloudinaryUploadScreen(viewModel = cloudinaryViewModel)
                    Spacer(Modifier.height(16.dp))

                    // 2. Ingredient Name
                    CommonInputField(
                        value = formState.ingredientName,
                        onValueChange = { viewModel.updateFormName(it) },
                        textId = R.string.ingredient_name,
                        modifier = Modifier.fillMaxWidth(),
                        isError = formState.nameError != null
                    )
                    formState.nameError?.let { resId ->
                        Text(
                            text = stringResource(id = resId),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    Spacer(Modifier.height(16.dp))

                    // 3. Category
                    LargeSearchableDropdownMenu(
                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                        title = stringResource(R.string.category),
                        fieldLabelTextStyle = MaterialTheme.typography.bodyLarge,
                        selectedOption = formState.category,
                        onItemSelected = { viewModel.updateFormCategory(it) },
                        selectedItemToString = { it.categoryName },
                        placeholder = stringResource(R.string.admin_add_category_placeholder),
                        placeholderTextStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                        ),
                        textStyle = MaterialTheme.typography.bodyLarge,
                        options = IngredientCategory.entries,
                        drawItem = { item, selected, itemEnabled, onClick ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = itemEnabled, onClick = onClick)
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Text(
                                    text = item.categoryName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (selected) MaterialTheme.colorScheme.primary else Color.Black
                                )
                            }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            errorContainerColor = MaterialTheme.colorScheme.background,
                        ),
                        isError = formState.categoryError != null
                    )
                    formState.categoryError?.let { resId ->
                        Text(
                            text = stringResource(id = resId),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    Spacer(Modifier.height(16.dp))

                    // 4. Description
                    CommonInputField(
                        value = formState.description,
                        onValueChange = { viewModel.updateFormDescription(it) },
                        textId = R.string.description,
                        modifier = Modifier.height(200.dp).fillMaxWidth(),
                        singleLine = false,
                        maxLines = 8,
                        isError = formState.descriptionError != null
                    )
                    formState.descriptionError?.let { resId ->
                        Text(
                            text = stringResource(id = resId),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    Spacer(Modifier.height(16.dp))

                    // 5. Calorie Information
                    Text(
                        text = stringResource(R.string.calorie_information),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    formState.unitRowsError?.let { resId ->
                        Text(
                            text = stringResource(id = resId),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))

                    if (availableUnits.isNotEmpty()) {
                        formState.unitRows.forEachIndexed { index, row ->
                            UnitRow(
                                index = index,
                                selectedUnit = row.selectedUnit,
                                calories = row.calories,
                                availableUnits = availableUnits,
                                onUpdate = { unit, cal -> viewModel.updateUnitRow(index, unit, cal) },
                                onRemove = if (formState.unitRows.size > 1) { { viewModel.removeUnitRow(index) } } else null,
                                unitError = row.unitError,
                                caloriesError = row.caloriesError
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(
                        onClick = { viewModel.addUnitRow() },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ){
                            Icon(
                                painter = painterResource(R.drawable.ic_outline_add),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(stringResource(R.string.ingredient_form_add_unit))
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    val validateErrorToastMsg = stringResource(R.string.admin_review_error_validate)
                    PrimaryButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            if (viewModel.validateForm()) {
                                showApproveDialog = true
                            } else {
                                Toast.makeText(context, validateErrorToastMsg, Toast.LENGTH_SHORT).show()
                            }
                        },
                        textID = R.string.admin_approve_ingredient_request,
                        enabled = !formState.isSubmitting && requestDetail != null && !isLoading
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }

            if (formState.isSubmitting || (isLoading && requestDetail == null)) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.1f))
                        .clickable(enabled = false) {}, // Prevent interaction
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }

    val requestApprovedMsg = stringResource(R.string.admin_request_approved)
    if (showApproveDialog) {
        ApproveRequestDialog(
            onDismiss = { showApproveDialog = false },
            onConfirm = { adminNote ->
                showApproveDialog = false
                scope.launch {
                    val imageUrl = if (cloudinaryViewModel.uiState.value.selectedImageUri != null) {
                        cloudinaryViewModel.uploadImage(context)
                    } else {
                        cloudinaryViewModel.uiState.value.uploadedImageUrl.ifEmpty { formState.imageUrl }
                    }

                    viewModel.approveRequest(
                        imageUrl = imageUrl,
                        adminNote = if (adminNote.isBlank()) null else adminNote,
                        onComplete = {
                            Toast.makeText(context, requestApprovedMsg, Toast.LENGTH_SHORT).show()

                            navController.navigate(Screen.AdminChefScreen.createRoute(tab = 1)) {
                                popUpTo(Screen.AdminChefScreen.route) { this.inclusive = true }
                            }
                        }
                    )
                }
            }
        )
    }

    if (showErrorDialog && errorMessage != null) {
        AlertDialog(
            onDismissRequest = { 
                showErrorDialog = false
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
                    showErrorDialog = false
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
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var adminNote by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.admin_approve_request_dialog_title), fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.admin_approve_request_dialog_warning),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Text(stringResource(R.string.admin_approve_request_dialog_note))
                OutlinedTextField(
                    value = adminNote,
                    onValueChange = { adminNote = it },
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
                    color = Color.Gray
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
