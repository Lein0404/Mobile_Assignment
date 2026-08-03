package com.example.foodieheal.Chef.Register

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.foodieheal.Chef.ViewModel.chefRegisterViewModel
import com.example.foodieheal.R

@Composable
fun ChefWelcomeScreen(
    navController: NavController,
    chefRegisterViewModel: chefRegisterViewModel
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),

        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {


        // Chef image
        Image(
            painter = painterResource(
                id = R.drawable.ic_hiring
            ),
            contentDescription = "Chef Image",
            modifier = Modifier
                .size(250.dp)
        )



        Spacer(
            modifier = Modifier.height(24.dp)
        )


        Text(
            text = "Become a Chef Partner",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )


        Spacer(
            modifier = Modifier.height(12.dp)
        )


        Text(
            text = "Share your cooking skills and inspire others with healthy meals.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )



        Spacer(
            modifier = Modifier.height(32.dp)
        )



        FeatureItem(
            text = "Create your chef profile"
        )

        FeatureItem(
            text = "Offer your cooking services"
        )

        FeatureItem(
            text = "Connect with customers"
        )



        Spacer(
            modifier = Modifier.height(40.dp)
        )

        Button(
            onClick = {
                navController.navigate("basicInfo")
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp),

            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Next")
        }

        TextButton(
            onClick = {
                navController.popBackStack()
                chefRegisterViewModel.clearData()
            }
        ) {

            Text(
                text = "Already a chef? Login"
            )

        }

    }

}

@Composable
fun FeatureItem(
    text: String
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {

        Text(
            text = "✓",
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )


        Spacer(
            modifier = Modifier.width(10.dp)
        )


        Text(
            text = text
        )

    }

}