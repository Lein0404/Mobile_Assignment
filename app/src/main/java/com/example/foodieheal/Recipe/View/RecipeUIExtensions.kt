package com.example.foodieheal.Recipe.View

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodieheal.R
import com.example.foodieheal.Recipe.Model.Ingredient
import com.example.foodieheal.ui.components.getHighlightedText

@Composable
fun getCourseDisplay(course: String): String {
    return when (course) {
        "All" -> stringResource(R.string.course_all)
        "Breakfast" -> stringResource(R.string.breakfast)
        "Lunch" -> stringResource(R.string.lunch)
        "Dinner" -> stringResource(R.string.dinner)
        "Snack" -> stringResource(R.string.snack)
        else -> course
    }
}

@Composable
fun getCookingSkillDisplay(skill: String): String {
    return when (skill) {
        "Beginner" -> stringResource(R.string.skill_beginner)
        "Intermediate" -> stringResource(R.string.skill_intermediate)
        "Master/Expert" -> stringResource(R.string.skill_master_expert)
        else -> skill
    }
}

@Composable
fun getBudgetDisplay(budget: String): String {
    return when (budget) {
        "0 - 20" -> stringResource(R.string.budget_range_0_20)
        "20 - 40" -> stringResource(R.string.budget_range_20_40)
        "40 - 60" -> stringResource(R.string.budget_range_40_60)
        "60 - 80" -> stringResource(R.string.budget_range_60_80)
        "80 - 100" -> stringResource(R.string.budget_range_80_100)
        else -> budget
    }
}

@Composable
fun getVisibilityDisplay(visibility: String): String {
    return when (visibility.lowercase()) {
        "public" -> stringResource(R.string.visibility_public)
        "followers" -> stringResource(R.string.visibility_followers)
        "private" -> stringResource(R.string.visibility_private)
        else -> visibility
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
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, fontSize = 14.sp, color = Color.Gray) },
        modifier = modifier.fillMaxWidth().then(if (singleLine) Modifier.height(52.dp) else Modifier),
        singleLine = singleLine,
        readOnly = readOnly,
        keyboardOptions = keyboardOptions,
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
fun <T> DropdownField(
    value: T,
    options: List<T>,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    labelTransformation: @Composable (T) -> String = { it.toString() }
) {
    var expanded by remember { mutableStateOf(false) }
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier.fillMaxWidth()
    ) {
        TextField(
            value = labelTransformation(value),
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
                    text = { Text(labelTransformation(option), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface) },
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
                TextField(
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
                    placeholder = { Text("e.g. Flour", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
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
                                    "No matching ingredients found", 
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
                                                maxLines = 2
                                            )
                                        }
                                        if (ingredient.defaultUnit != null) {
                                            Text(
                                                text = "Unit: ${ingredient.defaultUnit}", 
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
                    Text("Quantity", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(bottom = 4.dp))
                    var qtyExpanded by remember { mutableStateOf(false) }
                    
                    TextField(
                        value = item.quantity,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() || it == '.' } && input.count { it == '.' } <= 1) {
                                onUpdate(item.copy(quantity = input))
                            }
                        },
                        placeholder = { Text("0", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        singleLine = true,
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
