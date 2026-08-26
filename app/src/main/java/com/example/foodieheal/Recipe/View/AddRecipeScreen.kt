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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.navigation.NavController
import com.example.foodieheal.R
import com.example.foodieheal.Recipe.Model.Recipe
import com.example.foodieheal.Recipe.Model.IngredientItem
import kotlinx.serialization.json.*
import com.example.foodieheal.Recipe.Model.Ingredient
import com.example.foodieheal.Recipe.viewModel.RecipeViewModel
import com.example.foodieheal.User.viewModel.AuthViewModel
import com.example.foodieheal.meal_planner.screen.OfflinePlaceholder
import kotlinx.coroutines.delay
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.collections.find

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecipeScreen(
    navController: NavController,
    viewModel: RecipeViewModel,
    authViewModel: AuthViewModel
) {
    val context = LocalContext.current
    var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var recipeName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var course by remember { mutableStateOf("Breakfast") }
    var totalTime by remember { mutableStateOf("") }
    var cookingSkill by remember { mutableStateOf("Beginner") }
    var budget by remember { mutableStateOf("0 - 10") }
    var steps by remember { mutableStateOf("") }
    var showResetDialog by remember { mutableStateOf(false) }

    val ingredients = remember { mutableStateListOf(IngredientInputState()) }
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    // 🌟 Validation logic: All fields must be filled and valid
    val isFormValid by remember {
        derivedStateOf {
            recipeName.isNotBlank() && recipeName.length <= 30 &&
            // 🌟 Description and Image are now optional, so they are removed from validation
            totalTime.isNotBlank() &&
            (totalTime.toIntOrNull() ?: 0) in 1..1440 && // 🌟 Time must be between 1 and 1440 mins (24h)
            totalTime.toIntOrNull() != null &&
            steps.isNotBlank() &&
            ingredients.isNotEmpty() &&
            ingredients.all { it.name.isNotBlank() && it.quantity.isNotBlank() && it.quantity.toDoubleOrNull() != null }
        }
    }

    val totalCalories by remember {
        derivedStateOf {
            ingredients.sumOf { input ->
                val qty = input.quantity.toDoubleOrNull() ?: 0.0
                // 🌟 FIX: Match by BOTH name and unit to handle duplicates like Flour Tortilla
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
    }

    val view = LocalView.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.bookmarkMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.addRecipeSuccess.collect { success ->
            if (success) {
                // 🌟 SUCCESS: Small delay so they can see the message
                delay(800)
                viewModel.activeTab = 1
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
                title = { Text("Add Recipe", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = { 
                        // 🌟 BACK: ensure we go back to "My Recipes" tab
                        viewModel.activeTab = 1
                        navController.popBackStack() 
                    }) {
                        Icon(painterResource(id = R.drawable.ic_arrowback), "Back", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        }
    ) { paddingValues ->
        if (!viewModel.isNetworkAvailable) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                OfflinePlaceholder(message = stringResource(R.string.desc_connect_internet_recipe))
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
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            painter = painterResource(id = R.drawable.upload),
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Upload Recipe Image", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
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
                            // 🌟 FIX: Allow ONLY digits for Total Time (No decimals allowed)
                            if (input.all { it.isDigit() }) {
                                totalTime = input
                            }
                        },
                        placeholder = "e.g. 30",
                        // 🌟 Declared directly inside here
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        trailingIcon = { Icon(painterResource(id = R.drawable.ic_clock), null, modifier = Modifier.size(20.dp)) }
                    )
                    // 🌟 Added time limit hint
                    if (totalTime.isNotEmpty() && (totalTime.toIntOrNull() ?: 0) > 1440) {
                        Text(
                            text = "Time cannot exceed 24 hours.",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
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
                TextButton(onClick = { showResetDialog = true }) {
                    Text("Reset Ingredients", color = Color.Red, fontWeight = FontWeight.Bold)
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
                // 🌟 FIX: Show only a clean, simple message and remove the "red wall" of technical text
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

            // 🌟 Form Validation Message
            if (!isFormValid) {
                Text(
                    text = "Please fill in all fields with valid information.",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 8.dp).align(Alignment.CenterHorizontally)
                )
            }

            Button(
                onClick = {
                    val nextId = viewModel.generateNextRecipeId()
                    
                    var imageBytes: ByteArray? = null
                    if (imageBitmap != null) {
                        val stream = ByteArrayOutputStream()
                        imageBitmap!!.compress(Bitmap.CompressFormat.JPEG, 80, stream)
                        imageBytes = stream.toByteArray()
                    }

                    val recipe = Recipe(
                        recipe_id = nextId,
                        // 🌟 FIX: Use the short customId (U001) to match your search logic
                        author_id = authViewModel.currentUser?.customId,
                        recipeName = recipeName,
                        recipeDescription = description,
                        recipeCourse = course,
                        time = totalTime.toIntOrNull() ?: 0,
                        calories = totalCalories,
                        cookingSkill = cookingSkill,
                        estimatedBudget = budget,
                        recipeStep = steps,
                        recipeImageUrl = null,
                        ingredients = ingredients.map {
                            IngredientItem(
                                name = it.name,
                                quantity = JsonPrimitive(it.quantity),
                                unit = it.unit
                            )
                        }
                    )
                    viewModel.addRecipe(recipe, imageBytes)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), // 🌟 Themed Gray
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)   // 🌟 Themed Text
                ),
                enabled = isFormValid && !viewModel.isLoading
            ) {
                if (viewModel.isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("ADD RECIPE", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Clear Ingredients?") },
            text = { Text("Are you sure you want to clear all the ingredients you've entered?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        ingredients.clear()
                        ingredients.add(IngredientInputState())
                        showResetDialog = false
                    }
                ) {
                    Text("Yes", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("No", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun LabelText(text: String) {
    Text(
        text = text,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)
    )
}

@Composable
fun AddRecipeTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    readOnly: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default, // 🌟 Accept standard options
    trailingIcon: @Composable (() -> Unit)? = null
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, fontSize = 14.sp, color = Color.Gray) },
        modifier = modifier.fillMaxWidth().then(if (singleLine) Modifier.height(52.dp) else Modifier),
        singleLine = singleLine,
        readOnly = readOnly,
        keyboardOptions = keyboardOptions, // 🌟 Pass it through
        shape = RoundedCornerShape(12.dp),
        trailingIcon = trailingIcon,
        textStyle = TextStyle(fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface),
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownField(value: String, options: List<String>, onSelected: (String) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier.fillMaxWidth()
    ) {
        TextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            textStyle = TextStyle(fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true),
            shape = RoundedCornerShape(12.dp),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngredientRow(
    item: IngredientInputState,
    availableIngredients: List<Ingredient>,
    onRemove: () -> Unit,
    onUpdate: (IngredientInputState) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Name", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(bottom = 4.dp))
            var nameExpanded by remember { mutableStateOf(false) }
            
            val filteredIngredients = remember(item.name, availableIngredients) {
                availableIngredients
                    .filter { it.name?.contains(item.name, ignoreCase = true) == true }
                    .take(50)
            }

            ExposedDropdownMenuBox(
                expanded = nameExpanded && filteredIngredients.isNotEmpty(),
                onExpandedChange = { nameExpanded = it }
            ) {
                TextField(
                    value = item.name,
                    onValueChange = { 
                        onUpdate(item.copy(name = it))
                        nameExpanded = true 
                    },
                    placeholder = { Text("e.g. Flour", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    textStyle = TextStyle(fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = nameExpanded) },
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                
                ExposedDropdownMenu(
                    expanded = nameExpanded,
                    onDismissRequest = { nameExpanded = false },
                    modifier = Modifier.heightIn(max = 300.dp).background(MaterialTheme.colorScheme.surface)
                ) {
                    filteredIngredients.forEach { ingredient ->
                        DropdownMenuItem(
                            text = { 
                                Column {
                                    Text(ingredient.name ?: "Unknown", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                                    if (ingredient.defaultUnit != null) {
                                        Text("Unit: ${ingredient.defaultUnit}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            },
                            onClick = {
                                onUpdate(item.copy(
                                    name = ingredient.name ?: "",
                                    unit = ingredient.defaultUnit ?: "pieces"
                                ))
                                nameExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Quantity", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(bottom = 4.dp))
                    var qtyExpanded by remember { mutableStateOf(false) }
                    
                    TextField(
                        value = item.quantity,
                        onValueChange = { input ->
                            // 🌟 FIX: Allow only digits and a SINGLE decimal point
                            if (input.all { it.isDigit() || it == '.' } && input.count { it == '.' } <= 1) {
                                onUpdate(item.copy(quantity = input))
                            }
                        },
                        placeholder = { Text("0", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        singleLine = true,
                        // 🌟 Declared directly inside here
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = TextStyle(fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface),
                        trailingIcon = {
                            IconButton(onClick = { qtyExpanded = true }) {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = qtyExpanded)
                            }
                            DropdownMenu(expanded = qtyExpanded, onDismissRequest = { qtyExpanded = false }, modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                                listOf("1/2", "1/4", "3/4").forEach { fraction ->
                                    DropdownMenuItem(
                                        text = { Text(fraction, color = MaterialTheme.colorScheme.onSurface) },
                                        onClick = {
                                            val current = item.quantity.toDoubleOrNull() ?: 0.0
                                            val add = when(fraction) {
                                                "1/2" -> 0.5
                                                "1/4" -> 0.25
                                                "3/4" -> 0.75
                                                else -> 0.0
                                            }
                                            onUpdate(item.copy(quantity = (current + add).toString()))
                                            qtyExpanded = false
                                        }
                                    )
                                }
                            }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text("Unit", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(bottom = 4.dp))
                    AddRecipeTextField(
                        value = item.unit,
                        onValueChange = { },
                        placeholder = "-",
                        readOnly = true
                    )
                }
            }
        }

        IconButton(
            onClick = onRemove, 
            modifier = Modifier.padding(start = 8.dp, top = 20.dp).size(32.dp)
        ) {
            Icon(painterResource(id = R.drawable.ic_remove), "Remove", tint = MaterialTheme.colorScheme.onBackground)
        }
    }
}

data class IngredientInputState(
    val name: String = "",
    val quantity: String = "",
    val unit: String = "pieces"
)
