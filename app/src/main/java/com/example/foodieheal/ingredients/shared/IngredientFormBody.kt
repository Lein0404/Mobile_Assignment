package com.example.foodieheal.ingredients.shared

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.coroutines.launch
import com.example.foodieheal.Cloudinary.CloudinaryUploadScreen
import com.example.foodieheal.Cloudinary.CloudinaryUploadViewModel
import com.example.foodieheal.R
import com.example.foodieheal.ingredients.model.IngredientCategory
import com.example.foodieheal.ingredients.model.Units
import com.example.foodieheal.ui.components.CommonInputField
import com.kanyidev.searchable_dropdown.LargeSearchableDropdownMenu

/**
 * Shared, reusable form body for ingredient creation/editing/review screens.
 *
 * Renders: Image upload → Name → Category → Description → UnitRows → Add unit button,
 * then yields control to [bottomContent] for screen-specific buttons, error messages, etc.
 *
 * Used by AdminAddIngredientScreen, AdminIngredientRequestFormScreen,
 * and IngredientRequestFormScreen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColumnScope.IngredientFormBody(
    // CloudinaryUpload
    cloudinaryViewModel: CloudinaryUploadViewModel,

    // Form state (read-only for the composable)
    ingredientName: String,
    category: IngredientCategory?,
    description: String,
    unitRows: List<UnitRowState>,
    availableUnits: List<Units>,

    // Validation errors
    nameError: Int?,
    nameErrorArg: String? = null,
    categoryError: Int?,
    descriptionError: Int?,
    unitRowsError: Int?,

    // Category dropdown placeholder (differs between screens)
    categoryPlaceholder: String,

    // Callbacks
    onNameChange: (String) -> Unit,
    onCategoryChange: (IngredientCategory) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onUnitRowUpdate: (Int, Units?, String) -> Unit,
    onUnitRowRemove: (Int) -> Unit,
    onAddUnitRow: () -> Unit,

    // Screen-specific bottom section (button, inline error, spacing, etc.)
    bottomContent: @Composable ColumnScope.() -> Unit
) {
    // 1. Cloudinary Upload
    CloudinaryUploadScreen(viewModel = cloudinaryViewModel)
    Spacer(Modifier.height(dimensionResource(id = R.dimen.padding_l)))

    // 2. Ingredient Name
    CommonInputField(
        value = ingredientName,
        onValueChange = { if (it.length <= 40) onNameChange(it) },
        textId = R.string.ingredient_name,
        modifier = Modifier.fillMaxWidth(),
        isError = nameError != null,
        supportingText = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    nameError?.let { resId ->
                        val errorMsg = if (nameErrorArg != null) {
                            stringResource(id = resId, nameErrorArg)
                        } else {
                            stringResource(id = resId)
                        }
                        Text(
                            text = errorMsg,
                            modifier = Modifier.offset(x = -dimensionResource(id = R.dimen.padding_l)),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.ingredient_form_name_char_count_limit, ingredientName.length),
                    modifier = Modifier.offset(x = dimensionResource(id = R.dimen.padding_l)),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (ingredientName.length >= 40) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
    Spacer(Modifier.height(dimensionResource(id = R.dimen.padding_l)))

    // 3. Category
    LargeSearchableDropdownMenu(
        modifier = Modifier.fillMaxWidth().padding(top = dimensionResource(id = R.dimen.padding_xxsm)),
        title = stringResource(R.string.category),
        fieldLabelTextStyle = MaterialTheme.typography.bodyLarge,
        selectedOption = category,
        onItemSelected = onCategoryChange,
        selectedItemToString = { it.categoryName },
        placeholder = categoryPlaceholder,
        placeholderTextStyle = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
        ),
        textStyle = MaterialTheme.typography.bodyLarge,
        options = IngredientCategory.entries,
        drawItem = { item, selected, itemEnabled, onClick ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = itemEnabled, onClick = onClick)
                    .padding(
                        horizontal = dimensionResource(id = R.dimen.padding_l),
                        vertical = dimensionResource(id = R.dimen.padding_md)
                    )
            ) {
                Text(
                    text = item.categoryName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            errorContainerColor = MaterialTheme.colorScheme.background,
        ),
        isError = categoryError != null
    )
    categoryError?.let { resId ->
        Text(
            text = stringResource(id = resId),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = dimensionResource(id = R.dimen.padding_xxsm))
        )
    }
    Spacer(Modifier.height(dimensionResource(id = R.dimen.padding_l)))

    // 4. Description
    CommonInputField(
        value = description,
        onValueChange = { if (it.length <= 150) onDescriptionChange(it) },
        textId = R.string.description,
        modifier = Modifier
            .height(dimensionResource(R.dimen.large_OutlinedTextField_size))
            .fillMaxWidth(),
        singleLine = false,
        maxLines = 8,
        isError = descriptionError != null,
        supportingText = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    descriptionError?.let { resId ->
                        Text(
                            text = stringResource(id = resId),
                            modifier = Modifier.offset(x = -dimensionResource(id = R.dimen.padding_l)),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.ingredient_form_desc_char_count_limit, description.length),
                    modifier = Modifier.offset(x = dimensionResource(id = R.dimen.padding_l)),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (description.length >= 150) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
    Spacer(Modifier.height(dimensionResource(id = R.dimen.padding_l)))

    // 5. Calorie Information
    Text(
        text = stringResource(R.string.calorie_information),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onBackground
    )
    unitRowsError?.let { resId ->
        Text(
            text = stringResource(id = resId),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = dimensionResource(id = R.dimen.padding_xxsm))
        )
    }
    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.padding_xsm)))

    if (availableUnits.isNotEmpty()) {
        unitRows.forEachIndexed { index, row ->
            UnitRow(
                index = index,
                selectedUnit = row.selectedUnit,
                calories = row.calories,
                availableUnits = availableUnits,
                onUpdate = { unit, cal -> onUnitRowUpdate(index, unit, cal) },
                onRemove = if (unitRows.size > 1) { { onUnitRowRemove(index) } } else null,
                unitError = row.unitError,
                caloriesError = row.caloriesError
            )
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = dimensionResource(id = R.dimen.padding_l)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(dimensionResource(R.dimen.icon_large_size))
            )
        }
    }

    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.padding_xsm)))
    TextButton(
        onClick = onAddUnitRow,
        modifier = Modifier.align(Alignment.CenterHorizontally)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.padding_sm))
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_outline_add),
                contentDescription = null,
                modifier = Modifier.size(dimensionResource(R.dimen.icon_medium_size))
            )
            Text(stringResource(R.string.ingredient_form_add_unit))
        }
    }

    // 6. Screen-specific bottom content (buttons, inline errors, etc.)
    bottomContent()
}

@Composable
fun UnitRow(
    index: Int,
    selectedUnit: Units?,
    calories: String,
    availableUnits: List<Units>,
    onUpdate: (Units?, String) -> Unit,
    onRemove: (() -> Unit)?,
    unitError: Int? = null,
    caloriesError: Int? = null
) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = dimensionResource(id = R.dimen.padding_xsm)),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.padding_smd)),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.serving_unit),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
            LargeSearchableDropdownMenu(
                modifier = Modifier.fillMaxWidth(),
                selectedOption = selectedUnit,
                onItemSelected = { onUpdate(it, calories) },
                selectedItemToString = { it.unitName },
                placeholder = stringResource(R.string.ingredient_form_serving_unit_placeholder),
                placeholderTextStyle = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                ),
                options = availableUnits,
                drawItem = { item, selected, itemEnabled, onClick ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = itemEnabled, onClick = onClick)
                            .padding(
                                horizontal = dimensionResource(id = R.dimen.padding_l),
                                vertical = dimensionResource(id = R.dimen.padding_md)
                            )
                    ) {
                        Text(
                            text = item.unitName,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                ),
                isError = unitError != null
            )
            if (unitError != null) {
                Text(
                    text = stringResource(unitError),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = dimensionResource(id = R.dimen.padding_xxsm))
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.calories),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
            val placeholderText = if (selectedUnit != null) {
                stringResource(
                    R.string.ingredient_form_calories_placeholder_per,
                    selectedUnit.defaultQuantity.toInt(),
                    selectedUnit.unitDisplay
                )
            } else {
                stringResource(R.string.ingredient_form_calories_placeholder_default)
            }
            OutlinedTextField(
                value = calories,
                onValueChange = { newValue ->
                    // Allow digits and optional decimal point so users can type or paste decimals (e.g. 123.45)
                    if (newValue.isEmpty() || newValue.matches(Regex("""^\d*\.?\d*$"""))) {
                        onUpdate(selectedUnit, newValue)
                    }
                },
                placeholder = {
                    Text(
                        text = placeholderText,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewRequester(bringIntoViewRequester)
                    .onFocusEvent { focusState ->
                        if (focusState.isFocused) {
                            coroutineScope.launch {
                                bringIntoViewRequester.bringIntoView()
                            }
                        }
                    },
                isError = caloriesError != null,
                shape = RoundedCornerShape(dimensionResource(id = R.dimen.padding_smd)),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    errorContainerColor = MaterialTheme.colorScheme.background,
                ),
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Decimal
                )
            )
            if (caloriesError != null) {
                Text(
                    text = stringResource(caloriesError),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = dimensionResource(id = R.dimen.padding_xxsm))
                )
            }
        }

        if (onRemove != null) {
            IconButton(
                onClick = onRemove,
                modifier = Modifier.padding(top = dimensionResource(id = R.dimen.padding_l))
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_remove),
                    contentDescription = stringResource(R.string.ingredient_form_remove_unit)
                )
            }
        }
    }
}