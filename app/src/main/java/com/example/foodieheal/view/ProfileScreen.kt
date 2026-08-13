package com.example.foodieheal.view

import android.app.Activity
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.annotation.DrawableRes
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.foodieheal.R
import com.example.foodieheal.SupabaseClient
import com.example.foodieheal.navigation.Screen
import com.example.foodieheal.repository.RecipeRepository
import com.example.foodieheal.viewmodel.AuthViewModel
import com.example.foodieheal.viewmodel.RecipeViewModel
import com.example.foodieheal.Hiring.ViewModel.BookmarkViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: RecipeViewModel,
    authViewModel: AuthViewModel,
    bookmarkViewModel: BookmarkViewModel = viewModel()
) {
    val user = authViewModel.currentUser
    val primaryColor = MaterialTheme.colorScheme.primary
    val view = LocalView.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.bookmarkMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    var showBigImage by remember { mutableStateOf(false) }

    var selectedMainTab by remember { mutableIntStateOf(0) } 
    var selectedBookmarkType by remember { mutableIntStateOf(0) } 
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedCourse by remember { mutableStateOf("Breakfast") }
    val courses = listOf("Breakfast", "Lunch", "Dinner", "Snack")

    val myRecipes = viewModel.myRecipes
    val bookmarkedRecipes = viewModel.bookmarkedRecipes
    val bookmarkedChefs = bookmarkViewModel.bookmarkedChefsList

    SideEffect {
        val window = (view.context as Activity).window
        window.statusBarColor = primaryColor.toArgb()
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
    }

    LaunchedEffect(selectedMainTab, selectedBookmarkType, user) {
        val uid = user?.id ?: return@LaunchedEffect
        if (selectedMainTab == 0) {
            viewModel.fetchMyRecipes(uid)
        } else {
            if (selectedBookmarkType == 0) {
                viewModel.fetchBookmarkedRecipes(uid)
            } else {
                bookmarkViewModel.fetchBookmarkedChefs(uid)
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp),
                drawerContainerColor = Color(0xFFF7F2F9)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(text = "Menu", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(24.dp))

                    DrawerItem("Edit Profile", R.drawable.ic_edit) {
                        scope.launch {
                            drawerState.close()
                            navController.navigate(Screen.EditProfile.route)
                        }
                    }
                    DrawerItem("Edit Body Status", R.drawable.ic_outline_account_circle) {
                        scope.launch {
                            drawerState.close()
                            navController.navigate(Screen.EditBodyStatus.route)
                        }
                    }
                    DrawerItem("View & Request Ingredients", R.drawable.ic_ingredient_list) {
                        navController.navigate(Screen.Ingredients.route)
                    }
                    DrawerItem("Shopping List", R.drawable.ic_shopping_cart) {
                        navController.navigate(Screen.ShoppingList.route)
                    }
                    DrawerItem("Register as Chef", R.drawable.ic_hiring) {
                        navController.navigate(Screen.Welcome.route)
                    }
                    DrawerItem("Appointment History", R.drawable.ic_calendar) { }
                    
                    DrawerItem("Change Password", R.drawable.ic_check) {
                        scope.launch {
                            drawerState.close()
                            navController.navigate(Screen.ChangePassword.route)
                        }
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    TextButton(
                        onClick = {
                            // 🌟 FIX: Just call logout. MainActivity will swap screens automatically.
                            authViewModel.logout { }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Logout", color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) {
        Scaffold(
            containerColor = Color.White,
            // 🌟 FIX: Zero insets prevents the inner Scaffold from adding extra bottom space
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary)
                        .statusBarsPadding()
                        .padding(bottom = 24.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_hamburger_menu),
                            contentDescription = "Menu",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp).clickable {
                                scope.launch {
                                    drawerState.open()
                                }
                            }
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Profile",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar Circle
                        Surface(
                            modifier = Modifier
                                .size(80.dp)
                                .clickable { showBigImage = true },
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.2f)
                        ) {
                            val profilePicUrl = user?.profilePicUrl ?: ""
                            val bitmap = remember(profilePicUrl) {
                                if (profilePicUrl.isNotEmpty() && !profilePicUrl.startsWith("http")) {
                                    try {
                                        val imageBytes = Base64.decode(profilePicUrl, Base64.DEFAULT)
                                        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                                    } catch (e: Exception) { null }
                                } else null
                            }

                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Profile Picture",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else if (profilePicUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = profilePicUrl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Text(
                                        text = user?.name?.take(1)?.uppercase() ?: "?",
                                        color = Color.White,
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(20.dp))

                        Column {
                            Text(
                                text = user?.name ?: "",
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (!user?.description.isNullOrEmpty()) {
                                Text(
                                    text = user?.description!!,
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.primary) 
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .background(Color.White)
                ) {
                    TabRow(
                        selectedTabIndex = selectedMainTab,
                        containerColor = Color.White,
                        contentColor = MaterialTheme.colorScheme.primary,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedMainTab]),
                                color = MaterialTheme.colorScheme.primary
                            )
                        },
                        divider = { HorizontalDivider(color = Color(0xFFEEEEEE)) }
                    ) {
                        Tab(
                            selected = selectedMainTab == 0,
                            onClick = { selectedMainTab = 0 },
                            text = { Text("User Recipes", fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedMainTab == 1,
                            onClick = { selectedMainTab = 1 },
                            text = { Text("Bookmarks", fontWeight = FontWeight.Bold) }
                        )
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (selectedMainTab == 0 || (selectedMainTab == 1 && selectedBookmarkType == 0)) {
                            item(span = { GridItemSpan(2) }) {
                                Column {
                                    OutlinedTextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        placeholder = { Text("Search recipes here", fontSize = 14.sp, color = Color.Gray) },
                                        modifier = Modifier.fillMaxWidth().height(52.dp),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = Color(0xFFF2F2F2),
                                            unfocusedContainerColor = Color(0xFFF2F2F2),
                                            focusedBorderColor = Color.Transparent,
                                            unfocusedBorderColor = Color.Transparent
                                        ),
                                        trailingIcon = {
                                            Icon(
                                                painter = painterResource(id = R.drawable.search),
                                                contentDescription = "Search",
                                                modifier = Modifier.size(20.dp).clickable { /* Handle search click */ }
                                            )
                                        }
                                    )
                                    
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    Text("Course", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        lazyItems(courses) { course ->
                                            val isSelected = selectedCourse == course
                                            Surface(
                                                onClick = { selectedCourse = course },
                                                shape = RoundedCornerShape(20.dp),
                                                color = if (isSelected) primaryColor else Color(0xFFEEEEEE)
                                            ) {
                                                Text(
                                                    text = course,
                                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                                    color = if (isSelected) Color.White else Color.Black,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(selectedCourse, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                }
                            }
                        } else if (selectedMainTab == 1) {
                            item(span = { GridItemSpan(2) }) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    FilterChip(
                                        selected = selectedBookmarkType == 0,
                                        onClick = { selectedBookmarkType = 0 },
                                        label = { Text("Recipes") }
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    FilterChip(
                                        selected = selectedBookmarkType == 1,
                                        onClick = { selectedBookmarkType = 1 },
                                        label = { Text("Chefs") }
                                    )
                                }
                            }
                        }

                        if (selectedMainTab == 0) {
                            val filtered = myRecipes.filter { it.recipeName.contains(searchQuery, true) && it.recipeCourse == selectedCourse }
                            gridItems(filtered) { recipe ->
                                RecipeCardItem(recipe = recipe, showMenu = true)
                            }
                        } else if (selectedMainTab == 1) {
                            if (selectedBookmarkType == 0) {
                                val filtered = bookmarkedRecipes.filter { it.recipeName.contains(searchQuery, true) && it.recipeCourse == selectedCourse }
                                gridItems(filtered) { recipe ->
                                    // 🌟 Enable bookmark click to allow removing from bookmarks
                                    RecipeCardItem(
                                        recipe = recipe, 
                                        isBookmarked = true,
                                        onBookmarkClick = {
                                            // 🌟 FIX: Send short customId instead of long UUID
                                            user?.customId?.let { cid ->
                                                recipe.recipe_id?.let { rid -> 
                                                    viewModel.toggleBookmark(cid, rid, recipe.recipeName) 
                                                }
                                            }
                                        }
                                    )
                                }
                            } else {
                                gridItems(bookmarkedChefs) { chef ->
                                    ChefCardItem(chef = chef)
                                }
                            }
                        }
                        
                        // 🌟 Removed the extra 80dp spacer to fix the empty space issue at the bottom
                    }
                }
            }
        }
    }

    // Big Image View
    if (showBigImage && user != null) {
        Dialog(onDismissRequest = { showBigImage = false }) {
            Box(
                modifier = Modifier.fillMaxWidth().aspectRatio(1f).background(Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                val profilePicUrl = user.profilePicUrl ?: ""
                val bitmap = remember(profilePicUrl) {
                    if (profilePicUrl.isNotEmpty() && !profilePicUrl.startsWith("http")) {
                        try {
                            val imageBytes = Base64.decode(profilePicUrl, Base64.DEFAULT)
                            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                        } catch (e: Exception) { null }
                    } else null
                }

                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Full Profile Picture",
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else if (profilePicUrl.isNotEmpty()) {
                    AsyncImage(
                        model = profilePicUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(primaryColor.copy(alpha = 0.8f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user.name?.take(1)?.uppercase() ?: "?",
                            color = Color.White,
                            fontSize = 120.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DrawerItem(
    label: String,
    @DrawableRes iconRes: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = Color.Black.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.width(20.dp))
        Text(text = label, fontSize = 16.sp, color = Color.Black)
    }
}

@Composable
fun ChefCardItem(chef: com.example.mobileassignmentloginpart.Model.Chef) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().height(200.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(12.dp)) {
            if (!chef.profilePictureUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = chef.profilePictureUrl,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(modifier = Modifier.size(80.dp).clip(CircleShape).background(Color(0xFFEEEEEE)), contentAlignment = Alignment.Center) {
                    Icon(painterResource(id = R.drawable.foodieheallogo), null, modifier = Modifier.size(40.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(chef.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1)
            Text(chef.status, fontSize = 12.sp, color = Color.Gray)
        }
    }
}
