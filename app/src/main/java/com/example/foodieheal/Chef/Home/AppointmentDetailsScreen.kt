package com.example.foodieheal.Chef.Home

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.util.Locale
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import com.example.foodieheal.R
import com.example.foodieheal.hiring.components.RecipeDetailPreviewSheet
import com.example.foodieheal.hiring.model.Appointment
import com.example.foodieheal.hiring.model.AppointmentRecipeWithDetails
import com.example.foodieheal.hiring.model.SelectedAppointmentRecipe
import com.example.foodieheal.Chef.components.ChefQrScannerDialog
import com.example.foodieheal.ui.components.AppointmentStatusStepper
import com.example.foodieheal.ui.components.DetailSectionCard
import com.example.foodieheal.ui.components.formatToAmPm
import com.example.foodieheal.hiring.util.CalendarSyncHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentDetailScreen(
    appointment: Appointment,
    userName: String = "Client",
    userPhone: String = "",
    isNetworkAvailable: Boolean = true,
    attachedRecipes: List<AppointmentRecipeWithDetails> = emptyList(),
    isLoadingRecipes: Boolean = false,
    onBackClick: () -> Unit = {},
    onStatusChange: (newStatus: String, rejectionReason: String?) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current

    var showAcceptDialog by remember { mutableStateOf(false) }
    var showDeclineDialog by remember { mutableStateOf(false) }
    var showQrScannerDialog by remember { mutableStateOf(false) }
    var rejectionReason by remember { mutableStateOf("") }
    var rejectionReasonError by remember { mutableStateOf(false) }
    var previewingRecipeItem by remember { mutableStateOf<AppointmentRecipeWithDetails?>(null) }

    val acceptedToastMsg = stringResource(R.string.booking_accepted)
    val declinedToastMsg = stringResource(R.string.booking_declined)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.booking_details),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrowback),
                            contentDescription = stringResource(R.string.back),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Interactive Service Lifecycle Stepper
            AppointmentStatusStepper(
                currentStatus = appointment.Status.orEmpty(),
                rejectionReason = appointment.Reject_Reason
            )

            // Client Header & Status Card
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = userName.firstOrNull()?.uppercase() ?: "C",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = userName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                appointment.AppointmentID?.let { apptId ->
                                    Text(
                                        text = stringResource(R.string.appointment_id_format, apptId.take(8)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Status Badge
                        val (badgeContainerColor, badgeContentColor) = when (appointment.Status.lowercase()) {
                            "completed" -> Color(0xFFE3F2FD) to Color(0xFF1565C0) // Soft Blue
                            "confirmed" -> Color(0xFFE8F5E9) to Color(0xFF2E7D32) // Soft Green
                            "cancelled" -> Color(0xFFFFEBEE) to Color(0xFFC62828) // Soft Red
                            "rejected"  -> Color(0xFFFBE9E7) to Color(0xFFD84315) // Soft Deep Orange / Rust Red
                            "unpaid"    -> Color(0xFFFFF8E1) to Color(0xFFF57F17) // Soft Amber / Yellow-Orange
                            "pending"   -> Color(0xFFFFF3E0) to Color(0xFFE65100) // Soft Orange
                            else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = badgeContainerColor
                        ) {
                            Text(
                                text = appointment.Status.replaceFirstChar { it.uppercase() },
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = badgeContentColor
                            )
                        }
                    }

                    if (userPhone.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(10.dp))

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
                                    painter = painterResource(R.drawable.telephone),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = userPhone,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            TextButton(
                                onClick = {
                                    val callIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$userPhone"))
                                    context.startActivity(callIntent)
                                },
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(stringResource(R.string.call_client), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // Total Amount & Earnings Summary Card
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(R.drawable.wallet),
                                        contentDescription = stringResource(R.string.chef_details_earnings_desc),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = stringResource(R.string.total_booking_value),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "RM %.2f".format(appointment.Total_Price),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Payment Status Chip
                        val isPaid = appointment.Status.equals("confirmed", ignoreCase = true) ||
                                appointment.Status.equals("completed", ignoreCase = true)
                        val payStatusText = when {
                            isPaid -> stringResource(R.string.chef_details_status_paid)
                            appointment.Status.equals("cancelled", ignoreCase = true) -> stringResource(R.string.chef_details_status_cancelled)
                            appointment.Status.equals("rejected", ignoreCase = true) -> stringResource(R.string.chef_details_status_na)
                            appointment.Status.equals("unpaid", ignoreCase = true) -> stringResource(R.string.chef_details_status_awaiting_payment)
                            else -> stringResource(R.string.chef_details_status_pending)
                        }
                        val payStatusColor = when {
                            isPaid -> Color(0xFF2E7D32)
                            appointment.Status.equals("cancelled", ignoreCase = true) -> Color(0xFFC62828)
                            appointment.Status.equals("unpaid", ignoreCase = true) -> Color(0xFFF57F17)
                            else -> Color(0xFFE65100)
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = payStatusColor.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = payStatusText,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = payStatusColor
                            )
                        }
                    }
                }
            }

            // Rejection Reason Banner (if rejected)
            if (appointment.Status.equals("rejected", ignoreCase = true) && !appointment.Reject_Reason.isNullOrBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFBE9E7))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.cancel),
                                contentDescription = null,
                                tint = Color(0xFFD84315),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = stringResource(R.string.reason_for_declining),
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD84315),
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = appointment.Reject_Reason,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF3E2723)
                        )
                    }
                }
            }

            // Customer Review & Rating Banner (if completed with rating)
            if (appointment.Status.equals("completed", ignoreCase = true) && (appointment.rating ?: 0) > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.client_review_and_rating),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                (1..5).forEach { starIndex ->
                                    val isFilled = starIndex <= (appointment.rating ?: 0)
                                    Icon(
                                        painter = painterResource(
                                            if (isFilled) R.drawable.ic_star else R.drawable.ic_outline_star
                                        ),
                                        contentDescription = null,
                                        tint = if (isFilled) Color(0xFFFFB300) else MaterialTheme.colorScheme.outlineVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        if (!appointment.Comment.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.quoted_text_format, appointment.Comment),
                                style = MaterialTheme.typography.bodyMedium,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Schedule & Time Section
            DetailSectionCard(title = stringResource(R.string.date_time)) {
                DetailRow(
                    iconRes = R.drawable.ic_calendar,
                    label = stringResource(R.string.label_date),
                    value = appointment.Date
                )
                Spacer(modifier = Modifier.height(8.dp))
                val startTimeAmPm = formatToAmPm(appointment.Start_Time)
                val endTimeAmPm = formatToAmPm(appointment.End_Time)
                DetailRow(
                    iconRes = R.drawable.ic_clock,
                    label = stringResource(R.string.label_time_slot),
                    value = stringResource(
                        R.string.time_slot_format,
                        startTimeAmPm,
                        endTimeAmPm
                    )
                )
            }

            // Location & Navigation Section
            val openWithMapsChooserTitle = stringResource(R.string.chef_details_open_with_maps)
            DetailSectionCard(title = stringResource(R.string.event_location)) {
                DetailRow(
                    iconRes = R.drawable.location,
                    label = stringResource(R.string.label_address),
                    value = listOfNotNull(
                        appointment.Address.takeIf { it.isNotBlank() },
                        appointment.Postcode.takeIf { it.isNotBlank() },
                        appointment.State
                    ).joinToString(", ")
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        val mapUri = Uri.parse("geo:0,0?q=${Uri.encode("${appointment.Address}, ${appointment.State}")}")
                        val mapIntent = Intent(Intent.ACTION_VIEW, mapUri)
                        context.startActivity(Intent.createChooser(mapIntent, openWithMapsChooserTitle))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(stringResource(R.string.open_in_maps))
                }
            }

            // Dietary & Booking Details Section
            DetailSectionCard(title = stringResource(R.string.section_booking_details)) {
                DetailRow(
                    iconRes = R.drawable.serving_size,
                    label = stringResource(R.string.label_party_size),
                    value = stringResource(R.string.party_size_format, appointment.Serving_Size)
                )
                Spacer(modifier = Modifier.height(8.dp))
                DetailRow(
                    iconRes = R.drawable.health_preference,
                    label = stringResource(R.string.label_dietary_preference),
                    value = appointment.Health_Preference.ifBlank { stringResource(R.string.none_specified) }
                )

                if (appointment.Note.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.label_special_notes),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = appointment.Note,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Attached Dishes / Meal Plan Section
            DetailSectionCard(title = stringResource(R.string.section_attached_dishes)) {
                if (isLoadingRecipes) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                } else if (attachedRecipes.isEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_recipe),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = stringResource(R.string.no_recipes_requested),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        attachedRecipes.forEach { item ->
                            val recipe = item.recipe
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = recipe != null) {
                                        previewingRecipeItem = item
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    AsyncImage(
                                        model = recipe?.recipeImageUrl,
                                        contentDescription = recipe?.recipeName ?: stringResource(R.string.chef_details_recipe_image_desc),
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentScale = ContentScale.Crop,
                                        error = painterResource(R.drawable.ic_recipe),
                                        placeholder = painterResource(R.drawable.ic_recipe)
                                    )

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = recipe?.recipeName ?: stringResource(R.string.default_recipe_name, item.recipeId),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 14.sp
                                        )

                                        Spacer(modifier = Modifier.height(3.dp))

                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(2.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.recipe_portions_format, item.service_count.toInt()),
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    fontSize = 10.sp,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }

                                            if ((recipe?.calories ?: 0) > 0) {
                                                Text(
                                                    text = stringResource(R.string.recipe_calories_format, recipe?.calories ?: 0),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 11.sp,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }

                                            if ((recipe?.time ?: 0) > 0) {
                                                Text(
                                                    text = stringResource(R.string.recipe_time_format, recipe?.time ?: 0),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 11.sp,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        }

                                        if (!item.custom_note.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(
                                                text = stringResource(R.string.client_note_format, item.custom_note),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                                fontSize = 11.sp,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    if (recipe != null) {
                                        IconButton(
                                            onClick = { previewingRecipeItem = item },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_recipe),
                                                contentDescription = stringResource(R.string.chef_details_view_details_desc),
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Review Section
            if (appointment.Status.equals("completed", ignoreCase = true)) {
                DetailSectionCard(title = stringResource(R.string.review)) {
                    val hasReviewed = (appointment.rating != null && appointment.rating > 0)
                    if (hasReviewed) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = userName.take(1).uppercase(Locale.ROOT),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        Column {
                                            Text(
                                                text = userName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (!appointment.Date.isNullOrBlank()) {
                                                Text(
                                                    text = appointment.Date,
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                                )
                                            }
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = Color(0xFFFFF8E1),
                                        border = BorderStroke(1.dp, Color(0xFFFFE082))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_star),
                                                contentDescription = null,
                                                tint = Color(0xFFFFB300),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = stringResource(R.string.rating_format, appointment.rating ?: 0),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF795548)
                                            )
                                        }
                                    }
                                }

                                if (!appointment.Comment.isNullOrBlank()) {
                                    Text(
                                        text = stringResource(R.string.quoted_text_format, appointment.Comment),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_star),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = stringResource(R.string.no_review_submitted),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Action Buttons
            if (appointment.Status.equals("pending", ignoreCase = true)) {
                if (!isNetworkAvailable) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.wifi_off),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.desc_no_network),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            rejectionReason = ""
                            rejectionReasonError = false
                            showDeclineDialog = true
                        },
                        enabled = isNetworkAvailable,
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.decline),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Button(
                        onClick = { showAcceptDialog = true },
                        enabled = isNetworkAvailable,
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.accept_booking),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Action Buttons: Scan QR to Complete & Add to Calendar (Confirmed Status)
            if (appointment.Status.equals("confirmed", ignoreCase = true)) {
                Button(
                    onClick = { showQrScannerDialog = true },
                    enabled = isNetworkAvailable,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 52.dp),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_check),
                            contentDescription = stringResource(R.string.chef_details_scan_qr_desc),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.scan_client_qr_to_complete),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Add to Calendar Button (Chef Perspective)
                OutlinedButton(
                    onClick = {
                        val loc = listOfNotNull(
                            appointment.Address.takeIf { it.isNotBlank() },
                            appointment.Postcode.takeIf { it.isNotBlank() },
                            appointment.State.takeIf { it.isNotBlank() }
                        ).joinToString(", ")
                        val desc = "FoodieHeal Appointment with Client $userName\n" +
                                "Booking ID: ${appointment.AppointmentID.orEmpty()}\n" +
                                "Serving Size: ${appointment.Serving_Size} portions\n" +
                                "Dietary Preference: ${appointment.Health_Preference}\n" +
                                if (appointment.Note.isNotBlank()) "Notes: ${appointment.Note}" else ""

                        Toast.makeText(context, R.string.toast_opening_calendar, Toast.LENGTH_SHORT).show()
                        CalendarSyncHelper.addAppointmentToCalendar(
                            context = context,
                            title = "FoodieHeal Session - Client $userName",
                            description = desc.trim(),
                            location = loc,
                            dateStr = appointment.Date,
                            startTimeStr = appointment.Start_Time,
                            endTimeStr = appointment.End_Time
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 50.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_calendar),
                            contentDescription = stringResource(R.string.add_to_calendar),
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.add_to_calendar),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Bottom spacer for comfortable scrolling clearance above navigation bars
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Accept Confirmation Dialog
    if (showAcceptDialog) {
        AlertDialog(
            onDismissRequest = { showAcceptDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.accept_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(
                        R.string.accept_body_format,
                        userName,
                        appointment.Date
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAcceptDialog = false
                        Toast.makeText(context, acceptedToastMsg, Toast.LENGTH_SHORT).show()
                        onStatusChange("Unpaid", null)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(stringResource(R.string.confirm_accept))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAcceptDialog = false }) {
                    Text(
                        text = stringResource(R.string.cancel),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Decline Dialog
    if (showDeclineDialog) {
        AlertDialog(
            onDismissRequest = { showDeclineDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.decline_title),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.decline_body_format, userName),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = rejectionReason,
                        onValueChange = {
                            rejectionReason = it
                            if (it.isNotBlank()) rejectionReasonError = false
                        },
                        placeholder = { Text(stringResource(R.string.decline_reason)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5,
                        isError = rejectionReasonError,
                        supportingText = {
                            if (rejectionReasonError) {
                                Text(
                                    text = stringResource(R.string.rejection_reason_empty),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (rejectionReason.isBlank()) {
                            rejectionReasonError = true
                        } else {
                            showDeclineDialog = false
                            Toast.makeText(context, declinedToastMsg, Toast.LENGTH_SHORT).show()
                            onStatusChange("Rejected", rejectionReason.trim())
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.confirm_decline))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeclineDialog = false }) {
                    Text(
                        text = stringResource(R.string.cancel),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Recipe Detail Sheet Modal for Chef
    previewingRecipeItem?.let { item ->
        item.recipe?.let { targetRecipe ->
            val selectedState = SelectedAppointmentRecipe(
                recipe = targetRecipe,
                serviceCount = item.service_count.toInt(),
                customNote = item.custom_note.orEmpty()
            )
            RecipeDetailPreviewSheet(
                recipe = targetRecipe,
                selectedRecipeState = selectedState,
                isReadOnly = true,
                onDismiss = { previewingRecipeItem = null }
            )
        }
    }

    if (showQrScannerDialog) {
        ChefQrScannerDialog(
            expectedAppointmentId = appointment.AppointmentID.orEmpty(),
            expectedChefId = appointment.chefId,
            onVerified = {
                showQrScannerDialog = false
                onStatusChange("Completed", null)
                Toast.makeText(
                    context,
                    R.string.toast_service_verified_completed,
                    Toast.LENGTH_LONG
                ).show()
            },
            onDismiss = { showQrScannerDialog = false }
        )
    }
}


// Reusable Information Row
@Composable
fun DetailRow(
    iconRes: Int,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}