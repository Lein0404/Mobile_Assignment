package com.example.foodieheal.Recipe.View

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.foodieheal.R
import com.example.foodieheal.Recipe.Model.Recipe
import com.example.foodieheal.Recipe.Model.Ingredient
import com.example.foodieheal.Recipe.Model.IngredientItem
import com.example.foodieheal.Recipe.viewModel.RecipeViewModel
import com.example.foodieheal.User.viewModel.AuthViewModel
import kotlinx.coroutines.delay
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.collections.find

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRecipeScreen(
    navController: NavController,
    recipeId: String,
    viewModel: RecipeViewModel,
    authViewModel: AuthViewModel
) {
    val context = LocalContext.current
    
    // 1. Fetch data if needed
    LaunchedEffect(recipeId) {
        viewModel.fetchRecipeById(recipeId)
    }

    val existingRecipe = viewModel.selectedRecipe

    var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var recipeName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var course by remember { mutableStateOf("Breakfast") }
    var totalTime by remember { mutableStateOf("") }
    var cookingSkill by remember { mutableStateOf("Beginner") }
    var budget by remember { mutableStateOf("0 - 10") }
    var steps by remember { mutableStateOf("") }
    
    val ingredients = remember { mutableStateListOf<IngredientInputState>() }

    // 2. Populate fields when recipe data is loaded
    LaunchedEffect(existingRecipe) {
        existingRecipe?.let { r ->
            recipeName = r.recipeName
            description = r.recipeDescription
            course = r.recipeCourse
            totalTime = r.time.toString()
            cookingSkill = r.cookingSkill
            budget = r.estimatedBudget
            steps = r.recipeStep
            
            ingredients.clear()
            r.ingredients.forEach { 
                ingredients.add(IngredientInputState(it.name, it.quantity, it.unit))
            }
        }
    }

    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    val isFormValid by remember {
        derivedStateOf {
            recipeName.isNotBlank() && recipeName.length <= 30 &&
            totalTime.isNotBlank() &&
            totalTime.toIntOrNull() != null &&
            steps.isNotBlank() &&
            ingredients.isNotEmpty() &&
            ingredients.all { it.name.isNotBlank() && it.quantity.isNotBlank() && it.quantity.toDoubleOrNull() != null }
        }
    }

    val totalCalories = remember {
        derivedStateOf {
            ingredients.sumOf { input ->
                val qty = input.quantity.toDoubleOrNull() ?: 0.0
                val ingredientData = viewModel.availableIngredients.find { 
                    it.name?.equals(input.name, ignoreCase = true) == true &&
                    it.defaultUnit?.equals(input.unit, ignoreCase = true) == true
                }
                
                // 🌟 NEW CALCULATION: qty * caloriePerUnitValue / unitValue
                // Defaulting unitValue to 1.0 to avoid division by zero
                val caloriePerUnitValue = ingredientData?.kcal ?: 0.0
                val unitValue = ingredientData?.defaultQuantity ?: 1.0
                
                qty * caloriePerUnitValue / unitValue
            }.toInt()
        }
    }.value

    val view = LocalView.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.bookmarkMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.updateRecipeSuccess.collect { success ->
            if (success) {
                delay(800)
                navController.popBackStack()
            }
        }
    }

    SideEffect {
        val window = (view.context as Activity).window
        window.statusBarColor = primaryColor.toArgb()
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
    }

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            imageUri = it
            val inputStream: InputStream? = context.contentResolver.openInputStream(it)
            imageBitmap = BitmapFactory.decodeStream(inputStream)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Edit Recipe", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(painterResource(id = R.drawable.ic_arrowback), "Back", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        }
    ) { paddingValues ->
        if (existingRecipe == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
                    .navigationBarsPadding() 
                    .imePadding() 
                    .verticalScroll(scrollState)
                    .padding(20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { imageLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (imageBitmap != null) {
                        Image(
                            bitmap = imageBitmap!!.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else if (!existingRecipe.recipeImageUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = existingRecipe.recipeImageUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                painter = painterResource(id = R.drawable.upload),
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Change Recipe Image", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LabelText("Recipe Name")
                AddRecipeTextField(
                    value = recipeName,
                    onValueChange = { if (it.length <= 30) recipeName = it },
                    placeholder = "Recipe Name"
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "${recipeName.length}/30",
                        fontSize = 11.sp,
                        color = if (recipeName.length >= 30) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                LabelText("Description")
                AddRecipeTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = "What inspired you to make this recipe...",
                    singleLine = false,
                    modifier = Modifier.height(100.dp)
                )

                LabelText("Course")
                DropdownField(
                    value = course,
                    options = listOf("Breakfast", "Lunch", "Dinner", "Snack"),
                    onSelected = { course = it }
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        LabelText("Total Time (min)")
                        AddRecipeTextField(
                            value = totalTime,
                            onValueChange = { input ->
                                if (input.all { it.isDigit() }) {
                                    totalTime = input
                                }
                            },
                            placeholder = "e.g. 30",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            trailingIcon = {
                                Icon(
                                    painterResource(id = R.drawable.ic_clock),
                                    null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        LabelText("Calories (kcal)")
                        AddRecipeTextField(
                            value = "$totalCalories",
                            onValueChange = { },
                            readOnly = true,
                            placeholder = "0"
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        LabelText("Cooking Skill")
                        DropdownField(
                            value = cookingSkill,
                            options = listOf("Beginner", "Intermediate", "Master/Expert"),
                            onSelected = { cookingSkill = it }
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        LabelText("Budget (RM)")
                        DropdownField(
                            value = budget,
                            options = listOf("0 - 20", "20 - 40", "40 - 60", "60 - 80", "80 - 100"),
                            onSelected = { budget = it }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LabelText("Ingredients")
                    TextButton(onClick = { ingredients.clear(); ingredients.add(IngredientInputState()) }) {
                        Text("Reset Ingredients", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                }
                
                ingredients.forEachIndexed { index, item ->
                    IngredientRow(
                        item = item,
                        availableIngredients = viewModel.availableIngredients,
                        onRemove = { if (ingredients.size > 1) ingredients.removeAt(index) },
                        onUpdate = { updated -> ingredients[index] = updated }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                TextButton(
                    onClick = { ingredients.add(IngredientInputState()) },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Icon(painterResource(id = R.drawable.ic_outline_add), null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Ingredient", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }

                LabelText("Recipe Steps")
                AddRecipeTextField(
                    value = steps,
                    onValueChange = { steps = it },
                    placeholder = "1. Cook the Pasta",
                    singleLine = false,
                    modifier = Modifier
                        .height(120.dp)
                        .onFocusEvent { focusState ->
                            if (focusState.isFocused) {
                                coroutineScope.launch {
                                    bringIntoViewRequester.bringIntoView()
                                }
                            }
                        }
                        .bringIntoViewRequester(bringIntoViewRequester)
                )

                Spacer(modifier = Modifier.height(20.dp))

                if (viewModel.errorMessage != null) {
                    val cleanError = viewModel.errorMessage!!.split("\n").firstOrNull() ?: "An error occurred"
                    Surface(
                        color = Color(0xFFFFEBEE),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(bottom = 16.dp).fillMaxWidth()
                    ) {
                        Text(
                            text = cleanError,
                            color = Color(0xFFD32F2F),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                if (!isFormValid) {
                    Text(
                        text = "Please fill in all fields with valid information.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 8.dp).align(Alignment.CenterHorizontally)
                    )
                }

                Button(
                    onClick = {
                        var imageBytes: ByteArray? = null
                        if (imageBitmap != null) {
                            val stream = ByteArrayOutputStream()
                            imageBitmap!!.compress(Bitmap.CompressFormat.JPEG, 80, stream)
                            imageBytes = stream.toByteArray()
                        }

                        val updatedRecipe = existingRecipe.copy(
                            recipeName = recipeName,
                            recipeDescription = description,
                            recipeCourse = course,
                            time = totalTime.toIntOrNull() ?: 0,
                            calories = totalCalories,
                            cookingSkill = cookingSkill,
                            estimatedBudget = budget,
                            recipeStep = steps,
                            ingredients = ingredients.map { IngredientItem(it.name, it.quantity, it.unit) }
                        )
                        viewModel.updateRecipe(updatedRecipe, imageBytes)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    ),
                    enabled = isFormValid && !viewModel.isLoading
                ) {
                    if (viewModel.isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("UPDATE RECIPE", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
                
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}
