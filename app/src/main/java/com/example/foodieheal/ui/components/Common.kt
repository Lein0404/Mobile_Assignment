package com.example.foodieheal.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.foodieheal.R
import com.example.foodieheal.model.Status
import java.util.Locale

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
    padding: PaddingValues? = null,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary
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
            containerColor = containerColor,
            contentColor = contentColor,
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
    padding: PaddingValues? = null,
    borderColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.primary
) {
    val finalModifier = if (padding != null) {
        modifier.padding(padding)
    } else {
        modifier
    }

    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_sm)),
        border = BorderStroke(
            width = 1.dp,
            color = borderColor
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = contentColor
        ),
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
    supportingText: @Composable (() -> Unit)? = null,
    leadingIconRes: Int? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    placeholder: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    minLines: Int = 1,
) {
    // Modifier is applied ONLY to the parent container
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
                {
                    Icon(
                        painter = painterResource(id = it),
                        contentDescription = null,
                        modifier = Modifier.padding(dimensionResource(R.dimen.padding_md))
                    )
                }
            },
            trailingIcon = trailingIcon,
            supportingText = supportingText,
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
    modifier: Modifier = Modifier, // Removed default fillMaxWidth here to avoid confusion
    enabled: Boolean = true,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null, // Added supportingText slot
    placeholder: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    minLines: Int = 1
) {
    var passwordVisible by remember { mutableStateOf(false) }

    // Modifier applied ONLY to outer Column
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
            modifier = Modifier.fillMaxWidth(), // Inner field always fills container width
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
                    Icon(
                        painter = painterResource(id = visibilityIcon),
                        contentDescription = description,
                        modifier = Modifier
                            .padding(dimensionResource(R.dimen.padding_xsm))
                            .padding(end = dimensionResource(R.dimen.padding_xsm))
                    )
                }
            },
            isError = isError,
            supportingText = supportingText,
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
@JvmName("DropDownListIntOptions")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropDownList(
    modifier: Modifier = Modifier,
    @StringRes labelId: Int,
    @StringRes placeholderId: Int,
    selectedValue: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit,
    isError: Boolean = false,
    @StringRes errorMessageId: Int? = null
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp) // gap between items in a Column
    ) {
        Text(
            text = stringResource(labelId),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedValue ,
                onValueChange = { },
                readOnly = true,
                placeholder = {
                    Text(stringResource(placeholderId),
                        color = MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.6f)
                    ) },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropDownList(
    modifier: Modifier = Modifier,
    @StringRes labelId: Int,
    @StringRes placeholderId: Int,
    selectedValue: String,
    options: List<Int>,
    onOptionSelected: (Int) -> Unit,
    isError: Boolean = false,
    @StringRes errorMessageId: Int? = null
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp) // gap between items in a Column
    ) {
        Text(
            text = stringResource(labelId),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedValue ?: "",
                onValueChange = { },
                readOnly = true,
                placeholder = {
                    Text(stringResource(placeholderId),
                        color = MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.6f)
                    ) },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                isError = isError
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(stringResource(option)) },
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

@Composable
fun GenderDropdown(
    gender: String,
    onGenderChange: (String) -> Unit
) {

    var expanded by remember { mutableStateOf(false) }

    val genders = listOf("Male", "Female")


    Box(
        modifier = Modifier.fillMaxWidth()
    ) {

        OutlinedTextField(
            value = gender,
            onValueChange = {},
            label = { Text("Gender") },
            placeholder = { Text("Select Gender") },
            readOnly = true,
            enabled = true,
            trailingIcon = {
                Text("▼")
            },
            modifier = Modifier.fillMaxWidth()
        )


        // Click layer
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable {
                    expanded = true
                }
        )


        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            },
            modifier = Modifier.fillMaxWidth()
        ) {

            genders.forEach { option ->

                DropdownMenuItem(
                    text = {
                        Text(option)
                    },
                    onClick = {
                        onGenderChange(option)
                        expanded = false
                    }
                )

            }

        }
    }
}

@Composable
fun getHighlightedText(
    fullText: String,
    query: String,
    highlightColor: Color = MaterialTheme.colorScheme.primary
): AnnotatedString {
    val trimmedQuery = query.trim()
    if (trimmedQuery.isEmpty()) return AnnotatedString(fullText)

    return buildAnnotatedString {
        var startIndex = 0
        while (startIndex < fullText.length) {
            val index = fullText.indexOf(trimmedQuery, startIndex, ignoreCase = true)
            if (index == -1) {
                append(fullText.substring(startIndex))
                break
            }

            append(fullText.substring(startIndex, index))
            withStyle(
                style = SpanStyle(
                    color = highlightColor,
                    fontWeight = FontWeight.Bold,
                )
            ) {
                append(fullText.substring(index, index + trimmedQuery.length))
            }
            startIndex = index + trimmedQuery.length
        }
    }
}

