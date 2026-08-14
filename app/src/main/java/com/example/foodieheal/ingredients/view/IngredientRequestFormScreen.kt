package com.example.foodieheal.ingredients.view

import android.app.Application
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import com.example.foodieheal.R
import com.example.foodieheal.Cloudinary.CloudinaryUploadScreen
import com.example.foodieheal.Cloudinary.CloudinaryUploadViewModel
import com.example.foodieheal.ingredients.model.IngredientCategory
import com.example.foodieheal.ingredients.model.Units
import com.example.foodieheal.ingredients.viewModel.IngredientRequestViewModel
import com.example.foodieheal.ingredients.viewModel.IngredientRequestViewModelFactory
import com.example.foodieheal.navigation.Screen
import com.example.foodieheal.ui.components.CommonInputField
import com.kanyidev.searchable_dropdown.LargeSearchableDropdownMenu

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
        factory = IngredientRequestViewModelFactory(application)
    )
    
    val uiState by viewModel.uiState.collectAsState()
    val formState by viewModel.formState.collectAsState()
    val availableUnits by viewModel.availableUnits.collectAsState()
    
    val scope = rememberCoroutineScope()

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
                        text = if (requestId == null) "Add Ingredient" else "Update Ingredient",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { 
                        navController.navigate(Screen.Ingredients.createRoute(tab = 1)) {
                            popUpTo(Screen.Ingredients.route) { this.inclusive = true }
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFFF8F8F8)
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
                            modifier = Modifier.size(70.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "You need to be online to submit a request",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                        .imePadding(), // prevent keyboard from hiding input box
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
                    if (formState.nameError != null) {
                        Text(
                            text = formState.nameError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                        )
                    }
                    Spacer(Modifier.height(16.dp))

                    // 3. Ingredient Category
                    LargeSearchableDropdownMenu(
                        modifier = Modifier.fillMaxWidth(),
                        title = stringResource(R.string.category),
                        fieldLabelTextStyle = MaterialTheme.typography.bodyLarge,
                        selectedOption = formState.category,
                        onItemSelected = { viewModel.updateFormCategory(it) },
                        selectedItemToString = { it.categoryName },
                        placeholder = "e.g. Vegetables",
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
                        )
                    )
                    if (formState.categoryError != null) {
                        Text(
                            text = formState.categoryError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
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
                    if (formState.descriptionError != null) {
                        Text(
                            text = formState.descriptionError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                        )
                    }
                    Spacer(Modifier.height(16.dp))

                    // 5. Calorie Information (Dynamic Rows)
                    Text(
                        text = stringResource(R.string.calorie_information),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (formState.unitRowsError != null) {
                        Text(
                            text = formState.unitRowsError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
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
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ){
                            Icon(
                                painter = painterResource(R.drawable.ic_outline_add),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text("Add Unit")
                        }
                    }

                    // General error message
                    if (formState.errorMessage != null) {
                        Text(
                            text = formState.errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
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
                                        Toast.makeText(context, if (requestId == null) "Request submitted successfully" else "Request updated successfully", Toast.LENGTH_SHORT).show()
                                        navController.navigate(Screen.Ingredients.createRoute(tab = 1)) {
                                            popUpTo(Screen.Ingredients.route) { this.inclusive = true }
                                        }
                                    }
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        enabled = !formState.isSubmitting
                    ) {
                        if (formState.isSubmitting) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text(
                                text = if (requestId == null) "Request New Ingredient" else "Update Request",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun UnitRow(
    index: Int,
    selectedUnit: Units?,
    calories: String,
    availableUnits: List<Units>,
    onUpdate: (Units?, String) -> Unit,
    onRemove: (() -> Unit)?,
    unitError: String? = null,
    caloriesError: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Serving Unit", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            LargeSearchableDropdownMenu(
                modifier = Modifier.fillMaxWidth(),
                selectedOption = selectedUnit,
                onItemSelected = { onUpdate(it, calories) },
                selectedItemToString = { it.unitName },
                placeholder = "e.g. gram",
                // TODO: placeholderTextStyle
                options = availableUnits,
                drawItem = { item, selected, itemEnabled, onClick ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = itemEnabled, onClick = onClick)
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = item.unitName,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (selected) MaterialTheme.colorScheme.primary else Color.Black
                        )
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                )
            )
            if (unitError != null) {
                Text(
                    text = unitError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text("Calories", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            val placeholderText = if (selectedUnit != null) {
                "per ${selectedUnit.defaultQuantity.toInt()} ${selectedUnit.unitDisplay}"
            } else {
                "e.g. per 100 g"
            }
            OutlinedTextField(
                value = calories,
                onValueChange = { newValue ->
                    // Only allow numeric digits (stops decimal points, commas, etc. being entered)
                    if (newValue.all { it.isDigit() }) {
                        onUpdate(selectedUnit, newValue)
                    }
                },
                placeholder = { Text(placeholderText, fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth(),
                isError = caloriesError != null,
                shape = RoundedCornerShape(8.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Number
                )
            )
            if (caloriesError != null) {
                Text(
                    text = caloriesError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                )
            }
        }

        if (onRemove != null) {
            IconButton(onClick = onRemove, modifier = Modifier.padding(top = 16.dp)) {
                Icon(painter = painterResource(R.drawable.ic_remove), contentDescription = "Remove Unit",)
            }
        }
    }
}
