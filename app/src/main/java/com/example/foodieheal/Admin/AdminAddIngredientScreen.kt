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
import com.example.foodieheal.Cloudinary.CloudinaryUploadScreen
import com.example.foodieheal.Cloudinary.CloudinaryUploadViewModel
import com.example.foodieheal.R
import com.example.foodieheal.ingredients.model.IngredientCategory
import com.example.foodieheal.ingredients.view.UnitRow
import com.example.foodieheal.ui.components.CommonInputField
import com.example.foodieheal.ui.components.PrimaryButton
import com.kanyidev.searchable_dropdown.LargeSearchableDropdownMenu
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
                    // 1. Cloudinary Upload
                    CloudinaryUploadScreen(viewModel = cloudinaryViewModel)
                    Spacer(Modifier.height(dimensionResource(id = R.dimen.padding_l)))

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
                            modifier = Modifier.padding(top = dimensionResource(id = R.dimen.padding_xxsm))
                        )
                    }
                    Spacer(Modifier.height(dimensionResource(id = R.dimen.padding_l)))

                    // 3. Category
                    LargeSearchableDropdownMenu(
                        modifier = Modifier.fillMaxWidth().padding(top = dimensionResource(id = R.dimen.padding_xxsm)),
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
                                    .padding(horizontal = dimensionResource(id = R.dimen.padding_l), vertical = dimensionResource(id = R.dimen.padding_md))
                            ) {
                                Text(
                                    text = item.categoryName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
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
                            modifier = Modifier.padding(top = dimensionResource(id = R.dimen.padding_xxsm))
                        )
                    }
                    Spacer(Modifier.height(dimensionResource(id = R.dimen.padding_l)))

                    // 4. Description
                    CommonInputField(
                        value = formState.description,
                        onValueChange = { viewModel.updateFormDescription(it) },
                        textId = R.string.description,
                        modifier = Modifier
                            .height(dimensionResource(R.dimen.large_OutlinedTextField_size))
                            .fillMaxWidth(),
                        singleLine = false,
                        maxLines = 8,
                        isError = formState.descriptionError != null
                    )
                    formState.descriptionError?.let { resId ->
                        Text(
                            text = stringResource(id = resId),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = dimensionResource(id = R.dimen.padding_xxsm))
                        )
                    }
                    Spacer(Modifier.height(dimensionResource(id = R.dimen.padding_l)))

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
                            modifier = Modifier.padding(top = dimensionResource(id = R.dimen.padding_xxsm))
                        )
                    }
                    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.padding_xsm)))

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
                                .padding(vertical = dimensionResource(id = R.dimen.padding_l)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(dimensionResource(R.dimen.icon_large_size))
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.padding_xsm)))
                    TextButton(
                        onClick = { viewModel.addUnitRow() },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.padding_sm))
                        ){
                            Icon(
                                painter = painterResource(R.drawable.ic_outline_add),
                                contentDescription = null,
                                modifier = Modifier.size(dimensionResource(R.dimen.icon_medium_size))
                            )
                            Text(stringResource(R.string.ingredient_form_add_unit))
                        }
                    }

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
