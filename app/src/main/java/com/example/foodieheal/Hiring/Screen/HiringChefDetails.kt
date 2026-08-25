package com.example.foodieheal.Hiring.Screen

import android.content.Intent
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.foodieheal.Hiring.ViewModel.BookmarkViewModel
import com.example.foodieheal.Hiring.ViewModel.HiringViewModel
import com.example.foodieheal.R
import com.example.foodieheal.ui.components.DetailSectionCard
import com.example.foodieheal.User.viewModel.AuthViewModel
import com.example.mobileassignmentloginpart.Model.Chef

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HiringChefDetails(
    chef: Chef,
    userId: String,
    viewModel: BookmarkViewModel = viewModel(),
    onBackClick: () -> Unit,
    onHireClick: (Chef) -> Unit
) {
    val context = LocalContext.current
    val hiringViewModel: HiringViewModel = viewModel()
    val authViewModel: AuthViewModel = viewModel()
    val user = authViewModel.currentUser
    val currentChefId = chef.chefId.ifEmpty { chef.id }
    val isBookmarked = viewModel.isChefBookmarked(currentChefId)

    LaunchedEffect(userId) {
        if (userId.isNotBlank()) {
            viewModel.fetchBookmarkedChefs(userId)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            hiringViewModel.clearAppointmentForm()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.chef_profile_title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrowback),
                            contentDescription = stringResource(R.string.back_button),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
            )
        },
        bottomBar = {
            Surface(
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.label_rate),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = chef.Pricing?.let {
                                stringResource(R.string.rate_per_hour, it.toInt())
                            } ?: stringResource(R.string.not_available),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Button(
                        onClick = { onHireClick(chef) },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.btn_hire_this_chef),
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Profile Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (chef.profilePictureUrl.isNullOrEmpty()) {
                            Text(
                                text = chef.name?.take(1)?.uppercase() ?: stringResource(R.string.default_initial_chef),
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(150.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            AsyncImage(
                                model = chef.profilePictureUrl,
                                contentDescription = stringResource(R.string.profile_picture),
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(150.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }

                        // Bookmark Button
                        IconButton(
                            onClick = {
                                viewModel.onBookmarkToggled(
                                    userId = userId,
                                    chefId = currentChefId
                                )
                            },
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Icon(
                                painter = painterResource(
                                    if (isBookmarked) R.drawable.bookmark_fill
                                    else R.drawable.bookmark
                                ),
                                contentDescription = stringResource(R.string.bookmark_chef),
                                tint = if (isBookmarked) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = chef.name.ifBlank { stringResource(R.string.unknown_chef) },
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DetailChip(
                            label = stringResource(R.string.label_rating),
                            value = chef.averagerating?.let { "%.1f".format(it) }
                                ?: stringResource(R.string.not_available),
                            iconRes = R.drawable.ic_star,
                            iconTint = Color(0xFFFFB300) // Star Gold
                        )
                        DetailChip(
                            label = stringResource(R.string.label_experience),
                            value = stringResource(R.string.years_experience, chef.experience ?: 0)
                        )
                        DetailChip(
                            label = stringResource(R.string.label_gender),
                            value = chef.gender?.capitalized() ?: stringResource(R.string.not_available)
                        )
                    }
                }
            }

            // About Section (Reusing DetailSectionCard)
            if (!chef.description.isNullOrBlank()) {
                DetailSectionCard(title = stringResource(R.string.title_about_chef)) {
                    Text(
                        text = chef.description,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        lineHeight = 20.sp
                    )
                }
            }

            // Contact Info Section (Reusing DetailSectionCard)
            DetailSectionCard(title = stringResource(R.string.title_contact_info)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val phoneNumber = chef.phoneNumber?.ifBlank { null }
                    InfoRow(
                        label = stringResource(R.string.label_phone),
                        value = phoneNumber ?: stringResource(R.string.not_available),
                        isClickable = phoneNumber != null,
                        onClick = {
                            phoneNumber?.let {
                                val intent = Intent(Intent.ACTION_DIAL).apply {
                                    data = Uri.parse("tel:$it")
                                }
                                context.startActivity(intent)
                            }
                        }
                    )

                    val emailAddress = chef.email.ifBlank { null }
                    InfoRow(
                        label = stringResource(R.string.label_email),
                        value = emailAddress ?: stringResource(R.string.not_available),
                        isClickable = emailAddress != null,
                        onClick = {
                            emailAddress?.let {
                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:$it")
                                }
                                context.startActivity(intent)
                            }
                        }
                    )

                    val fullAddress = listOfNotNull(
                        chef.address?.ifBlank { null },
                        chef.postcode?.ifBlank { null },
                        chef.state?.ifBlank { null }
                    ).joinToString(", ")

                    InfoRow(
                        label = stringResource(R.string.label_address),
                        value = fullAddress.ifBlank { stringResource(R.string.not_available) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailChip(
    label: String,
    value: String,
    @DrawableRes iconRes: Int? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (iconRes != null) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    isClickable: Boolean = false,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isClickable) Modifier.clickable { onClick() }
                else Modifier
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = if (isClickable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun String.capitalized(): String = replaceFirstChar { it.uppercase() }