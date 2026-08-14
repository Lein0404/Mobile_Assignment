package com.example.foodieheal.Admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.foodieheal.R
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

                // Action Buttons
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
                        onClick = {
                            viewModel.updateChefStatus(currentChef.chefId, "Approved")
                            navController.popBackStack()
                        }
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
                        onClick = {
                            viewModel.updateChefStatus(currentChef.chefId, "Rejected")
                            navController.popBackStack()
                        }
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
}
