package com.example.foodieheal.Payment.Screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import com.example.foodieheal.hiring.util.CalendarSyncHelper
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.foodieheal.Payment.ViewModel.PaymentMethod
import com.example.foodieheal.Payment.ViewModel.PaymentMethodViewModel
import com.example.foodieheal.Payment.ViewModel.PaymentViewModel
import com.example.foodieheal.R
import com.example.foodieheal.ui.components.DetailRow
import com.example.foodieheal.ui.components.PaymentScreenSkeleton
import com.example.foodieheal.ui.components.formatToAmPm
import android.content.ContextWrapper
import androidx.fragment.app.FragmentActivity
import com.example.foodieheal.Payment.util.BiometricAuthManager
import com.example.foodieheal.Payment.util.BiometricStatus
import java.util.Locale

private fun android.content.Context.findFragmentActivity(): FragmentActivity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is FragmentActivity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    appointmentId: String,
    paymentViewModel: PaymentViewModel,
    paymentMethodViewModel: PaymentMethodViewModel,
    onBackClick: () -> Unit,
    onPaymentSuccess: (paymentId: String) -> Unit,
    onPaymentError: (errorMessage: String) -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findFragmentActivity() }
    val biometricStatus = remember(context) { BiometricAuthManager.checkBiometricAvailability(context) }
    val snackbarHostState = remember { SnackbarHostState() }

    val paymentState by paymentViewModel.uiState.collectAsStateWithLifecycle()
    val methodState by paymentMethodViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(appointmentId) {
        if (paymentState.appointment == null) {
            paymentViewModel.loadAppointmentById(appointmentId)
        }
    }

    val appointment = paymentState.appointment

    // Fetch payment methods
    LaunchedEffect(appointment?.userId) {
        appointment?.userId?.let { userId ->
            if (userId.isNotEmpty()) {
                paymentMethodViewModel.observeAndFetchPaymentMethods(userId)
            }
        }
    }

    var showCalendarPromptDialog by remember { mutableStateOf(false) }
    var completedTransactionId by remember { mutableStateOf<String?>(null) }
    var walletBalance by remember { mutableStateOf<Double?>(null) }
    var isWalletActive by remember { mutableStateOf(false) }
    val walletRepo = remember { com.example.foodieheal.wallet.data.WalletRepository() }

    // Fetch user wallet without auto-creating
    LaunchedEffect(appointment?.userId) {
        appointment?.userId?.let { uid ->
            if (uid.isNotEmpty()) {
                try {
                    val w = walletRepo.getWallet(uid)
                    walletBalance = w.balance ?: 0.0
                    isWalletActive = w.isActive == true
                } catch (_: Exception) {}
            }
        }
    }

    val inAppWalletMethod = remember(walletBalance, isWalletActive) {
        PaymentMethod.InAppWallet(
            balance = walletBalance ?: 0.0,
            isActive = isWalletActive
        )
    }

    // Auto-select default payment method when methods are available
    LaunchedEffect(methodState.availableMethods, inAppWalletMethod, appointment) {
        val apptPrice = appointment?.Total_Price ?: 0.0
        if (methodState.selectedMethod == null) {
            val defaultCard = methodState.availableMethods.firstOrNull {
                (it as? PaymentMethod.CreditCard)?.isDefault == true
            }
            if (defaultCard != null) {
                paymentMethodViewModel.selectPaymentMethod(defaultCard)
            } else if (isWalletActive && (walletBalance ?: 0.0) >= apptPrice && apptPrice > 0.0) {
                paymentMethodViewModel.selectPaymentMethod(inAppWalletMethod)
            } else if (methodState.availableMethods.isNotEmpty()) {
                paymentMethodViewModel.selectPaymentMethod(methodState.availableMethods.first())
            } else {
                paymentMethodViewModel.selectPaymentMethod(inAppWalletMethod)
            }
        }
    }

    LaunchedEffect(paymentState.errorMessage) {
        paymentState.errorMessage?.let { error ->
            if (appointment != null) {
                snackbarHostState.showSnackbar(error)
                paymentViewModel.clearError()
            }
        }
    }

    if (appointment == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (paymentState.errorMessage != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = paymentState.errorMessage ?: stringResource(R.string.error_loading_appointment),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Button(onClick = { paymentViewModel.loadAppointmentById(appointmentId) }) {
                        Text(stringResource(R.string.btn_retry))
                    }
                }
            } else {
                PaymentScreenSkeleton()
            }
        }
        return
    }

    var showAddCardSheet by remember { mutableStateOf(false) }

    val isOnline by paymentViewModel.isNetworkAvailable.collectAsStateWithLifecycle()

    // Pricing
    val totalPrice = appointment.Total_Price ?: 0.0
    val isSelectedWallet = methodState.selectedMethod is PaymentMethod.InAppWallet
    val isWalletInactiveSelected = isSelectedWallet && !isWalletActive
    val isInsufficientWalletBalance = isSelectedWallet && isWalletActive && ((walletBalance ?: 0.0) < totalPrice)
    val isPayButtonEnabled = !paymentState.isLoading && methodState.selectedMethod != null && !isWalletInactiveSelected && !isInsufficientWalletBalance && isOnline

    val biometricPromptTitle = stringResource(R.string.biometric_prompt_title)
    val calendarTitle = stringResource(R.string.payment_calendar_event_title)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_checkout_payment), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrowback),
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!isOnline) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.wifi_off),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = stringResource(R.string.payment_offline_banner),
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    } else if (isWalletInactiveSelected) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(R.string.wallet_inactive_warning),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    } else if (isInsufficientWalletBalance) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(R.string.wallet_insufficient_warning_format, String.format(Locale.US, "RM %.2f", walletBalance ?: 0.0)),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.label_total_amount),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = String.format(Locale.US, "RM %.2f", totalPrice),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    val executePayment = {
                        paymentViewModel.processPayment(
                            selectedMethod = methodState.selectedMethod,
                            onSuccess = { transactionId ->
                                Toast.makeText(context, R.string.toast_payment_success, Toast.LENGTH_SHORT).show()
                                completedTransactionId = transactionId
                                showCalendarPromptDialog = true
                            },
                            onError = { error ->
                                onPaymentError(error)
                            }
                        )
                    }

                    val formattedPrice = String.format(Locale.US, "RM %.2f", totalPrice)
                    val inAppWalletDesc = stringResource(R.string.payment_method_in_app_wallet)
                    val fallbackMethodDesc = stringResource(R.string.payment_method_selected_fallback)
                    val methodDescription = when (val m = methodState.selectedMethod) {
                        is PaymentMethod.InAppWallet -> inAppWalletDesc
                        is PaymentMethod.CreditCard -> stringResource(R.string.payment_method_card_format, m.last4Digits)
                        else -> fallbackMethodDesc
                    }
                    val biometricPromptSubtitle = stringResource(
                        R.string.biometric_prompt_subtitle,
                        formattedPrice,
                        methodDescription
                    )

                    Button(
                        onClick = {
                            if (biometricStatus == BiometricStatus.AVAILABLE && activity != null) {
                                BiometricAuthManager.promptBiometricAuth(
                                    activity = activity,
                                    title = biometricPromptTitle,
                                    subtitle = biometricPromptSubtitle,
                                    onSuccess = {
                                        executePayment()
                                    },
                                    onError = { errorMsg ->
                                        Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                                    },
                                    onCancel = {
                                        // User cancelled biometric prompt - do nothing and remain on screen safely
                                    }
                                )
                            } else {
                                // Biometrics not enrolled or unavailable -> fallback to direct payment execution
                                executePayment()
                            }
                        },
                        enabled = isPayButtonEnabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        if (paymentState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            val formattedPrice = String.format(Locale.US, "RM %.2f", totalPrice)
                            val buttonLabel = if (isSelectedWallet) {
                                stringResource(R.string.pay_amount_via_wallet_format, formattedPrice)
                            } else {
                                stringResource(R.string.pay_amount_format, formattedPrice)
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = buttonLabel,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Order Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.title_order_summary),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    HorizontalDivider()

                    DetailRow(
                        label = stringResource(R.string.label_date),
                        value = appointment.Date.orEmpty()
                    )
                    val formattedStart = formatToAmPm(appointment.Start_Time)
                    val formattedEnd = formatToAmPm(appointment.End_Time)
                    DetailRow(
                        label = stringResource(R.string.label_appointment_time),
                        value = if (formattedStart.isNotBlank() && formattedEnd.isNotBlank()) "$formattedStart - $formattedEnd" else "${appointment.Start_Time.orEmpty()} - ${appointment.End_Time.orEmpty()}"
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    AnimatedPriceBreakdownBar(
                        totalPrice  = totalPrice,
                        startTime   = appointment.Start_Time,
                        endTime     = appointment.End_Time,
                        servingSize = appointment.Serving_Size
                    )
                }
            }

            // Payment Options Selector
            Text(
                text = stringResource(R.string.title_select_payment_method),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    // 1. Always display In-App Wallet option
                    PaymentMethodItem(
                        method = inAppWalletMethod,
                        isSelected = methodState.selectedMethod?.id == inAppWalletMethod.id,
                        onSelect = { paymentMethodViewModel.selectPaymentMethod(inAppWalletMethod) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    // 2. Display saved cards list fetched by PaymentMethodViewModel
                    methodState.availableMethods.forEach { method ->
                        PaymentMethodItem(
                            method = method,
                            isSelected = methodState.selectedMethod?.id == method.id,
                            onSelect = { paymentMethodViewModel.selectPaymentMethod(method) }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    OutlinedButton(
                        onClick = { showAddCardSheet = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.dollar_symbol),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.add_new_card))
                    }
                }
            }
        }
    }

    // Add New Card Bottom Sheet
    if (showAddCardSheet) {
        AddNewCardBottomSheet(
            onDismiss = { showAddCardSheet = false },
            onCardAdded = { last4, brand, expiry ->
                appointment?.userId?.let { userId ->
                    paymentMethodViewModel.addNewCard(
                        userId = userId,
                        last4Digits = last4,
                        brand = brand,
                        expiryDate = expiry,
                        onSuccess = { showAddCardSheet = false },
                        onError = { errorMsg ->
                            Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                        }
                    )
                }
            }
        )
    }

    // Calendar Sync dialog upon payment done
    if (showCalendarPromptDialog && appointment != null) {
        AlertDialog(
            onDismissRequest = {
                showCalendarPromptDialog = false
                onPaymentSuccess(completedTransactionId.orEmpty())
            },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.ic_calendar),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.dialog_calendar_title),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.dialog_calendar_message),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val loc = listOfNotNull(
                            appointment.Address.takeIf { it.isNotBlank() },
                            appointment.Postcode.takeIf { it.isNotBlank() },
                            appointment.State.takeIf { it.isNotBlank() }
                        ).joinToString(", ")
                        val desc = "FoodieHeal Appointment\n" +
                                "Booking ID: ${appointment.AppointmentID.orEmpty()}\n" +
                                "Serving Size: ${appointment.Serving_Size} portions\n" +
                                "Health Preference: ${appointment.Health_Preference}\n" +
                                if (appointment.Note.isNotBlank()) "Notes: ${appointment.Note}" else ""

                        CalendarSyncHelper.addAppointmentToCalendar(
                            context = context,
                            title = calendarTitle,
                            description = desc.trim(),
                            location = loc,
                            dateStr = appointment.Date,
                            startTimeStr = appointment.Start_Time,
                            endTimeStr = appointment.End_Time
                        )
                        showCalendarPromptDialog = false
                        onPaymentSuccess(completedTransactionId.orEmpty())
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(stringResource(R.string.btn_add_to_calendar))
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showCalendarPromptDialog = false
                        onPaymentSuccess(completedTransactionId.orEmpty())
                    }
                ) {
                    Text(stringResource(R.string.btn_view_booking))
                }
            }
        )
    }
}