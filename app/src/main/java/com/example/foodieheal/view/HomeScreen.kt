package com.example.foodieheal.view

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.foodieheal.R

import androidx.compose.ui.platform.LocalView
import android.app.Activity
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import com.example.foodieheal.viewmodel.AuthViewModel

@Composable
fun HomeScreen(navController: NavController) {
    val viewModel: AuthViewModel = viewModel()
    val user = viewModel.currentUser
    val view = LocalView.current
    val primaryColor = MaterialTheme.colorScheme.primary

    // Set Status Bar color to match the orange header
    SideEffect {
        val window = (view.context as Activity).window
        window.statusBarColor = primaryColor.toArgb()
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary) // Full header orange
    ) {
        // 1. Top Header (Orange Background)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, bottom = 24.dp, start = 20.dp, end = 20.dp)
        ) {
            Column {
                Text(text = "Good Morning", color = Color.White, fontSize = 14.sp)
                Text(
                    text = user?.name ?: "Username",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 2. White Sheet (Flat top, no radius)
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(0.dp), // Removed radius
            color = Color(0xFFF8F8F8)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 24.dp)
            ) {
                // Chef Section
                Text(
                    text = "Chef",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = Color.Black
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                ChefListSection()

                Spacer(modifier = Modifier.height(24.dp))

                // Promo Banner
                PromoBanner()

                Spacer(modifier = Modifier.height(24.dp))

                // Popular Recipes Section
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Popular Recipes",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Button(
                        onClick = { },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("See All", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                CategoryChips()
                RecipeGrid()
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun ChefListSection() {
    val chefs = listOf(
        ChefData("Gordon Ramsay", "4.5", "20 yrs"),
        ChefData("Uncle Roger", "4.3", "19 yrs"),
        ChefData("Mr. Cyan", "4.5", "20 yrs")
    )
    
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(chefs) { chef ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.width(160.dp)
            ) {
                Column {
                    Box(modifier = Modifier.fillMaxWidth().height(130.dp).background(Color(0xFFE0E0E0)))
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = chef.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(painterResource(id = R.drawable.ic_home), null, modifier = Modifier.size(10.dp), tint = Color(0xFFFFB300))
                            Text(text = " ${chef.rating}", fontSize = 11.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = "⏱ ${chef.exp}", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PromoBanner() {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(150.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F2F1))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Only a few ingredients left?", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color.Black)
            Text("You might be surprised what you can cook!", fontSize = 13.sp, color = Color.Black, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text("Let's Find Out", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CategoryChips() {
    val categories = listOf("Breakfast", "Lunch", "Dinner", "Snack")
    var selectedCategory by remember { mutableStateOf("Breakfast") }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(vertical = 16.dp)
    ) {
        items(categories) { category ->
            val isSelected = selectedCategory == category
            Surface(
                onClick = { selectedCategory = category },
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFE0E0E0),
                modifier = Modifier.height(36.dp)
            ) {
                Box(modifier = Modifier.padding(horizontal = 20.dp), contentAlignment = Alignment.Center) {
                    Text(text = category, color = if (isSelected) Color.White else Color.Black, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun RecipeGrid() {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        RecipeCard(title = "Macaroni Tomato Pasta", modifier = Modifier.weight(1f))
        RecipeCard(title = "Toast With Egg", modifier = Modifier.weight(1f))
    }
}

@Composable
fun RecipeCard(title: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(220.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(150.dp).background(Color(0xFFE0E0E0))) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.align(Alignment.TopEnd).padding(10.dp).size(28.dp)
                ) {
                    Icon(painterResource(id = R.drawable.ic_recipe), null, modifier = Modifier.padding(6.dp), tint = Color.Black)
                }
            }
            Box(modifier = Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.CenterStart) {
                Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }
        }
    }
}

data class ChefData(val name: String, val rating: String, val exp: String)
