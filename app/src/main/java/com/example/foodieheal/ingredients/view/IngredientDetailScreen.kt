package com.example.foodieheal.ingredients.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.ImageNotSupported
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.compose.SubcomposeAsyncImage
import com.example.foodieheal.ingredients.viewModel.IngredientsViewModel
import androidx.compose.ui.tooling.preview.Preview
import com.example.foodieheal.ui.theme.MobileAssignmentTheme
import androidx.navigation.compose.rememberNavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngredientDetailScreen(navController: NavController, ingredientId: String) {
    val viewModel: IngredientsViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(ingredientId) {
        viewModel.fetchIngredientDetail(ingredientId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("View Ingredient", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Back", 
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            uiState.ingredientDetail?.let { info ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    // Image display
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .background(Color(0xFFE0E0E0)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!info.ingredient.ingredientImage.isNullOrEmpty()) {
                            SubcomposeAsyncImage(
                                model = info.ingredient.ingredientImage,
                                contentDescription = info.ingredient.ingredientName,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                loading = {
                                    CircularProgressIndicator(modifier = Modifier.scale(0.5f))
                                },
                                error = {
                                    ImagePlaceholder()
                                }
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
                                    text = info.ingredient.ingredientName, 
                                    style = MaterialTheme.typography.headlineMedium, 
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                                Text(
                                    text = info.ingredient.ingredientCategory?.categoryName ?: "Others", 
                                    color = Color.Gray,
                                    fontSize = 14.sp
                                )
                            }
                            IconButton(onClick = { /* Add to cart */ }) {
                                Icon(
                                    Icons.Default.AddShoppingCart, 
                                    contentDescription = "Add to cart",
                                    tint = Color.Black
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 16.dp), 
                            thickness = 1.dp, 
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text("Note", fontWeight = FontWeight.Bold, color = Color.Black)
                        Text(
                            text = info.ingredient.ingredientDesc.ifEmpty { "No description available." },
                            color = Color.Black,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Text("Calorie Information", fontWeight = FontWeight.Bold, color = Color.Black)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = info.calorieSummary.ifEmpty { "No calorie information available." },
                            color = Color.Black
                        )
                    }
                }
            } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Ingredient not found")
            }
        }
    }
}

@Composable
fun ImagePlaceholder() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = Icons.Default.ImageNotSupported,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color.Gray
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text("No image available", color = Color.Gray)
    }
}
