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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import com.example.foodieheal.Payment.ViewModel.PaymentMethodViewModel
import com.example.foodieheal.Payment.ViewModel.PaymentViewModel
import com.example.foodieheal.R
import com.example.foodieheal.ui.components.DetailRow
import java.util.Locale

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
    val snackbarHostState = remember { SnackbarHostState() }

    val paymentState by paymentViewModel.uiState.collectAsStateWithLifecycle()
    val methodState by paymentMethodViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(appointmentId) {
        if (paymentState.appointment == null) {
            paymentViewModel.loadAppointmentById(appointmentId)
        }
    }

    val appointment = paymentState.appointment

    //Fetch payment method
    LaunchedEffect(appointment?.userId) {
        appointment?.userId?.let { userId ->
            if (userId.isNotEmpty()) {
                paymentMethodViewModel.observeAndFetchPaymentMethods(userId)
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
                        text = paymentState.errorMessage ?: "Error loading appointment.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Button(onClick = { paymentViewModel.loadAppointmentById(appointmentId) }) {
                        Text("Retry")
                    }
                }
            } else {
                CircularProgressIndicator()
            }
        }
        return
    }

    var showAddCardSheet by remember { mutableStateOf(false) }

    // Pricing
    val totalPrice = appointment.Total_Price ?: 0.0

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Checkout & Payment", fontWeight = FontWeight.Bold) },
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total Amount",
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

                    Button(
                        onClick = {
                            paymentViewModel.processPayment(
                                selectedMethod = methodState.selectedMethod,
                                onSuccess = { transactionId ->
                                    Toast.makeText(context, "Payment Successful!", Toast.LENGTH_SHORT).show()
                                    onPaymentSuccess(transactionId)
                                },
                                onError = { error ->
                                    onPaymentError(error)
                                }
                            )
                        },
                        enabled = !paymentState.isLoading && methodState.selectedMethod != null,
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
                            Text(
                                text = "Pay RM ${String.format(Locale.US, "%.2f", totalPrice)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
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
                        text = "Order Summary",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    HorizontalDivider()

                    DetailRow(
                        label = stringResource(R.string.label_date),
                        value = appointment.Date.orEmpty()
                    )
                    DetailRow(
                        label = stringResource(R.string.label_appointment_time),
                        value = "${appointment.Start_Time.orEmpty()} - ${appointment.End_Time.orEmpty()}"
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Price", fontWeight = FontWeight.Bold)
                        Text(
                            String.format(Locale.US, "RM %.2f", totalPrice),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Payment Options Selector
            Text(
                text = "Select Payment Method",
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
                    // Display list fetched by PaymentMethodViewModel
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
                        Text("+ Add New Credit / Debit Card")
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
                appointment.userId?.let { userId ->
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
}