package com.example.foodieheal.User.View

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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.foodieheal.R
import com.example.foodieheal.Recipe.Model.Recipe
import com.example.foodieheal.navigation.Screen
import com.example.foodieheal.User.viewModel.AuthViewModel
import com.example.foodieheal.Recipe.viewModel.RecipeViewModel
import com.example.foodieheal.Recipe.View.RecipeCardItem
import com.example.foodieheal.Chef.model.Chef
import com.example.foodieheal.hiring.viewmodel.BookmarkViewModel
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

    // 🌟 Show success messages for password, profile, and body status updates
    LaunchedEffect(Unit) {
        authViewModel.profileEvents.collect { event ->
            val message = when(event) {
                is AuthViewModel.ProfileEvent.PasswordSuccess -> "Password updated successfully!"
                is AuthViewModel.ProfileEvent.ProfileSuccess -> "Profile updated successfully!"
                is AuthViewModel.ProfileEvent.BodyStatusSuccess -> "Body status updated successfully!"
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.bookmarkMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    var showBigImage by remember { mutableStateOf(false) }
    
    // 🌟 State for the Delete Confirmation Dialog
    var recipeToDelete by remember { mutableStateOf<Recipe?>(null) }

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
        // 🌟 FIX: Use the short customId (U001) for all filtering to stay consistent with Recipes screen
        val cid = user?.customId ?: return@LaunchedEffect
        if (selectedMainTab == 0) {
            viewModel.fetchMyRecipes(cid)
        } else {
            if (selectedBookmarkType == 0) {
                viewModel.fetchBookmarkedRecipes(cid)
            } else {
                // For chefs, we still use the regular ID as per the Hiring module logic
                user.id?.let { bookmarkViewModel.fetchBookmarkedChefs(it) }
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surface // 🌟 Themed Drawer
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(text = "Menu", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) // 🌟 Themed Text
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
                        scope.launch {
                            drawerState.close()
                            navController.navigate(Screen.Ingredients.route)
                        }
                    }
                    DrawerItem("Shopping List", R.drawable.ic_shopping_cart) {
                        scope.launch {
                            drawerState.close()
                            navController.navigate(Screen.ShoppingList.route)
                        }
                    }
                    DrawerItem("Register as Chef", R.drawable.ic_hiring) {
                        navController.navigate(Screen.Welcome.route)
                    }
                    DrawerItem("Appointment History", R.drawable.ic_calendar) {
                        navController.navigate(Screen.AppoinmtmentHistory.route)
                    }
                    DrawerItem("My Wallet", R.drawable.wallet) {
                        scope.launch {
                            drawerState.close()
                            navController.navigate(Screen.Wallet.route)
                        }
                    }
                    DrawerItem("Payment Methods", R.drawable.dollar_symbol) {
                        scope.launch {
                            drawerState.close()
                            navController.navigate(Screen.PaymentMethod.route)
                        }
                    }
                    DrawerItem("Change Password", R.drawable.changepassword4) {
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
                        Text("Logout", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background, // 🌟 Themed Background
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
                            tint = MaterialTheme.colorScheme.onPrimary, // 🌟 Themed Icon
                            modifier = Modifier.size(24.dp).clickable {
                                scope.launch {
                                    drawerState.open()
                                }
                            }
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Profile",
                            color = MaterialTheme.colorScheme.onPrimary, // 🌟 Themed Text
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
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f) // 🌟 Themed Color
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
                                        color = MaterialTheme.colorScheme.onPrimary, // 🌟 Themed Text
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
                                color = MaterialTheme.colorScheme.onPrimary, // 🌟 Themed Text
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (!user?.description.isNullOrEmpty()) {
                                Text(
                                    text = user?.description!!,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f), // 🌟 Themed Text
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
                        .background(MaterialTheme.colorScheme.background) // 🌟 Themed Background
                ) {
                    TabRow(
                        selectedTabIndex = selectedMainTab,
                        containerColor = MaterialTheme.colorScheme.surface, // 🌟 Themed Background
                        contentColor = MaterialTheme.colorScheme.primary,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedMainTab]),
                                color = MaterialTheme.colorScheme.primary
                            )
                        },
                        divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) } // 🌟 Themed Divider
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
                                        placeholder = { Text("Search recipes here", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }, // 🌟 Themed Text
                                        modifier = Modifier.fillMaxWidth().height(52.dp),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant, // 🌟 Themed Background
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant, // 🌟 Themed Background
                                            focusedBorderColor = Color.Transparent,
                                            unfocusedBorderColor = Color.Transparent
                                        ),
                                        trailingIcon = {
                                            Icon(
                                                painter = painterResource(id = R.drawable.search),
                                                contentDescription = "Search",
                                                modifier = Modifier.size(20.dp).clickable { /* Handle search click */ },
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant // 🌟 Themed Icon
                                            )
                                        }
                                    )
                                    
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    Text("Course", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground) // 🌟 Themed Text
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        lazyItems(courses) { course ->
                                            val isSelected = selectedCourse == course
                                            Surface(
                                                onClick = { selectedCourse = course },
                                                shape = RoundedCornerShape(20.dp),
                                                color = if (isSelected) primaryColor else MaterialTheme.colorScheme.surfaceVariant // 🌟 Themed Background
                                            ) {
                                                Text(
                                                    text = course,
                                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, // 🌟 Themed Text
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(selectedCourse, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground) // 🌟 Themed Text
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
                                        label = { Text("Recipes") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = primaryColor,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    FilterChip(
                                        selected = selectedBookmarkType == 1,
                                        onClick = { selectedBookmarkType = 1 },
                                        label = { Text("Chefs") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = primaryColor,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    )
                                }
                            }
                        }

                        if (selectedMainTab == 0) {
                            val filtered = myRecipes.filter { it.recipeName.contains(searchQuery, true) && it.recipeCourse == selectedCourse }
                            gridItems(filtered) { recipe ->
                                RecipeCardItem(
                                    recipe = recipe,
                                    showMenu = true,
                                    isBookmarked = viewModel.bookmarkedRecipeIds.contains(recipe.recipe_id),
                                    onBookmarkClick = {
                                        user?.customId?.let { cid ->
                                            recipe.recipe_id?.let { rid ->
                                                viewModel.toggleBookmark(
                                                    cid,
                                                    rid,
                                                    recipe.recipeName
                                                )
                                            }
                                        }
                                    },
                                    onDeleteClick = {
                                        recipeToDelete = recipe
                                    }, // 🌟 FIX: Pass the delete trigger
                                    onEditClick = {
                                        recipe.recipe_id?.let { id ->
                                            navController.navigate(Screen.EditRecipe.createRoute(id))
                                        }
                                    },
                                    onClick = {
                                        recipe.recipe_id?.let { id ->
                                            navController.navigate(
                                                Screen.RecipeDetails.createRoute(
                                                    id
                                                )
                                            )
                                        }
                                    }
                                )
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
                                                    viewModel.toggleBookmark(
                                                        cid,
                                                        rid,
                                                        recipe.recipeName
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            recipe.recipe_id?.let { id ->
                                                navController.navigate(
                                                    Screen.RecipeDetails.createRoute(
                                                        id
                                                    )
                                                )
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

    // 🌟 Delete Confirmation Dialog for Profile Screen
    if (recipeToDelete != null) {
        AlertDialog(
            onDismissRequest = { recipeToDelete = null },
            title = { Text("Delete Recipe") },
            text = { Text("Are you sure you want to delete '${recipeToDelete?.recipeName}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val rid = recipeToDelete?.recipe_id
                        val cid = user?.customId
                        if (rid != null && cid != null) {
                            viewModel.deleteRecipe(rid, cid)
                        }
                        recipeToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { recipeToDelete = null }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
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
                            color = MaterialTheme.colorScheme.onPrimary,
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
            modifier = Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f) // 🌟 Themed Icon
        )
        Spacer(modifier = Modifier.width(20.dp))
        Text(text = label, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface) // 🌟 Themed Text
    }
}

@Composable
fun ChefCardItem(chef: Chef) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().height(200.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), // 🌟 Themed Card
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
                Box(modifier = Modifier.size(80.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) { // 🌟 Themed Background
                    Icon(painterResource(id = R.drawable.foodieheallogo), null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary) // 🌟 Themed Icon
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(chef.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, color = MaterialTheme.colorScheme.onSurface) // 🌟 Themed Text
            Text(chef.status, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) // 🌟 Themed Text
        }
    }
}
