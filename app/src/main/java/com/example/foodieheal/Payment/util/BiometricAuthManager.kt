package com.example.foodieheal.Payment.util

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

enum class BiometricStatus {
    AVAILABLE,
    NOT_ENROLLED,
    NO_HARDWARE,
    UNAVAILABLE
}

// Reusable, production-grade Biometric Authentication Manager.
object BiometricAuthManager {

    fun checkBiometricAvailability(context: Context): BiometricStatus {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NOT_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NO_HARDWARE
            else -> BiometricStatus.UNAVAILABLE
        }
    }

    // Triggers the native Biometric Prompt dialog for payment confirmation.
    fun promptBiometricAuth(
        activity: FragmentActivity,
        title: String = "Confirm Payment",
        subtitle: String = "Authenticate to authorize transaction",
        description: String? = null,
        negativeButtonText: String = "Cancel",
        onSuccess: () -> Unit,
        onError: (errorMessage: String) -> Unit = {},
        onCancel: () -> Unit = {}
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)

        description?.let { promptInfoBuilder.setDescription(it) }

        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    when (errorCode) {
                        BiometricPrompt.ERROR_USER_CANCELED,
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                        BiometricPrompt.ERROR_CANCELED -> {
                            onCancel()
                        }
                        else -> {
                            onError(errString.toString())
                        }
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    // Biometric sensor did not recognize fingerprint/face - Android UI handles the retry animation
                }
            }
        )

        biometricPrompt.authenticate(promptInfoBuilder.build())
    }
}
