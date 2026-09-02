package com.example.foodieheal.Chef.notification

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Service to dispatch automated notification emails to chefs upon
 * registration approval or rejection by the Admin.
 */
object ChefEmailNotificationService {

    private const val TAG = "ChefEmailService"

    // Configuration for EmailJS
    var EMAILJS_SERVICE_ID = "service_wrt4scb"
    var EMAILJS_TEMPLATE_ID = "template_lixhcui"
    var EMAILJS_PUBLIC_KEY = "CXYjljzKoitemJbfr"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    /**
     * Sends an official approval notification email to the chef.
     */
    suspend fun sendChefApprovalEmail(
        toEmail: String,
        chefName: String
    ): Boolean = withContext(Dispatchers.IO) {
        val subject = "Congratulations! Your FoodieHeal Chef Application is Approved 🎉"
        val message = """
            Dear $chefName,

            Congratulations! We are delighted to inform you that your application to become a certified chef on FoodieHeal has been APPROVED by our administration team.

            What you can do now:
            1. Log in to the FoodieHeal app using your chef account.
            2. Configure your weekly availability schedule so clients can discover you.
            3. Set your rate per hour and start receiving booking appointments!

            Welcome to the FoodieHeal culinary family!

            Warm regards,
            The FoodieHeal Administration Team
        """.trimIndent()

        sendViaEmailJs(
            toEmail = toEmail,
            toName = chefName,
            subject = subject,
            message = message,
            status = "Approved"
        )
    }

    suspend fun sendChefRejectionEmail(
        toEmail: String,
        chefName: String,
        rejectionReason: String
    ): Boolean = withContext(Dispatchers.IO) {
        val subject = "Update Regarding Your FoodieHeal Chef Application"
        val message = """
            Dear $chefName,

            Thank you for your interest in joining FoodieHeal as a certified chef.

            After careful review of your application, we regret to inform you that we are unable to approve your application at this time.

            Reason for decision:
            $rejectionReason

            You are welcome to re-apply through your FoodieHeal profile after updating your qualifications or addressing the feedback above.

            Sincerely,
            The FoodieHeal Administration Team
        """.trimIndent()

        sendViaEmailJs(
            toEmail = toEmail,
            toName = chefName,
            subject = subject,
            message = message,
            status = "Rejected"
        )
    }

    private fun sendViaEmailJs(
        toEmail: String,
        toName: String,
        subject: String,
        message: String,
        status: String
    ): Boolean {
        if (toEmail.isBlank()) {
            Log.w(TAG, "Cannot send status email: recipient email address is blank.")
            return false
        }

        Log.i(TAG, "Dispatching EmailJS notification to $toEmail [$status]...")

        val jsonPayload = JSONObject().apply {
            put("service_id", EMAILJS_SERVICE_ID)
            put("template_id", EMAILJS_TEMPLATE_ID)
            put("user_id", EMAILJS_PUBLIC_KEY)
            put("template_params", JSONObject().apply {
                put("to_name", toName)
                put("to_email", toEmail)
                put("subject", subject)
                put("message", message)
                put("status", status)
            })
        }

        val requestBody = jsonPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url("https://api.emailjs.com/api/v1.0/email/send")
            .addHeader("Content-Type", "application/json")
            .addHeader("origin", "http://localhost")
            .addHeader("User-Agent", "Mozilla/5.0")
            .post(requestBody)
            .build()

        return try {
            // Using .use automatically closes the response body to prevent network socket leaks
            httpClient.newCall(request).execute().use { response ->
                val isSuccess = response.isSuccessful
                if (isSuccess) {
                    Log.i(TAG, "Successfully sent email to $toEmail via EmailJS! (HTTP ${response.code})")
                } else {
                    val errorBody = response.body?.string().orEmpty()
                    Log.w(TAG, "EmailJS error (HTTP ${response.code}): $errorBody")
                }
                isSuccess
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network error while calling EmailJS: ${e.message}", e)
            false
        }
    }
}
