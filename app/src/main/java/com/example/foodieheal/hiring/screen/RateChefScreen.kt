package com.example.foodieheal.hiring.screen

import android.widget.Toast
import es.dmoral.toasty.Toasty
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.foodieheal.R
import com.example.foodieheal.hiring.model.UserAppointmentsUiState
import com.example.foodieheal.hiring.viewmodel.ReviewViewModel
import com.example.foodieheal.hiring.viewmodel.UserAppointmentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RateChefScreen(
    appointmentId: String,
    userViewModel: UserAppointmentViewModel = viewModel(),
    reviewViewModel: ReviewViewModel = viewModel(),
    onSubmitSuccess: () -> Unit
) {
    val context = LocalContext.current
    val appointmentsState by userViewModel.userAppointmentsState.collectAsState()
    val isNetworkAvailable by userViewModel.isNetworkAvailable.collectAsState()

    // Retrieve appointment and chef details
    val successState = appointmentsState as? UserAppointmentsUiState.Success
    val appointment = successState?.appointments?.find { it.AppointmentID == appointmentId }
    val chefUser = appointment?.let { successState.usersMap[it.chefId] }

    // Form States
    var rating by remember { mutableIntStateOf(0) }
    var comment by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    val maxCommentLength = 300
    val defaultChefName = stringResource(R.string.private_chef_name)
    val defaultBookingDate = stringResource(R.string.this_booking)
    val successToastMsg = stringResource(R.string.toast_rating_success)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.rate_experience),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onSubmitSuccess) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrowback),
                            contentDescription = stringResource(R.string.back_button)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (!isNetworkAvailable) {
                androidx.compose.material3.Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.wifi_off),
                            contentDescription = stringResource(R.string.desc_no_network),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(R.string.rate_offline_mode),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {

            // Chef Profile Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AsyncImage(
                        model = chefUser?.profilePicUrl,
                        contentDescription = stringResource(R.string.chef_picture),
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentScale = ContentScale.Crop,
                        error = painterResource(R.drawable.ic_outline_account_circle),
                        placeholder = painterResource(R.drawable.ic_outline_account_circle)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = chefUser?.name ?: defaultChefName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(
                                R.string.How_experience_on_date,
                                appointment?.Date ?: defaultBookingDate
                            ),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Rating Section Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.label_overall_rating),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Interactive Star Bar
                    StarRatingBar(
                        rating = rating,
                        onRatingChanged = { newRating -> rating = newRating },
                        starSize = 40.dp
                    )

                    // Dynamic text description based on star count
                    Text(
                        text = when (rating) {
                            1 -> stringResource(R.string.rating_poor)
                            2 -> stringResource(R.string.rating_fair)
                            3 -> stringResource(R.string.rating_good)
                            4 -> stringResource(R.string.rating_very_good)
                            5 -> stringResource(R.string.rating_excellent)
                            else -> stringResource(R.string.rating_select_prompt)
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (rating == 0) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Feedback Text Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.label_write_feedback),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    OutlinedTextField(
                        value = comment,
                        onValueChange = { if (it.length <= maxCommentLength) comment = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        placeholder = { Text(stringResource(R.string.placeholder_feedback_hint)) },
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 5,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    // Character count indicator
                    Text(
                        text = stringResource(R.string.char_count_format, comment.length, maxCommentLength),
                        modifier = Modifier.align(Alignment.End),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Submit Button
            Button(
                onClick = {
                    if (appointment == null || rating == 0) return@Button
                    isSubmitting = true

                    reviewViewModel.submitReview(
                        appointmentId = appointment.AppointmentID.orEmpty(),
                        rating = rating,
                        comment = comment.trim(),
                        onSuccess = {
                            isSubmitting = false
                            Toasty.custom(context, successToastMsg, R.drawable.foodieheallogo_removebg_and_word, R.color.black, Toast.LENGTH_SHORT, true, true).show()
                            onSubmitSuccess()
                        },
                        onError = { err ->
                            isSubmitting = false
                            Toasty.custom(context, err, R.drawable.foodieheallogo_removebg_and_word, R.color.black, Toast.LENGTH_LONG, true, true).show()
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = isNetworkAvailable && !isSubmitting && rating > 0,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = stringResource(R.string.submit_review),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}
}

@Composable
fun StarRatingBar(
    rating: Int,
    onRatingChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    maxStars: Int = 5,
    starSize: Dp = 36.dp
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 1..maxStars) {
            val isSelected = i <= rating
            val starContentDescription = stringResource(R.string.star_rating_number, i)

            Icon(
                painter = painterResource(
                    if (isSelected) R.drawable.ic_star else R.drawable.ic_outline_star
                ),
                contentDescription = starContentDescription,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                modifier = Modifier
                    .size(starSize)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false, radius = starSize / 1.5f),
                        onClick = { onRatingChanged(i) }
                    )
            )
        }
    }
}