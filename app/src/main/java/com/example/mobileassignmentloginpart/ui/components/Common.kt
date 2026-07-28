package com.example.mobileassignmentloginpart.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import com.example.mobileassignmentloginpart.R

/**
 * Common is a helper class with composable templates such as Buttons, Carousels, Lists configured
 * to our app's colors.
 */
@Composable
fun PrimaryButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    textID: Int,
    enabled: Boolean = true,
    padding: PaddingValues? = null
) {
    val finalModifier = if (padding != null) {
        modifier.padding(padding)
    } else {
        modifier
    }

    Button(
        onClick = onClick,
        shape = RoundedCornerShape(dimensionResource(id = R.dimen.corner_radius_sm)),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        enabled = enabled,
        modifier = finalModifier
    ) {
        Text(
            text = stringResource(id = textID).uppercase(),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
fun SecondaryButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    textId: Int,
    enabled: Boolean = true,
    padding: PaddingValues? = null
) {
    val finalModifier = if (padding != null) {
        modifier.padding(padding)
    } else {
        modifier
    }

    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_sm)),
        enabled = enabled,
        modifier = finalModifier
    ) {
        Text(
            text = stringResource(id = textId).uppercase(),
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
fun CommonTextButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    textID: Int,
    enabled: Boolean = true,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Text(
            text = stringResource(id = textID).uppercase(),
            style = MaterialTheme.typography.labelLarge
        )
    }
}


@Composable
fun CommonInputField(
    value: String,
    onValueChange: (String) -> Unit,
    textId: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    leadingIconRes: Int? = null, // Drawable resource ID
    placeholder: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    minLines: Int = 1,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(textId),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(vertical = dimensionResource(R.dimen.padding_xsm))
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier, // Apply modifier directly
            enabled = enabled,
            placeholder = placeholder?.let { { Text(it) } },
            leadingIcon = leadingIconRes?.let {
                { Icon(painterResource(id = it),
                    contentDescription = null,
                    modifier = Modifier.padding(dimensionResource(R.dimen.padding_md)))
                }
            },
            isError = isError,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = singleLine,
            maxLines = maxLines,
            minLines = minLines,
            shape = RoundedCornerShape(
                dimensionResource(id = R.dimen.corner_radius_sm)
            ),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            )
        )
    }
}

@Composable
fun PasswordInputField(
    value: String,
    onValueChange: (String) -> Unit,
    textId: Int, // string resource for label
    modifier: Modifier = Modifier.fillMaxWidth(),
    enabled: Boolean = true,
    isError: Boolean = false,
    placeholder: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    minLines: Int = 1
) {
    var passwordVisible by remember { mutableStateOf(false) }
    Column(modifier = modifier) {
        Text(
            text = stringResource(textId),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(vertical = dimensionResource(R.dimen.padding_xsm))
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            enabled = enabled,
            textStyle = MaterialTheme.typography.bodyLarge,
            placeholder = placeholder?.let { { Text(it) } },
            trailingIcon = {
                val visibilityIcon = if (passwordVisible) R.drawable.ic_view else R.drawable.ic_hide
                val description = if (passwordVisible) {
                    stringResource(id = R.string.hide_password)
                } else {
                    stringResource(id = R.string.show_password)
                }
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(painterResource(id = visibilityIcon),
                        contentDescription = description,
                        modifier = Modifier.padding(dimensionResource(R.dimen.padding_xsm))
                            .padding(end = dimensionResource(R.dimen.padding_xsm))
                    )
                }
            },
            isError = isError,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = singleLine,
            maxLines = maxLines,
            minLines = minLines,
            shape = RoundedCornerShape(
                dimensionResource(id = R.dimen.corner_radius_sm)
            ),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropDownList(
    @StringRes labelId: Int,
    @StringRes placeholderId: Int,
    selectedValue: String?,
    options: List<String>,
    onOptionSelected: (String?) -> Unit,
    isError: Boolean = false,
    @StringRes errorMessageId: Int? = null
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp) // gap between items in a Column
    ) {
        Text(
            text = stringResource(labelId),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedValue ?: "",
                onValueChange = onOptionSelected,
                readOnly = true,
                placeholder = { Text(stringResource(placeholderId)) },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                isError = isError
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onOptionSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
        if (isError && errorMessageId != null) {
            ErrorMessageCard(
                textId = errorMessageId
            )
        }
    }
}

@Composable
fun ErrorMessageCard(
    textId: Int,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.error
        ),
        shape = RoundedCornerShape(
            dimensionResource(R.dimen.corner_radius_sm)
        ),
        modifier = modifier) {
        Text(
            text = stringResource(textId),
            style = MaterialTheme.typography.bodyMedium,
            modifier = modifier.padding(dimensionResource(R.dimen.padding_l))
        )
    }
}


/*@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DefaultTopBar(
    uiState: TopBarState,
    onUpClick: () -> Unit = {},
) {
    TopAppBar(
        title = {
            if (uiState.title != null) {
                Text(
                    text = stringResource(uiState.title),
                    style = MaterialTheme.typography.titleLarge
                )
            }
        },
        navigationIcon = {
            if (uiState.showUpButton) {
                IconButton(onClick = onUpClick) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrowback),
                        contentDescription = stringResource(R.string.topapp_back)
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

// Assuming CommonUiState has bottomNavItems: List<BottomNavItem> and selectedRoute: String
// Assuming BottomNavItem has route: String, iconRes: Int, labelID: Int
@Composable
fun BottomNavBar(
    item: List<BottomNavItem>,
    currentDestination : NavDestination?, // get's current destination from navhost.
    onItemSelected: (BottomNavItem) -> Unit,
    onLogoutClicked: () -> Unit
) {

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant
        )
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            item.forEach { item ->
                // Check if current destination or any of its parents match the route class
                val isSelected = currentDestination?.hierarchy?.any {
                    it.hasRoute(item.route::class)
                } == true
                NavigationBarItem(
                    selected = isSelected,
                    onClick = {
                        if (item is BottomNavItem.LogoutNav) {
                            onLogoutClicked()
                        } else {
                            onItemSelected(item)
                        }
                    },
                    icon = {
                        Icon(
                            painter = painterResource(id = item.iconRes),
                            contentDescription = stringResource(id = item.labelID),
                            modifier = Modifier.size(dimensionResource(R.dimen.bottom_nav_icon_size))
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(id = item.labelID),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onSurface,
                        selectedTextColor = MaterialTheme.colorScheme.onSurface,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        indicatorColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                )
            }
        }
    }
}*/