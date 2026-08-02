package com.example.foodieheal.view

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.navigation.NavController
import com.example.foodieheal.R
import java.io.InputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecipeScreen(navController: NavController) {
    val context = LocalContext.current
    var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var recipeName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var course by remember { mutableStateOf("Breakfast") }
    var totalTime by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("0") }
    var cookingSkill by remember { mutableStateOf("Beginner") }
    var budget by remember { mutableStateOf("RM 0 - 10") }
    var steps by remember { mutableStateOf("") }

    val ingredients = remember { mutableStateListOf(IngredientInput()) }

    val view = LocalView.current
    val primaryColor = MaterialTheme.colorScheme.primary

    SideEffect {
        val window = (view.context as Activity).window
        window.statusBarColor = primaryColor.toArgb()
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
    }

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val inputStream: InputStream? = context.contentResolver.openInputStream(it)
            imageBitmap = BitmapFactory.decodeStream(inputStream)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Add Recipe", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(painterResource(id = R.drawable.ic_arrowback), "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF8F8F8))
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // Image Upload Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFE8E8E8))
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
                            painter = painterResource(id = R.drawable.ic_image),
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Upload Recipe Image", color = Color.Gray, fontSize = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = { imageLauncher.launch("image/*") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("SELECT IMAGE", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            LabelText("Recipe Name")
            AddRecipeTextField(value = recipeName, onValueChange = { recipeName = it }, placeholder = "Recipe Name")

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
                options = listOf("Breakfast", "Lunch", "Dinner"),
                onSelected = { course = it }
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    LabelText("Total Time")
                    AddRecipeTextField(
                        value = totalTime,
                        onValueChange = { totalTime = it },
                        placeholder = "Pick time here",
                        trailingIcon = { Icon(painterResource(id = R.drawable.ic_clock), null, modifier = Modifier.size(20.dp)) }
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    LabelText("Calories")
                    AddRecipeTextField(
                        value = "$calories kcal",
                        onValueChange = { },
                        readOnly = true,
                        placeholder = "0 kcal"
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
                    LabelText("Estimated Budget")
                    DropdownField(
                        value = budget,
                        options = listOf("RM 0 - 10", "RM 10 - 20", "RM 20 - 30", "RM 30 - 40", "RM 40 - 50", "RM 50 - 60", "RM 60 - 70", "RM 70 - 80", "RM 80 - 90", "RM 90 - 100"),
                        onSelected = { budget = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            LabelText("Ingredients")
            
            ingredients.forEachIndexed { index, item ->
                IngredientRow(
                    item = item,
                    onRemove = { if (ingredients.size > 1) ingredients.removeAt(index) },
                    onUpdate = { updated -> ingredients[index] = updated }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            TextButton(
                onClick = { ingredients.add(IngredientInput()) },
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
                modifier = Modifier.height(120.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = { /* Save Logic */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("ADD RECIPE", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun LabelText(text: String) {
    Text(
        text = text,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        color = Color.Black,
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
    trailingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, fontSize = 14.sp, color = Color.Gray) },
        modifier = modifier.fillMaxWidth(),
        singleLine = singleLine,
        readOnly = readOnly,
        shape = RoundedCornerShape(12.dp),
        trailingIcon = trailingIcon,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
            unfocusedContainerColor = Color(0xFFE8E8E8),
            focusedContainerColor = Color(0xFFE8E8E8),
            focusedTextColor = Color.Black,
            unfocusedTextColor = Color.Black
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownField(value: String, options: List<String>, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true),
            shape = RoundedCornerShape(12.dp),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                unfocusedContainerColor = Color(0xFFE8E8E8),
                focusedContainerColor = Color(0xFFE8E8E8),
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun IngredientRow(item: IngredientInput, onRemove: () -> Unit, onUpdate: (IngredientInput) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(modifier = Modifier.weight(1.5f)) {
            Text("Name", fontSize = 12.sp, color = Color.Gray)
            AddRecipeTextField(value = item.name, onValueChange = { onUpdate(item.copy(name = it)) }, placeholder = "e.g. Flour")
        }
        Column(modifier = Modifier.weight(1f)) {
            Text("Quantity", fontSize = 12.sp, color = Color.Gray)
            AddRecipeTextField(value = item.quantity, onValueChange = { onUpdate(item.copy(quantity = it)) }, placeholder = "0")
        }
        Column(modifier = Modifier.weight(1f)) {
            Text("Unit", fontSize = 12.sp, color = Color.Gray)
            AddRecipeTextField(value = item.unit, onValueChange = { onUpdate(item.copy(unit = it)) }, placeholder = "Unit")
        }
        
        IconButton(onClick = onRemove, modifier = Modifier.padding(top = 16.dp)) {
            Icon(painterResource(id = R.drawable.ic_remove), "Remove", tint = Color.Black, modifier = Modifier.size(20.dp))
        }
    }
}

data class IngredientInput(
    val name: String = "",
    val quantity: String = "",
    val unit: String = "pcs"
)
