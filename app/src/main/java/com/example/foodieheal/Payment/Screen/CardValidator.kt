package com.example.foodieheal.Payment.Screen

import androidx.annotation.StringRes
import com.example.foodieheal.R

fun luhnCheck(cardNumber: String): Boolean {
    val digits = cardNumber.filter { it.isDigit() }
    if (digits.length < 13) return false          // too short to be any real card

    val sum = digits
        .reversed()
        .mapIndexed { index, ch ->
            val digit = ch.digitToInt()
            if (index % 2 == 1) {
                val doubled = digit * 2
                if (doubled > 9) doubled - 9 else doubled
            } else {
                digit
            }
        }
        .sum()

    return sum % 10 == 0
}

@StringRes
fun cardNumberError(cardNumber: String): Int? {
    val digits = cardNumber.filter { it.isDigit() }
    return when {
        digits.length < 13 -> null
        !luhnCheck(digits) -> R.string.add_card_error_invalid_number
        else -> null
    }
}

fun detectCardBrand(cardNumber: String): String {
    val digits = cardNumber.filter { it.isDigit() }
    return when {
        digits.startsWith("4")                          -> "Visa"
        digits.startsWith("51") || digits.startsWith("52") ||
        digits.startsWith("53") || digits.startsWith("54") ||
        digits.startsWith("55") ||
        (digits.length >= 6 && digits.take(6).toIntOrNull()
            ?.let { it in 222100..272099 } == true)    -> "Mastercard"
        digits.startsWith("34") || digits.startsWith("37") -> "Amex"
        digits.startsWith("6011") || digits.startsWith("65") -> "Discover"
        digits.startsWith("62")                        -> "UnionPay"
        else                                           -> "Card"
    }
}
