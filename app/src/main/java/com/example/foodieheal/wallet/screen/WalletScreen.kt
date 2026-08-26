package com.example.foodieheal.wallet.screen

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.foodieheal.Payment.ViewModel.PaymentMethod
import com.example.foodieheal.Payment.ViewModel.PaymentMethodViewModel
import com.example.foodieheal.R
import com.example.foodieheal.wallet.model.WalletTransaction
import com.example.foodieheal.wallet.model.WalletTransactionType
import com.example.foodieheal.wallet.viewmodel.TransactionFilterOption
import com.example.foodieheal.wallet.viewmodel.WalletUiState
import com.example.foodieheal.wallet.viewmodel.WalletViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    userId: String,
    viewModel: WalletViewModel,
    paymentMethodViewModel: PaymentMethodViewModel? = null,
    onBackClick: () -> Unit,
    onTransactionClick: (transactionId: String) -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isOnline by viewModel.isNetworkAvailable.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showTopUpSheet by remember { mutableStateOf(false) }

    LaunchedEffect(userId) {
        if (userId.isNotBlank()) {
            viewModel.initialize(userId)
            paymentMethodViewModel?.observeAndFetchPaymentMethods(userId)
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.wallet_title),
                        fontWeight = FontWeight.Bold
                    )
                },
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
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = {
                if (isOnline) {
                    viewModel.loadWalletData(userId, isRefresh = true)
                } else {
                    Toast.makeText(context, R.string.wallet_offline_cannot_refresh, Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (uiState.isLoading && uiState.wallet == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (!isOnline) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
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
                                        text = stringResource(R.string.wallet_offline_banner),
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }

                    item {
                        WalletBalanceCard(
                            uiState = uiState,
                            onToggleVisibility = { viewModel.toggleBalanceVisibility() },
                            onTopUpClick = {
                                if (isOnline) {
                                    showTopUpSheet = true
                                } else {
                                    Toast.makeText(context, R.string.wallet_offline_cannot_top_up, Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }

                    item {
                        FilterChipsRow(
                            selectedFilter = uiState.selectedFilter,
                            onFilterSelect = { viewModel.setFilter(it) }
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.transaction_history),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.wallet_records_count, uiState.filteredTransactions.size),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    val transactions = uiState.filteredTransactions
                    if (transactions.isEmpty()) {
                        item {
                            EmptyTransactionsView(
                                filter = uiState.selectedFilter,
                                onTopUpClick = { showTopUpSheet = true }
                            )
                        }
                    } else {
                        items(
                            items = transactions,
                            key = { it.id.ifBlank { "${it.createdAt}_${it.amount}_${it.transactionType}" } }
                        ) { txn ->
                            WalletTransactionItem(
                                transaction = txn,
                                isBalanceHidden = uiState.isBalanceHidden,
                                onClick = { onTransactionClick(txn.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Top-Up Bottom Sheet
    if (showTopUpSheet) {
        val paymentUiState = paymentMethodViewModel?.uiState?.collectAsStateWithLifecycle()?.value
        TopUpBottomSheet(
            isSubmitting = uiState.isSubmittingTopUp,
            savedCards = paymentUiState?.availableMethods ?: emptyList(),
            onDismiss = { showTopUpSheet = false },
            onConfirmTopUp = { amount, paymentMethodId ->
                val selectedCard = paymentUiState?.availableMethods?.firstOrNull { it.id == paymentMethodId }
                val methodDesc = if (selectedCard != null) "Top-up via ${selectedCard.title}" else "Wallet Top Up"
                viewModel.topUp(
                    amount = amount,
                    paymentMethodId = paymentMethodId,
                    description = methodDesc,
                    onSuccess = {
                        showTopUpSheet = false
                    },
                    onError = { err ->
                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                    }
                )
            }
        )
    }
}

@Composable
fun WalletBalanceCard(
    uiState: WalletUiState,
    onToggleVisibility: () -> Unit,
    onTopUpClick: () -> Unit
) {
    val balance = uiState.currentBalance
    val isHidden = uiState.isBalanceHidden
    val isActive = uiState.isWalletActive

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFEC5E3A), // Brand primary orange
                            Color(0xFFE64A19), // Deep warm orange
                            Color(0xFFBF360C)  // Rich burnt orange for depth
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top row: Wallet Label + Active Status Pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.dollar_symbol),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = stringResource(R.string.wallet_pay_balance_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Status chip
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isActive) Color(0xFF2E7D32) else Color(0xFFE65100),
                        contentColor = Color.White
                    ) {
                        Text(
                            text = if (isActive) stringResource(R.string.wallet_status_active) else stringResource(R.string.wallet_status_inactive),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Middle: Balance Amount + Show/Hide Eye Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isActive) stringResource(R.string.wallet_current_balance) else stringResource(R.string.wallet_inactive_balance),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isHidden) stringResource(R.string.wallet_bal_hidden) else String.format(Locale.US, "RM %.2f", balance),
                            fontSize = 30.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        if (!isActive) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.wallet_activate_prompt),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.9f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    IconButton(
                        onClick = onToggleVisibility,
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                            .size(40.dp)
                    ) {
                        Icon(
                            painter = painterResource(if (isHidden) R.drawable.ic_hide else R.drawable.ic_view),
                            contentDescription = if (isHidden) stringResource(R.string.wallet_show_balance) else stringResource(R.string.wallet_hide_balance),
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.25f))

                // Bottom: Action Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onTopUpClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_outline_add),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isActive) stringResource(R.string.wallet_top_up) else stringResource(R.string.wallet_activate_and_top_up),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FilterChipsRow(
    selectedFilter: TransactionFilterOption,
    onFilterSelect: (TransactionFilterOption) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(TransactionFilterOption.values()) { option ->
            val label = when (option) {
                TransactionFilterOption.ALL -> stringResource(R.string.wallet_filter_all)
                TransactionFilterOption.TOP_UP -> stringResource(R.string.wallet_filter_top_up)
                TransactionFilterOption.PAYMENT -> stringResource(R.string.wallet_filter_payments)
                TransactionFilterOption.REFUND -> stringResource(R.string.wallet_filter_refunds)
            }
            FilterChip(
                selected = selectedFilter == option,
                onClick = { onFilterSelect(option) },
                label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(10.dp)
            )
        }
    }
}

@Composable
fun WalletTransactionItem(
    transaction: WalletTransaction,
    isBalanceHidden: Boolean,
    onClick: () -> Unit = {}
) {
    val isCredit = transaction.isCredit
    val recentFallback = stringResource(R.string.wallet_date_recent)
    val formattedDate = remember(transaction.createdAt, recentFallback) {
        formatIsoDate(transaction.createdAt, recentFallback)
    }

    val topUpLabel = stringResource(R.string.wallet_txn_top_up)
    val appointmentPaymentLabel = stringResource(R.string.wallet_txn_appointment_payment)
    val refundLabel = stringResource(R.string.wallet_txn_refund)
    val rescheduleAdjustmentLabel = stringResource(R.string.wallet_txn_reschedule_adjustment)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon Badge (+ in green circle or - in red circle)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = if (isCredit) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(if (isCredit) R.drawable.ic_arrow_downward else R.drawable.ic_arrow_upward),
                    contentDescription = null,
                    tint = if (isCredit) Color(0xFF2E7D32) else Color(0xFFC62828),
                    modifier = Modifier.size(22.dp)
                )
            }

            // Middle info: Title, description, date
            Column(modifier = Modifier.weight(1f)) {
                val titleText = if (transaction.typeEnum == WalletTransactionType.TOP_UP) {
                    if (transaction.paymentMethod != null) {
                        stringResource(R.string.wallet_txn_top_up_via, transaction.paymentMethod.displayTitle)
                    } else if (!transaction.description.isNullOrBlank() && transaction.description.contains("via", ignoreCase = true)) {
                        transaction.description
                    } else {
                        topUpLabel
                    }
                } else {
                    when (transaction.typeEnum) {
                        WalletTransactionType.APPOINTMENT_PAYMENT -> appointmentPaymentLabel
                        WalletTransactionType.REFUND -> refundLabel
                        WalletTransactionType.RESCHEDULE_ADJUSTMENT -> rescheduleAdjustmentLabel
                        WalletTransactionType.TOP_UP -> topUpLabel
                    }
                }

                Text(
                    text = titleText,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val subText = if (transaction.typeEnum == WalletTransactionType.TOP_UP) {
                    if (transaction.paymentMethod != null && transaction.description != titleText) transaction.description else null
                } else {
                    transaction.description
                }

                if (!subText.isNullOrBlank() && subText != titleText) {
                    Text(
                        text = subText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                val sign = if (isCredit) "+" else "-"
                val amountColor = if (isCredit) Color(0xFF2E7D32) else Color(0xFFC62828)

                Text(
                    text = "$sign RM ${String.format(Locale.US, "%.2f", transaction.safeAmount)}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = if (isBalanceHidden) stringResource(R.string.wallet_bal_hidden) else stringResource(R.string.wallet_bal_format, String.format(Locale.US, "%.2f", transaction.safeBalanceAfter)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun EmptyTransactionsView(
    filter: TransactionFilterOption,
    onTopUpClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_history),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
            Text(
                text = when (filter) {
                    TransactionFilterOption.ALL -> stringResource(R.string.wallet_empty_all)
                    TransactionFilterOption.TOP_UP -> stringResource(R.string.wallet_empty_topup)
                    TransactionFilterOption.PAYMENT -> stringResource(R.string.wallet_empty_payment)
                    TransactionFilterOption.REFUND -> stringResource(R.string.wallet_empty_refund)
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.wallet_empty_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (filter == TransactionFilterOption.ALL || filter == TransactionFilterOption.TOP_UP) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onTopUpClick,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(painter = painterResource(R.drawable.ic_outline_add), contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.wallet_top_up_now))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopUpBottomSheet(
    isSubmitting: Boolean,
    savedCards: List<PaymentMethod>,
    onDismiss: () -> Unit,
    onConfirmTopUp: (amount: Double, paymentId: String?) -> Unit
) {
    var customAmountText by remember { mutableStateOf("") }
    var selectedPreset by remember { mutableStateOf<Double?>(100.0) }
    var selectedCardId by remember {
        mutableStateOf(savedCards.firstOrNull { (it as? PaymentMethod.CreditCard)?.isDefault == true }?.id ?: savedCards.firstOrNull()?.id)
    }

    val presetAmounts = listOf(50.0, 100.0, 200.0, 500.0)

    val finalAmount: Double = if (selectedPreset != null) {
        selectedPreset ?: 0.0
    } else {
        customAmountText.toDoubleOrNull() ?: 0.0
    }

    val isValid = finalAmount >= 1.0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.wallet_top_up_sheet_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = stringResource(R.string.wallet_top_up_preset_prompt),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Preset Amount Grid (Flexible 2x2 layout for all screen sizes)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                presetAmounts.chunked(2).forEach { rowPresets ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowPresets.forEach { preset ->
                            val isSelected = selectedPreset == preset
                            Surface(
                                selected = isSelected,
                                onClick = {
                                    selectedPreset = preset
                                    customAmountText = ""
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface,
                                border = if (!isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Text(
                                        text = stringResource(R.string.currency_rm_int, preset.toInt()),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Custom Amount Input
            OutlinedTextField(
                value = customAmountText,
                onValueChange = { input ->
                    val filtered = input.filter { it.isDigit() || it == '.' }
                    if (filtered.count { it == '.' } <= 1) {
                        customAmountText = filtered
                        if (filtered.isNotEmpty()) {
                            selectedPreset = null
                        }
                    }
                },
                label = { Text(stringResource(R.string.wallet_custom_amount_label)) },
                placeholder = { Text(stringResource(R.string.wallet_custom_amount_placeholder)) },
                prefix = { Text(stringResource(R.string.currency_prefix), fontWeight = FontWeight.Bold) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Payment Source Selection
            if (savedCards.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.wallet_payment_method_label),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    savedCards.forEach { card ->
                        val isCardSelected = selectedCardId == card.id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedCardId = card.id },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCardSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                else MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RadioButton(
                                    selected = isCardSelected,
                                    onClick = { selectedCardId = card.id }
                                )
                                Icon(
                                    painter = painterResource(R.drawable.dollar_symbol),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = card.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isCardSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.check_circle),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(R.string.wallet_demo_gateway_label),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Action Buttons
            Button(
                onClick = { onConfirmTopUp(finalAmount, selectedCardId) },
                enabled = isValid && !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = stringResource(R.string.wallet_top_up_btn_format, String.format(Locale.US, "%.2f", finalAmount)),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private fun formatIsoDate(isoString: String?, recentFallback: String = "Recent"): String {
    if (isoString.isNullOrBlank()) return recentFallback
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val date = parser.parse(isoString.substringBefore("."))
        if (date != null) {
            val formatter = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            formatter.format(date)
        } else {
            isoString
        }
    } catch (_: Exception) {
        try {
            isoString.take(19).replace("T", " ")
        } catch (_: Exception) {
            isoString
        }
    }
}
