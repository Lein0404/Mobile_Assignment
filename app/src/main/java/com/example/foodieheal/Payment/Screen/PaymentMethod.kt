package com.example.foodieheal.Payment.Screen

import android.widget.Toast
import es.dmoral.toasty.Toasty
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.foodieheal.Payment.ViewModel.PaymentMethod
import com.example.foodieheal.Payment.ViewModel.PaymentMethodViewModel
import com.example.foodieheal.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentMethodScreen(
    userId: String,
    viewModel: PaymentMethodViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val cannotAddCardsOfflineToast = stringResource(R.string.toast_cannot_add_cards_offline)
    val cardAddedSuccessToast = stringResource(R.string.toast_card_added_success)
    val paymentMethodDeletedToast = stringResource(R.string.toast_payment_method_deleted)
    val setDefaultPaymentMethodToast = stringResource(R.string.msg_set_default_payment_method)
    val removedDefaultPaymentMethodToast = stringResource(R.string.msg_removed_default_payment_method)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isOnline by viewModel.isNetworkAvailable.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showAddCardSheet by remember { mutableStateOf(false) }
    var methodToDelete by remember { mutableStateOf<PaymentMethod?>(null) }

    // Fetch payment methods when userId is available
    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            viewModel.observeAndFetchPaymentMethods(userId)
        }
    }

    // Display error messages
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_payment_methods), fontWeight = FontWeight.Bold) },
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (isOnline) {
                        showAddCardSheet = true
                    } else {
                        Toasty.custom(context, cannotAddCardsOfflineToast, R.drawable.foodieheallogo_removebg_and_word, R.color.black, Toast.LENGTH_SHORT, true, true).show()
                    }
                },
                containerColor = if (isOnline) MaterialTheme.colorScheme.primary else Color.Gray,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_outline_add),
                    contentDescription = stringResource(R.string.cd_add_payment_method)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (!isOnline) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.wifi_off),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(R.string.payment_methods_offline_banner),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                if (uiState.isLoading && uiState.availableMethods.isEmpty()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (uiState.availableMethods.isEmpty()) {
                    EmptyPaymentMethodsView(onAddClick = {
                        if (isOnline) showAddCardSheet = true
                        else Toasty.custom(context, cannotAddCardsOfflineToast, R.drawable.foodieheallogo_removebg_and_word, R.color.black, Toast.LENGTH_SHORT, true, true).show()
                    })
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = uiState.availableMethods,
                            key = { it.id }
                        ) { method ->
                            val isDefault = (method as? PaymentMethod.CreditCard)?.isDefault == true
                            PaymentMethodManagementItem(
                                method = method,
                                isDefault = isDefault,
                                onToggleDefault = {
                                    val nextDefaultState = !isDefault
                                    viewModel.setDefaultPaymentMethod(
                                        methodId = method.id,
                                        userId = userId,
                                        isDefault = nextDefaultState,
                                        onSuccess = {
                                            val msg = if (nextDefaultState) setDefaultPaymentMethodToast else removedDefaultPaymentMethodToast
                                            Toasty.custom(context, msg, R.drawable.foodieheallogo_removebg_and_word, R.color.black, Toast.LENGTH_SHORT, true, true).show()
                                        },
                                        onError = { err ->
                                            Toasty.custom(context, err, R.drawable.foodieheallogo_removebg_and_word, R.color.black, Toast.LENGTH_SHORT, true, true).show()
                                        }
                                    )
                                },
                                onDelete = { methodToDelete = method }
                            )
                        }
                    }
                }
            }
        }
    }

    // Bottom sheet for adding a new card
    if (showAddCardSheet) {
        AddNewCardBottomSheet(
            onDismiss = { showAddCardSheet = false },
            onCardAdded = { last4, brand, expiry ->
                viewModel.addNewCard(
                    userId = userId,
                    last4Digits = last4,
                    brand = brand,
                    expiryDate = expiry,
                    onSuccess = {
                        showAddCardSheet = false
                        Toasty.custom(context, cardAddedSuccessToast, R.drawable.foodieheallogo_removebg_and_word, R.color.black, Toast.LENGTH_SHORT, true, true).show()
                    },
                    onError = { error ->
                        Toasty.custom(context, error, R.drawable.foodieheallogo_removebg_and_word, R.color.black, Toast.LENGTH_LONG, true, true).show()
                    }
                )
            }
        )
    }

    // Confirmation dialog before deleting
    methodToDelete?.let { method ->
        AlertDialog(
            onDismissRequest = { methodToDelete = null },
            title = { Text(stringResource(R.string.dialog_delete_payment_method_title)) },
            text = { Text(stringResource(R.string.dialog_delete_payment_method_msg)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deletePaymentMethod(
                            methodId = method.id,
                            userId = userId,
                            onSuccess = {
                                methodToDelete = null
                                Toasty.custom(context, paymentMethodDeletedToast, R.drawable.foodieheallogo_removebg_and_word, R.color.black, Toast.LENGTH_SHORT, true, true).show()
                            },
                            onError = { err ->
                                methodToDelete = null
                                Toasty.custom(context, err, R.drawable.foodieheallogo_removebg_and_word, R.color.black, Toast.LENGTH_LONG, true, true).show()
                            }
                        )
                    }
                ) {
                    Text(stringResource(R.string.delete_action), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { methodToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun PaymentMethodManagementItem(
    method: PaymentMethod,
    isDefault: Boolean,
    onToggleDefault: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDefault) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 4.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.dollar_symbol),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    when (method) {
                        is PaymentMethod.InAppWallet -> {
                            Text(
                                text = stringResource(R.string.payment_method_in_app_wallet),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = stringResource(
                                    R.string.payment_method_wallet_balance_format,
                                    String.format(java.util.Locale.US, "%.2f", method.balance)
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        is PaymentMethod.CreditCard -> {
                            val formattedExpiry = method.expiryDate?.let { exp ->
                                if (exp.length == 4 && !exp.contains("/")) "${exp.take(2)}/${exp.takeLast(2)}" else exp
                            } ?: stringResource(R.string.not_available)

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = method.cardBrand,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f, fill = false),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = stringResource(R.string.card_masked_number_format, method.last4Digits),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1
                                )
                            }

                            Text(
                                text = stringResource(R.string.card_expires_format, formattedExpiry),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (isDefault) {
                        Text(
                            text = stringResource(R.string.label_default_payment_method),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 2.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    onClick = onToggleDefault,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_star),
                        contentDescription = if (isDefault) stringResource(R.string.label_default_payment_method) else stringResource(R.string.cd_set_as_default),
                        tint = if (isDefault) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_delete),
                        contentDescription = stringResource(R.string.cd_delete_method),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyPaymentMethodsView(onAddClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.dollar_symbol),
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.empty_payment_methods_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.empty_payment_methods_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onAddClick,
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_outline_add),
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.btn_add_payment_method))
        }
    }
}