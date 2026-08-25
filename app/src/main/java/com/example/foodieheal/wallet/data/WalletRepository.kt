package com.example.foodieheal.wallet.data

import android.util.Log
import com.example.foodieheal.Payment.data.payment
import com.example.foodieheal.SupabaseClient.client
import com.example.foodieheal.hiring.model.Appointment
import com.example.foodieheal.wallet.local.WalletDao
import com.example.foodieheal.wallet.local.toDomainModel
import com.example.foodieheal.wallet.local.toRoomEntity
import com.example.foodieheal.wallet.model.Wallet
import com.example.foodieheal.wallet.model.WalletTransaction
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import java.util.UUID

class WalletRepository(
    private val supabaseClient: SupabaseClient = client,
    private val dao: WalletDao? = null
) {
    companion object {
        private const val TAG = "WalletRepository"
    }

    fun getCurrentUserId(): String? {
        return supabaseClient.auth.currentUserOrNull()?.id
    }

    suspend fun getWallet(userId: String): Wallet = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext Wallet(isActive = false)

        try {
            val existing = supabaseClient.from("wallet")
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeSingleOrNull<Wallet>()

            if (existing != null) {
                dao?.insertWallet(existing.toRoomEntity())
                return@withContext existing
            }

            // Fallback to local Room if present
            val local = dao?.getWalletDirect(userId)?.toDomainModel()
            local ?: Wallet(userId = userId, balance = 0.0, isActive = false)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching wallet for $userId: ${e.localizedMessage}", e)
            val cached = dao?.getWalletDirect(userId)?.toDomainModel()
            cached ?: Wallet(userId = userId, balance = 0.0, isActive = false)
        }
    }

    suspend fun getTransactions(walletId: String): List<WalletTransaction> = withContext(Dispatchers.IO) {
        if (walletId.isBlank()) return@withContext emptyList()

        try {
            // 1. Fetch transactions directly from wallet_transaction table
            val txns = supabaseClient.from("wallet_transaction")
                .select {
                    filter {
                        eq("wallet_id", walletId)
                    }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<WalletTransaction>()

            // 2. Fetch payment method summaries for transactions that recorded a paymentMethod_id
            val methodIds = txns.mapNotNull { it.paymentMethodId }.filter { it.isNotBlank() }.distinct()
            val methodsMap = if (methodIds.isNotEmpty()) {
                try {
                    supabaseClient.from("payment_method")
                        .select {
                            filter {
                                isIn("payment_method_id", methodIds)
                            }
                        }
                        .decodeList<com.example.foodieheal.wallet.model.PaymentMethodRecord>()
                        .associateBy { it.paymentMethodId }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to load payment methods for transactions: ${e.localizedMessage}")
                    emptyMap()
                }
            } else {
                emptyMap()
            }

            val enriched = txns.map { txn ->
                val pm = txn.paymentMethodId?.let { methodsMap[it]?.toSummary() } ?: txn.paymentMethod
                txn.copy(paymentMethod = pm)
            }

            // Save to Room cache
            if (enriched.isNotEmpty()) {
                dao?.insertTransactions(enriched.map { it.toRoomEntity() })
            }

            enriched
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching wallet transactions for $walletId: ${e.localizedMessage}", e)
            val cached = dao?.getTransactionsDirect(walletId)?.map { it.toDomainModel() }
            cached ?: emptyList()
        }
    }

    suspend fun topUpWallet(
        userId: String,
        amount: Double,
        paymentMethodId: String? = null,
        description: String = "Wallet Top Up"
    ): Result<Wallet> = withContext(Dispatchers.IO) {
        if (amount <= 0) {
            return@withContext Result.failure(IllegalArgumentException("Amount must be greater than RM 0.00"))
        }

        try {
            // Attempt Supabase PL/pgSQL RPC
            try {
                supabaseClient.postgrest.rpc(
                    function = "top_up_wallet",
                    parameters = buildJsonObject {
                        put("p_user_id", userId)
                        put("p_amount", amount)
                        if (paymentMethodId != null) put("p_payment_method_id", paymentMethodId)
                        put("p_payment_id", null as String?)
                        put("p_description", description)
                    }
                )
            } catch (rpcEx: Exception) {
                Log.w(TAG, "RPC top_up_wallet failed or not yet deployed, falling back to direct table update: ${rpcEx.localizedMessage}")
                fallbackTopUp(userId, amount, paymentMethodId, description)
            }

            // Retrieve updated wallet and cache
            val updated = getWallet(userId)
            dao?.insertWallet(updated.toRoomEntity())
            Result.success(updated)
        } catch (e: Exception) {
            Log.e(TAG, "Top-up failed: ${e.localizedMessage}", e)
            Result.failure(e)
        }
    }

    private suspend fun fallbackTopUp(
        userId: String,
        amount: Double,
        paymentMethodId: String?,
        description: String
    ) {
        val existing = try {
            supabaseClient.from("wallet")
                .select { filter { eq("user_id", userId) } }
                .decodeSingleOrNull<Wallet>()
        } catch (_: Exception) { null }

        val walletId: String
        val balanceBefore: Double
        val balanceAfter: Double

        if (existing == null) {
            // 1st time top-up: Create wallet record into Supabase on-demand
            walletId = UUID.randomUUID().toString()
            balanceBefore = 0.0
            balanceAfter = amount

            val newWallet = Wallet(
                id = walletId,
                userId = userId,
                balance = balanceAfter,
                isActive = true,
                createdAt = Instant.now().toString(),
                updateAt = Instant.now().toString()
            )
            supabaseClient.from("wallet").insert(newWallet)
            dao?.insertWallet(newWallet.toRoomEntity())
        } else {
            walletId = existing.id
            balanceBefore = existing.balance ?: 0.0
            balanceAfter = balanceBefore + amount

            supabaseClient.from("wallet").update({
                set("balance", balanceAfter)
                set("is_Active", true)
                set("update_at", Instant.now().toString())
            }) {
                filter { eq("id", walletId) }
            }
            dao?.insertWallet(existing.copy(balance = balanceAfter, isActive = true).toRoomEntity())
        }

        val txn = WalletTransaction(
            id = UUID.randomUUID().toString(),
            walletId = walletId,
            paymentId = null, // Payment table is only for appointments; null for top-up
            paymentMethodId = paymentMethodId,
            transactionType = "TOP_UP",
            amount = amount,
            balanceBefore = balanceBefore,
            balanceAfter = balanceAfter,
            description = description,
            createdAt = Instant.now().toString()
        )
        supabaseClient.from("wallet_transaction").insert(txn)
        dao?.insertTransaction(txn.toRoomEntity())
    }

    suspend fun payAppointmentViaWallet(
        appointmentId: String,
        userId: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val currentWallet = getWallet(userId)

            if (currentWallet.isActive != true || currentWallet.id.isBlank()) {
                return@withContext Result.failure(IllegalStateException("Wallet is inactive. Please activate your wallet with a top-up first."))
            }

            try {
                supabaseClient.postgrest.rpc(
                    function = "pay_appointment_via_wallet",
                    parameters = buildJsonObject {
                        put("p_appointment_id", appointmentId)
                        put("p_user_id", userId)
                    }
                )
                val refreshed = getWallet(userId)
                dao?.insertWallet(refreshed.toRoomEntity())
                return@withContext Result.success("TXN-WALLET-${System.currentTimeMillis()}")
            } catch (rpcEx: Exception) {
                Log.w(TAG, "RPC pay_appointment_via_wallet failed, falling back to direct table update: ${rpcEx.localizedMessage}")
                val txnId = fallbackPayAppointment(appointmentId, userId, currentWallet)
                return@withContext Result.success(txnId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Appointment wallet checkout failed: ${e.localizedMessage}", e)
            Result.failure(e)
        }
    }

    private suspend fun fallbackPayAppointment(
        appointmentId: String,
        userId: String,
        wallet: Wallet
    ): String {
        if (wallet.isActive != true || wallet.id.isBlank()) {
            throw IllegalStateException("Wallet is inactive. Please activate your wallet with a top-up first.")
        }

        val appt = supabaseClient.from("Appointment")
            .select { filter { eq("AppointmentID", appointmentId) } }
            .decodeSingle<Appointment>()

        val price = appt.Total_Price
        val walletBalance = wallet.balance ?: 0.0
        if (walletBalance < price) {
            throw IllegalStateException(
                "Insufficient wallet balance. Available: RM ${String.format("%.2f", walletBalance)}, Required: RM ${String.format("%.2f", price)}"
            )
        }

        val balanceBefore = walletBalance
        val balanceAfter = balanceBefore - price

        // Deduct wallet balance
        supabaseClient.from("wallet").update({
            set("balance", balanceAfter)
            set("update_at", Instant.now().toString())
        }) {
            filter { eq("id", wallet.id) }
        }
        dao?.insertWallet(wallet.copy(balance = balanceAfter).toRoomEntity())

        val targetPaymentId = if (!appt.PaymentId.isNullOrBlank()) appt.PaymentId else UUID.randomUUID().toString()
        val txnId = "TXN-WALLET-${System.currentTimeMillis()}"

        val paymentRecord = payment(
            paymentId = targetPaymentId,
            transactionId = txnId,
            appointmentId = appointmentId,
            userId = userId,
            totalAmount = price,
            paymentMethod = "In-App Wallet",
            paymentMethodId = null,
            status = "Completed",
            payAt = Instant.now().toString(),
            createdAt = Instant.now().toString()
        )
        supabaseClient.from("Payment").upsert(paymentRecord)

        val txn = WalletTransaction(
            id = UUID.randomUUID().toString(),
            walletId = wallet.id,
            paymentId = targetPaymentId,
            transactionType = "APPOINTMENT_PAYMENT",
            amount = price,
            balanceBefore = balanceBefore,
            balanceAfter = balanceAfter,
            description = "Appointment Booking Payment (#${appointmentId.take(8)})",
            createdAt = Instant.now().toString()
        )
        supabaseClient.from("wallet_transaction").insert(txn)
        dao?.insertTransaction(txn.toRoomEntity())

        supabaseClient.from("Appointment").update({
            set("Status", "Confirmed")
            set("PaymentId", targetPaymentId)
        }) {
            filter { eq("AppointmentID", appointmentId) }
        }

        return txnId
    }

    suspend fun refundForCancellation(
        appointmentId: String,
        userId: String,
        refundAmount: Double? = null,
        paymentId: String? = null,
        reason: String = "Cancellation"
    ): Result<Double> = withContext(Dispatchers.IO) {
        refundAppointmentToWallet(
            appointmentId = appointmentId,
            userId = userId,
            refundAmount = refundAmount,
            paymentId = paymentId,
            reason = reason
        )
    }

    suspend fun refundForReschedule(
        appointmentId: String,
        userId: String,
        refundAmount: Double? = null,
        paymentId: String? = null,
        reason: String = "Reschedule"
    ): Result<Double> = withContext(Dispatchers.IO) {
        refundAppointmentToWallet(
            appointmentId = appointmentId,
            userId = userId,
            refundAmount = refundAmount,
            paymentId = paymentId,
            reason = reason
        )
    }

    suspend fun refundAppointmentToWallet(
        appointmentId: String,
        userId: String,
        refundAmount: Double? = null,
        paymentId: String? = null,
        reason: String = "Appointment Cancellation"
    ): Result<Double> = withContext(Dispatchers.IO) {
        try {
            try {
                supabaseClient.postgrest.rpc(
                    function = "refund_appointment_to_wallet",
                    parameters = buildJsonObject {
                        put("p_appointment_id", appointmentId)
                        put("p_user_id", userId)
                        put("p_reason", reason)
                    }
                )
                val refreshed = getWallet(userId)
                dao?.insertWallet(refreshed.toRoomEntity())
                val amount = refundAmount ?: refreshed.balance ?: 0.0
                return@withContext Result.success(amount)
            } catch (rpcEx: Exception) {
                Log.w(TAG, "RPC refund_appointment_to_wallet failed, falling back to direct table update: ${rpcEx.localizedMessage}")
                val credited = fallbackRefund(appointmentId, userId, refundAmount, paymentId, reason)
                return@withContext Result.success(credited)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Refund to wallet failed: ${e.localizedMessage}", e)
            Result.failure(e)
        }
    }

    private suspend fun fallbackRefund(
        appointmentId: String,
        userId: String,
        explicitAmount: Double?,
        explicitPaymentId: String?,
        reason: String
    ): Double {
        val existing = try {
            supabaseClient.from("wallet")
                .select { filter { eq("user_id", userId) } }
                .decodeSingleOrNull<Wallet>()
        } catch (_: Exception) { null }

        var refundAmount = explicitAmount ?: 0.0
        var targetPaymentId = explicitPaymentId
        if (refundAmount <= 0.0) {
            val appt = try {
                supabaseClient.from("Appointment")
                    .select { filter { eq("AppointmentID", appointmentId) } }
                    .decodeSingleOrNull<Appointment>()
            } catch (_: Exception) { null }

            refundAmount = appt?.Total_Price ?: 0.0
            if (targetPaymentId.isNullOrBlank()) {
                targetPaymentId = appt?.PaymentId
            }
        }

        if (refundAmount <= 0.0) return 0.0

        val walletId: String
        val balanceBefore: Double
        val balanceAfter: Double

        if (existing == null) {
            walletId = UUID.randomUUID().toString()
            balanceBefore = 0.0
            balanceAfter = refundAmount

            val newWallet = Wallet(
                id = walletId,
                userId = userId,
                balance = balanceAfter,
                isActive = true,
                createdAt = Instant.now().toString(),
                updateAt = Instant.now().toString()
            )
            supabaseClient.from("wallet").insert(newWallet)
            dao?.insertWallet(newWallet.toRoomEntity())
        } else {
            walletId = existing.id
            balanceBefore = existing.balance ?: 0.0
            balanceAfter = balanceBefore + refundAmount

            supabaseClient.from("wallet").update({
                set("balance", balanceAfter)
                set("is_Active", true)
                set("update_at", Instant.now().toString())
            }) {
                filter { eq("id", walletId) }
            }
            dao?.insertWallet(existing.copy(balance = balanceAfter, isActive = true).toRoomEntity())
        }

        // Verify payment record exists in Payment table before inserting foreign key
        var verifiedPaymentId: String? = null
        if (!targetPaymentId.isNullOrBlank()) {
            try {
                val p = supabaseClient.from("Payment")
                    .select { filter { eq("paymentId", targetPaymentId) } }
                    .decodeSingleOrNull<payment>()
                if (p != null) {
                    verifiedPaymentId = targetPaymentId
                    supabaseClient.from("Payment").update({
                        set("status", "Refunded")
                    }) {
                        filter { eq("paymentId", targetPaymentId) }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not verify/update payment status: ${e.localizedMessage}")
            }
        }

        // Log refund transaction
        val txn = WalletTransaction(
            id = UUID.randomUUID().toString(),
            walletId = walletId,
            paymentId = verifiedPaymentId,
            transactionType = "REFUND",
            amount = refundAmount,
            balanceBefore = balanceBefore,
            balanceAfter = balanceAfter,
            description = "Refund for Appointment #${appointmentId.take(8)} ($reason)",
            createdAt = Instant.now().toString()
        )
        supabaseClient.from("wallet_transaction").insert(txn)
        dao?.insertTransaction(txn.toRoomEntity())

        return refundAmount
    }

    suspend fun adjustRescheduleWallet(
        appointmentId: String,
        userId: String,
        amountDiff: Double,
        description: String = "Reschedule Price Adjustment"
    ): Result<Double> = withContext(Dispatchers.IO) {
        if (amountDiff == 0.0) return@withContext Result.success(0.0)

        try {
            try {
                supabaseClient.postgrest.rpc(
                    function = "adjust_reschedule_wallet",
                    parameters = buildJsonObject {
                        put("p_appointment_id", appointmentId)
                        put("p_user_id", userId)
                        put("p_amount_diff", amountDiff)
                        put("p_description", description)
                    }
                )
                val refreshed = getWallet(userId)
                dao?.insertWallet(refreshed.toRoomEntity())
                return@withContext Result.success(amountDiff)
            } catch (rpcEx: Exception) {
                Log.w(TAG, "RPC adjust_reschedule_wallet failed, falling back: ${rpcEx.localizedMessage}")
                val wallet = getWallet(userId)
                if (wallet.isActive != true || wallet.id.isBlank()) {
                    throw IllegalStateException("Wallet is inactive. Please activate your wallet first.")
                }

                val absDiff = kotlin.math.abs(amountDiff)
                val isSurcharge = amountDiff > 0

                val balanceBefore = wallet.balance ?: 0.0
                if (isSurcharge && balanceBefore < absDiff) {
                    throw IllegalStateException("Insufficient balance for reschedule surcharge of RM ${String.format("%.2f", absDiff)}")
                }

                val balanceAfter = if (isSurcharge) balanceBefore - absDiff else balanceBefore + absDiff

                supabaseClient.from("wallet").update({
                    set("balance", balanceAfter)
                    set("update_at", Instant.now().toString())
                }) {
                    filter { eq("id", wallet.id) }
                }
                dao?.insertWallet(wallet.copy(balance = balanceAfter).toRoomEntity())

                val txn = WalletTransaction(
                    id = UUID.randomUUID().toString(),
                    walletId = wallet.id,
                    transactionType = "RESCHEDULE_ADJUSTMENT",
                    amount = absDiff,
                    balanceBefore = balanceBefore,
                    balanceAfter = balanceAfter,
                    description = description,
                    createdAt = Instant.now().toString()
                )
                supabaseClient.from("wallet_transaction").insert(txn)
                dao?.insertTransaction(txn.toRoomEntity())

                return@withContext Result.success(amountDiff)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Reschedule adjustment failed: ${e.localizedMessage}", e)
            Result.failure(e)
        }
    }
}
