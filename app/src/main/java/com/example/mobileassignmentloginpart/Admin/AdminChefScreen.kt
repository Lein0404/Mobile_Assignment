package com.example.mobileassignmentloginpart.Admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.mobileassignmentloginpart.Admin.ViewModel.AdminApprovalViewModel
import com.example.mobileassignmentloginpart.Model.Chef
import com.example.mobileassignmentloginpart.R
import com.example.mobileassignmentloginpart.ViewModel.AuthViewModel
import com.example.mobileassignmentloginpart.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminApprovalScreen(
    navController: NavController,
    viewModel: AdminApprovalViewModel = viewModel()
) {

    // Set chef approval as deault screen
    var selectedTab by remember { mutableIntStateOf(0)
    }
    val AuthviewModel: AuthViewModel = viewModel()

    LaunchedEffect(Unit) {
        viewModel.loadPendingChefs()
    }

    Scaffold(
        containerColor = Color(0xFFF7F8FC),
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "Admin Management",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                // use it as logout
                                AuthviewModel.logout {
                                    navController.navigate(Screen.Login.route) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_arrowback),
                                contentDescription = "Back"
                            )
                        }
                    }
                )
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.White
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = {
                            selectedTab = 0
                        },
                        text = {
                            Text("Chef Approval")
                        },
                        icon = {
                            Icon(
                                painter = painterResource(
                                    R.drawable.ic_outline_account_circle
                                ),
                                contentDescription = null
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = {
                            selectedTab = 1
                        },
                        text = {
                            Text("Ingredients")
                        },
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.ingredient),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    )
                }
            }
        }
    ) { padding ->

        when(selectedTab) {
            // Chef Approval Page (default)
            0 -> {
                if (viewModel.pendingChefs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_outline_account_circle),
                                contentDescription = null,
                                modifier = Modifier.size(60.dp),
                                tint = Color.Gray
                            )

                            Spacer(
                                modifier = Modifier.height(12.dp)
                            )

                            Text(
                                "No pending applications",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.Gray
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .padding(padding)
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        items(viewModel.pendingChefs) { chef ->
                            ChefApprovalCard(
                                chef = chef,
                                onViewClick = {
                                    navController.navigate(
                                        "chefDetail/${chef.chefId}"
                                    )
                                }
                            )
                        }
                    }
                }
            }
            // Ingredient Management Page
            1 -> {
                //IngredientAdminScreen
            }
        }
    }
}

@Composable
fun ChefApprovalCard(
    chef: Chef,
    onViewClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 5.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape),

                    contentAlignment = Alignment.Center
                ) {
                    if (chef.profilePictureUrl.isNullOrEmpty()) {
                        Icon(
                            painter = painterResource(R.drawable.ic_outline_account_circle),
                            contentDescription = "Default Profile",
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(75.dp)
                        )
                    } else {
                        AsyncImage(
                            model = chef.profilePictureUrl,
                            contentDescription = "Profile Picture",
                            modifier = Modifier
                                .size(75.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.width(16.dp)
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = chef.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(6.dp)
                    )

                    StatusChip(
                        status = chef.status
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            HorizontalDivider()

            Spacer(
                modifier = Modifier.height(15.dp)
            )

            ChefInfoRow(
                painter = painterResource(R.drawable.mail),
                text = chef.email
            )

            ChefInfoRow(
                painter = painterResource(R.drawable.telephone),
                text = chef.phoneNumber
            )

            ChefInfoRow(
                painter = painterResource(R.drawable.ic_clock),
                text = "${chef.experience} years experience"
            )

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            Button(
                onClick = onViewClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_view),
                    contentDescription = null
                )

                Spacer(
                    modifier = Modifier.width(8.dp)
                )

                Text(
                    "Review Application"
                )
            }
        }
    }
}

@Composable
fun StatusChip(
    status: String
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = Color(0xFFFFF3E0)
    ) {
        Text(
            text = status,
            modifier = Modifier.padding(
                horizontal = 12.dp,
                vertical = 6.dp
            ),
            color = Color(0xFFFF9800),
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
fun ChefInfoRow(
    painter: Painter,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(
            vertical = 6.dp
        )
    ) {

        Icon(
            painter = painter,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(20.dp)
        )

        Spacer(
            modifier = Modifier.width(12.dp)
        )

        Text(
            text = text,
            color = Color.DarkGray,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