@Composable
fun StatusBadge(status: Status) {
    val isDark = isSystemInDarkTheme()
    val (color, text) = when (status) {
        Status.APPROVED -> (if (isDark) Color(0xFF1B5E20) else Color(0xFFB1E0C0)) to Status.APPROVED.statusName
        Status.PENDING -> (if (isDark) Color(0xFFE65100) else Color(0xFFFFF3E0)) to Status.PENDING.statusName
        Status.REJECTED -> MaterialTheme.colorScheme.errorContainer to Status.REJECTED.statusName
    }

    val textColor = when (status) {
        Status.APPROVED -> (if (isDark) Color(0xFFC8E6C9) else Color(0xFF008000))
        Status.PENDING -> (if (isDark) Color(0xFFFFE0B2) else Color(0xFFFF9800))
        Status.REJECTED -> MaterialTheme.colorScheme.onErrorContainer
    }

    Surface(
        color = color,
        shape = RoundedCornerShape(dimensionResource(id = R.dimen.corner_radius_sm))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(
                horizontal = dimensionResource(R.dimen.padding_md),
                vertical = dimensionResource(R.dimen.padding_xsm)
            ),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
fun ImagePlaceholder(
    modifier: Modifier = Modifier,
    iconSize: Dp = dimensionResource(R.dimen.icon_xlarge_size),
    spacerSize: Dp = dimensionResource(id = R.dimen.padding_smd),
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_no_image_available),
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            tint = tint
        )
        Spacer(modifier = Modifier.height(spacerSize))
        Text(
            text = stringResource(R.string.image_unavailable),
            color = tint,
        )
    }
}

@Composable
fun DetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    painter: Painter? = null,
    iconSize: Dp = 20.dp,
    valueFontWeight: FontWeight = if (painter == null) FontWeight.Bold else FontWeight.Medium,
    isSpaceBetween: Boolean = (painter == null)
) {
    if (isSpaceBetween) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                painter?.let { p ->
                    Icon(
                        painter = p,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(iconSize)
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = valueFontWeight,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = androidx.compose.ui.text.style.TextAlign.End
            )
        }
    } else {
        // Vertical Stack Layout: [Icon]  Label
        //                                Value
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            painter?.let { p ->
                Icon(
                    painter = p,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(iconSize)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = valueFontWeight,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun DetailRow(
    painter: Painter,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    DetailRow(
        label = label,
        value = value,
        modifier = modifier,
        painter = painter
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    DatePickerDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        dismissButton = dismissButton,
        content = content
    )
}

@Composable
fun AppointmentStatusBadge(
    status: String,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 8.dp,
    horizontalPadding: Dp = 10.dp,
    verticalPadding: Dp = 4.dp
) {
    val (backgroundColor, textColor) = when (status.lowercase(Locale.ROOT).trim()) {
        "completed" -> Color(0xFFE3F2FD) to Color(0xFF1565C0) // Soft Blue
        "confirmed" -> Color(0xFFE8F5E9) to Color(0xFF2E7D32) // Soft Green
        "cancelled" -> Color(0xFFFFEBEE) to Color(0xFFC62828) // Soft Red
        "rejected"  -> Color(0xFFFBE9E7) to Color(0xFFD84315) // Soft Deep Orange / Rust Red
        "unpaid"    -> Color(0xFFFFF8E1) to Color(0xFFF57F17) // Soft Amber / Yellow-Orange
        "pending"   -> Color(0xFFFFF3E0) to Color(0xFFE65100) // Soft Orange
        else        -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    // Format display text (capitalized or fallback)
    val displayText = status.ifBlank { "Confirmed" }.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(cornerRadius),
        color = backgroundColor
    ) {
        Text(
            text = displayText,
            modifier = Modifier.padding(horizontal = horizontalPadding, vertical = verticalPadding),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}


@Composable
fun DetailSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
    cornerRadius: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (showDivider) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }

            content()
        }
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

fun formatToAmPm(timeStr: String?): String {
    if (timeStr.isNullOrBlank()) return ""
    val trimmed = timeStr.trim()
    if (trimmed.contains("AM", ignoreCase = true) || trimmed.contains("PM", ignoreCase = true)) {
        return trimmed
    }
    val patterns = listOf("HH:mm:ss", "HH:mm", "H:mm:ss", "H:mm")
    for (pattern in patterns) {
        try {
            val parser = java.text.SimpleDateFormat(pattern, Locale.US)
            val date = parser.parse(trimmed)
            if (date != null) {
                val outputFormat = java.text.SimpleDateFormat("hh:mm a", Locale.US)
                return outputFormat.format(date)
            }
        } catch (_: Exception) {}
    }
    return trimmed
}