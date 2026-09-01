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
import com.example.foodieheal.ui.components.getHighlightedText
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.unit.IntOffset
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
    var visibility by remember { mutableStateOf("public") }
    val courseOptions = listOf("Breakfast", "Lunch", "Dinner", "Snack")
    val visibilityOptions = listOf("public", "followers", "private")
    val cookingSkillOptions = listOf("Beginner", "Intermediate", "Master/Expert")
    var totalTime by remember { mutableStateOf("") }
    var cookingSkill by remember { mutableStateOf("Beginner") }
    var budget by remember { mutableStateOf("0 - 10") }
    var steps by remember { mutableStateOf("") }
    var showResetDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }

    val ingredients = remember { mutableStateListOf(IngredientInputState()) }
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    // 🌟 Validation logic: All fields must be filled and valid
    val isFormValid by remember {
        derivedStateOf {
            recipeName.isNotBlank() && recipeName.length <= 30 &&
            description.length <= 150 &&
            // 🌟 Description and Image are now optional, so they are removed from validation
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
        viewModel.fetchAvailableIngredients()
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
                title = { Text(stringResource(R.string.title_add_recipe), color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
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
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)), RoundedCornerShape(16.dp))
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
            OutlinedTextField(
                value = description,
                onValueChange = { 
                    val newlineCount = it.count { char -> char == '\n' }
                    if (it.length <= 150 && newlineCount <= 4) {
                        description = it 
                    }
                },
                placeholder = { Text(stringResource(R.string.hint_description), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) },
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
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
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
                            // 🌟 FIX: Allow ONLY digits for Total Time (No decimals allowed)
                            if (input.all { it.isDigit() }) {
                                totalTime = input
                            }
                        },
                        placeholder = stringResource(R.string.hint_total_time),
                        // 🌟 Declared directly inside here
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        trailingIcon = { Icon(painterResource(id = R.drawable.ic_clock), null, modifier = Modifier.size(20.dp)) }
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LabelText(stringResource(R.string.label_ingredients_list))
                    Spacer(modifier = Modifier.width(4.dp))
                    Box {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_help),
                            contentDescription = "Help",
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { showHelpDialog = true },
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                        if (showHelpDialog) {
                            Popup(
                                alignment = Alignment.TopStart,
                                offset = IntOffset(x = -100, y = 40),
                                onDismissRequest = { showHelpDialog = false },
                                properties = PopupProperties(focusable = true)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                    shadowElevation = 8.dp,
                                    modifier = Modifier.width(300.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = stringResource(R.string.help_ingredient_title),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = stringResource(R.string.help_ingredient_desc),
                                            fontSize = 11.sp,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                TextButton(onClick = { showResetDialog = true }) {
                    Text(stringResource(R.string.btn_reset_ingredients), color = Color.Red, fontWeight = FontWeight.Bold)
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
            OutlinedTextField(
                value = steps,
                onValueChange = { 
                    val newlineCount = it.count { char -> char == '\n' }
                    if (it.length <= 1000 && newlineCount <= 19) {
                        steps = it 
                    }
                },
                placeholder = { Text(stringResource(R.string.hint_recipe_steps), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) },
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
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
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
                                modifier = Modifier.padding(vertical = 8.dp), // 🌟 Increased padding for bigger chip
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
                // 🌟 FIX: Show only a clean, simple message and remove the "red wall" of technical text
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

            // 🌟 Form Validation Message
            if (!isFormValid) {
                Text(
                    text = stringResource(R.string.msg_fill_all_fields),
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
                        authorName = authViewModel.currentUser?.name, // 🌟 Save author info for offline
                        authorImageUrl = authViewModel.currentUser?.profilePicUrl,
                        recipeName = recipeName,
                        recipeDescription = description,
                        recipeCourse = course,
                        visibility = visibility,
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
                    Text(stringResource(R.string.btn_add_recipe_submit), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(R.string.dialog_clear_ingredients_title)) },
            text = { Text(stringResource(R.string.dialog_clear_ingredients_msg)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        ingredients.clear()
                        ingredients.add(IngredientInputState())
                        showResetDialog = false
                    }
                ) {
                    Text(stringResource(R.string.dialog_yes), color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(R.string.dialog_cancel), color = Color.Gray)
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
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) },
        modifier = modifier.fillMaxWidth().then(if (singleLine) Modifier.height(52.dp) else Modifier),
        singleLine = singleLine,
        readOnly = readOnly,
        keyboardOptions = keyboardOptions, // 🌟 Pass it through
        shape = RoundedCornerShape(12.dp),
        trailingIcon = trailingIcon,
        textStyle = TextStyle(fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownField(
    value: String, 
    options: List<String>, 
    onSelected: (String) -> Unit, 
    modifier: Modifier = Modifier,
    labelProvider: @Composable (String) -> String = { it }
) {
    var expanded by remember { mutableStateOf(false) }
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = labelProvider(value),
            onValueChange = {},
            readOnly = true,
            textStyle = TextStyle(fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true),
            shape = RoundedCornerShape(12.dp),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
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
                    text = { Text(labelProvider(option), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface) },
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
            Text(stringResource(R.string.label_ingredient_name), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(bottom = 4.dp))
            var nameExpanded by remember { mutableStateOf(false) }
            var searchQuery by remember(item.name) { mutableStateOf(item.name) }

            val filteredIngredients = remember(searchQuery, availableIngredients) {
                if (searchQuery.isBlank()) {
                    availableIngredients.take(50)
                } else {
                    availableIngredients
                        .filter { 
                            it.name?.contains(searchQuery, ignoreCase = true) == true ||
                            it.description?.contains(searchQuery, ignoreCase = true) == true
                        }
                        .take(50)
                }
            }

            val isValidSelection = remember(item.name, availableIngredients) {
                availableIngredients.any { it.name?.equals(item.name, ignoreCase = true) == true }
            }

            ExposedDropdownMenuBox(
                expanded = nameExpanded,
                onExpandedChange = { nameExpanded = it }
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { input -> 
                        searchQuery = input
                        nameExpanded = true 
                        val exactMatch = availableIngredients.find { it.name?.equals(input.trim(), ignoreCase = true) == true }
                        if (exactMatch != null) {
                            onUpdate(item.copy(
                                name = exactMatch.name ?: "",
                                unit = exactMatch.defaultUnit ?: "pieces"
                            ))
                        } else {
                            onUpdate(item.copy(
                                name = input,
                                unit = "-"
                            ))
                        }
                    },
                    placeholder = { Text(stringResource(R.string.hint_ingredient_name), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true)
                        .onFocusEvent { focusState ->
                            if (!focusState.isFocused && !isValidSelection) {
                                val confirmedMatch = availableIngredients.find { it.name?.equals(item.name, ignoreCase = true) == true }
                                if (confirmedMatch == null) {
                                    searchQuery = ""
                                    onUpdate(item.copy(name = "", unit = "-"))
                                } else {
                                    searchQuery = confirmedMatch.name ?: ""
                                }
                            }
                        },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    textStyle = TextStyle(fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = nameExpanded) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                
                ExposedDropdownMenu(
                    expanded = nameExpanded,
                    onDismissRequest = { 
                        nameExpanded = false 
                        if (!isValidSelection) {
                            val confirmedMatch = availableIngredients.find { it.name?.equals(item.name, ignoreCase = true) == true }
                            if (confirmedMatch == null) {
                                searchQuery = ""
                                onUpdate(item.copy(name = "", unit = "-"))
                            } else {
                                searchQuery = confirmedMatch.name ?: ""
                            }
                        }
                    },
                    modifier = Modifier.heightIn(max = 300.dp).background(MaterialTheme.colorScheme.surface)
                ) {
                    if (filteredIngredients.isEmpty()) {
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    stringResource(R.string.msg_no_matching_ingredients), 
                                    fontSize = 13.sp, 
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                ) 
                            },
                            onClick = { },
                            enabled = false
                        )
                    } else {
                        filteredIngredients.forEachIndexed { index, ingredient ->
                            DropdownMenuItem(
                                text = { 
                                    Column {
                                        Text(
                                            text = getHighlightedText(ingredient.name ?: "Unknown", searchQuery),
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (!ingredient.description.isNullOrBlank()) {
                                            Text(
                                                text = getHighlightedText(ingredient.description, searchQuery),
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                                maxLines = 2 // Increased maxLines to show more context
                                            )
                                        }
                                        if (ingredient.defaultUnit != null) {
                                            Text(
                                                text = stringResource(R.string.label_unit_prefix, ingredient.defaultUnit), 
                                                fontSize = 11.sp, 
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    val selectedName = ingredient.name ?: ""
                                    searchQuery = selectedName
                                    onUpdate(item.copy(
                                        name = selectedName,
                                        unit = ingredient.defaultUnit ?: "pieces"
                                    ))
                                    nameExpanded = false
                                }
                            )
                            if (index < filteredIngredients.size - 1) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(4.dp),
                                    thickness = 1.dp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.label_ingredient_qty), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(bottom = 4.dp))
                    var qtyExpanded by remember { mutableStateOf(false) }
                    
                    OutlinedTextField(
                        value = item.quantity,
                        onValueChange = { input ->
                            // 🌟 FIX: Allow only digits and a SINGLE decimal point
                            if (input.all { it.isDigit() || it == '.' } && input.count { it == '.' } <= 1) {
                                onUpdate(item.copy(quantity = input))
                            }
                        },
                        placeholder = { Text(stringResource(R.string.placeholder_zero), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) },
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
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.label_ingredient_unit), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(bottom = 4.dp))
                    AddRecipeTextField(
                        value = item.unit,
                        onValueChange = { },
                        placeholder = stringResource(R.string.placeholder_hyphen),
                        readOnly = true
                    )
                }
            }
        }

        IconButton(
            onClick = onRemove, 
            modifier = Modifier.padding(start = 8.dp, top = 20.dp).size(32.dp)
        ) {
            Icon(painterResource(id = R.drawable.ic_remove), stringResource(R.string.remove), tint = MaterialTheme.colorScheme.onBackground)
        }
    }
}

data class IngredientInputState(
    val name: String = "",
    val quantity: String = "",
    val unit: String = "pieces"
)
