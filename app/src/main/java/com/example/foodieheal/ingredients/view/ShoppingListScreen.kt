package com.example.foodieheal.ingredients.view

import android.app.Application
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.foodieheal.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.foodieheal.ingredients.model.IngredientCategory
import com.example.foodieheal.ingredients.model.ShoppingListItem
import com.example.foodieheal.ingredients.viewModel.IngredientsViewModelFactory
import com.example.foodieheal.ingredients.viewModel.ShoppingListViewModel
import com.example.foodieheal.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(
    navController: NavController,
    viewModel: ShoppingListViewModel = viewModel(
        factory = IngredientsViewModelFactory(LocalContext.current.applicationContext as Application)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    var showMenu by remember { mutableStateOf(false) }
    var showClearCheckedDialog by remember { mutableStateOf(false) }
    var showClearAllDialog by remember { mutableStateOf(false) }

    val checkedCount = uiState.items.count { it.entity.isChecked }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Shopping List",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
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
        floatingActionButton = {
            Box {
                FloatingActionButton(
                    onClick = { showMenu = !showMenu },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    /*shape = RoundedCornerShape(28.dp),
                    modifier = Modifier.padding(bottom = 16.dp),*/
                    shape = CircleShape,
                    modifier = Modifier
                        .size(64.dp)
                        .offset(y = (-32).dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_horiz_more),
                        contentDescription = "Options",
                        modifier = Modifier.size(32.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(Color(0xFFEBE6EF)) // Match subtle purple background
                ) {
                    DropdownMenuItem(
                        text = { Text("Add items", fontSize = 12.sp) },
                        onClick = {
                            showMenu = false
                            navController.navigate(Screen.AddShoppingListItem.route)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Clear checked items", fontSize = 12.sp) },
                        onClick = {
                            showMenu = false
                            if (checkedCount > 0) {
                                showClearCheckedDialog = true
                            } else {
                                Toast.makeText(context, "No checked items to clear.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Clear all items", fontSize = 12.sp) },
                        onClick = {
                            showMenu = false
                            if (uiState.items.isNotEmpty()) {
                                showClearAllDialog = true
                            } else {
                                Toast.makeText(context, "Shopping List is already empty.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        },
        containerColor = Color(0xFFF8F8F8)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Search Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                placeholder = { Text("Search ingredients here", fontSize = 14.sp) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                trailingIcon = { Icon(painter = painterResource(R.drawable.ic_search), contentDescription = null) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                )
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text("Categories", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))

            // Categories chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(IngredientCategory.entries) { category ->
                    FilterChip(
                        selected = uiState.selectedCategories.contains(category),
                        onClick = { viewModel.toggleCategory(category) },
                        label = { Text(category.categoryName, fontSize = 12.sp) },
                        shape = RoundedCornerShape(20.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.filteredItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = if (uiState.searchQuery.isEmpty()) "Your shopping list is empty. Add new item now!" else "No items match your search.")
                }
            } else {
                val grouped = uiState.filteredItems.groupBy { it.category ?: IngredientCategory.OTHERS }
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    grouped.forEach { (category, items) ->
                        item {
                            Text(
                                text = category.categoryName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                        items(items) { item ->
                            ShoppingListItemCard(item) {
                                viewModel.toggleChecked(item)
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(32.dp)) } // Space for FAB
                }
            }
        }
    }

    // Confirmation Dialogs
    if (showClearCheckedDialog) {
        AlertDialog(
            onDismissRequest = { showClearCheckedDialog = false },
            title = { Text("Clear Checked Items") },
            text = { Text("There are $checkedCount checked item(s) to be clear from your Shopping List. Are you sure?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearChecked()
                    Toast.makeText(context, "$checkedCount checked item(s) cleared from your Shopping List", Toast.LENGTH_SHORT).show()
                    showClearCheckedDialog = false
                }) {
                    Text("Yes", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCheckedDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text("Clear All Items") },
            text = { Text("All items will be cleared from your Shopping List. Are you sure?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAll()
                    Toast.makeText(context, "All items cleared from your Shopping List", Toast.LENGTH_SHORT).show()
                    showClearAllDialog = false
                }) {
                    Text("Yes", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun ShoppingListItemCard(
    item: ShoppingListItem,
    onCheckedChange: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = item.entity.ingredientName,
                    style = MaterialTheme.typography.labelLarge.copy(
                        textDecoration = if (item.entity.isChecked) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = item.ingredientDesc,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            Checkbox(
                checked = item.entity.isChecked,
                onCheckedChange = { onCheckedChange() },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}
