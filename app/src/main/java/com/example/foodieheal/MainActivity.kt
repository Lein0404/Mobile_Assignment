package com.example.foodieheal

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.foodieheal.ingredients.notification.IngredientRequestNotificationHelper
import com.example.foodieheal.ingredients.notification.IngredientRequestStatusMonitor
import com.example.foodieheal.ingredients.notification.IngredientRequestSyncWorker
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.example.foodieheal.Chef.ViewModel.Register.ChefRegisterViewModel
import com.example.foodieheal.hiring.viewmodel.ChefListViewModel
import com.example.foodieheal.hiring.viewmodel.AppointmentBookingViewModel
import com.example.foodieheal.hiring.viewmodel.UserAppointmentViewModel
import com.example.foodieheal.hiring.viewmodel.BookmarkViewModel
import com.example.foodieheal.hiring.local.HiringDatabase
import com.example.foodieheal.hiring.data.HiringRepository
import com.example.foodieheal.hiring.data.BookmarkRepository
import com.example.foodieheal.meal_planner.viewModel.NetworkMonitor
import com.example.foodieheal.ingredients.local.IngredientsDatabase
import com.example.foodieheal.ingredients.repo.IngredientsRepository
import com.example.foodieheal.meal_planner.viewModel.MealPlannerViewModel
import com.example.foodieheal.meal_planner.viewModel.MealPlannerViewModelFactory
import com.example.foodieheal.navigation.Screen
import com.example.foodieheal.navigation.AppNavGraph
import com.example.foodieheal.Recipe.Repo.RecipeRepository
import com.example.foodieheal.Recipe.local.RecipeDatabase
import com.example.foodieheal.Recipe.viewModel.RecipeViewModel
import com.example.foodieheal.User.viewModel.AuthViewModel
import com.example.foodieheal.ui.theme.FoodieHealTheme
import es.dmoral.toasty.Toasty
import java.time.LocalDate

class MainActivity : FragmentActivity() {

    private val mealPlannerViewModel: MealPlannerViewModel by viewModels {
        MealPlannerViewModelFactory(application)
    }

    companion object {
        private const val TAG = "ColdStartDebug"
        var appContext: Context? = null
            private set
    }

    private var pendingDeepLinkRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appContext = applicationContext
        enableEdgeToEdge()

        Toasty.Config.getInstance().allowQueue(true).apply()

        handleDeepLink(intent)

        try {
            startService(Intent(applicationContext, com.example.foodieheal.hiring.local.HiringCacheCleanupService::class.java))
            startService(Intent(applicationContext, com.example.foodieheal.Chef.local.ChefCacheCleanupService::class.java))
            startService(Intent(applicationContext, com.example.foodieheal.ui.components.AppCacheCleanupService::class.java))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start cleanup services in MainActivity", e)
        }

