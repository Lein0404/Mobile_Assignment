package com.example.foodieheal.ingredients.view

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
import androidx.compose.ui.graphics.Color
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
import com.example.foodieheal.R
import com.example.foodieheal.ingredients.viewModel.IngredientsViewModel
import com.example.foodieheal.ingredients.viewModel.IngredientRequestViewModel
import com.example.foodieheal.ingredients.viewModel.IngredientsViewModelFactory
import com.example.foodieheal.model.Status
import com.example.foodieheal.navigation.Screen
import com.example.foodieheal.ui.components.PrimaryButton
import com.example.foodieheal.ui.components.StatusBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngredientDetailScreen(
    navController: NavController,
    ingredientId: String,
    isRequest: Boolean = false,
    showAddToCart: Boolean = true
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application

    val factory = IngredientsViewModelFactory(application)
    val ingredientsViewModel: IngredientsViewModel = viewModel(factory = factory)
    val requestViewModel: IngredientRequestViewModel = viewModel(factory = factory)
    
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
    val isRefreshing = if (isRequest) requestUiState.isRefreshing else ingredientsUiState.isRefreshing

    if (isRequest && requestUiState.isStatusConflict) {
        AlertDialog(
            onDismissRequest = { },
            title = {
                Text(
                    text = stringResource(R.string.ingredient_detail_request_processed_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(stringResource(R.string.ingredient_detail_request_processed_text))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        requestViewModel.clearStatusConflict()
                        // Refresh to show latest status and hide delete button
                        requestViewModel.fetchRequestDetail(ingredientId)
                    }
                ) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }

    val displayData = remember(isRequest, requestDetail, ingredientsUiState.ingredientDetail) {
        if (isRequest) {
            val req = requestDetail?.request
            IngredientDisplayData(
                name = req?.ingredientName,
                category = req?.ingredientCategory?.categoryName,
                image = req?.ingredientImage,
                description = req?.ingredientDesc,
                calorieInfo = requestDetail?.calorieSummary
            )
        } else {
            val detail = ingredientsUiState.ingredientDetail
            IngredientDisplayData(
                name = detail?.ingredient?.ingredientName,
                category = detail?.ingredient?.ingredientCategory?.categoryName,
                image = detail?.ingredient?.ingredientImage,
                description = detail?.ingredient?.ingredientDesc,
                calorieInfo = detail?.calorieSummary
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isRequest) stringResource(R.string.ingredient_detail_title_request) else stringResource(R.string.ingredient_detail_title_ingredient),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.titleLarge,
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
                            contentDescription = stringResource(R.string.back), 
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        }
    ) { paddingValues ->
        if (isLoading && !isRefreshing) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    if (isRequest) {
                        requestViewModel.refreshRequestDetail(ingredientId)
                    } else {
                        ingredientsViewModel.refreshIngredientDetail(ingredientId)
                    }
                },
                modifier = Modifier.fillMaxSize().padding(paddingValues)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
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
                            if (!displayData.image.isNullOrEmpty()) {
                                SubcomposeAsyncImage(
                                    model = displayData.image,
                                    contentDescription = displayData.name,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                    loading = {
                                        CircularProgressIndicator(modifier = Modifier.scale(0.2f)) // TODO: make the CPI smaller
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
                                Column(
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(
                                        text = displayData.name ?: "", 
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                    Text(
                                        text = displayData.category ?: stringResource(R.string.shopping_list_categories),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color.Gray,
                                    )
                                    
                                    if (isRequest) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        StatusBadge(status = requestDetail?.request?.requestStatus ?: Status.PENDING)
                                    }
                                }
                                
                                if (showAddToCart && (!isRequest || (isRequest && requestDetail?.request?.requestStatus == Status.APPROVED))) {
                                    IconButton(onClick = {
                                        if (isRequest) {
                                            requestDetail?.request?.let { request ->
                                                val productionId = request.ingredientId
                                                if (productionId != null) {
                                                    ingredientsViewModel.addToShoppingList(productionId, request.ingredientName)
                                                    Toast.makeText(
                                                        context,
                                                        application.getString(R.string.ingredients_toast_added, request.ingredientName),
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                } else {
                                                    Toast.makeText(
                                                        context,
                                                        R.string.ingredient_detail_production_id_missing,
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        } else {
                                            ingredientsUiState.ingredientDetail?.ingredient?.let {
                                                ingredientsViewModel.addToShoppingList(it)
                                                Toast.makeText(context, application.getString(R.string.ingredients_toast_added, it.ingredientName), Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_add_to_shopping_cart),
                                            contentDescription = stringResource(R.string.desc_add_recipe),
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

                            Text(stringResource(R.string.ingredient_detail_description_label), fontWeight = FontWeight.Bold, color = Color.Black)
                            Text(
                                text = displayData.description?.ifEmpty { stringResource(R.string.ingredient_detail_no_description) } ?: stringResource(R.string.ingredient_detail_no_description),
                                color = Color.Black,
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Text(stringResource(R.string.calorie_information), fontWeight = FontWeight.Bold, color = Color.Black)
                            Text(
                                text = displayData.calorieInfo?.ifEmpty { stringResource(R.string.ingredient_detail_no_calorie_info) } ?: stringResource(R.string.ingredient_detail_no_calorie_info),
                                color = Color.Black
                            )

                            if (isRequest && requestDetail?.request?.requestStatus == Status.REJECTED) {
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(stringResource(R.string.ingredient_detail_rejected_reason_label), fontWeight = FontWeight.Bold, color = Color.Black)
                                Text(
                                    text = requestDetail?.request?.rejectedReason ?: stringResource(R.string.ingredient_detail_unspecified),
                                    color = Color.Black,
                                )
                            }

                            if (isRequest && requestDetail?.request?.requestStatus == Status.APPROVED && !requestDetail?.request?.adminNote.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(stringResource(R.string.ingredient_detail_admin_notes_label), fontWeight = FontWeight.Bold, color = Color.Black)
                                Text(
                                    text = requestDetail?.request?.adminNote ?: "",
                                    color = Color.Black,
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(120.dp))
                        }
                    }


                    // Floating Action Buttons for Pending Request (only when online)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 16.dp),
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
    }

    if (showDeleteDialog && isRequest) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.ingredient_detail_delete_title)) },
            text = { Text(stringResource(R.string.ingredient_detail_delete_text)) },
            confirmButton = {
                TextButton(onClick = {
                    requestViewModel.deleteRequest(ingredientId) {
                        Toast.makeText(context, R.string.ingredient_detail_toast_deleted, Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
                    }
                    showDeleteDialog = false
                }) {
                    Text(stringResource(R.string.dialog_yes), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.dialog_cancel), color = Color.Gray)
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
        Text(stringResource(R.string.image_unavailable), color = Color.Gray)
    }
}

private data class IngredientDisplayData(
    val name: String? = null,
    val category: String? = null,
    val image: String? = null,
    val description: String? = null,
    val calorieInfo: String? = null
)
