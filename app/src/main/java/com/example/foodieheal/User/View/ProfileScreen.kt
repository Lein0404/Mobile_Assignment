package com.example.foodieheal.User.View

import android.app.Activity
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.annotation.DrawableRes
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.foodieheal.Chef.States
import com.example.foodieheal.R
import com.example.foodieheal.Recipe.Model.Recipe
import com.example.foodieheal.navigation.Screen
import com.example.foodieheal.User.viewModel.AuthViewModel
import com.example.foodieheal.Recipe.viewModel.RecipeViewModel
import com.example.foodieheal.Recipe.View.RecipeCardItem
import com.example.foodieheal.Recipe.View.FilterSectionHeader
import com.example.foodieheal.Chef.model.Chef
import com.example.foodieheal.Chef.ViewModel.Register.ChefRegisterViewModel
import com.example.foodieheal.User.Model.User
import com.example.foodieheal.hiring.components.ActiveFiltersRow
import com.example.foodieheal.hiring.components.ChefFilterBottomSheet
import com.example.foodieheal.hiring.components.ChefFilterState
import com.example.foodieheal.hiring.components.filterAndSortChefs
import com.example.foodieheal.hiring.viewmodel.BookmarkViewModel
import com.example.foodieheal.User.viewModel.FollowViewModel
import com.example.foodieheal.ui.components.ShareRecipeDialog
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: RecipeViewModel,
    authViewModel: AuthViewModel,
    chefRegisterViewModel: ChefRegisterViewModel,
    bookmarkViewModel: BookmarkViewModel = viewModel(),
    followViewModel: FollowViewModel = viewModel(),
    targetCustomId: String? = null
) {
    val user = authViewModel.currentUser
    
    // 🌟 isVisitor Check: True only if we have a valid target ID that isn't ours or the nav placeholder
    val isVisitor = targetCustomId != null && 
                    targetCustomId != user?.customId && 
                    targetCustomId != "{customId}"

    val isMyProfile = !isVisitor
    
    // Use targetCustomId if visiting someone else, else use current user
    val effectiveCustomId = if (isVisitor) targetCustomId else user?.customId
    
    val primaryColor = MaterialTheme.colorScheme.primary
    val view = LocalView.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var chefStatusDialogInfo by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showReapplyDialog by remember { mutableStateOf(false) }

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
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
        }
    }

    LaunchedEffect(Unit) {
        followViewModel.followEvents.collect { event ->
            val message = when(event) {
                FollowViewModel.FollowEvent.RequestSent -> view.context.getString(R.string.follow_request_sent)
                FollowViewModel.FollowEvent.RequestCancelled -> view.context.getString(R.string.follow_request_cancelled)
                FollowViewModel.FollowEvent.Unfollowed -> view.context.getString(R.string.unfollowed_user)
                FollowViewModel.FollowEvent.RequestAccepted -> view.context.getString(R.string.follow_request_accepted)
                FollowViewModel.FollowEvent.RequestRejected -> view.context.getString(R.string.follow_request_rejected)
                FollowViewModel.FollowEvent.NoInternet -> view.context.getString(R.string.desc_connect_internet_follow)
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    var showBigImage by remember { mutableStateOf(false) }
    
    // 🌟 State for the Delete Confirmation Dialog
    var recipeToDelete by remember { mutableStateOf<Recipe?>(null) }
    var recipeToShare by remember { mutableStateOf<Recipe?>(null) }

    var selectedMainTab by remember { mutableIntStateOf(0) } 
    var showChefBookmarks by remember { mutableStateOf(false) } // 🌟 Toggle between Recipes and Chefs
    
    val visitorProfile = remember { mutableStateOf<User?>(null) }

    LaunchedEffect(targetCustomId) {
        if (!isMyProfile && targetCustomId != null) {
            val repo = com.example.foodieheal.User.Repo.UserRepository()
            visitorProfile.value = repo.getUserByCustomId(targetCustomId)
            
            user?.customId?.let { myId ->
                followViewModel.fetchFollowStatus(myId, targetCustomId)
            }
        }
    }

    LaunchedEffect(effectiveCustomId) {
        effectiveCustomId?.let { cid ->
            followViewModel.fetchFollowCounts(cid)
        }
    }

    val displayUser = if (isMyProfile) user else visitorProfile.value
    
    var userRecipesSearchQuery by remember { mutableStateOf("") }
    var bookmarksSearchQuery by remember { mutableStateOf("") }
    
    // 🌟 Chef Filter State (matching hiring screen)
    var chefFilterState by remember { mutableStateOf(ChefFilterState()) }
    var showChefFilterSheet by remember { mutableStateOf(false) }
    
    // 🌟 Recipe Filter State (matching recipes screen)
    var filterMaxTime by remember { mutableFloatStateOf(240f) }
    var filterMaxCalories by remember { mutableFloatStateOf(5000f) }
    var filterSkill by remember { mutableStateOf<String?>(null) }
    var filterBudget by remember { mutableStateOf<String?>(null) }
    var filterIngredients by remember { mutableStateOf(setOf<String>()) }
    var showRecipeFilterSheet by remember { mutableStateOf(false) }
    
    var selectedCourse by remember { mutableStateOf("All") }
    val courses = listOf("All", "Breakfast", "Lunch", "Dinner", "Snack")

    val myRecipes = if (isMyProfile) viewModel.myRecipes else viewModel.myRecipes.filter { 
        it.visibility == "public" || (it.visibility == "followers" && followViewModel.followStatus == "ACCEPTED")
    }
    val bookmarkedRecipes = viewModel.bookmarkedRecipes
    val bookmarkedChefs = bookmarkViewModel.bookmarkedChefsList

    // 🌟 Search & Filter Chefs Logic (Hiring Screen Style)
    val filteredChefs = remember(bookmarkedChefs, chefFilterState) {
        filterAndSortChefs(bookmarkedChefs, chefFilterState)
    }

    SideEffect {
        val window = (view.context as Activity).window
        window.statusBarColor = primaryColor.toArgb()
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
    }

    LaunchedEffect(selectedMainTab, showChefBookmarks, effectiveCustomId) {
        val cid = effectiveCustomId ?: return@LaunchedEffect
        
        // Reset filters when switching tabs
        userRecipesSearchQuery = ""
        bookmarksSearchQuery = ""
        filterMaxTime = 240f
        filterMaxCalories = 5000f
        filterSkill = null
        filterBudget = null
        filterIngredients = emptySet()
        
        if (selectedMainTab == 0) {
            viewModel.fetchMyRecipes(cid)
        } else {
            if (!showChefBookmarks) {
                viewModel.fetchBookmarkedRecipes(cid)
            } else {
                displayUser?.id?.let { bookmarkViewModel.fetchBookmarkedChefs(it) }
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = isMyProfile,
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
                    DrawerItem("Become a Chef", R.drawable.ic_hiring) {
                        scope.launch {
                            drawerState.close()
                            authViewModel.checkChefApplicationStatus { existingChef ->
                                val effectiveUserId = authViewModel.getEffectiveUserId()
                                val effectiveEmail = authViewModel.getEffectiveUserEmail()

                                if (existingChef != null) {
                                    when (existingChef.status.lowercase()) {
                                        "pending" -> {
                                            chefStatusDialogInfo = "Application Under Review" to "Your chef application is currently under review by our administrators. Please wait for approval."
                                        }
                                        "approved" -> {
                                            chefStatusDialogInfo = "Chef Account Active" to "You are already an approved Chef! Please use the Chef Login Portal from the login screen to access your chef dashboard."
                                        }
                                        "rejected" -> {
                                            showReapplyDialog = true
                                        }
                                        else -> {
                                            chefRegisterViewModel.initForUpgrade(user, effectiveEmail, effectiveUserId)
                                            navController.navigate(Screen.BasicInfo.route)
                                        }
                                    }
                                } else {
                                    chefRegisterViewModel.initForUpgrade(user, effectiveEmail, effectiveUserId)
                                    navController.navigate(Screen.BasicInfo.route)
                                }
                            }
                        }
                    }
                    DrawerItem("Appointment History", R.drawable.ic_calendar) {
                        navController.navigate(Screen.AppoinmtmentHistory.route)
                    }
                    DrawerItem("Follow Requests", R.drawable.ic_hiring) {
                        scope.launch {
                            drawerState.close()
                            navController.navigate(Screen.FollowRequests.route)
                        }
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
                            // 🌟 FIX: Clear user-specific memory data on logout to prevent "Leon vs KK" data pollution
                            authViewModel.logout { 
                                viewModel.clearUserData()
                                // Optionally clear bookmarkViewModel too if needed
                            }
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
                            painter = painterResource(id = if (isMyProfile) R.drawable.ic_hamburger_menu else R.drawable.ic_arrowback),
                            contentDescription = if (isMyProfile) "Menu" else "Back",
                            tint = MaterialTheme.colorScheme.onPrimary, // 🌟 Themed Icon
                            modifier = Modifier.size(24.dp).clickable {
                                if (isMyProfile) {
                                    scope.launch {
                                        drawerState.open()
                                    }
                                } else {
                                    navController.popBackStack()
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
                            val profilePicUrl = displayUser?.profilePicUrl ?: ""
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
                                        text = if (displayUser == null && isVisitor) "?" else (displayUser?.name?.take(1)?.uppercase() ?: "?"),
                                        color = MaterialTheme.colorScheme.onPrimary, // 🌟 Themed Text
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(20.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (displayUser == null && isVisitor) "Offline User" else (displayUser?.name ?: ""),
                                color = MaterialTheme.colorScheme.onPrimary, // 🌟 Themed Text
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (displayUser != null && !displayUser.description.isNullOrEmpty()) {
                                Text(
                                    text = displayUser.description!!,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f), // 🌟 Themed Text
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            } else if (displayUser == null && isVisitor) {
                                Text(
                                    text = "Cannot load profile while offline.",
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                                    fontSize = 12.sp
                                )
                            }
                            
                            Row(modifier = Modifier.padding(top = 12.dp)) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.clickable { 
                                        effectiveCustomId?.let { cid ->
                                            navController.navigate(Screen.FollowList.createRoute(cid, "followers"))
                                        }
                                    }
                                ) {
                                    Text(
                                        text = "${followViewModel.followerCount}",
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Followers",
                                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                                        fontSize = 11.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(24.dp))
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.clickable { 
                                        effectiveCustomId?.let { cid ->
                                            navController.navigate(Screen.FollowList.createRoute(cid, "following"))
                                        }
                                    }
                                ) {
                                    Text(
                                        text = "${followViewModel.followingCount}",
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Following",
                                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            if (isVisitor && user != null && targetCustomId != null) {
                                val status = followViewModel.followStatus
                                val buttonText = when (status) {
                                    null -> "Follow"
                                    "PENDING" -> "Request Sent"
                                    "ACCEPTED" -> "Unfollow"
                                    else -> "Follow"
                                }
                                Button(
                                    onClick = {
                                        user.customId?.let { myId ->
                                            followViewModel.toggleFollow(myId, targetCustomId)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.onPrimary,
                                        contentColor = MaterialTheme.colorScheme.primary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                                    modifier = Modifier.padding(top = 12.dp).height(32.dp)
                                ) {
                                    Text(buttonText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
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
                            if (selectedMainTab < tabPositions.size) {
                                TabRowDefaults.SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(tabPositions[selectedMainTab]),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) } // 🌟 Themed Divider
                    ) {
                        Tab(
                            selected = selectedMainTab == 0,
                            onClick = { selectedMainTab = 0 },
                            text = { Text(if (isMyProfile) "User Recipes" else "Recipes", fontWeight = FontWeight.Bold) }
                        )
                        if (isMyProfile) {
                            Tab(
                                selected = selectedMainTab == 1,
                                onClick = { selectedMainTab = 1 },
                                text = { Text("Bookmarks", fontWeight = FontWeight.Bold) }
                            )
                        }
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 🌟 Beautiful Pill Toggle (Only for Bookmarks)
                        if (selectedMainTab == 1) {
                            item(span = { GridItemSpan(2) }) {
                                val recipeBgColor by animateColorAsState(if (!showChefBookmarks) MaterialTheme.colorScheme.primary else Color.Transparent, label = "")
                                val chefBgColor by animateColorAsState(if (showChefBookmarks) MaterialTheme.colorScheme.primary else Color.Transparent, label = "")
                                val recipeTextColor by animateColorAsState(if (!showChefBookmarks) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, label = "")
                                val chefTextColor by animateColorAsState(if (showChefBookmarks) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, label = "")

                                Surface(
                                    shape = RoundedCornerShape(24.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize().padding(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Recipes Option
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(recipeBgColor)
                                                .clickable { showChefBookmarks = false },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    painter = painterResource(R.drawable.ic_recipe),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp),
                                                    tint = recipeTextColor
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Recipes", color = recipeTextColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            }
                                        }

                                        // Chefs Option
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(chefBgColor)
                                                .clickable { showChefBookmarks = true },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    painter = painterResource(R.drawable.ic_hiring),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp),
                                                    tint = chefTextColor
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Chefs", color = chefTextColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 🌟 Search & Course Section
                        item(span = { GridItemSpan(2) }) {
                            Column {
                                if (selectedMainTab == 1 && showChefBookmarks) {
                                    // 🌟 Chef Search Bar (Redesigned for Consistency)
                                    OutlinedTextField(
                                        value = chefFilterState.searchQuery,
                                        onValueChange = { chefFilterState = chefFilterState.copy(searchQuery = it) },
                                        placeholder = { Text("Search chefs here", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        modifier = Modifier.fillMaxWidth().height(56.dp),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            focusedBorderColor = Color.Transparent,
                                            unfocusedBorderColor = Color.Transparent
                                        ),
                                        trailingIcon = {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 12.dp)) {
                                                // Filter Icon with active color if filters are applied
                                                val filterIconTint = if (chefFilterState.activeFilterCount > 0) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant
                                                
                                                BadgedBox(
                                                    badge = {
                                                        if (chefFilterState.activeFilterCount > 0) {
                                                            Badge(containerColor = primaryColor, contentColor = Color.White) {
                                                                Text(chefFilterState.activeFilterCount.toString())
                                                            }
                                                        }
                                                    }
                                                ) {
                                                    Icon(
                                                        painter = painterResource(id = R.drawable.filter),
                                                        contentDescription = "Filter",
                                                        modifier = Modifier.size(20.dp).clickable { showChefFilterSheet = true },
                                                        tint = filterIconTint
                                                    )
                                                }
                                                
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Icon(
                                                    painter = painterResource(id = R.drawable.search),
                                                    contentDescription = "Search",
                                                    modifier = Modifier.size(20.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    )
                                    
                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Active filter chips row
                                    ActiveFiltersRow(
                                        filterState = chefFilterState,
                                        onFilterChange = { chefFilterState = it },
                                        onResetAll = {
                                            chefFilterState =
                                                ChefFilterState(searchQuery = chefFilterState.searchQuery)
                                        }
                                    )
                                } else {
                                    // 🌟 Recipe Search Bar (Standard Style)
                                    val currentQuery = if (selectedMainTab == 0) userRecipesSearchQuery else bookmarksSearchQuery
                                    OutlinedTextField(
                                        value = currentQuery,
                                        onValueChange = { 
                                            if (selectedMainTab == 0) userRecipesSearchQuery = it else bookmarksSearchQuery = it
                                        },
                                        placeholder = { 
                                            Text(
                                                if (selectedMainTab == 0) "Search recipe here" else "Search recipes/authors", 
                                                fontSize = 14.sp, 
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            ) 
                                        },
                                        modifier = Modifier.fillMaxWidth().height(56.dp),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            focusedBorderColor = Color.Transparent,
                                            unfocusedBorderColor = Color.Transparent
                                        ),
                                        trailingIcon = {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 12.dp)) {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.filter),
                                                    contentDescription = "Filter",
                                                    modifier = Modifier.size(20.dp).clickable { showRecipeFilterSheet = true },
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Icon(
                                                    painter = painterResource(id = R.drawable.search),
                                                    contentDescription = "Search",
                                                    modifier = Modifier.size(20.dp).clickable { /* Handle search click */ },
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    )
                                    
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    Text("Course", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        lazyItems(courses) { course ->
                                            val isSelected = selectedCourse == course
                                            Surface(
                                                onClick = { selectedCourse = course },
                                                shape = RoundedCornerShape(20.dp),
                                                color = if (isSelected) primaryColor else MaterialTheme.colorScheme.surfaceVariant
                                            ) {
                                                Text(
                                                    text = course,
                                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(selectedCourse, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                }
                            }
                        }

                        // 🌟 Content Display
                        if (selectedMainTab == 0) {
                            if (viewModel.isLoading) {
                                item(span = { GridItemSpan(2) }) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(top = 80.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(color = primaryColor)
                                    }
                                }
                            } else {
                                val filtered = myRecipes.filter { recipe ->
                                    val matchesSearch = recipe.recipeName.contains(userRecipesSearchQuery, true)
                                    val matchesCourse = selectedCourse == "All" || recipe.recipeCourse.equals(selectedCourse, ignoreCase = true)
                                    val matchesTime = recipe.time <= filterMaxTime.toInt()
                                    val matchesCalories = recipe.calories <= filterMaxCalories.toInt()
                                    val matchesSkill = filterSkill == null || recipe.cookingSkill.equals(filterSkill, ignoreCase = true)
                                    val matchesBudget = filterBudget == null || recipe.estimatedBudget == filterBudget
                                    val matchesIngredients = filterIngredients.isEmpty() || recipe.ingredients.any { it.name in filterIngredients }
                                    
                                    matchesSearch && matchesCourse && (filterMaxTime == 240f || matchesTime) && (filterMaxCalories == 5000f || matchesCalories) && matchesSkill && matchesBudget && matchesIngredients
                                }
                                
                                if (filtered.isEmpty()) {
                                    item(span = { GridItemSpan(2) }) {
                                        EmptyState(
                                            iconRes = R.drawable.ic_recipe,
                                            title = stringResource(R.string.empty_no_my_recipes),
                                            subtitle = stringResource(R.string.empty_my_recipes_sub)
                                        )
                                    }
                                } else {
                                    gridItems(filtered) { recipe ->
                                        RecipeCardItem(
                                            recipe = recipe,
                                            currentUser = user, // 🌟 Pass current user for card name sync
                                            showMenu = isMyProfile,
                                            isBookmarked = viewModel.bookmarkedRecipeIds.contains(recipe.recipe_id),
                                            onBookmarkClick = {
                                                user?.customId?.let { cid ->
                                                    recipe.recipe_id?.let { rid ->
                                                        viewModel.toggleBookmark(cid, rid, recipe.recipeName)
                                                    }
                                                }
                                            },
                                            onDeleteClick = { recipeToDelete = recipe },
                                            onEditClick = {
                                                recipe.recipe_id?.let { id ->
                                                    navController.navigate(Screen.EditRecipe.createRoute(id))
                                                }
                                            },
                                            onShareClick = { recipeToShare = it },
                                            onClick = {
                                                recipe.recipe_id?.let { id ->
                                                    navController.navigate(Screen.RecipeDetails.createRoute(id))
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        } else if (selectedMainTab == 1) {
                            if (!showChefBookmarks) {
                                if (viewModel.isLoading) {
                                    item(span = { GridItemSpan(2) }) {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().padding(top = 80.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(color = primaryColor)
                                        }
                                    }
                                } else {
                                    val filtered = bookmarkedRecipes.filter { recipe ->
                                        val matchesSearch = (recipe.recipeName.contains(bookmarksSearchQuery, true) || 
                                                           recipe.authorName?.contains(bookmarksSearchQuery, true) == true)
                                        val matchesCourse = selectedCourse == "All" || recipe.recipeCourse.equals(selectedCourse, ignoreCase = true)
                                        val matchesTime = recipe.time <= filterMaxTime.toInt()
                                        val matchesCalories = recipe.calories <= filterMaxCalories.toInt()
                                        val matchesSkill = filterSkill == null || recipe.cookingSkill.equals(filterSkill, ignoreCase = true)
                                        val matchesBudget = filterBudget == null || recipe.estimatedBudget == filterBudget
                                        val matchesIngredients = filterIngredients.isEmpty() || recipe.ingredients.any { it.name in filterIngredients }

                                        matchesSearch && matchesCourse && (filterMaxTime == 240f || matchesTime) && (filterMaxCalories == 5000f || matchesCalories) && matchesSkill && matchesBudget && matchesIngredients
                                    }
                                    
                                    if (filtered.isEmpty()) {
                                        item(span = { GridItemSpan(2) }) {
                                            EmptyState(
                                                iconRes = R.drawable.bookmark,
                                                title = stringResource(R.string.empty_no_bookmarked_recipes),
                                                subtitle = stringResource(R.string.empty_bookmarked_recipes_sub)
                                            )
                                        }
                                    } else {
                                        gridItems(filtered) { recipe ->
                                            RecipeCardItem(
                                                recipe = recipe,
                                                currentUser = user, // 🌟 Pass current user for card name sync
                                                isBookmarked = true,
                                                onBookmarkClick = {
                                                user?.customId?.let { cid ->
                                                    recipe.recipe_id?.let { rid ->
                                                        viewModel.toggleBookmark(cid, rid, recipe.recipeName)
                                                    }
                                                }
                                            },
                                                onClick = {
                                                    recipe.recipe_id?.let { id ->
                                                        navController.navigate(Screen.RecipeDetails.createRoute(id))
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            } else {
                                if (bookmarkViewModel.isLoadingBookmarks) {
                                    item(span = { GridItemSpan(2) }) {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().padding(top = 80.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(color = primaryColor)
                                        }
                                    }
                                } else {
                                    if (filteredChefs.isEmpty()) {
                                        item(span = { GridItemSpan(2) }) {
                                            EmptyState(
                                                iconRes = R.drawable.ic_hiring,
                                                title = if (chefFilterState.isFilterActive) "No chefs match filters" else stringResource(R.string.empty_no_bookmarked_chefs),
                                                subtitle = if (chefFilterState.isFilterActive) "Try resetting your search or filters" else stringResource(R.string.empty_bookmarked_chefs_sub)
                                            )
                                        }
                                    } else {
                                        gridItems(filteredChefs) { chef ->
                                            ChefCardItem(
                                                chef = chef,
                                                onClick = {
                                                    val chefId = chef.chefId.ifEmpty { chef.id }
                                                    navController.navigate("${Screen.HiringChefDetails.route}/$chefId")
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

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

    // 🌟 Share Dialog
    recipeToShare?.let { recipe ->
        ShareRecipeDialog(
            recipe = recipe,
            authorName = if (recipe.author_id == user?.customId) user?.name else recipe.authorName,
            onDismiss = { recipeToShare = null }
        )
    }

    // 🌟 Chef Application Status Dialogs
    chefStatusDialogInfo?.let { (title, message) ->
        AlertDialog(
            onDismissRequest = { chefStatusDialogInfo = null },
            title = { Text(title, fontWeight = FontWeight.Bold) },
            text = { Text(message) },
            confirmButton = {
                Button(onClick = { chefStatusDialogInfo = null }) {
                    Text("OK")
                }
            }
        )
    }

    if (showReapplyDialog) {
        AlertDialog(
            onDismissRequest = { showReapplyDialog = false },
            title = { Text("Re-apply as Chef", fontWeight = FontWeight.Bold) },
            text = { Text("Your previous application was rejected. Would you like to submit an updated application?") },
            confirmButton = {
                Button(onClick = {
                    showReapplyDialog = false
                    chefRegisterViewModel.initForUpgrade(user, user?.email.orEmpty(), user?.id.orEmpty())
                    navController.navigate(Screen.BasicInfo.route)
                }) {
                    Text("Re-apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReapplyDialog = false }) {
                    Text("Cancel")
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

    // 🌟 Chef Filter Bottom Sheet (matching hiring screen)
    if (showChefFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showChefFilterSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            ChefFilterBottomSheet(
                filterState = chefFilterState,
                availableStates = States,
                onApply = { updated ->
                    chefFilterState = updated
                    showChefFilterSheet = false
                },
                onDismiss = { showChefFilterSheet = false }
            )
        }
    }

    // 🌟 Recipe Filter Bottom Sheet (matching recipes screen)
    if (showRecipeFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showRecipeFilterSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            var ingredientSearchQuery by remember { mutableStateOf("") }
            val availableIngredientsList = remember(viewModel.availableIngredients) {
                viewModel.availableIngredients.mapNotNull { it.name }.distinct().sorted()
            }
            val filteredIngredientList = remember(ingredientSearchQuery, availableIngredientsList) {
                if (ingredientSearchQuery.isEmpty()) availableIngredientsList
                else availableIngredientsList.filter { it.contains(ingredientSearchQuery, ignoreCase = true) }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Filter Recipes",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = {
                        filterMaxTime = 240f
                        filterMaxCalories = 5000f
                        filterSkill = null
                        filterBudget = null
                        filterIngredients = emptySet()
                        ingredientSearchQuery = ""
                    }) {
                        Text("Reset All", color = MaterialTheme.colorScheme.error)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 1. Max Prep Time
                FilterSectionHeader(icon = R.drawable.ic_clock, title = "Max Prep Time")
                val timeDisplay = if (filterMaxTime >= 240f) "Any Time" else "${filterMaxTime.toInt()} mins"
                Text(timeDisplay, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Slider(
                    value = filterMaxTime,
                    onValueChange = { filterMaxTime = it },
                    valueRange = 10f..240f,
                    steps = 22,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 2. Max Calories
                FilterSectionHeader(icon = R.drawable.ic_fire, title = "Max Calories")
                val calDisplay = if (filterMaxCalories >= 5000f) "Any Calories" else "${filterMaxCalories.toInt()} kcal"
                Text(calDisplay, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Slider(
                    value = filterMaxCalories,
                    onValueChange = { filterMaxCalories = it },
                    valueRange = 100f..5000f,
                    steps = 48,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 3. Ingredients
                FilterSectionHeader(icon = R.drawable.ic_ingredient_list, title = "Ingredients")
                
                OutlinedTextField(
                    value = ingredientSearchQuery,
                    onValueChange = { ingredientSearchQuery = it },
                    placeholder = { Text("Search ingredients...", fontSize = 14.sp) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(24.dp)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        unfocusedBorderColor = Color.Transparent
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (availableIngredientsList.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        lazyItems(filteredIngredientList) { ingredient ->
                            FilterChip(
                                selected = ingredient in filterIngredients,
                                onClick = {
                                    filterIngredients = if (ingredient in filterIngredients) {
                                        filterIngredients - ingredient
                                    } else {
                                        filterIngredients + ingredient
                                    }
                                },
                                label = { Text(ingredient) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }

                    if (filterIngredients.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Selected (${filterIngredients.size}):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            filterIngredients.forEach { selected ->
                                InputChip(
                                    selected = true,
                                    onClick = { filterIngredients = filterIngredients - selected },
                                    label = { Text(selected, fontSize = 11.sp) },
                                    trailingIcon = {
                                        Icon(
                                            painter = painterResource(id = R.drawable.cancel),
                                            contentDescription = "Remove",
                                            modifier = Modifier.size(14.dp)
                                        )
                                    },
                                    colors = InputChipDefaults.inputChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }
                } else {
                    Text("Loading ingredients...", fontSize = 12.sp, color = Color.Gray)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 4. Cooking Skill
                FilterSectionHeader(icon = R.drawable.skill, title = "Cooking Skill")
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Beginner", "Intermediate", "Master/Expert").forEach { skill ->
                        FilterChip(
                            selected = filterSkill == skill,
                            onClick = { filterSkill = if (filterSkill == skill) null else skill },
                            label = { Text(skill) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 5. Budget
                FilterSectionHeader(icon = R.drawable.dollar_symbol, title = "Budget (RM)")
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("0 - 20", "20 - 40", "40 - 60", "60 - 80", "80 - 100").forEach { budget ->
                        FilterChip(
                            selected = filterBudget == budget,
                            onClick = { filterBudget = if (filterBudget == budget) null else budget },
                            label = { Text(budget) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Apply Button
                Button(
                    onClick = { showRecipeFilterSheet = false },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Apply Filters", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                
                Spacer(modifier = Modifier.height(32.dp))
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
fun EmptyState(
    @DrawableRes iconRes: Int,
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 32.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun ChefCardItem(chef: Chef, onClick: () -> Unit = {}) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), // 🌟 Themed Card
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier
            .fillMaxWidth() // 🌟 Changed from width(165.dp) to fillMaxWidth to fit grid
            .padding(vertical = 4.dp)
    ) {
        Column {
            // Profile Image Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(135.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant) // 🌟 Themed Background
            ) {
                if (!chef.profilePictureUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = chef.profilePictureUrl,
                        contentDescription = chef.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(id = R.drawable.foodieheallogo),
                            contentDescription = null,
                            modifier = Modifier.size(60.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                    }
                }

                // Price Tag Badge
                Surface(
                    shape = RoundedCornerShape(topStart = 0.dp, bottomStart = 12.dp, topEnd = 0.dp, bottomEnd = 0.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text(
                        text = "$${chef.Pricing?.toInt() ?: 0}/hr",
                        color = MaterialTheme.colorScheme.onPrimary, // 🌟 Themed Text
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
                    text = chef.name.ifEmpty { "Chef" },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface, // 🌟 Themed Text
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant // 🌟 Themed Text
                        )
                    }

                    // Experience Tag
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant, // 🌟 Themed Background
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${chef.experience} yrs exp",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, // 🌟 Themed Text
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
