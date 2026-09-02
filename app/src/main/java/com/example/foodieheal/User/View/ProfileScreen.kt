package com.example.foodieheal.User.View

import android.app.Activity
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.annotation.DrawableRes
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
import com.example.foodieheal.hiring.viewmodel.AppointmentBookingViewModel
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
    bookingViewModel: AppointmentBookingViewModel = viewModel(),
    bookmarkViewModel: BookmarkViewModel = viewModel(),
    followViewModel: FollowViewModel = viewModel(),
    targetCustomId: String? = null
) {
    val user = authViewModel.currentUser
    
    // isVisitor Check: True only if we have a valid target ID that isn't ours or the nav placeholder
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
    var showLogoutConfirmation by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Show success messages for password, profile, and body status updates
    LaunchedEffect(Unit) {
        authViewModel.profileEvents.collect { event ->
            val message = when(event) {
                is AuthViewModel.ProfileEvent.PasswordSuccess -> view.context.getString(R.string.profile_password_success)
                is AuthViewModel.ProfileEvent.ProfileSuccess -> view.context.getString(R.string.profile_updated_success)
                is AuthViewModel.ProfileEvent.BodyStatusSuccess -> view.context.getString(R.string.profile_body_status_success)
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
                FollowViewModel.FollowEvent.Error -> view.context.getString(R.string.error_network_try_again)
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    var showBigImage by remember { mutableStateOf(false) }
    
    // State for the Delete Confirmation Dialog
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
    
    // Chef Filter State (matching hiring screen)
    var chefFilterState by remember { mutableStateOf(ChefFilterState()) }
    var showChefFilterSheet by remember { mutableStateOf(false) }
    
    // Recipe Filter State (matching recipes screen)
    var filterMaxTime by remember { mutableFloatStateOf(240f) }
    var filterMaxCalories by remember { mutableFloatStateOf(5000f) }
    var filterSkill by remember { mutableStateOf<String?>(null) }
    var filterBudget by remember { mutableStateOf<String?>(null) }
    var filterIngredients by remember { mutableStateOf(setOf<String>()) }
    var showRecipeFilterSheet by remember { mutableStateOf(false) }
    
    var selectedCourse by remember { mutableStateOf("All") }
    val courses = listOf("All", "Breakfast", "Lunch", "Dinner", "Snack")

    val myRecipes = remember(viewModel.myRecipes, followViewModel.followStatus, isMyProfile) {
        if (isMyProfile) viewModel.myRecipes 
        else viewModel.myRecipes.filter { 
            val visibility = it.visibility.lowercase()
            visibility == "public" || (visibility == "followers" && followViewModel.followStatus == "ACCEPTED")
        }
    }
    val bookmarkedRecipes = viewModel.bookmarkedRecipes
    val bookmarkedChefs = bookmarkViewModel.bookmarkedChefsList

    // Search & Filter Chefs Logic (Hiring Screen Style)
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
            // Always fetch following list to keep 'followedUserIds' updated for privacy checks
            viewModel.fetchFollowingRecipes(cid)

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
                modifier = Modifier.width(300.dp).fillMaxHeight(),
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 12.dp)
                        .navigationBarsPadding()
                ) {
                    Text(text = stringResource(R.string.profile_menu), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(24.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        DrawerItem(stringResource(R.string.edit_profile), R.drawable.ic_edit) {
                            scope.launch {
                                drawerState.close()
                                navController.navigate(Screen.EditProfile.route)
                            }
                        }
                        DrawerItem(stringResource(R.string.profile_edit_body_status), R.drawable.ic_outline_account_circle) {
                            scope.launch {
                                drawerState.close()
                                navController.navigate(Screen.EditBodyStatus.route)
                            }
                        }
                        DrawerItem(stringResource(R.string.profile_view_ingredients), R.drawable.ic_ingredient_list) {
                            scope.launch {
                                drawerState.close()
                                navController.navigate(Screen.Ingredients.route)
                            }
                        }
                        DrawerItem(stringResource(R.string.profile_shopping_lists), R.drawable.ic_shopping_cart) {
                            scope.launch {
                                drawerState.close()
                                navController.navigate(Screen.ShoppingListHome.route)
                            }
                        }
                        DrawerItem(stringResource(R.string.profile_become_chef), R.drawable.ic_hiring) {
                            scope.launch {
                                drawerState.close()
                                authViewModel.checkChefApplicationStatus { existingChef ->
                                    val effectiveUserId = authViewModel.getEffectiveUserId()
                                    val effectiveEmail = authViewModel.getEffectiveUserEmail()

                                    if (existingChef != null) {
                                        when (existingChef.status.lowercase()) {
                                            "pending" -> {
                                                chefStatusDialogInfo = view.context.getString(R.string.profile_app_review_title) to view.context.getString(R.string.profile_app_review_msg)
                                            }
                                            "approved" -> {
                                                chefStatusDialogInfo = view.context.getString(R.string.profile_chef_active_title) to view.context.getString(R.string.profile_chef_active_msg)
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
                        DrawerItem(stringResource(R.string.profile_appt_history), R.drawable.ic_calendar) {
                            navController.navigate(Screen.AppoinmtmentHistory.route)
                        }
                        DrawerItem(stringResource(R.string.profile_follow_requests), R.drawable.follower) {
                            scope.launch {
                                drawerState.close()
                                navController.navigate(Screen.FollowRequests.route)
                            }
                        }
                        DrawerItem(stringResource(R.string.profile_my_wallet), R.drawable.wallet) {
                            scope.launch {
                                drawerState.close()
                                navController.navigate(Screen.Wallet.route)
                            }
                        }
                        DrawerItem(stringResource(R.string.profile_payment_methods), R.drawable.dollar_symbol) {
                            scope.launch {
                                drawerState.close()
                                navController.navigate(Screen.PaymentMethod.route)
                            }
                        }
                        DrawerItem(stringResource(R.string.profile_change_password), R.drawable.changepassword4) {
                            scope.launch {
                                drawerState.close()
                                navController.navigate(Screen.ChangePassword.route)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    TextButton(
                        onClick = { showLogoutConfirmation = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.nav_logout), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) {
        if (showLogoutConfirmation) {
            AlertDialog(
                onDismissRequest = { showLogoutConfirmation = false },
                title = {
                    Text(
                        text = stringResource(R.string.logout_confirm_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                text = { Text(stringResource(R.string.logout_confirm_message)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showLogoutConfirmation = false
                            // Clear user-specific memory data on logout to prevent data pollution
                            authViewModel.logout {
                                viewModel.clearUserData()
                            }
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.nav_logout),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutConfirmation = false }) {
                        Text(stringResource(R.string.dialog_cancel))
                    }
                }
            )
        }
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { 
                SnackbarHost(snackbarHostState) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            topBar = {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .height(64.dp)
                            .padding(horizontal = 20.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = if (isMyProfile) R.drawable.ic_hamburger_menu else R.drawable.ic_arrowback),
                                contentDescription = if (isMyProfile) "Menu" else "Back",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp).clickable {
                                    if (isMyProfile) {
                                        scope.launch { drawerState.open() }
                                    } else {
                                        // Safety check to prevent spam-clicks from causing navigation crashes
                                        val currentRoute = navController.currentDestination?.route
                                        if (currentRoute?.contains("profile") == true) {
                                            navController.popBackStack()
                                        }
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = stringResource(R.string.profile_title),
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = paddingValues.calculateBottomPadding())
                    .background(MaterialTheme.colorScheme.background),
            ) {
                // 🌟 Profile Header Section
                item(span = { GridItemSpan(2) }) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primary)
                            .statusBarsPadding()
                            .padding(top = 64.dp, bottom = 24.dp)
                    ) {
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
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
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
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontSize = 32.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(20.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                val rawName = if (displayUser == null && isVisitor) stringResource(R.string.profile_offline_user) else (displayUser?.name ?: "")
                                val displayName = remember(rawName) {
                                    if (rawName.startsWith("User (U") && rawName.endsWith(")")) "User" else rawName
                                }
                                Text(
                                    text = displayName,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Row(modifier = Modifier.padding(top = 12.dp)) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.clickable(enabled = isMyProfile) { 
                                            effectiveCustomId?.let { cid ->
                                                navController.navigate(Screen.FollowList.createRoute(cid, "followers"))
                                            }
                                        }
                                    ) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.height(24.dp)) {
                                            if (followViewModel.isLoadingFollowCounts) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(14.dp),
                                                    strokeWidth = 2.dp,
                                                    color = MaterialTheme.colorScheme.onPrimary
                                                )
                                            } else {
                                                Text(
                                                    text = "${followViewModel.followerCount}",
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Text(
                                            text = stringResource(R.string.profile_followers),
                                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                                            fontSize = 11.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(24.dp))
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.clickable(enabled = isMyProfile) { 
                                            effectiveCustomId?.let { cid ->
                                                navController.navigate(Screen.FollowList.createRoute(cid, "following"))
                                            }
                                        }
                                    ) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.height(24.dp)) {
                                            if (followViewModel.isLoadingFollowCounts) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(14.dp),
                                                    strokeWidth = 2.dp,
                                                    color = MaterialTheme.colorScheme.onPrimary
                                                )
                                            } else {
                                                Text(
                                                    text = "${followViewModel.followingCount}",
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Text(
                                            text = stringResource(R.string.profile_following),
                                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                if (isVisitor && user != null && targetCustomId != null) {
                                    val status = followViewModel.followStatus
                                    val buttonText = when (status) {
                                        null -> stringResource(R.string.btn_follow)
                                        "PENDING" -> stringResource(R.string.btn_request_sent)
                                        "ACCEPTED" -> stringResource(R.string.btn_unfollow)
                                        else -> stringResource(R.string.btn_follow)
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

                        //Description Block
                        if (displayUser != null && !displayUser.description.isNullOrEmpty()) {
                            Text(
                                text = displayUser.description!!,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                maxLines = 5,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)
                            )
                        } else if (displayUser == null && isVisitor) {
                            Text(
                                text = stringResource(R.string.profile_offline_desc),
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                // Tabs Section
                item(span = { GridItemSpan(2) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            TabRow(
                                selectedTabIndex = selectedMainTab,
                                containerColor = Color.Transparent,
                                contentColor = MaterialTheme.colorScheme.primary,
                                indicator = { tabPositions ->
                                    if (selectedMainTab < tabPositions.size) {
                                        TabRowDefaults.SecondaryIndicator(
                                            Modifier.tabIndicatorOffset(tabPositions[selectedMainTab]),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                },
                                divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) }
                            ) {
                                Tab(
                                    selected = selectedMainTab == 0,
                                    onClick = { selectedMainTab = 0 },
                                    text = { Text(if (isMyProfile) stringResource(R.string.profile_user_recipes) else stringResource(R.string.profile_recipes), fontWeight = FontWeight.Bold) }
                                )
                                if (isMyProfile) {
                                    Tab(
                                        selected = selectedMainTab == 1,
                                        onClick = { selectedMainTab = 1 },
                                        text = { Text(stringResource(R.string.profile_bookmarks), fontWeight = FontWeight.Bold) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Search & Course Section
                item(span = { GridItemSpan(2) }) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))
                        if (selectedMainTab == 1 && showChefBookmarks) {
                            // Compact Search Bar (matching RecipesScreen style)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = chefFilterState.searchQuery,
                                    onValueChange = { chefFilterState = chefFilterState.copy(searchQuery = it) },
                                    placeholder = { Text(stringResource(R.string.profile_search_chefs), fontSize = 14.sp) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(id = R.drawable.search),
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                                    )
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Surface(
                                    onClick = { showChefFilterSheet = true },
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                    modifier = Modifier.size(52.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
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
                                                modifier = Modifier.size(20.dp),
                                                tint = if (chefFilterState.activeFilterCount > 0) primaryColor else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))

                            // Compact Recipes/Chefs Toggle (Now under search bar)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)), RoundedCornerShape(24.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val subTabModifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(20.dp))

                                Box(
                                    modifier = subTabModifier
                                        .background(if (!showChefBookmarks) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .clickable { showChefBookmarks = false },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_recipe),
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = if (!showChefBookmarks) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            stringResource(R.string.profile_recipes),
                                            color = if (!showChefBookmarks) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Box(
                                    modifier = subTabModifier
                                        .background(if (showChefBookmarks) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .clickable { showChefBookmarks = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_hiring),
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = if (showChefBookmarks) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            stringResource(R.string.profile_chefs),
                                            color = if (showChefBookmarks) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Active filter chips row
                            ActiveFiltersRow(
                                filterState = chefFilterState,
                                onFilterChange = { chefFilterState = it },
                                onResetAll = { chefFilterState = ChefFilterState(searchQuery = chefFilterState.searchQuery) }
                            )
                        } else {
                            // Compact Recipe Search Bar (matching RecipesScreen style)
                            val currentQuery = if (selectedMainTab == 0) userRecipesSearchQuery else bookmarksSearchQuery
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = currentQuery,
                                    onValueChange = { 
                                        if (selectedMainTab == 0) userRecipesSearchQuery = it else bookmarksSearchQuery = it
                                    },
                                    placeholder = { 
                                        Text(
                                            if (selectedMainTab == 0) stringResource(R.string.profile_search_recipes) else stringResource(R.string.profile_search_recipes_authors), 
                                            fontSize = 14.sp
                                        ) 
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(id = R.drawable.search),
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                                    )
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Surface(
                                    onClick = { showRecipeFilterSheet = true },
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                    modifier = Modifier.size(52.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        val activeFilterCount = (if (filterMaxTime < 240f) 1 else 0) +
                                                (if (filterMaxCalories < 5000f) 1 else 0) +
                                                (if (filterSkill != null) 1 else 0) +
                                                (if (filterBudget != null) 1 else 0) +
                                                (if (filterIngredients.isNotEmpty()) 1 else 0)

                                        BadgedBox(
                                            badge = {
                                                if (activeFilterCount > 0) {
                                                    Badge(containerColor = primaryColor, contentColor = Color.White) {
                                                        Text(activeFilterCount.toString())
                                                    }
                                                }
                                            }
                                        ) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.filter),
                                                contentDescription = "Filter",
                                                modifier = Modifier.size(20.dp),
                                                tint = if (activeFilterCount > 0) primaryColor else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))

                            // Compact Recipes/Chefs Toggle
                            if (selectedMainTab == 1) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(24.dp))
                                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)), RoundedCornerShape(24.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .padding(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val subTabModifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(20.dp))

                                    Box(
                                        modifier = subTabModifier
                                            .background(if (!showChefBookmarks) MaterialTheme.colorScheme.primary else Color.Transparent)
                                            .clickable { showChefBookmarks = false },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_recipe),
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = if (!showChefBookmarks) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                stringResource(R.string.profile_recipes),
                                                color = if (!showChefBookmarks) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Box(
                                        modifier = subTabModifier
                                            .background(if (showChefBookmarks) MaterialTheme.colorScheme.primary else Color.Transparent)
                                            .clickable { showChefBookmarks = true },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_hiring),
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = if (showChefBookmarks) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                stringResource(R.string.profile_chefs),
                                                color = if (showChefBookmarks) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                            
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                lazyItems(courses) { course ->
                                    val isSelected = selectedCourse == course
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedCourse = course },
                                        label = { 
                                            Text(
                                                text = when(course) {
                                                    "All" -> stringResource(R.string.recipe_course_all)
                                                    "Breakfast" -> stringResource(R.string.recipe_course_breakfast)
                                                    "Lunch" -> stringResource(R.string.recipe_course_lunch)
                                                    "Dinner" -> stringResource(R.string.recipe_course_dinner)
                                                    "Snack" -> stringResource(R.string.recipe_course_snack)
                                                    else -> course
                                                }, 
                                                fontSize = 15.sp, 
                                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp)
                                            ) 
                                        },
                                        shape = RoundedCornerShape(20.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = primaryColor,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = isSelected,
                                            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                            selectedBorderColor = Color.Transparent,
                                            borderWidth = 1.dp,
                                            selectedBorderWidth = 0.dp
                                        )
                                    )
                                }
                            }
                            
                            // Removed selectedCourse header text
                        }
                    }
                }

                // Content Items (Recipes/Chefs)
                if (selectedMainTab == 0) {
                    if (viewModel.isLoading) {
                        item(span = { GridItemSpan(2) }) {
                            Box(modifier = Modifier.fillMaxWidth().padding(top = 80.dp), contentAlignment = Alignment.Center) {
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
                                Box(modifier = Modifier.padding(8.dp)) {
                                    RecipeCardItem(
                                        recipe = recipe,
                                        currentUser = user,
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
                                        onAddClick = {
                                            if (viewModel.isNetworkAvailable) {
                                                recipe.recipe_id?.let { rid ->
                                                    navController.navigate(Screen.AddRecipeToPlanner.createRoute(rid))
                                                }
                                            } else {
                                                viewModel.showOfflinePlannerMessage()
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
                    }
                } else if (selectedMainTab == 1) {
                    if (!showChefBookmarks) {
                        if (viewModel.isLoading) {
                            item(span = { GridItemSpan(2) }) {
                                Box(modifier = Modifier.fillMaxWidth().padding(top = 80.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
                                }
                            }
                        } else {
                            val filtered = bookmarkedRecipes.filter { recipe ->
                                // Privacy & Visibility Logic
                                val isVisible = when {
                                    recipe.author_id == user?.customId -> true // My recipes are always visible to me
                                    recipe.visibility == "public" -> true // Public recipes are visible to everyone
                                    recipe.visibility == "followers" -> viewModel.followedUserIds.contains(recipe.author_id) // Visible only if following
                                    else -> false // Private recipes (or any other status) are hidden from others
                                }
                                if (!isVisible) return@filter false

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
                                    Box(modifier = Modifier.padding(8.dp)) {
                                        RecipeCardItem(
                                            recipe = recipe,
                                            currentUser = user,
                                            isBookmarked = true,
                                            onBookmarkClick = {
                                                user?.customId?.let { cid ->
                                                    recipe.recipe_id?.let { rid ->
                                                        viewModel.toggleBookmark(cid, rid, recipe.recipeName)
                                                    }
                                                }
                                            },
                                            onAddClick = {
                                                if (viewModel.isNetworkAvailable) {
                                                    recipe.recipe_id?.let { rid ->
                                                        navController.navigate(Screen.AddRecipeToPlanner.createRoute(rid))
                                                    }
                                                } else {
                                                    viewModel.showOfflinePlannerMessage()
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
                        }
                    } else {
                        if (bookmarkViewModel.isLoadingBookmarks) {
                            item(span = { GridItemSpan(2) }) {
                                Box(modifier = Modifier.fillMaxWidth().padding(top = 80.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
                                }
                            }
                        } else {
                            if (filteredChefs.isEmpty()) {
                                item(span = { GridItemSpan(2) }) {
                                    EmptyState(
                                        iconRes = R.drawable.ic_hiring,
                                        title = if (chefFilterState.isFilterActive) stringResource(R.string.no_chefs_match_filters) else stringResource(R.string.empty_no_bookmarked_chefs),
                                        subtitle = if (chefFilterState.isFilterActive) stringResource(R.string.adjust_search_criteria_or_reset) else stringResource(R.string.empty_bookmarked_chefs_sub)
                                    )
                                }
                            } else {
                                gridItems(filteredChefs) { chef ->
                                    Box(modifier = Modifier.padding(8.dp)) {
                                        ChefCardItem(
                                            chef = chef,
                                            onClick = {
                                                bookingViewModel.selectChef(chef)
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
                
                // Extra bottom padding
                item(span = { GridItemSpan(2) }) {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }

    // Delete Confirmation Dialog for Profile Screen
    if (recipeToDelete != null) {
        AlertDialog(
            onDismissRequest = { recipeToDelete = null },
            title = { Text(stringResource(R.string.profile_delete_recipe)) },
            text = { Text(stringResource(R.string.profile_delete_confirm_msg, recipeToDelete?.recipeName ?: "")) },
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
                    Text(stringResource(R.string.delete_action), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { recipeToDelete = null }) {
                    Text(stringResource(R.string.dialog_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    // Share Dialog
    recipeToShare?.let { recipe ->
        ShareRecipeDialog(
            recipe = recipe,
            authorName = if (recipe.author_id == user?.customId) user?.name else recipe.authorName,
            onDismiss = { recipeToShare = null }
        )
    }

    // Chef Application Status Dialogs
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
            title = { Text(stringResource(R.string.profile_reapply_chef_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.profile_reapply_chef_msg)) },
            confirmButton = {
                Button(onClick = {
                    showReapplyDialog = false
                    chefRegisterViewModel.initForUpgrade(user, user?.email.orEmpty(), user?.id.orEmpty())
                    navController.navigate(Screen.BasicInfo.route)
                }) {
                    Text(stringResource(R.string.profile_reapply_btn))
                }
            },
            dismissButton = {
                TextButton(onClick = { showReapplyDialog = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }

    // Big Image View
    if (showBigImage) {
        Dialog(onDismissRequest = { showBigImage = false }) {
            Box(
                modifier = Modifier.fillMaxWidth().aspectRatio(1f).background(Color.Transparent),
                contentAlignment = Alignment.Center
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
                            text = displayUser?.name?.take(1)?.uppercase() ?: "?",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 120.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // Chef Filter Bottom Sheet (matching hiring screen)
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

    // Recipe Filter Bottom Sheet
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
                        text = stringResource(R.string.profile_filter_recipes),
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
                        Text(stringResource(R.string.profile_reset_all), color = MaterialTheme.colorScheme.error)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 1. Max Prep Time
                FilterSectionHeader(icon = R.drawable.ic_clock, title = stringResource(R.string.profile_max_prep_time))
                val timeDisplay = if (filterMaxTime >= 240f) stringResource(R.string.profile_any_time) else "${filterMaxTime.toInt()} ${stringResource(R.string.profile_mins_suffix)}"
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
                FilterSectionHeader(icon = R.drawable.ic_fire, title = stringResource(R.string.profile_max_calories))
                val calDisplay = if (filterMaxCalories >= 5000f) stringResource(R.string.profile_any_calories) else "${filterMaxCalories.toInt()} ${stringResource(R.string.profile_kcal_suffix)}"
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
                FilterSectionHeader(icon = R.drawable.ic_ingredient_list, title = stringResource(R.string.profile_ingredients))
                
                OutlinedTextField(
                    value = ingredientSearchQuery,
                    onValueChange = { ingredientSearchQuery = it },
                    placeholder = { Text(stringResource(R.string.profile_search_ingredients), fontSize = 14.sp) },
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
                            text = stringResource(R.string.profile_selected_count, filterIngredients.size),
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
                    Text(stringResource(R.string.msg_loading_ingredients), fontSize = 12.sp, color = Color.Gray)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 4. Cooking Skill
                FilterSectionHeader(icon = R.drawable.skill, title = stringResource(R.string.profile_cooking_skill))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Beginner", "Intermediate", "Master/Expert").forEach { skill ->
                        FilterChip(
                            selected = filterSkill == skill,
                            onClick = { filterSkill = if (filterSkill == skill) null else skill },
                            label = { 
                                Text(
                                    when(skill) {
                                        "Beginner" -> stringResource(R.string.recipe_skill_beginner)
                                        "Intermediate" -> stringResource(R.string.recipe_skill_intermediate)
                                        "Master/Expert" -> stringResource(R.string.recipe_skill_master_expert)
                                        else -> skill
                                    }
                                ) 
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 5. Budget
                FilterSectionHeader(icon = R.drawable.dollar_symbol, title = stringResource(R.string.profile_budget))
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
                    Text(stringResource(R.string.profile_apply_filters), fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.width(20.dp))
        Text(text = label, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column {
            // Profile Image Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(135.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
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

                Surface(
                    shape = RoundedCornerShape(topStart = 0.dp, bottomStart = 12.dp, topEnd = 0.dp, bottomEnd = 0.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text(
                        text = stringResource(R.string.rate_per_hour, chef.Pricing?.toInt() ?: 0),
                        color = MaterialTheme.colorScheme.onPrimary,
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
                    color = MaterialTheme.colorScheme.onSurface,
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
                            text = chef.averagerating?.toString() ?: stringResource(R.string.not_available),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Experience Tag
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.experience_years_short, chef.experience),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
