package com.example.foodieheal.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.annotation.StringRes
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodieheal.R
import java.util.Locale

enum class AppointmentProgressStep(@get:StringRes val labelRes: Int, val iconRes: Int) {
    REQUESTED(R.string.stepper_step_requested, R.drawable.ic_clock),
    PAYMENT(R.string.stepper_step_payment, R.drawable.wallet),
    CONFIRMED(R.string.stepper_step_confirmed, R.drawable.ic_check_circle),
    COMPLETED(R.string.stepper_step_completed, R.drawable.ic_star)
}


@Composable
fun AppointmentStatusStepper(
    currentStatus: String,
    rejectionReason: String? = null,
    modifier: Modifier = Modifier
) {
    val cleanStatus = currentStatus.lowercase(Locale.ROOT).trim()

    // Handle terminal negative states with an alert card
    if (cleanStatus == "cancelled" || cleanStatus == "rejected") {
        val isRejected = cleanStatus == "rejected"
        val title = if (isRejected) {
            stringResource(R.string.stepper_appointment_rejected)
        } else {
            stringResource(R.string.stepper_appointment_cancelled)
        }
        val subtitle = if (isRejected) {
            if (!rejectionReason.isNullOrBlank()) {
                stringResource(R.string.stepper_rejection_reason_format, rejectionReason)
            } else {
                stringResource(R.string.stepper_rejection_default_subtitle)
            }
        } else {
            stringResource(R.string.stepper_cancelled_default_subtitle)
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f)
            ),
            modifier = modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_cancel),
                        contentDescription = title,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f),
                        lineHeight = 16.sp
                    )
                }
            }
        }
        return
    }

    val activeIndex = when (cleanStatus) {
        "pending" -> 0
        "unpaid" -> 1
        "confirmed" -> 2
        "completed" -> 3
        else -> 0
    }

    val statusHint = when (cleanStatus) {
        "pending" -> stringResource(R.string.stepper_hint_pending)
        "unpaid" -> stringResource(R.string.stepper_hint_unpaid)
        "confirmed" -> stringResource(R.string.stepper_hint_confirmed)
        "completed" -> stringResource(R.string.stepper_hint_completed)
        else -> stringResource(R.string.stepper_hint_in_progress)
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.stepper_booking_lifecycle),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                AppointmentStatusBadge(status = currentStatus)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Stepper timeline row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val steps = AppointmentProgressStep.values()
                steps.forEachIndexed { index, step ->
                    val stepLabel = stringResource(step.labelRes)
                    val isCompleted = index < activeIndex
                    val isCurrent = index == activeIndex

                    val circleBgColor by animateColorAsState(
                        targetValue = when {
                            isCompleted -> MaterialTheme.colorScheme.primary
                            isCurrent -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        },
                        label = "circleBg"
                    )

                    val iconTint by animateColorAsState(
                        targetValue = when {
                            isCompleted -> Color.White
                            isCurrent -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                        },
                        label = "iconTint"
                    )

                    // Step Node (Circle + Label)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(62.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(circleBgColor)
                                .then(
                                    if (isCurrent) {
                                        Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    } else Modifier
                                )
                        ) {
                            Icon(
                                painter = painterResource(
                                    id = if (isCompleted) R.drawable.ic_check else step.iconRes
                                ),
                                contentDescription = stepLabel,
                                tint = iconTint,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = stepLabel,
                            fontSize = 10.sp,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }

                    // Progress Connecting Bar (between steps)
                    if (index < steps.size - 1) {
                        val barColor by animateColorAsState(
                            targetValue = if (index < activeIndex) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            },
                            label = "barColor"
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(barColor)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Micro hint container below stepper
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Text(
                        text = statusHint,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
