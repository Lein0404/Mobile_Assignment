package com.example.foodieheal.Chef.Register

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.foodieheal.Chef.getGenderResId
import com.example.foodieheal.Chef.getStateResId
import com.example.foodieheal.Chef.ViewModel.Register.ChefRegisterViewModel
import com.example.foodieheal.R
import com.example.foodieheal.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun reviewInfo(
    navController: NavController,
    chefRegisterViewModel: ChefRegisterViewModel
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.title_review_info), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrowback),
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                AsyncImage(
                    model = chefRegisterViewModel.selectedImageUri,
                    contentDescription = stringResource(R.string.chef_picture),
                    contentScale = ContentScale.Crop,
                    error = painterResource(R.drawable.ic_outline_account_circle),
                    placeholder = painterResource(R.drawable.ic_outline_account_circle),
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        )
                )
            }

            val genderDisplay = getGenderResId(chefRegisterViewModel.gender)?.let { stringResource(it) } ?: chefRegisterViewModel.gender
            val stateDisplay = getStateResId(chefRegisterViewModel.state)?.let { stringResource(it) } ?: chefRegisterViewModel.state

            SectionCard(title = stringResource(R.string.section_personal_details)) {
                ReviewItem(stringResource(R.string.full_name), chefRegisterViewModel.name)
                ReviewItem(stringResource(R.string.gender), genderDisplay)
                ReviewItem(stringResource(R.string.age), chefRegisterViewModel.age)
            }

            SectionCard(title = stringResource(R.string.title_contact_info)) {
                ReviewItem(stringResource(R.string.email), chefRegisterViewModel.email)
                ReviewItem(stringResource(R.string.phone_number), chefRegisterViewModel.phoneNumber)
            }

            SectionCard(title = stringResource(R.string.section_address)) {
                ReviewItem(stringResource(R.string.address), chefRegisterViewModel.address)
                ReviewItem(stringResource(R.string.postcode), chefRegisterViewModel.postcode)
                ReviewItem(stringResource(R.string.state), stateDisplay)
            }

            SectionCard(title = stringResource(R.string.section_professional_bg)) {
                ReviewItem(stringResource(R.string.experience), stringResource(R.string.years_format, chefRegisterViewModel.experience))
                ReviewItem(stringResource(R.string.description), chefRegisterViewModel.description)
            }

            chefRegisterViewModel.errorMessage?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    if (chefRegisterViewModel.isUpgradeFlow) {
                        chefRegisterViewModel.upgradeChef(context = context) {
                            navController.navigate(Screen.Profile.route) {
                                popUpTo("chefRegisterRoute") { inclusive = true }
                            }
                        }
                    } else {
                        chefRegisterViewModel.registerChef(context = context) {
                            navController.navigate(Screen.ChefLogin.route) {
                                popUpTo("chefRegisterRoute") { inclusive = true }
                            }
                        }
                    }
                },
                enabled = !chefRegisterViewModel.isSubmitting,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (chefRegisterViewModel.isSubmitting) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Text(
                        text = if (chefRegisterViewModel.isUpgradeFlow) "Submit Application" else stringResource(R.string.submit_registration),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
            )
            content()
        }
    }
}

@Composable
private fun ReviewItem(
    label: String,
    value: String,
    isLongText: Boolean = false
) {
    val displayValue = value.ifBlank { "-" }

    if (isLongText) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = displayValue,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 12.dp)
            )
            Text(
                text = displayValue,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f, fill = false)
            )
        }
    }
}