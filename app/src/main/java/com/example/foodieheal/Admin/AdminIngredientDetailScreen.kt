package com.example.foodieheal.Admin

import android.app.Application
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil3.compose.SubcomposeAsyncImage
import com.example.foodieheal.Admin.ViewModel.AdminIngredientRequestViewModel
import com.example.foodieheal.Admin.ViewModel.AdminIngredientRequestViewModelFactory
import com.example.foodieheal.R
import com.example.foodieheal.ingredients.view.ImagePlaceholder
import com.example.foodieheal.model.Status
import com.example.foodieheal.navigation.Screen
import com.example.foodieheal.ui.components.PrimaryButton
import com.example.foodieheal.ui.components.StatusBadge
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminIngredientDetailScreen(
    navController: NavController,
    requestId: String
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val viewModel: AdminIngredientRequestViewModel = viewModel(
        factory = AdminIngredientRequestViewModelFactory(application)
    )
    val requestItem by viewModel.requestDetail.collectAsState()
    val actionUiState by viewModel.uiState.collectAsState()
    val isLoading = actionUiState.isLoading
    
    var showRejectDialog by remember { mutableStateOf(false) }

    LaunchedEffect(requestId) {
        viewModel.fetchRequestDetail(requestId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "View Request",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() })
                    {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrowback),
                            contentDescription = "Back"
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
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            requestItem?.let { info ->
                val request = info.request
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // 1. Image
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
                                .background(Color(0xFFE0E0E0)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!request.ingredientImage.isNullOrEmpty()) {
                                SubcomposeAsyncImage(
                                    model = request.ingredientImage,
                                    contentDescription = request.ingredientName,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                    loading = { CircularProgressIndicator(modifier = Modifier.scale(0.5f)) }, // TODO: make the CPI smaller
                                    error = { ImagePlaceholder() }
                                )
                            } else {
                                ImagePlaceholder()
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // 2. Title & Category
                            Text(
                                text = request.ingredientName,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Text(
                                text = request.ingredientCategory?.categoryName ?: "Others",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            StatusBadge(status = request.requestStatus)

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 16.dp),
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.primary
                            )

                            // 3. Description
                            Text("Description", fontWeight = FontWeight.Bold, color = Color.Black)
                            Text(
                                text = request.ingredientDesc.ifEmpty { "No description available." },
                                color = Color.Black,
                                modifier = Modifier.padding(top = 4.dp)
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            // 4. Calorie Information
                            Text("Calorie Information", fontWeight = FontWeight.Bold, color = Color.Black)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = info.calorieSummary.ifEmpty { "No calorie information available." },
                                color = Color.Black
                            )

                            // 5. Rejected Reason (if applicable)
                            if (request.requestStatus == Status.REJECTED) {
                                Spacer(modifier = Modifier.height(24.dp))
                                Text("Rejected Reason", fontWeight = FontWeight.Bold, color = Color.Black)
                                Text(
                                    text = request.rejectedReason ?: "Unspecified.",
                                    color = Color.Black,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            if (request.requestStatus == Status.APPROVED && !request.adminNote.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(24.dp))
                                Text("Admin Notes", fontWeight = FontWeight.Bold, color = Color.Black)
                                Text(
                                    text = request.adminNote,
                                    color = Color.Black,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 16.dp),
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.primary
                            )

                            // 6. Request Information
                            Text("Request Information", fontWeight = FontWeight.Bold, color = Color.Black)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("User ID: ${info.requesterCustomId}", fontSize = 14.sp)
                            Text("User Name: ${info.requesterName}", fontSize = 14.sp)
                            Text("Created date: ${formatDisplayDateTime(request.datetimeCreated)}", fontSize = 14.sp)

                            Spacer(modifier = Modifier.height(120.dp))
                        }
                    }

                    // 7. Admin Action Buttons (for Pending)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 16.dp),
                        verticalArrangement = Arrangement.Bottom, // push the button down to the bottom
                        horizontalAlignment = Alignment.CenterHorizontally
                    ){
                        if (request.requestStatus == Status.PENDING && actionUiState.isNetworkAvailable) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Button(
                                    onClick = { showRejectDialog = true },
                                    modifier = Modifier.weight(0.45f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    ),
                                    shape = RoundedCornerShape(dimensionResource(id = R.dimen.corner_radius_sm))
                                ) {
                                    Text(
                                        text = stringResource(R.string.reject),
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                                PrimaryButton(
                                    modifier = Modifier.weight(0.55f),
                                    onClick = {
                                        navController.navigate(Screen.AdminIngredientReview.createRoute(requestId))
                                    },
                                    textID = R.string.review_approve
                                )
                            }
                        }
                    }
                }
            } ?:
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Request not found")
            }
        }
    }

    if (showRejectDialog) {
        RejectRequestDialog(
            onDismiss = { showRejectDialog = false },
            onConfirm = { reason ->
                viewModel.rejectRequest(requestId, reason) {
                    Toast.makeText(context, "Request rejected", Toast.LENGTH_SHORT).show()
                    showRejectDialog = false
                    navController.popBackStack()
                }
            }
        )
    }
}

fun formatDisplayDateTime(isoDateTime: String?): String {
    if (isoDateTime.isNullOrEmpty()) return "N/A"
    return try {
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
        val zonedDateTime = ZonedDateTime.parse(isoDateTime)
            .withZoneSameInstant(ZoneId.of("UTC+8"))
        "${zonedDateTime.format(formatter)}"
    } catch (_: Exception) {
        isoDateTime
    }
}

@Composable
fun RejectRequestDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var reason by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reject Request", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Please provide a reason for rejecting this Ingredient Request.")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = {
                        reason = it
                        if (it.isNotBlank()) error = null
                    },
                    placeholder = { Text("Rejected reason") },
                    modifier = Modifier.fillMaxWidth().height(240.dp),
                    isError = error != null,
                    supportingText = {
                        if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (reason.isBlank()) {
                    error = "Reason cannot be empty"
                } else {
                    onConfirm(reason)
                }
            }) {
                Text("Reject Request", color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}