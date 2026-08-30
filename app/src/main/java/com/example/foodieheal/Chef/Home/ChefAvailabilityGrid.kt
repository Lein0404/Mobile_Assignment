package com.example.foodieheal.Chef.Home

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodieheal.Chef.model.Chef
import com.example.foodieheal.Chef.model.DayOfWeekKey
import com.example.foodieheal.Chef.model.TimeSlotKey
import com.example.foodieheal.Chef.model.WeeklyAvailability
import com.example.foodieheal.R
import com.example.foodieheal.User.viewModel.AuthViewModel
import java.util.Calendar
import java.util.Locale

@Composable
fun ChefAvailabilityCard(
    chef: Chef,
    authViewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var availability by remember(chef.availability_hours) {
        mutableStateOf(WeeklyAvailability.fromJsonElement(chef.availability_hours))
    }
    var isSaving by remember { mutableStateOf(false) }

    val saveSuccessMsg = stringResource(R.string.toast_chef_avail_save_success)
    val saveErrorFormat = stringResource(R.string.toast_chef_avail_save_error)

    val todayKey = remember {
        DayOfWeekKey.fromCalendarDay(Calendar.getInstance().get(Calendar.DAY_OF_WEEK))
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_clock),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.chef_avail_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.chef_avail_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            val isConfigured = com.example.foodieheal.Chef.model.WeeklyAvailability.isConfigured(chef.availability_hours)
            if (!isConfigured) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = stringResource(R.string.chef_avail_note_label), fontSize = 14.sp)
                        Text(
                            text = stringResource(R.string.chef_avail_not_configured_warning),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Quick Preset Selection Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PresetChip(
                    label = stringResource(R.string.chef_avail_preset_weekdays),
                    onClick = { availability = WeeklyAvailability.weekdaysOnly() },
                    modifier = Modifier.weight(1f)
                )
                PresetChip(
                    label = stringResource(R.string.chef_avail_preset_all_days),
                    onClick = { availability = WeeklyAvailability.allEnabled() },
                    modifier = Modifier.weight(1f)
                )
                PresetChip(
                    label = stringResource(R.string.chef_avail_preset_clear),
                    onClick = { availability = WeeklyAvailability.allDisabled() },
                    modifier = Modifier.weight(1f)
                )
            }

            // Grid Column Headers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.chef_avail_col_day),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(46.dp),
                    textAlign = TextAlign.Start
                )

                TimeSlotKey.values().forEach { slot ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = slot.displayName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = when (slot) {
                                TimeSlotKey.MORNING -> stringResource(R.string.chef_avail_slot_morning_time)
                                TimeSlotKey.AFTERNOON -> stringResource(R.string.chef_avail_slot_afternoon_time)
                                TimeSlotKey.EVENING -> stringResource(R.string.chef_avail_slot_evening_time)
                            },
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // 7 Day × 3 Slots Row
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DayOfWeekKey.values().forEach { dayKey ->
                    val isToday = dayKey == todayKey
                    val dayAvail = availability.getDay(dayKey)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                else Color.Transparent
                            )
                            .padding(vertical = 4.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Day Indicator Pill
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.width(44.dp).height(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = dayKey.shortName,
                                    fontSize = 11.sp,
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isToday) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // 3 Interactive Slot Toggle Cells
                        TimeSlotKey.values().forEach { slot ->
                            val isSelected = dayAvail.isSlotAvailable(slot)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                AvailabilitySlotCell(
                                    isSelected = isSelected,
                                    onClick = {
                                        availability = availability.toggleSlot(dayKey, slot)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Save Button
            Button(
                onClick = {
                    if (isSaving) return@Button
                    isSaving = true
                    val updatedJson = availability.toJsonElement()

                    authViewModel.updateChefAvailability(
                        weeklyAvailability = availability,
                        onSuccess = {
                            isSaving = false
                            Toast.makeText(context, saveSuccessMsg, Toast.LENGTH_SHORT).show()
                        },
                        onError = { errorMsg ->
                            isSaving = false
                            Toast.makeText(context, String.format(Locale.getDefault(), saveErrorFormat, errorMsg), Toast.LENGTH_LONG).show()
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Text(
                        text = stringResource(R.string.chef_avail_save),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun PresetChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = modifier.height(30.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AvailabilitySlotCell(
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        animationSpec = tween(durationMillis = 180),
        label = "slot_bg"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
        animationSpec = tween(durationMillis = 180),
        label = "slot_content"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
            .border(
                width = 1.dp,
                color = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp)
            )
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (isSelected) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.check_circle),
                        contentDescription = "Available",
                        tint = contentColor,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "ON",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                }
            } else {
                Text(
                    text = "OFF",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Normal,
                    color = contentColor
                )
            }
        }
    }
}
