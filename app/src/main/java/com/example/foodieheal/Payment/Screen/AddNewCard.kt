package com.example.foodieheal.Payment.Screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.foodieheal.Payment.ViewModel.NewCardFormState
import com.example.foodieheal.Payment.ViewModel.PaymentMethod
import com.example.foodieheal.R
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNewCardBottomSheet(
    onDismiss: () -> Unit,
    onCardAdded: (last4Digits: String, brand: String, expiryDate: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var formState by remember { mutableStateOf(NewCardFormState()) }

    val expiryError = validateExpiryDate(formState.expiryDate)
    val isExpiryComplete = formState.expiryDate.length == 5
    val isExpiryInvalid = formState.expiryDate.isNotEmpty() && (isExpiryComplete && expiryError != null || (!isExpiryComplete && formState.expiryDate.length >= 2 && validateMonth(formState.expiryDate) != null))

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Add Credit or Debit Card",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = formState.cardNumber,
                onValueChange = { input ->
                    val clean = input.filter { it.isDigit() }.take(16)
                    formState = formState.copy(cardNumber = clean)
                },
                label = { Text("Card Number") },
                placeholder = { Text("16-digit card number") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = formState.cardHolderName,
                onValueChange = { formState = formState.copy(cardHolderName = it) },
                label = { Text("Cardholder Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = formState.expiryDate,
                    onValueChange = { input ->
                        val formatted = formatExpiryDateInput(input)
                        formState = formState.copy(expiryDate = formatted)
                    },
                    label = { Text("MM/YY") },
                    placeholder = { Text("MM/YY") },
                    isError = isExpiryInvalid,
                    supportingText = if (isExpiryInvalid) {
                        { Text(expiryError ?: validateMonth(formState.expiryDate) ?: "Invalid MM/YY", color = MaterialTheme.colorScheme.error) }
                    } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = formState.cvv,
                    onValueChange = { input ->
                        val clean = input.filter { it.isDigit() }.take(4)
                        formState = formState.copy(cvv = clean)
                    },
                    label = { Text("CVV") },
                    placeholder = { Text("3-4 digits") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = formState.isSaveForFuture,
                    onCheckedChange = { formState = formState.copy(isSaveForFuture = it) }
                )
                Text(
                    text = "Save card for future bookings",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            val isFormValid = formState.cardNumber.length in 15..16 &&
                    formState.cardHolderName.isNotBlank() &&
                    formState.expiryDate.length == 5 &&
                    expiryError == null &&
                    formState.cvv.length in 3..4

            Button(
                onClick = {
                    val last4 = if (formState.cardNumber.length >= 4) formState.cardNumber.takeLast(4) else "0000"
                    val brand = detectCardBrand(formState.cardNumber)
                    onCardAdded(last4, brand, formState.expiryDate)
                },
                enabled = isFormValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save & Select Card", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// Auto-format MM/YY input
fun formatExpiryDateInput(input: String): String {
    val digits = input.filter { it.isDigit() }.take(4)
    return when {
        digits.length <= 2 -> digits
        else -> "${digits.take(2)}/${digits.drop(2)}"
    }
}

// Validate month (01-12)
fun validateMonth(expiry: String): String? {
    if (expiry.length < 2) return null
    val month = expiry.take(2).toIntOrNull()
    if (month == null || month < 1 || month > 12) {
        return "Month must be 01-12"
    }
    return null
}

// Validate complete MM/YY format & check if expired
fun validateExpiryDate(expiry: String): String? {
    if (expiry.isBlank()) return null
    if (expiry.length < 5) {
        return validateMonth(expiry)
    }
    val parts = expiry.split("/")
    if (parts.size != 2 || parts[0].length != 2 || parts[1].length != 2) {
        return "Format: MM/YY"
    }
    val month = parts[0].toIntOrNull()
    val yearShort = parts[1].toIntOrNull()
    if (month == null || month < 1 || month > 12) {
        return "Month must be 01-12"
    }
    if (yearShort == null) {
        return "Invalid year"
    }

    val calendar = Calendar.getInstance()
    val currentYearShort = calendar.get(Calendar.YEAR) % 100 // 2-digit year (e.g. 26)
    val currentMonth = calendar.get(Calendar.MONTH) + 1 // 1-12

    if (yearShort < currentYearShort || (yearShort == currentYearShort && month < currentMonth)) {
        return "Card has expired"
    }
    if (yearShort > currentYearShort + 25) {
        return "Invalid expiry year"
    }

    return null
}

// Simple brand detection logic
private fun detectCardBrand(cardNumber: String): String {
    return when {
        cardNumber.startsWith("4") -> "Visa"
        cardNumber.startsWith("5") || cardNumber.startsWith("2") -> "Mastercard"
        cardNumber.startsWith("3") -> "Amex"
        else -> "Card"
    }
}

@Composable
fun PaymentMethodItem(
    method: PaymentMethod,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val isDefault = (method as? PaymentMethod.CreditCard)?.isDefault == true
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onSelect() }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onSelect
        )
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            painter = painterResource(R.drawable.dollar_symbol),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = method.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.weight(1f, fill = false),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (isDefault) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "Default",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            method.subtitle?.let { subtitleText ->
                Text(
                    text = subtitleText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}