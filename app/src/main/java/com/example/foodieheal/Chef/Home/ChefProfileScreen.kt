package com.example.foodieheal.Chef

import android.app.Activity
import androidx.compose.foundation.background
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.foodieheal.Chef.Home.DetailRow
import com.example.foodieheal.Chef.model.Chef
import com.example.foodieheal.R
import com.example.foodieheal.User.viewModel.AuthViewModel
import com.example.foodieheal.ui.components.DetailSectionCard

import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource

@Composable
fun ChefProfileScreen(
    navController: NavController,
    chef: Chef?,
    viewModel: AuthViewModel,
    onEditClick: () -> Unit,
    onChangePasswordClick: () -> Unit = {},
    onLogoutSuccess: () -> Unit
) {
    val view = LocalView.current
    val primaryColor = MaterialTheme.colorScheme.primary

    // Sync status bar color with the top primary header
    SideEffect {
        val window = (view.context as Activity).window
        window.statusBarColor = primaryColor.toArgb()
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
    }

    if (chef == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = primaryColor)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(primaryColor)
    ) {
        // Header Section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, bottom = 24.dp, start = 20.dp, end = 20.dp)
        ) {
            Column {
                Text(
                    text = stringResource(R.string.account_settings),
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
                Text(
                    text = stringResource(R.string.chef_profile),
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(0.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Profile Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            if (chef.profilePictureUrl.isNullOrEmpty()) {
                                Text(
                                    text = chef.name?.take(1)?.uppercase() ?: stringResource(R.string.default_initial_chef),
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                AsyncImage(
                                    model = chef.profilePictureUrl,
                                    contentDescription = stringResource(R.string.profile_picture),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = chef.name ?: stringResource(R.string.unknown_chef),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (!chef.email.isNullOrBlank()) {
                            Text(
                                text = chef.email,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Approval Status Badge
                        val isApproved = chef.status?.equals("approved", ignoreCase = true) == true
                        val badgeBg = if (isApproved) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                        val badgeText = if (isApproved) Color(0xFF2E7D32) else Color(0xFFE65100)

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = badgeBg
                        ) {
                            Text(
                                text = chef.status?.replaceFirstChar { it.uppercase() }
                                    ?: stringResource(R.string.status_pending),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = badgeText,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Professional Information Section (reusing DetailSectionCard)
                DetailSectionCard(title = stringResource(R.string.professional_info)) {
                    DetailRow(
                        iconRes = R.drawable.ic_clock,
                        label = stringResource(R.string.label_experience),
                        value = stringResource(R.string.years_experience, chef.experience ?: 0)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    DetailRow(
                        iconRes = R.drawable.dollar_symbol,
                        label = stringResource(R.string.label_price),
                        value = stringResource(R.string.rate_per_hour, (chef.Pricing ?: 0.0).toInt())
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    DetailRow(
                        iconRes = R.drawable.ic_outline_account_circle,
                        label = stringResource(R.string.label_chef_id),
                        value = chef.id.ifBlank { stringResource(R.string.not_available) }
                    )

                    if (!chef.description.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = stringResource(R.string.label_bio_description),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = chef.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Contact Information Section (reusing DetailSectionCard)
                DetailSectionCard(title = stringResource(R.string.contact_info)) {
                    DetailRow(
                        iconRes = R.drawable.telephone,
                        label = stringResource(R.string.label_phone),
                        value = chef.phoneNumber?.takeIf { it.isNotBlank() } ?: stringResource(R.string.not_provided)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    DetailRow(
                        iconRes = R.drawable.mail,
                        label = stringResource(R.string.label_email),
                        value = chef.email?.takeIf { it.isNotBlank() } ?: stringResource(R.string.not_provided)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val fullAddress = listOfNotNull(chef.address, chef.postcode, chef.state)
                        .filter { it.isNotBlank() }
                        .joinToString(", ")

                    DetailRow(
                        iconRes = R.drawable.location,
                        label = stringResource(R.string.label_location),
                        value = fullAddress.ifEmpty { stringResource(R.string.not_provided) }
                    )
                }

                // Account Security Section
                DetailSectionCard(title = stringResource(R.string.account_security)) {
                    Surface(
                        onClick = onChangePasswordClick,
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.changepassword),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = stringResource(R.string.change_password),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Icon(
                                painter = painterResource(id = R.drawable.ic_arrow_right),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onEditClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.edit_profile),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.logout {
                                onLogoutSuccess()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.logout),
                            color = MaterialTheme.colorScheme.onError,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

//@Preview(showBackground = true)
//@Composable
//fun Preview(){
//    ChefProfileScreen(
//        chef = Chef("C001", "dsdsda", "James", "Male", 20, "0123456789",
//            "james@gmail.com","Sri Hi","Pulau Pinang", "11500",20,"I am cooker"
//            ,"https://cdn-icons-png.flaticon.com/512/149/149071.png","Approved"))
//}