        setContent {
            FoodieHealTheme(dynamicColor = false) {
                val navController = rememberNavController()
                val context = LocalContext.current
                val networkMonitor = remember { NetworkMonitor(context) }
                val sharedAuthViewModel: AuthViewModel = viewModel(
                    factory = AuthViewModel.Factory(networkMonitor)
                )

                // Initialize notification channel
                IngredientRequestNotificationHelper.createNotificationChannel(context)

                // Request POST_NOTIFICATIONS runtime permission on Android 13+
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val isGranted = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                        if (!isGranted) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                val isNetworkConnected by networkMonitor.isConnected.collectAsStateWithLifecycle(initialValue = true)

                // Global background observer for ingredient request status updates
                val currentUserId = sharedAuthViewModel.currentUser?.id
                LaunchedEffect(sharedAuthViewModel.loginSuccess, currentUserId, sharedAuthViewModel.isAdmin, sharedAuthViewModel.isChef) {
                    if (sharedAuthViewModel.loginSuccess && !currentUserId.isNullOrBlank() && !sharedAuthViewModel.isAdmin && !sharedAuthViewModel.isChef) {
                        IngredientRequestStatusMonitor.startPolling(currentUserId, context)
                        IngredientRequestSyncWorker.enqueuePeriodicSync(context, currentUserId)
                    } else if (!sharedAuthViewModel.loginSuccess && !sharedAuthViewModel.isInitializing) {
                        IngredientRequestStatusMonitor.stopPolling(context)
                        IngredientRequestSyncWorker.cancelPeriodicSync(context)
                    }
                }

                // 1. Unified Entry Navigation Logic (Cold & Warm Start)
                LaunchedEffect(sharedAuthViewModel.loginSuccess, sharedAuthViewModel.isInitializing, pendingDeepLinkRoute) {
                    if (sharedAuthViewModel.loginSuccess && !sharedAuthViewModel.isInitializing) {
                        val route = pendingDeepLinkRoute
                        if (route != null) {
                            Log.d(TAG, "Navigating to deep link route: $route")

                            val currentRoute = navController.currentDestination?.route
                            val isColdStart = currentRoute == null || currentRoute == Screen.Login.route

                            if (isColdStart) {
                                // Cold start: App was launched from killed state. Build default synthetic backstack:
                                // [Landing (e.g. Home) -> Ingredients (tab = 1) -> IngredientDetail]
                                val landingDest = when {
                                    sharedAuthViewModel.isAdmin -> Screen.AdminChefScreen.route
                                    sharedAuthViewModel.isChef -> Screen.ChefMain.route
                                    else -> Screen.Home.route
                                }
                                navController.navigate(landingDest) {
                                    popUpTo(0) { inclusive = true }
                                }
                                if (route.startsWith("ingredient_detail/") && route.contains("/true")) {
                                    navController.navigate(Screen.Ingredients.createRoute(tab = 1))
                                }
                                navController.navigate(route)
                            } else {
                                // Warm start: App is already active with user's existing backstack
                                // (e.g., Home -> Profile -> Ingredients). Preserve their entire backstack!
                                if (currentRoute != route) {
                                    navController.navigate(route)
                                }
                            }

                            pendingDeepLinkRoute = null
                        } else {
                            // Handle Initial Login Landing (Standard Redirect)
                            val currentRoute = navController.currentDestination?.route
                            if (currentRoute == null || currentRoute == Screen.Login.route) {
                                val dest = when {
                                    sharedAuthViewModel.isAdmin -> Screen.AdminChefScreen.route
                                    sharedAuthViewModel.isChef -> Screen.ChefMain.route
                                    else -> Screen.Home.route
                                }
                                Log.d(TAG, "Standard login landing. Navigating to: $dest")
                                navController.navigate(dest) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        }
                    }
                }


                val ingredientsDb = remember { IngredientsDatabase.getInstance(context) }
                val ingredientsRepo = remember { IngredientsRepository(ingredientsDb.ingredientsDao()) }
                val recipeDb = remember { RecipeDatabase.getDatabase(context) }
                val recipeRepo = remember { RecipeRepository(recipeDb.recipeDao(), ingredientsRepo) }
                val sharedRecipeViewModel: RecipeViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return RecipeViewModel(
                                application = application,
                                repository = recipeRepo,
                                networkMonitor = networkMonitor
                            ) as T
                        }
                    }
                )

                LaunchedEffect(sharedAuthViewModel.currentUser?.name, sharedAuthViewModel.currentUser?.profilePicUrl) {
                    sharedAuthViewModel.currentUser?.let { user ->
                        sharedRecipeViewModel.syncRecipeAuthorInfo(user)
                    }
                }
                val hiringDb = remember { HiringDatabase.getInstance(context) }
                val hiringRepo = remember {
                    HiringRepository(
                        chefDao = hiringDb.chefDao(),
                        appointmentDao = hiringDb.appointmentDao(),
                        reviewDao = hiringDb.chefReviewDao()
                    )
                }
                val bookmarkRepo = remember {
                    BookmarkRepository(
                        bookmarkDao = hiringDb.chefBookmarkDao(),
                        chefDao = hiringDb.chefDao()
                    )
                }

