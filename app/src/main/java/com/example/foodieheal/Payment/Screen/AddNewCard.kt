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
import androidx.compose.ui.unit.dp
import com.example.foodieheal.Payment.ViewModel.NewCardFormState
import com.example.foodieheal.Payment.ViewModel.PaymentMethod
import com.example.foodieheal.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNewCardBottomSheet(
    onDismiss: () -> Unit,
    onCardAdded: (last4Digits: String, brand: String, expiryDate: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var formState by remember { mutableStateOf(NewCardFormState()) }

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
                onValueChange = { if (it.length <= 16) formState = formState.copy(cardNumber = it) },
                label = { Text("Card Number") },
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
                    onValueChange = { if (it.length <= 5) formState = formState.copy(expiryDate = it) },
                    label = { Text("MM/YY") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = formState.cvv,
                    onValueChange = { if (it.length <= 4) formState = formState.copy(cvv = it) },
                    label = { Text("CVV") },
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

            Button(
                onClick = {
                    val last4 = if (formState.cardNumber.length >= 4) formState.cardNumber.takeLast(4) else "0000"
                    val brand = detectCardBrand(formState.cardNumber)
                    onCardAdded(last4, brand, formState.expiryDate)
                },
                enabled = formState.cardNumber.length >= 15 && formState.expiryDate.isNotEmpty() && formState.cvv.isNotEmpty(),
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onSelect() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onSelect
        )
        Spacer(modifier = Modifier.width(12.dp))
        Icon(
            painter = painterResource(R.drawable.dollar_symbol),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = method.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
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