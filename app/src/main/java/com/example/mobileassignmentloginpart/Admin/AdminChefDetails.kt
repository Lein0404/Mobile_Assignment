package com.example.mobileassignmentloginpart.Admin

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobileassignmentloginpart.R
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.mobileassignmentloginpart.Admin.ViewModel.AdminApprovalViewModel
import com.example.mobileassignmentloginpart.navigation.Screen

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
        containerColor = Color(0xFFF7F8FC),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("Chef Details", fontWeight = FontWeight.Bold) },

                navigationIcon = {
                    IconButton(
                        onClick = { navController.navigate(Screen.AdminChefScreen.route) }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrowback),
                            contentDescription = "Back"
                        )
                    }
                }

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
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // Profile Header
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(5.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(CircleShape)
                                .background(
                                    Color(0xFFFFE0B2)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_outline_account_circle),
                                contentDescription = null,
                                modifier = Modifier.size(55.dp),
                                tint = Color(0xFFFF9800)
                            )
                        }

                        Spacer(
                            modifier = Modifier.height(12.dp)
                        )

                        Text(
                            text = chef!!.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )

                        StatusChip(
                            status = chef!!.status
                        )
                    }
                }

                // Personal Information
                DetailSectionCard(
                    title = "Personal Information"
                ) {
                    DetailRow(
                        painter = painterResource(R.drawable.ic_outline_account_circle),
                        "Gender",
                        chef!!.gender
                    )

                    DetailRow(
                        painter = painterResource(R.drawable.age),
                        "Age",
                        chef!!.age.toString()
                    )
                }

                // Contact Information
                DetailSectionCard(
                    title = "Contact Information"
                ) {
                    DetailRow(
                        painter = painterResource(R.drawable.mail),
                        "Email",
                        chef!!.email
                    )
                    DetailRow(
                        painter = painterResource(R.drawable.telephone),
                        "Phone",
                        chef!!.phoneNumber
                    )
                    DetailRow(
                        painter = painterResource(R.drawable.location),
                        "Address",
                        "${chef!!.address}, ${chef!!.postcode}, ${chef!!.state}"
                    )
                }

                // Experience
                DetailSectionCard(
                    title = "Professional Information"
                ) {
                    DetailRow(
                        painter = painterResource(R.drawable.ic_clock),
                        "Experience",
                        "${chef!!.experience} years"
                    )
                    Text(
                        text = "Description",
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(
                        modifier = Modifier.height(6.dp)
                    )
                    Text(
                        text = chef!!.description,
                        color = Color.DarkGray
                    )
                }

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        onClick = {
                            viewModel.updateChefStatus(
                                chef!!.chefId,
                                "Approved"
                            )
                            navController.popBackStack()
                        }
                    ) {
                        Spacer(
                            Modifier.width(8.dp)
                        )
                        Text("Approve")
                    }

                    Button(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE53935)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        onClick = {
                            viewModel.updateChefStatus(
                                chef!!.chefId,
                                "Rejected"
                            )
                            navController.popBackStack()
                        }
                    ) {
                        Spacer(
                            Modifier.width(8.dp)
                        )
                        Text("Reject")
                    }
                }
            }
        }
    }
}

@Composable
fun DetailSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            HorizontalDivider()

            content()

        }
    }
}

@Composable
fun DetailRow(
    painter: Painter,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painter,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(20.dp)
        )
        Spacer(
            Modifier.width(12.dp)
        )
        Column {
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color.Gray
            )
            Text(
                text = value,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