                val chefListViewModel: ChefListViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T =
                            ChefListViewModel(hiringRepo, networkMonitor) as T
                    }
                )
                val bookingViewModel: AppointmentBookingViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T =
                            AppointmentBookingViewModel(hiringRepo, networkMonitor) as T
                    }
                )
                val userAppointmentViewModel: UserAppointmentViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T =
                            UserAppointmentViewModel(hiringRepo, networkMonitor) as T
                    }
                )
                val bookmarkViewModel: BookmarkViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T =
                            BookmarkViewModel(bookmarkRepo, networkMonitor) as T
                    }
                )
                val chefViewModel: ChefRegisterViewModel = viewModel()

                //Content loads first, Splash sits on top.
                Box(modifier = Modifier.fillMaxSize()) {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination

                    // Check if we are on one of the main tabs
                    val tabRoutes = listOf(Screen.Home.route, Screen.Recipes.route, Screen.Planner.route, Screen.Hiring.route, Screen.Profile.route)
                    val isTabScreen = currentDestination?.route in tabRoutes
                    val shouldShowBottomBar = isTabScreen && sharedAuthViewModel.loginSuccess

                    Scaffold(
                        // contentWindowInsets=0 ensures the orange header reaches the top without gaps
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                        containerColor = MaterialTheme.colorScheme.background, // Stop the "black gap" during transitions
                        bottomBar = {
                            //Animate the bar visibility to prevent sudden layout jumps/flickering
                            AnimatedVisibility(
                                visible = shouldShowBottomBar,
                                enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(300)),
                                exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300))
                            ) {
                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.tertiary,
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                    windowInsets = WindowInsets.navigationBars
                                ) {
                                    val items = listOf(
                                        NavigationItem(Screen.Home.route, R.string.nav_home, R.drawable.ic_home),
                                        NavigationItem(Screen.Recipes.route, R.string.nav_recipes, R.drawable.ic_recipe),
                                        NavigationItem(Screen.Planner.route, R.string.meal_planner, R.drawable.ic_planner),
                                        NavigationItem(Screen.Hiring.route, R.string.nav_hiring, R.drawable.ic_hiring),
                                        NavigationItem(Screen.Profile.route, R.string.nav_profile, R.drawable.ic_outline_account_circle)
                                    )
                                    items.forEach { item ->
                                        NavigationBarItem(
                                            icon = { Icon(painterResource(id = item.icon), contentDescription = stringResource(item.label)) },
                                            label = { Text(stringResource(item.label), fontSize = 10.sp) },
                                            selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                                unselectedIconColor = Color.Gray,
                                                unselectedTextColor = Color.Gray,
                                                indicatorColor = Color.Transparent
                                            ),
                                            onClick = {
                                                // Only navigate if the clicked tab is NOT already selected
                                                // This prevents the screen from "refreshing/flickering" when re-clicking the same tab.
                                                val isAlreadySelected = currentDestination?.hierarchy?.any { it.route == item.route } == true

                                                if (!isAlreadySelected) {
                                                    navController.navigate(item.route) {
                                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        AppNavGraph(
                            navController = navController,
                            innerPadding = innerPadding,
                            sharedAuthViewModel = sharedAuthViewModel,
                            sharedRecipeViewModel = sharedRecipeViewModel,
                            mealPlannerViewModel = mealPlannerViewModel,
                            chefListViewModel = chefListViewModel,
                            bookingViewModel = bookingViewModel,
                            userAppointmentViewModel = userAppointmentViewModel,
                            bookmarkViewModel = bookmarkViewModel,
                            chefViewModel = chefViewModel,
                            networkMonitor = networkMonitor,
                            startDestination = Screen.Login.route
                        )
                    }

                    // Fades away to reveal the Login screen already sitting underneath
                    AnimatedVisibility(
                        visible = sharedAuthViewModel.isInitializing,
                        enter = fadeIn(),
                        exit = fadeOut(animationSpec = tween(600))
                    ) {
                        SplashLogoOverlay()
                    }

                    // Global Logout Logic
                    LaunchedEffect(sharedAuthViewModel.loginSuccess) {
                        if (!sharedAuthViewModel.loginSuccess && !sharedAuthViewModel.isInitializing) {
                            val currentRoute = navController.currentDestination?.route

                            // Screens that are allowed WITHOUT login (Auth flow)
                            val authRoutes = listOf(
                                Screen.Login.route,
                                Screen.ChefLogin.route,
                                Screen.Register.route,
                                Screen.Welcome.route,
                                Screen.BasicInfo.route,
                                Screen.Contact.route,
                                Screen.Address.route,
                                Screen.Description.route,
                                Screen.ChefPicture.route,
                                Screen.Review.route,
                                Screen.EditBodyStatus.route // "editBodyStatus"
                            )

                            // Only kick to log in if we are NOT on an auth screen
                            // Use startsWith to handle routes with query parameters like ?fromRegister=true
                            val isAuthRoute = authRoutes.any { currentRoute?.startsWith(it) == true }

                            if (!isAuthRoute && currentRoute != null) {
                                navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val userId = SupabaseClient.client.auth.currentUserOrNull()?.id
            ?: IngredientRequestStatusMonitor.getActiveUserId(applicationContext)
        if (!userId.isNullOrBlank()) {
            lifecycleScope.launch {
                IngredientRequestStatusMonitor.checkStatusUpdates(userId, applicationContext)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // When app is minimized or backgrounded, schedule background sync
        val userId = SupabaseClient.client.auth.currentUserOrNull()?.id
            ?: IngredientRequestStatusMonitor.getActiveUserId(applicationContext)
        if (!userId.isNullOrBlank()) {
            IngredientRequestSyncWorker.enqueueImmediateSync(applicationContext, userId)
            IngredientRequestSyncWorker.enqueuePeriodicSync(applicationContext, userId)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        // 1. Direct route string passed in extras (e.g., from notifications)
        intent?.getStringExtra("route")?.let { route ->
            Log.d("DeepLink", "Processing route extra: $route")
            pendingDeepLinkRoute = route
            intent.removeExtra("route")
            return
        }

        // 2. URI-based deep links
        intent?.data?.let { uri ->
            processDeepLink(uri)
            intent.data = null
        }
    }

    private fun processDeepLink(uri: Uri) {
        Log.d("DeepLink", "Processing URI: $uri")
        val isHttpsLink = uri.scheme == "https" && uri.host == "tzh652.github.io"
        val isCustomScheme = uri.scheme == "foodieheal"

        if (isHttpsLink || isCustomScheme) {
            when {
                // 1. Weekly Plan Share (Original)
                uri.path?.startsWith("/share") == true || uri.host == "share" -> {
                    val dateStr = uri.getQueryParameter("sourceStart")
                    val sharerId = uri.getQueryParameter("sharerId")

                    dateStr?.let {
                        runCatching { LocalDate.parse(it) }.onSuccess { startDate ->
                            Log.d("DeepLink", "Successfully parsed start date: $startDate | Sharer: $sharerId")
                            pendingDeepLinkRoute = Screen.Planner.route
                            mealPlannerViewModel.prepareSharedWeeklyPlan(startDate, sharerId)
                        }
                    }
                }
                // 2. Template Share (New)
                uri.path?.startsWith("/template") == true || uri.host == "template" -> {
                    val planId = uri.getQueryParameter("id") ?: uri.getQueryParameter("templateId")

                    planId?.let { id ->
                        Log.d("DeepLink", "Successfully parsed template ID: $id")
                        pendingDeepLinkRoute = Screen.TemplateDetails.createRoute(id, false)
                    }
                }
            }
        }
    }
}

data class NavigationItem(val route: String, val label: Int, val icon: Int)


// PLEASE DON'T DELETE IT, IT CAN PREVENT THE SCREEN FLASH YOUR EYE
@Composable
fun SplashLogoOverlay() {
    val primaryColor = MaterialTheme.colorScheme.primary
    val backgroundColor = MaterialTheme.colorScheme.background
    val darkTheme = androidx.compose.foundation.isSystemInDarkTheme()
    val view = LocalView.current

    // Sync Status Bar to background color
    SideEffect {
        val window = (view.context as Activity).window
        window.statusBarColor = backgroundColor.toArgb()
        androidx.core.view.WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor) //Sync Background
    ) {
        // Seamless Background Spacer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .statusBarsPadding()
        )

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.app_name),
                    color = primaryColor,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.slogan_one),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}