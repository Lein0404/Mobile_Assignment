package com.example.foodieheal.ingredients.view

import android.app.Application
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil3.compose.SubcomposeAsyncImage
import com.example.foodieheal.R
import com.example.foodieheal.ingredients.viewModel.IngredientsViewModel
import com.example.foodieheal.ingredients.viewModel.IngredientRequestViewModel
import com.example.foodieheal.ingredients.viewModel.IngredientRequestViewModelFactory
import com.example.foodieheal.model.Status
import com.example.foodieheal.navigation.Screen
import com.example.foodieheal.ui.components.PrimaryButton
import com.example.foodieheal.ui.components.StatusBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngredientDetailScreen(
    navController: NavController,
    ingredientId: String,
    isRequest: Boolean = false
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application

    val ingredientsViewModel: IngredientsViewModel = viewModel()
    val requestViewModel: IngredientRequestViewModel = viewModel(
        factory = IngredientRequestViewModelFactory(application)
    )
    
    val ingredientsUiState by ingredientsViewModel.uiState.collectAsState()
    val requestDetail by requestViewModel.requestDetail.collectAsState()
    val requestUiState by requestViewModel.uiState.collectAsState()
    
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(ingredientId, isRequest) {
        if (isRequest) {
            requestViewModel.fetchRequestDetail(ingredientId)
        } else {
            ingredientsViewModel.fetchIngredientDetail(ingredientId)
        }
    }

    val isLoading = if (isRequest) requestUiState.isLoading else ingredientsUiState.isLoading
    val name = if (isRequest) requestDetail?.request?.ingredientName else ingredientsUiState.ingredientDetail?.ingredient?.ingredientName
    val category = if (isRequest) requestDetail?.request?.ingredientCategory?.categoryName else ingredientsUiState.ingredientDetail?.ingredient?.ingredientCategory?.categoryName
    val image = if (isRequest) requestDetail?.request?.ingredientImage else ingredientsUiState.ingredientDetail?.ingredient?.ingredientImage
    val description = if (isRequest) requestDetail?.request?.ingredientDesc else ingredientsUiState.ingredientDetail?.ingredient?.ingredientDesc
    val calorieInfo = if (isRequest) requestDetail?.calorieSummary else ingredientsUiState.ingredientDetail?.calorieSummary

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isRequest) "View Request" else "View Ingredient",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { 
                        if (isRequest) {
                            navController.navigate(Screen.Ingredients.createRoute(tab = 1)) {
                                popUpTo(Screen.Ingredients.route) { this.inclusive = true }
                            }
                        } else {
                            navController.popBackStack()
                        }
                    }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrowback),
                            contentDescription = "Back", 
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(MaterialTheme.colorScheme.background)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Image display
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .background(Color(0xFFE0E0E0)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!image.isNullOrEmpty()) {
                            SubcomposeAsyncImage(
                                model = image,
                                contentDescription = name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                loading = {
                                    CircularProgressIndicator(modifier = Modifier.scale(0.5f)) // TODO: make the CPI smaller
                                },
                                error = { ImagePlaceholder() }
                            )
                        } else {
                            ImagePlaceholder()
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = name ?: "", 
                                    style = MaterialTheme.typography.headlineMedium, 
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                                Text(
                                    text = category ?: "Others", 
                                    color = Color.Gray,
                                    fontSize = 14.sp
                                )
                                
                                if (isRequest) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    StatusBadge(status = requestDetail?.request?.requestStatus ?: Status.PENDING)
                                }
                            }
                            
                            if (!isRequest || (isRequest && requestDetail?.request?.requestStatus == Status.APPROVED)) {
                                IconButton(onClick = {
                                    if (isRequest) {
                                        requestDetail?.request?.let { request ->
                                            val productionId = request.ingredientId
                                            if (productionId != null) {
                                                ingredientsViewModel.addToShoppingList(productionId, request.ingredientName)
                                                Toast.makeText(context, "${request.ingredientName} added to Shopping List", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Error: Production ID missing for this request", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } else {
                                        ingredientsUiState.ingredientDetail?.ingredient?.let {
                                            ingredientsViewModel.addToShoppingList(it)
                                            Toast.makeText(context, "${it.ingredientName} added to Shopping List", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_add_to_shopping_cart),
                                        contentDescription = "Add to shopping list",
                                        tint = Color.Black
                                    )
                                }
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 16.dp), 
                            thickness = 1.dp, 
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text("Description", fontWeight = FontWeight.Bold, color = Color.Black)
                        Text(
                            text = description?.ifEmpty { "No description available." } ?: "No description available.",
                            color = Color.Black,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Text("Calorie Information", fontWeight = FontWeight.Bold, color = Color.Black)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = calorieInfo?.ifEmpty { "No calorie information available." } ?: "No calorie information available.",
                            color = Color.Black
                        )

                        if (isRequest && requestDetail?.request?.requestStatus == Status.REJECTED) {
                            Spacer(modifier = Modifier.height(24.dp))
                            Text("Rejected Reason", fontWeight = FontWeight.Bold, color = Color.Black)
                            Text(
                                text = requestDetail?.request?.rejectedReason ?: "Unspecified.",
                                color = Color.Black,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        if (isRequest && requestDetail?.request?.requestStatus == Status.APPROVED && !requestDetail?.request?.adminNote.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(24.dp))
                            Text("Admin Notes", fontWeight = FontWeight.Bold, color = Color.Black)
                            Text(
                                text = requestDetail?.request?.adminNote ?: "",
                                color = Color.Black,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(120.dp))
                    }
                }


                // Floating Action Buttons for Pending Request (only when online)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 30.dp),
                    verticalArrangement = Arrangement.Bottom, // push the button down to the bottom
                    horizontalAlignment = Alignment.CenterHorizontally
                ){
                    if (isRequest && requestDetail?.request?.requestStatus == Status.PENDING && requestUiState.isNetworkAvailable) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Button(
                                onClick = { showDeleteDialog = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                ),
                                shape = RoundedCornerShape(dimensionResource(id = R.dimen.corner_radius_sm))
                            ) {
                                Text(
                                    text = stringResource(R.string.delete_request),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                            PrimaryButton(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    navController.navigate(Screen.IngredientRequestForm.createRoute(ingredientId))
                                },
                                textID = R.string.edit_request
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog && isRequest) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Request") },
            text = { Text("Are you sure you want to delete this request?") },
            confirmButton = {
                TextButton(onClick = {
                    requestViewModel.deleteRequest(ingredientId) {
                        Toast.makeText(context, "Request deleted", Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
                    }
                    showDeleteDialog = false
                }) {
                    Text("Yes", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun ImagePlaceholder() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            painter = painterResource(R.drawable.ic_no_image_available),
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color.Gray
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text("No image available", color = Color.Gray)
    }
}
