package com.example.mobileassignmentloginpart.Chef.Home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController

@Composable
fun ChefHomeScreen(
    navController: NavController
){
    Column(
        modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
    ){
        Text(
            text = "Welcome Chef",
            style = MaterialTheme.typography.headlineMedium
        )
        Column() {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxSize()
            ) {
                Text(
                    text = "Hello Chef",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(16.dp)
                )
                Text(
                    text = "Good morning",
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ChefHomeScreenPreview(){
    ChefHomeScreen(navController = rememberNavController())
}
