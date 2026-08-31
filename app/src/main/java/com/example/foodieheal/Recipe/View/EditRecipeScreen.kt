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
import coil.compose.AsyncImage
import com.example.foodieheal.R
import com.example.foodieheal.Recipe.Model.Recipe
import com.example.foodieheal.Recipe.Model.Ingredient
import com.example.foodieheal.Recipe.Model.IngredientItem
import kotlinx.serialization.json.*
import com.example.foodieheal.Recipe.viewModel.RecipeViewModel
import com.example.foodieheal.User.viewModel.AuthViewModel
import com.example.foodieheal.meal_planner.screen.OfflinePlaceholder
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
    var visibility by remember { mutableStateOf("public") }

    val courseOptions = listOf("Breakfast", "Lunch", "Dinner", "Snack")
    val visibilityOptions = listOf("public", "followers", "private")
    val cookingSkillOptions = listOf("Beginner", "Intermediate", "Master/Expert")

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
            visibility = r.visibility
            totalTime = r.time.toString()
            cookingSkill = r.cookingSkill
            budget = r.estimatedBudget
            steps = r.recipeStep

            ingredients.clear()
            r.ingredients.forEach { 
                ingredients.add(IngredientInputState(it.name, it.displayQuantity, it.unit))
            }
        }
    }

    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    val isFormValid by remember {
        derivedStateOf {
            recipeName.isNotBlank() && recipeName.length <= 30 &&
            description.length <= 150 &&
            totalTime.isNotBlank() &&
            (totalTime.toIntOrNull() ?: 0) in 1..1440 && // 🌟 Time must be between 1 and 1440 mins (24h)
            totalTime.toIntOrNull() != null &&
            steps.isNotBlank() && steps.length <= 1000 &&
            ingredients.isNotEmpty() &&
            ingredients.all { input ->
                input.name.isNotBlank() &&
                input.quantity.isNotBlank() &&
                input.quantity.toDoubleOrNull() != null &&
                viewModel.availableIngredients.any { it.name?.equals(input.name, ignoreCase = true) == true }
            }
        }
    }

    val totalCalories by remember {
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
    }

    val view = LocalView.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.fetchAvailableIngredients()
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
        snackbarHost = { 
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.title_edit_recipe), color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = { 
                        // 🌟 BACK: ensure we go back to "My Recipes" tab
                        viewModel.activeTab = 1
                        navController.popBackStack() 
                    }) {
                        Icon(painterResource(id = R.drawable.ic_arrowback), stringResource(R.string.back_button), tint = MaterialTheme.colorScheme.onPrimary)
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
        } else if (existingRecipe == null) {
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
                            Text(stringResource(R.string.label_upload_image), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LabelText(stringResource(R.string.label_recipe_name))
                AddRecipeTextField(
                    value = recipeName,
                    onValueChange = { if (it.length <= 30) recipeName = it },
                    placeholder = stringResource(R.string.hint_recipe_name)
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

                LabelText(stringResource(R.string.label_description_optional))
                TextField(
                    value = description,
                    onValueChange = { 
                        val newlineCount = it.count { char -> char == '\n' }
                        if (it.length <= 150 && newlineCount <= 4) {
                            description = it 
                        }
                    },
                    placeholder = { Text(stringResource(R.string.hint_description), fontSize = 14.sp, color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = TextStyle(fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface),
                    supportingText = {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            val newlineCount = description.count { char -> char == '\n' }
                            Text(stringResource(R.string.label_recipe_lines_format, newlineCount + 1), fontSize = 12.sp, color = if (newlineCount >= 4) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(stringResource(R.string.label_recipe_chars_format, description.length, 150), fontSize = 12.sp, color = if (description.length >= 150) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
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

                LabelText(stringResource(R.string.label_course_type))
                DropdownField(
                    value = course,
                    options = courseOptions,
                    onSelected = { course = it },
                    labelProvider = { key ->
                        when(key) {
                            "Breakfast" -> stringResource(R.string.breakfast)
                            "Lunch" -> stringResource(R.string.lunch)
                            "Dinner" -> stringResource(R.string.dinner)
                            "Snack" -> stringResource(R.string.snack)
                            else -> key
                        }
                    }
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        LabelText(stringResource(R.string.label_total_time_min))
                        AddRecipeTextField(
                            value = totalTime,
                            onValueChange = { input ->
                                if (input.all { it.isDigit() }) {
                                    totalTime = input
                                }
                            },
                            placeholder = stringResource(R.string.hint_total_time),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            trailingIcon = {
                                Icon(
                                    painterResource(id = R.drawable.ic_clock),
                                    null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )
                        // 🌟 Added time limit hint
                        if (totalTime.isNotEmpty() && (totalTime.toIntOrNull() ?: 0) > 1440) {
                            Text(
                                text = stringResource(R.string.error_time_limit),
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        LabelText(stringResource(R.string.label_calories_kcal))
                        AddRecipeTextField(
                            value = "$totalCalories",
                            onValueChange = { },
                            readOnly = true,
                            placeholder = stringResource(R.string.placeholder_zero)
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        LabelText(stringResource(R.string.label_cooking_skill_level))
                        DropdownField(
                            value = cookingSkill,
                            options = cookingSkillOptions,
                            onSelected = { cookingSkill = it },
                            labelProvider = { key ->
                                when(key) {
                                    "Beginner" -> stringResource(R.string.skill_beginner)
                                    "Intermediate" -> stringResource(R.string.skill_intermediate)
                                    "Master/Expert" -> stringResource(R.string.skill_expert)
                                    else -> key
                                }
                            }
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        LabelText(stringResource(R.string.label_budget_rm_range))
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
                    LabelText(stringResource(R.string.label_ingredients_list))
                    TextButton(onClick = { ingredients.clear(); ingredients.add(IngredientInputState()) }) {
                        Text(stringResource(R.string.btn_reset_ingredients), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
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
                    Text(stringResource(R.string.btn_add_ingredient), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }

                LabelText(stringResource(R.string.label_recipe_steps))
                Text(
                    text = stringResource(R.string.tip_recipe_steps),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                TextField(
                    value = steps,
                    onValueChange = { 
                        val newlineCount = it.count { char -> char == '\n' }
                        if (it.length <= 1000 && newlineCount <= 19) {
                            steps = it 
                        }
                    },
                    placeholder = { Text(stringResource(R.string.hint_recipe_steps), fontSize = 14.sp, color = Color.Gray) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                        .onFocusEvent { focusState ->
                            if (focusState.isFocused) {
                                coroutineScope.launch {
                                    bringIntoViewRequester.bringIntoView()
                                }
                            }
                        }
                        .bringIntoViewRequester(bringIntoViewRequester),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = TextStyle(fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface),
                    supportingText = {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            val newlineCount = steps.count { char -> char == '\n' }
                            Text(stringResource(R.string.label_recipe_steps_format, newlineCount + 1), fontSize = 12.sp, color = if (newlineCount >= 19) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(stringResource(R.string.label_recipe_chars_format, steps.length, 1000), fontSize = 12.sp, color = if (steps.length >= 1000) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
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

                LabelText(stringResource(R.string.label_visibility))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    visibilityOptions.forEach { option ->
                        FilterChip(
                            selected = visibility == option,
                            onClick = { visibility = option },
                            label = { 
                                val label = when(option) {
                                    "public" -> stringResource(R.string.visibility_public)
                                    "followers" -> stringResource(R.string.visibility_followers)
                                    "private" -> stringResource(R.string.visibility_private)
                                    else -> option.replaceFirstChar { it.uppercase() }
                                }
                                Text(
                                    text = label,
                                    modifier = Modifier.padding(vertical = 8.dp), // 🌟 Increased padding
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                ) 
                            },
                            modifier = Modifier.weight(1f).height(48.dp), // 🌟 Set height explicitly
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (viewModel.errorMessage != null) {
                    val cleanError = viewModel.errorMessage!!.split("\n").firstOrNull() ?: stringResource(R.string.error_unknown_occurred)
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
                        text = stringResource(R.string.msg_fill_all_fields),
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
                            visibility = visibility,
                            time = totalTime.toIntOrNull() ?: 0,
                            calories = totalCalories,
                            cookingSkill = cookingSkill,
                            estimatedBudget = budget,
                            recipeStep = steps,
                            ingredients = ingredients.map { IngredientItem(it.name, JsonPrimitive(it.quantity), it.unit) },
                            authorName = authViewModel.currentUser?.name, // 🌟 Refresh author info
                            authorImageUrl = authViewModel.currentUser?.profilePicUrl
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
                        Text(stringResource(R.string.btn_save_changes), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
                
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}
