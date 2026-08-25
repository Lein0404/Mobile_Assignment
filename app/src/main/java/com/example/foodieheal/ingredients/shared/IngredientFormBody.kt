package com.example.foodieheal.ingredients.shared

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.example.foodieheal.Cloudinary.CloudinaryUploadScreen
import com.example.foodieheal.Cloudinary.CloudinaryUploadViewModel
import com.example.foodieheal.R
import com.example.foodieheal.ingredients.model.IngredientCategory
import com.example.foodieheal.ingredients.model.Units
import com.example.foodieheal.ingredients.view.UnitRow
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
        onValueChange = onNameChange,
        textId = R.string.ingredient_name,
        modifier = Modifier.fillMaxWidth(),
        isError = nameError != null
    )
    nameError?.let { resId ->
        Text(
            text = stringResource(id = resId),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = dimensionResource(id = R.dimen.padding_xxsm))
        )
    }
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
        onValueChange = onDescriptionChange,
        textId = R.string.description,
        modifier = Modifier
            .height(dimensionResource(R.dimen.large_OutlinedTextField_size))
            .fillMaxWidth(),
        singleLine = false,
        maxLines = 8,
        isError = descriptionError != null
    )
    descriptionError?.let { resId ->
        Text(
            text = stringResource(id = resId),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = dimensionResource(id = R.dimen.padding_xxsm))
        )
    }
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
