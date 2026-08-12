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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import android.app.Activity
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.view.WindowCompat
import coil.compose.AsyncImage
import com.example.foodieheal.Hiring.ViewModel.BookmarkViewModel
import com.example.foodieheal.Hiring.ViewModel.HiringViewModel
import com.example.foodieheal.navigation.Screen
import com.example.foodieheal.viewmodel.AuthViewModel
import com.example.mobileassignmentloginpart.Model.Chef
import java.util.Calendar

@Composable
fun HomeScreen(
    navController: NavController,
    chefViewModel: HiringViewModel,
    onChefClick: (Chef) -> Unit,
    bookmarkViewModel: BookmarkViewModel = viewModel(),
) {

    val viewModel: AuthViewModel = viewModel(viewModelStoreOwner = LocalContext.current as androidx.lifecycle.ViewModelStoreOwner)
    val user = viewModel.currentUser
    val view = LocalView.current
    val primaryColor = MaterialTheme.colorScheme.primary

    LaunchedEffect(Unit) {
        if (chefViewModel.chefList.isEmpty()) {
            chefViewModel.fetchAllChefs()
        }
    }

    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 0..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }

    // Set Status Bar color to match the orange header
    SideEffect {
        val window = (view.context as Activity).window
        window.statusBarColor = primaryColor.toArgb()
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
    }

    Scaffold(
        containerColor = Color(0xFFF8F8F8),
        topBar = {
            // Top Bar & Tabs Header (Consistent size with Recipes)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                Column(modifier = Modifier.statusBarsPadding()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, start = 20.dp, end = 16.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = user?.name ?: "Username",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    // Mimic the size and position of TabRow in RecipesScreen
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .padding(start = 20.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = greeting,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
            ChefListSection(
                chefs = chefViewModel.chefList,
                isLoading = chefViewModel.isProcessing,
                errorMessage = chefViewModel.errorMessage,
                bookmarkViewModel = bookmarkViewModel,
                onChefClick = onChefClick
            )

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

@Composable
fun ChefListSection(
    chefs: List<Chef>,
    isLoading: Boolean,
    errorMessage: String?,
    onChefClick: (Chef) -> Unit,
    bookmarkViewModel: BookmarkViewModel = viewModel()
) {
    when {
        // 1. Loading State (Replaced Skeleton with Progress Indicator)
        isLoading -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }

        // 2. Error State
        errorMessage != null -> {
            Text(
                text = errorMessage,
                color = Color.Red,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }

        // 3. Empty State
        chefs.isEmpty() -> {
            Text(
                text = "No chefs available",
                color = Color.Gray,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }

        // 4. Data State
        else -> {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(
                    items = chefs,
                    key = { chef -> chef.chefId.ifEmpty { chef.id } }
                ) { chef ->
                    ChefCard(
                        chef = chef,
                        onClick = { onChefClick(chef) }
                    )
                }
            }
        }
    }
}

@Composable
fun ChefCard(
    chef: Chef,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier
            .width(165.dp)
            .padding(vertical = 4.dp)
    ) {
        Column {
            // Profile Image Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(135.dp)
                    .background(Color(0xFFEEEEEE))
            ) {
                if (!chef.profilePictureUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = chef.profilePictureUrl,
                        contentDescription = chef.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Price Tag Badge
                Surface(
                    shape = RoundedCornerShape(topStart = 0.dp, bottomStart = 12.dp, topEnd = 0.dp, bottomEnd = 0.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text(
                        text = "$${chef.Pricing?.toInt() ?: 0}/hr",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Info Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = chef.name?.ifEmpty { "Chef" } ?: "Chef",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E1E1E),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Rating Display
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.ic_star),
                            contentDescription = "Rating",
                            modifier = Modifier.size(13.dp),
                            tint = Color(0xFFFFB300)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${chef.averagerating ?: "N/A"}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF424242)
                        )
                    }

                    // Experience Tag
                    Surface(
                        color = Color(0xFFF0F0F0),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${chef.experience ?: "0"} yrs exp",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
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
