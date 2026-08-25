package com.example.foodieheal.Admin

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
import com.example.foodieheal.Admin.ViewModel.AdminAddIngredientViewModel
import com.example.foodieheal.Admin.ViewModel.AdminViewModelFactory
import com.example.foodieheal.Cloudinary.CloudinaryUploadViewModel
import com.example.foodieheal.R
import com.example.foodieheal.ingredients.shared.IngredientFormBody
import com.example.foodieheal.ui.components.PrimaryButton
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAddIngredientScreen(
    navController: NavController,
    cloudinaryViewModel: CloudinaryUploadViewModel = viewModel()
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val viewModel: AdminAddIngredientViewModel = viewModel(
        factory = AdminViewModelFactory(application)
    )
    
    val uiState by viewModel.uiState.collectAsState()
    val formState by viewModel.formState.collectAsState()
    val availableUnits by viewModel.availableUnits.collectAsState()
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.admin_add_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
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
                .consumeWindowInsets(paddingValues)
        ) {
            if (!uiState.isNetworkAvailable) {
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
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(dimensionResource(id = R.dimen.padding_l))
                        .imePadding(),
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
                        categoryPlaceholder = stringResource(R.string.admin_add_category_placeholder),
                        onNameChange = { viewModel.updateFormName(it) },
                        onCategoryChange = { viewModel.updateFormCategory(it) },
                        onDescriptionChange = { viewModel.updateFormDescription(it) },
                        onUnitRowUpdate = { index, unit, cal -> viewModel.updateUnitRow(index, unit, cal) },
                        onUnitRowRemove = { viewModel.removeUnitRow(it) },
                        onAddUnitRow = { viewModel.addUnitRow() },
                    ) {
                        // Screen-specific bottom content
                        uiState.errorMessage?.let { resId ->
                            Text(
                                text = stringResource(id = resId),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = dimensionResource(id = R.dimen.padding_smd))
                            )
                        }

                        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.padding_xl)))
                        val successToastMsg = stringResource(R.string.admin_add_toast_success)
                        PrimaryButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                scope.launch {
                                    val imageUrl = if (cloudinaryViewModel.uiState.value.selectedImageUri != null) {
                                        cloudinaryViewModel.uploadImage(context)
                                    } else {
                                        cloudinaryViewModel.uiState.value.uploadedImageUrl.ifEmpty { null }
                                    }

                                    viewModel.submitIngredient(
                                        imageUrl = imageUrl,
                                        onComplete = {
                                            Toast.makeText(context, successToastMsg, Toast.LENGTH_SHORT).show()
                                            navController.popBackStack()
                                        }
                                    )
                                }
                            },
                            textID = R.string.admin_add_button,
                            enabled = !uiState.isSubmitting
                        )

                        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.padding_xxl)))
                    }
                }
            }

            if (uiState.isSubmitting) {
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
