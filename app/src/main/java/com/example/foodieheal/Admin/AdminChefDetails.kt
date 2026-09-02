package com.example.foodieheal.Admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.example.foodieheal.R
import es.dmoral.toasty.Toasty
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.foodieheal.Admin.ViewModel1.AdminApprovalViewModel
import com.example.foodieheal.ui.components.DetailSectionCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChefDetailScreen(
    chefId: String,
    navController: NavController,
    viewModel: AdminApprovalViewModel = viewModel()
) {
    val chef = viewModel.selectedChef
    val context = LocalContext.current
    val chefName = chef?.name.orEmpty()
    val approvedToastMsg = stringResource(R.string.admin_chef_toast_approved_email, chefName)
    val rejectedToastMsg = stringResource(R.string.admin_chef_toast_rejected_email, chefName)
    var showApproveConfirmDialog by remember { mutableStateOf(false) }
    var showRejectDialog by remember { mutableStateOf(false) }
    var rejectionReason by remember { mutableStateOf("") }
    var rejectionReasonError by remember { mutableStateOf(false) }

    LaunchedEffect(chefId) {
        viewModel.loadChefDetail(chefId)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.chef_details),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() }
                    ) {
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
    ) { padding ->

        if (chef == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            val currentChef = chef

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // Profile Header Card
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (currentChef.profilePictureUrl.isNullOrEmpty()) {
                            Icon(
                                painter = painterResource(R.drawable.ic_outline_account_circle),
                                contentDescription = stringResource(R.string.default_profile),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            AsyncImage(
                                model = currentChef.profilePictureUrl,
                                contentDescription = stringResource(R.string.profile_picture),
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = currentChef.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        StatusChip(status = currentChef.status)
                    }
                }

                // Personal Information Card
                DetailSectionCard(
                    title = stringResource(R.string.personal_info)
                ) {
                    DetailRow(
                        painter = painterResource(R.drawable.ic_outline_account_circle),
                        label = stringResource(R.string.label_gender),
                        value = currentChef.gender
                    )

                    DetailRow(
                        painter = painterResource(R.drawable.age),
                        label = stringResource(R.string.label_age),
                        value = currentChef.age.toString()
                    )
                }

                // Contact Information Card
                DetailSectionCard(
                    title = stringResource(R.string.contact_info)
                ) {
                    DetailRow(
                        painter = painterResource(R.drawable.mail),
                        label = stringResource(R.string.label_email),
                        value = currentChef.email
                    )

                    DetailRow(
                        painter = painterResource(R.drawable.telephone),
                        label = stringResource(R.string.label_phone),
                        value = currentChef.phoneNumber
                    )

                    DetailRow(
                        painter = painterResource(R.drawable.location),
                        label = stringResource(R.string.label_address),
                        value = stringResource(
                            R.string.full_address_format,
                            currentChef.address,
                            currentChef.postcode,
                            currentChef.state
                        )
                    )
                }

                // Professional Information Card
                DetailSectionCard(
                    title = stringResource(R.string.professional_info)
                ) {
                    DetailRow(
                        painter = painterResource(R.drawable.ic_clock),
                        label = stringResource(R.string.label_experience),
                        value = stringResource(R.string.experience_years_format, currentChef.experience)
                    )

                    DetailRow(
                        painter = painterResource(R.drawable.ic_view),
                        label = stringResource(R.string.label_description),
                        value = currentChef.description
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                val isApproved = currentChef.status.equals("Approved", ignoreCase = true)
                val isRejected = currentChef.status.equals("Rejected", ignoreCase = true)

                if (isApproved) {
                    // Status banner and revoke button
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFE8F5E9),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.check_circle),
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = stringResource(R.string.admin_chef_already_approved),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        shape = RoundedCornerShape(14.dp),
                        onClick = { showRejectDialog = true }
                    ) {
                        Text(
                            text = stringResource(R.string.admin_revoke_approval),
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else if (isRejected) {
                    // Re-approve button
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFFEBEE),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.cancel),
                                contentDescription = null,
                                tint = Color(0xFFC62828),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = stringResource(R.string.admin_chef_already_rejected),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFC62828),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(14.dp),
                        onClick = { showApproveConfirmDialog = true }
                    ) {
                        Text(
                            text = stringResource(R.string.admin_reapprove),
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    // Pending - Show Approve and Reject
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(14.dp),
                            onClick = { showApproveConfirmDialog = true }
                        ) {
                            Text(
                                text = stringResource(R.string.approve),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ),
                            shape = RoundedCornerShape(14.dp),
                            onClick = { showRejectDialog = true }
                        ) {
                            Text(
                                text = stringResource(R.string.reject),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Approval Confirmation Dialog
        if (showApproveConfirmDialog && chef != null) {
            AlertDialog(
                onDismissRequest = { showApproveConfirmDialog = false },
                title = {
                    Text(
                        text = stringResource(R.string.admin_approve_confirm_title),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.admin_approve_confirm_msg, chef.name),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showApproveConfirmDialog = false
                            viewModel.updateChefStatus(
                                chefId = chef.chefId,
                                status = "Approved",
                                chefEmail = chef.email,
                                chefName = chef.name
                            )
                            Toasty.custom(
                                context,
                                approvedToastMsg,
                                R.drawable.foodieheallogo_removebg_and_word,
                                R.color.black,
                                Toast.LENGTH_SHORT,
                                true,
                                true
                            ).show()
                            navController.popBackStack()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.admin_confirm_approve), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showApproveConfirmDialog = false }) {
                        Text(stringResource(R.string.dialog_cancel))
                    }
                }
            )
        }

        // Rejection Dialog with Reason Prompt
        if (showRejectDialog && chef != null) {
            val presetReasons = listOf(
                stringResource(R.string.admin_reject_preset_doc),
                stringResource(R.string.admin_reject_preset_contact),
                stringResource(R.string.admin_reject_preset_exp),
                stringResource(R.string.admin_reject_preset_photo)
            )

            AlertDialog(
                onDismissRequest = {
                    showRejectDialog = false
                    rejectionReasonError = false
                },
                title = {
                    Text(
                        text = stringResource(R.string.admin_reject_dialog_title),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.admin_reject_dialog_msg, chef.name),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Preset Chips
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(presetReasons) { preset ->
                                val isSelected = rejectionReason == preset
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        rejectionReason = if (isSelected) "" else preset
                                        rejectionReasonError = false
                                    },
                                    label = { Text(preset, fontSize = 11.sp) },
                                    shape = RoundedCornerShape(50)
                                )
                            }
                        }

                        // Input TextField
                        OutlinedTextField(
                            value = rejectionReason,
                            onValueChange = {
                                rejectionReason = it
                                if (it.isNotBlank()) rejectionReasonError = false
                            },
                            placeholder = { Text(stringResource(R.string.admin_reject_reason_hint), fontSize = 13.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 90.dp),
                            shape = RoundedCornerShape(12.dp),
                            isError = rejectionReasonError,
                            supportingText = {
                                if (rejectionReasonError) {
                                    Text(
                                        text = stringResource(R.string.admin_reject_reason_error),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (rejectionReason.isBlank()) {
                                rejectionReasonError = true
                            } else {
                                val reasonToSend = rejectionReason.trim()
                                showRejectDialog = false
                                rejectionReasonError = false
                                viewModel.updateChefStatus(
                                    chefId = chef.chefId,
                                    status = "Rejected",
                                    chefEmail = chef.email,
                                    chefName = chef.name,
                                    rejectionReason = reasonToSend
                                )
                                Toasty.custom(
                                    context,
                                    rejectedToastMsg,
                                    R.drawable.foodieheallogo_removebg_and_word,
                                    R.color.black,
                                    Toast.LENGTH_SHORT,
                                    true,
                                    true
                                ).show()
                                navController.popBackStack()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.admin_confirm_reject), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showRejectDialog = false
                        rejectionReasonError = false
                    }) {
                        Text(stringResource(R.string.dialog_cancel))
                    }
                }
            )
        }
    }
}
