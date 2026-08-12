package com.example.foodieheal.Admin

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.foodieheal.Admin.ViewModel.AdminIngredientRequestViewModel
import com.example.foodieheal.R
import com.example.foodieheal.Cloudinary.CloudinaryUploadScreen
import com.example.foodieheal.Cloudinary.CloudinaryUploadViewModel
import com.example.foodieheal.ingredients.model.IngredientCategory
import com.example.foodieheal.ingredients.view.UnitRow
import com.example.foodieheal.navigation.Screen
import com.example.foodieheal.ui.components.CommonInputField
import com.kanyidev.searchable_dropdown.LargeSearchableDropdownMenu

@Composable
fun AdminIngredientRequestFormScreen(
    navController: NavController,
    requestId: String,
    viewModel: AdminIngredientRequestViewModel = viewModel(),
    cloudinaryViewModel: CloudinaryUploadViewModel = viewModel()
) {
    val formState by viewModel.formState.collectAsState()
    val availableUnits by viewModel.availableUnits.collectAsState()
    val requestDetail by viewModel.requestDetail.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current

    // TODO
    val errorMessage = formState.errorMessage
    var showErrorDialog by remember { mutableStateOf(false) }

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            showErrorDialog = true
        }
    }
    
    var showApproveDialog by remember { mutableStateOf(false) }

    LaunchedEffect(requestId) {
        viewModel.populateFormForReview(requestId)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                Column(modifier = Modifier.statusBarsPadding()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_arrowback),
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                        Text(
                            text = "Review Ingredient Request",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                // 1. Cloudinary Upload
                CloudinaryUploadScreen(viewModel = cloudinaryViewModel)
                Spacer(Modifier.height(16.dp))

                // 2. Ingredient Name
                CommonInputField(
                    value = formState.ingredientName,
                    onValueChange = { viewModel.updateFormName(it) },
                    textId = R.string.ingredient_name,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))

                // 3. Category
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
                Spacer(Modifier.height(16.dp))

                // 4. Description
                CommonInputField(
                    value = formState.description,
                    onValueChange = { viewModel.updateFormDescription(it) },
                    textId = R.string.description,
                    modifier = Modifier.height(200.dp).fillMaxWidth(),
                    singleLine = false,
                    maxLines = 8
                )
                Spacer(Modifier.height(16.dp))

                // 5. Calorie Information
                Text(
                    text = stringResource(R.string.calorie_information),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))

                formState.unitRows.forEachIndexed { index, row ->
                    UnitRow(
                        index = index,
                        selectedUnit = row.selectedUnit,
                        calories = row.calories,
                        availableUnits = availableUnits,
                        onUpdate = { unit, cal -> viewModel.updateUnitRow(index, unit, cal) },
                        onRemove = if (formState.unitRows.size > 1) { { viewModel.removeUnitRow(index) } } else null
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                TextButton(
                    onClick = { viewModel.addUnitRow() },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)){
                        Icon(painter = painterResource(R.drawable.ic_outline_add), contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("Add Unit")
                    }
                }

                Spacer(modifier = Modifier.height(120.dp))
            }
        }

        // Floating Approve Button
        Button(
            onClick = { showApproveDialog = true },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            enabled = !formState.isSubmitting && requestDetail != null && !isLoading
        ) {
            if (formState.isSubmitting || (isLoading && requestDetail == null)) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text(
                    text = "APPROVE INGREDIENT REQUEST",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
            }
        }
    }

    if (showApproveDialog) {
        AlertDialog(
            onDismissRequest = { showApproveDialog = false },
            title = { Text("Approve Request") },
            text = { Text("Ingredient Request that is approved cannot be edited anymore. Are you sure?") },
            confirmButton = {
                TextButton(onClick = {
                    showApproveDialog = false // Close dialog immediately
                    viewModel.approveRequest(
                        imageUrl = cloudinaryViewModel.uiState.value.uploadedImageUrl.ifEmpty { formState.imageUrl },
                        onComplete = {
                            Toast.makeText(context, "Request Approved Successfully", Toast.LENGTH_SHORT).show()
                            navController.navigate(Screen.AdminChefScreen.route) {
                                popUpTo(Screen.AdminChefScreen.route) { this.inclusive = true }
                            }
                        }
                    )
                }) {
                    Text("Yes", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showApproveDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    // TODO
    if (showErrorDialog && errorMessage != null) {
        AlertDialog(
            onDismissRequest = { 
                showErrorDialog = false
                viewModel.clearError()
            },
            title = { Text("Approval Failed", fontWeight = FontWeight.Bold) },
            text = { 
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = { 
                    showErrorDialog = false
                    viewModel.clearError()
                }) {
                    Text("OK", color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }
}
