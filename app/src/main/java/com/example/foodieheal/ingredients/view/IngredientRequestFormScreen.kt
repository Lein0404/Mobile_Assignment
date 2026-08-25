package com.example.foodieheal.ingredients.view

import android.app.Application
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import com.example.foodieheal.R
import com.example.foodieheal.Cloudinary.CloudinaryUploadViewModel
import com.example.foodieheal.ingredients.shared.IngredientFormBody
import com.example.foodieheal.ingredients.viewModel.IngredientRequestViewModel
import com.example.foodieheal.ingredients.viewModel.IngredientsViewModelFactory
import com.example.foodieheal.navigation.Screen
import com.example.foodieheal.ui.components.PrimaryButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngredientRequestFormScreen(
    navController: NavController,
    requestId: String? = null,
    cloudinaryViewModel: CloudinaryUploadViewModel = viewModel()
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    
    val viewModel: IngredientRequestViewModel = viewModel(
        factory = IngredientsViewModelFactory(application)
    )
    
    val uiState by viewModel.uiState.collectAsState()
    val formState by viewModel.formState.collectAsState()
    val availableUnits by viewModel.availableUnits.collectAsState()
    
    val scope = rememberCoroutineScope()

    if (formState.isStatusConflict) {
        AlertDialog(
            onDismissRequest = { },
            title = {
                Text(
                    text = stringResource(R.string.ingredient_form_processed_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = { Text(stringResource(R.string.ingredient_form_processed_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        navController.popBackStack()
                    }
                ) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }

    LaunchedEffect(requestId) {
        if (requestId != null) {
            viewModel.populateFormForEdit(requestId)
        } else {
            viewModel.clearForm()
        }
    }

    // TODO: duplicated LaunchedEffect in AdminIngredientRequestFormScreen
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
                        text = if (requestId == null) stringResource(R.string.ingredient_form_title_add) else stringResource(R.string.ingredient_form_title_update),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.popBackStack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
                .consumeWindowInsets(paddingValues)  // prevent adding navigation bar padding + keyboard padding
        ) {
            // Offline gate: show message instead of form
            if (!uiState.isNetworkAvailable) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.wifi_off),
                            contentDescription = null,
                            modifier = Modifier.size(dimensionResource(R.dimen.icon_xlarge_size)),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.padding_md)))
                        Text(
                            text = stringResource(R.string.ingredient_form_online_required),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyLarge
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
                        categoryError = formState.categoryError,
                        descriptionError = formState.descriptionError,
                        unitRowsError = formState.unitRowsError,
                        categoryPlaceholder = stringResource(R.string.ingredient_form_category_placeholder),
                        onNameChange = { viewModel.updateFormName(it) },
                        onCategoryChange = { viewModel.updateFormCategory(it) },
                        onDescriptionChange = { viewModel.updateFormDescription(it) },
                        onUnitRowUpdate = { index, unit, cal -> viewModel.updateUnitRow(index, unit, cal) },
                        onUnitRowRemove = { viewModel.removeUnitRow(it) },
                        onAddUnitRow = { viewModel.addUnitRow() },
                    ) {
                        // Screen-specific bottom content
                        formState.errorMessage?.let { resId ->
                            Text(
                                text = stringResource(id = resId),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = dimensionResource(R.dimen.padding_xxsm))
                            )
                        }

                        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.padding_xl)))
                        val successToastSubmitted = stringResource(R.string.ingredient_form_toast_submitted)
                        val successToastUpdated = stringResource(R.string.ingredient_form_toast_updated)
                        PrimaryButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                scope.launch {
                                    val imageUrl = if (cloudinaryViewModel.uiState.value.selectedImageUri != null) {
                                        cloudinaryViewModel.uploadImage(context)
                                    } else {
                                        cloudinaryViewModel.uiState.value.uploadedImageUrl.ifEmpty { null }
                                    }

                                    viewModel.submitRequest(
                                        imageUrl = imageUrl,
                                        onComplete = {
                                            Toast.makeText(
                                                context,
                                                if (requestId == null) successToastSubmitted else successToastUpdated,
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            navController.navigate(Screen.Ingredients.createRoute(tab = 1)) {
                                                popUpTo(Screen.Ingredients.route) { this.inclusive = true }
                                            }
                                        }
                                    )
                                }
                            },
                            textID = if (requestId == null) R.string.request_new_ingredient else R.string.update_request,
                            enabled = !formState.isSubmitting
                        )

                        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.padding_xxl)))
                    }
                }
            }

            if (formState.isSubmitting) {
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
}
