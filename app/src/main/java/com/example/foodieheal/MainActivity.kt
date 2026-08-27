package com.example.foodieheal

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.foodieheal.Admin.AdminAddIngredientScreen
import com.example.foodieheal.Admin.AdminApprovalScreen
import com.example.foodieheal.Admin.ChefDetailScreen
import com.example.foodieheal.Chef.ChefMainScreen
import com.example.foodieheal.Chef.Register.*
import com.example.foodieheal.Chef.ViewModel.Register.ChefRegisterViewModel
import com.example.foodieheal.hiring.screen.HiringAppointment
import com.example.foodieheal.hiring.screen.HiringChefDetails
import com.example.foodieheal.hiring.screen.HiringScreen
import com.example.foodieheal.hiring.screen.AddAppointmentFormScreen
import com.example.foodieheal.hiring.screen.AppointmentReviewScreen
import com.example.foodieheal.hiring.screen.RateChefScreen
import com.example.foodieheal.hiring.screen.RescheduleAppointmentScreen
import com.example.foodieheal.hiring.screen.UserAppointmentDetailScreen
import com.example.foodieheal.hiring.viewmodel.ChefListViewModel
import com.example.foodieheal.hiring.viewmodel.AppointmentBookingViewModel
import com.example.foodieheal.hiring.viewmodel.UserAppointmentViewModel
import com.example.foodieheal.hiring.viewmodel.BookmarkViewModel
import com.example.foodieheal.hiring.local.HiringDatabase
import com.example.foodieheal.hiring.data.HiringRepository
import com.example.foodieheal.hiring.data.BookmarkRepository
import com.example.foodieheal.meal_planner.viewModel.NetworkMonitor
import com.example.foodieheal.Admin.AdminIngredientDetailScreen
import com.example.foodieheal.Admin.AdminIngredientRequestFormScreen
import com.example.foodieheal.Admin.AdminIngredientsScreen
import com.example.foodieheal.Payment.Screen.PaymentMethodScreen
import com.example.foodieheal.Payment.Screen.PaymentScreen
import com.example.foodieheal.Payment.ViewModel.PaymentMethodViewModel
import com.example.foodieheal.Payment.ViewModel.PaymentViewModel
import com.example.foodieheal.Payment.local.PayMethodDatabase
import com.example.foodieheal.Payment.repo.PaymentRepository
import com.example.foodieheal.ingredients.view.ShoppingListAddFromScreen
import com.example.foodieheal.ingredients.view.ShoppingListAddItemScreen
import com.example.foodieheal.ingredients.view.ShoppingListHomeScreen
import com.example.foodieheal.ingredients.view.IngredientDetailScreen
import com.example.foodieheal.ingredients.view.IngredientRequestFormScreen
import com.example.foodieheal.ingredients.view.IngredientsMainScreen
import com.example.foodieheal.ingredients.view.ShoppingListScreen
import com.example.foodieheal.meal_planner.data.PlanRepository
import com.example.foodieheal.meal_planner.model.MealType
import com.example.foodieheal.meal_planner.screen.AddRecipeToPlanScreen
import com.example.foodieheal.meal_planner.screen.AddEditTemplateRoute
import com.example.foodieheal.meal_planner.screen.MealPlannerScreen
import com.example.foodieheal.meal_planner.screen.RecipesSelectingScreen
import com.example.foodieheal.meal_planner.screen.TemplateDetailsScreen
import com.example.foodieheal.meal_planner.screen.calculateSuggestedDailyCalories
import com.example.foodieheal.meal_planner.viewModel.AddEditTemplateViewModel
import com.example.foodieheal.meal_planner.viewModel.MealPlannerViewModel
import com.example.foodieheal.meal_planner.viewModel.MealPlannerViewModelFactory
import com.example.foodieheal.Recipe.View.AddRecipeScreen
import com.example.foodieheal.meal_planner.viewModel.TemplateViewModel
import com.example.foodieheal.navigation.Screen
import com.example.foodieheal.Recipe.Repo.RecipeRepository
import com.example.foodieheal.Recipe.View.EditRecipeScreen
import com.example.foodieheal.Recipe.View.RecipeDetailsScreen
import com.example.foodieheal.Recipe.View.RecipesScreen
import com.example.foodieheal.Recipe.local.RecipeDao
import com.example.foodieheal.Recipe.local.RecipeDatabase
import com.example.foodieheal.Recipe.viewModel.RecipeViewModel
import com.example.foodieheal.User.View.ChangePasswordScreen
import com.example.foodieheal.User.View.EditBodyStatusScreen
import com.example.foodieheal.User.View.EditProfileScreen
import com.example.foodieheal.User.View.HomeScreen
import com.example.foodieheal.User.View.LoginScreen
import com.example.foodieheal.User.View.ProfileScreen
import com.example.foodieheal.User.View.FollowRequestsScreen
import com.example.foodieheal.User.View.FollowListScreen
import com.example.foodieheal.User.View.RegisterScreen
import com.example.foodieheal.User.viewModel.AuthViewModel
import com.example.foodieheal.hiring.screen.AppointmentHistoryScreen
import com.example.foodieheal.ui.theme.FoodieHealTheme
import com.example.foodieheal.wallet.screen.WalletScreen
import com.example.foodieheal.wallet.screen.WalletTransactionDetailScreen
import kotlinx.coroutines.delay
import kotlinx.datetime.DayOfWeek
import java.time.LocalDate
import kotlin.time.Duration.Companion.milliseconds

class MainActivity : ComponentActivity() {

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

        // 1. Process deep link on initial cold start
        handleDeepLink(intent)

        // 2. Start cache cleanup services to listen for app removal on App Switcher
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

                // 1. Unified Entry Navigation Logic (Cold & Warm Start)
                LaunchedEffect(sharedAuthViewModel.loginSuccess, sharedAuthViewModel.isInitializing, pendingDeepLinkRoute) {
                    if (sharedAuthViewModel.loginSuccess && !sharedAuthViewModel.isInitializing) {
                        val route = pendingDeepLinkRoute
                        if (route != null) {
                            Log.d(TAG, "Navigating to deep link route: $route")

                            // 🌟 FIX: Construct a Synthetic Backstack (Landing -> DeepLink)
                            // This ensures that when the user presses 'Back', they go to Home instead of exiting.
                            val landingDest = when {
                                sharedAuthViewModel.isAdmin -> Screen.AdminChefScreen.route
                                sharedAuthViewModel.isChef -> Screen.ChefMain.route
                                else -> Screen.Home.route
                            }

                            // 1. Reset to the appropriate root screen first
                            navController.navigate(landingDest) {
                                popUpTo(0) { inclusive = true }
                            }

                            // 2. Push the deep link target on top of the root
                            navController.navigate(route)

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


                val recipeDb = remember { com.example.foodieheal.Recipe.local.RecipeDatabase.getDatabase(context) }
                val recipeRepo = remember { RecipeRepository(recipeDb.recipeDao()) }
                val sharedRecipeViewModel: RecipeViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return RecipeViewModel(
                                repository = recipeRepo,
                                networkMonitor = networkMonitor
                            ) as T
                        }
                    }
                )

                // 🌟 FIX: Observe the name and pic specifically to trigger instant card sync
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

                // 🌟 FIX: The curtain strategy. Content loads first, Splash sits on top.
                Box(modifier = Modifier.fillMaxSize()) {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination

                    // Check if we are on one of the main tabs
                    val tabRoutes = listOf(Screen.Home.route, Screen.Recipes.route, Screen.Planner.route, Screen.Hiring.route, Screen.Profile.route)
                    val isTabScreen = currentDestination?.route in tabRoutes
                    val shouldShowBottomBar = isTabScreen && sharedAuthViewModel.loginSuccess

                    Scaffold(
                        // 🌟 contentWindowInsets=0 ensures the orange header reaches the top without gaps
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                        containerColor = MaterialTheme.colorScheme.background, // 🌟 FIX: Stop the "black gap" during transitions
                        bottomBar = {
                            // 🌟 FIX: Animate the bar visibility to prevent sudden layout jumps/flickering
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
                                        NavigationItem(Screen.Home.route, "Home", R.drawable.ic_home),
                                        NavigationItem(Screen.Recipes.route, "Recipes", R.drawable.ic_recipe),
                                        NavigationItem(Screen.Planner.route, "Planner", R.drawable.ic_planner),
                                        NavigationItem(Screen.Hiring.route, "Hiring", R.drawable.ic_hiring),
                                        NavigationItem(Screen.Profile.route, "Profile", R.drawable.ic_outline_account_circle)
                                    )
                                    items.forEach { item ->
                                        NavigationBarItem(
                                            icon = { Icon(painterResource(id = item.icon), contentDescription = item.label) },
                                            label = { Text(item.label, fontSize = 10.sp) },
                                            selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                                unselectedIconColor = Color.Gray,
                                                unselectedTextColor = Color.Gray,
                                                indicatorColor = Color.Transparent
                                            ),
                                            onClick = {
                                                // 🌟 FIX: Only navigate if the clicked tab is NOT already selected
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
                        // 🌟 Stable Start Route: The NavHost always starts at Login,
                        // and LaunchedEffects handle the redirection once Auth is ready.
                        val startRoute = Screen.Login.route

                        // Use a Box to keep the NavHost full screen and avoid resizing lag
                        Box(modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)) {
                            NavHost(
                                navController = navController,
                                startDestination = startRoute,
                                modifier = Modifier.fillMaxSize(),
                                enterTransition = { fadeIn(animationSpec = tween(400)) },
                                exitTransition = { fadeOut(animationSpec = tween(400)) }
                            ) {
                                // --- AUTH ---
                                composable(Screen.Login.route) {
                                    LoginScreen(
                                        navController,
                                        sharedAuthViewModel
                                    )
                                }
                                composable(Screen.ChefLogin.route) {
                                    ChefLoginScreen(
                                        navController,
                                        sharedAuthViewModel
                                    )
                                }
                                composable(Screen.Register.route) {
                                    RegisterScreen(
                                        navController,
                                        sharedAuthViewModel
                                    )
                                }

                                // --- TABS ---
                                composable(Screen.Home.route) {
                                    Box(modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())) { HomeScreen(
                                        navController = navController,
                                        viewModel = sharedAuthViewModel,
                                        recipeViewModel = sharedRecipeViewModel,
                                        chefViewModel = chefListViewModel,
                                        onChefClick = { chef ->
                                            chefListViewModel.selectChef(chef)
                                            bookingViewModel.selectChef(chef)
                                            val chefId = chef.chefId.ifEmpty { chef.id }
                                            navController.navigate("${Screen.HiringChefDetails.route}/$chefId") {
                                                launchSingleTop = true
                                            }
                                        }
                                    )
                                    }
                                }

                                composable(Screen.Recipes.route) {
                                    Box(modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())) {
                                        RecipesScreen(
                                            navController,
                                            sharedRecipeViewModel,
                                            sharedAuthViewModel
                                        )
                                    }
                                }

                                composable(Screen.Planner.route) {
                                    MealPlannerScreen(
                                        mealPlannerViewModel = mealPlannerViewModel,
                                        userAppointmentViewModel = userAppointmentViewModel,
                                        authViewModel = sharedAuthViewModel,
                                        onNavigateToProfile = { navController.navigate(Screen.EditBodyStatus.route) },
                                        onRecipeDetails = { recipeId ->
                                            navController.navigate(
                                                Screen.RecipeDetails.createRoute(recipeId)
                                            )
                                        },
                                        onAddMeal = { date, type ->
                                            navController.navigate(
                                                Screen.RecipeSelection.createRoute(
                                                    date = date,
                                                    type = type
                                                )
                                            )
                                        },
                                        onAddTemplateClick = { navController.navigate(Screen.AddEditTemplate.createRoute()) },
                                        onPlanDetails = { planId, isMyTemplate ->
                                            navController.navigate(
                                                Screen.TemplateDetails.createRoute(
                                                    planId,
                                                    isMyTemplate
                                                )
                                            )
                                        },
                                        onEdit = { id -> navController.navigate(Screen.AddEditTemplate.createRoute(id)) }
                                    )
                                }

                                composable(route = Screen.AddRecipeToPlanner.route,
                                    arguments = listOf(
                                        navArgument("recipeId") {
                                            type = NavType.StringType
                                            nullable = false
                                        }
                                    )
                                ) { backStackEntry ->
                                    val recipeId = backStackEntry.arguments?.getString("recipeId")

                                    // Trigger fetch only if the ID is valid
                                    LaunchedEffect(recipeId) {
                                        if (!recipeId.isNullOrEmpty()) {
                                            sharedRecipeViewModel.fetchRecipeById(recipeId)
                                        }
                                    }

                                    val recipe = sharedRecipeViewModel.selectedRecipe

                                    if (recipeId.isNullOrEmpty()) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("Invalid Recipe ID provided.")
                                        }
                                    } else {
                                        AddRecipeToPlanScreen(
                                            mealPlannerViewModel = mealPlannerViewModel,
                                            authViewModel = sharedAuthViewModel,
                                            onExecutionComplete = { navController.popBackStack() },
                                            recipe = recipe,
                                            onNavigateToProfile = { navController.navigate(Screen.EditProfile.route) }
                                        )
                                    }
                                }

                                composable(
                                    route = Screen.RecipeSelection.route,
                                    arguments = listOf(
                                        navArgument("date") { type = NavType.StringType },
                                        navArgument("type") { type = NavType.StringType }
                                    )
                                ) { backStackEntry ->
                                    val dateString = backStackEntry.arguments?.getString("date") ?: ""
                                    val typeString = backStackEntry.arguments?.getString("type") ?: ""

                                    // 1. Safely parse MealType enum
                                    val type: MealType = runCatching {
                                        MealType.valueOf(typeString.uppercase())
                                    }.getOrDefault(MealType.BREAKFAST)

                                    // 2. Parse DayOfWeek first ("MONDAY", "TUESDAY", etc.)
                                    val dayOfWeek: DayOfWeek? = runCatching {
                                        DayOfWeek.valueOf(dateString.uppercase())
                                    }.getOrNull()

                                    // 3. Only parse LocalDate if dayOfWeek is null ("2026-08-18")
                                    val localDate: LocalDate? = if (dayOfWeek == null && dateString.isNotEmpty()) {
                                        runCatching { LocalDate.parse(dateString) }.getOrNull()
                                    } else null

                                    // 4. Retrieve AddEditTemplateViewModel safely if backstack entry exists
                                    val addEditTemplateViewModel: AddEditTemplateViewModel? = if (dayOfWeek != null) {
                                        val parentEntry = remember(backStackEntry) {
                                            runCatching {
                                                navController.getBackStackEntry(Screen.AddEditTemplate.route)
                                            }.getOrNull()
                                        }
                                        parentEntry?.let { viewModel(it) }
                                    } else null

                                    RecipesSelectingScreen(
                                        recipeViewModel = sharedRecipeViewModel,
                                        authViewModel = sharedAuthViewModel,
                                        onSave = { selectedIds ->
                                            val selectedRecipes = selectedIds.mapNotNull { recipeId ->
                                                sharedRecipeViewModel.recipeList.find { it.recipe_id == recipeId }
                                                    ?: sharedRecipeViewModel.myRecipes.find { it.recipe_id == recipeId }
                                                    ?: sharedRecipeViewModel.bookmarkedRecipes.find { it.recipe_id == recipeId }
                                            }

                                            if (dayOfWeek != null && addEditTemplateViewModel != null) {
                                                selectedRecipes.forEach { recipe ->
                                                    addEditTemplateViewModel.addRecipeToSlot(
                                                        dayOfWeek,
                                                        type,
                                                        recipe
                                                    )
                                                }
                                            } else if (localDate != null) {
                                                selectedRecipes.forEach { recipe ->
                                                    mealPlannerViewModel.addRecipeToMeal(
                                                        date = localDate,
                                                        mealType = type,
                                                        recipe = recipe
                                                    )
                                                }
                                            }
                                            navController.popBackStack()
                                        },
                                        onBackClick = { navController.popBackStack() }
                                    )
                                }

                                composable(
                                    route = Screen.AddEditTemplate.route,
                                    arguments = listOf(
                                        navArgument("planId") {
                                            type = NavType.StringType
                                            nullable = true
                                            defaultValue = null
                                        }
                                    )
                                ) {
                                    val recipeRepository = remember { RecipeRepository(RecipeDatabase.getDatabase(context).recipeDao()) }
                                    val addEditTemplateViewModel: AddEditTemplateViewModel = viewModel(
                                        factory = object : ViewModelProvider.Factory {
                                            @Suppress("UNCHECKED_CAST")
                                            override fun <T : ViewModel> create(
                                                modelClass: Class<T>,
                                                extras: CreationExtras
                                            ): T {
                                                val savedStateHandle = extras.createSavedStateHandle()
                                                return AddEditTemplateViewModel(
                                                    planRepository = PlanRepository(),
                                                    authViewModel = sharedAuthViewModel,
                                                    recipeRepository = recipeRepository,
                                                    savedStateHandle = savedStateHandle
                                                ) as T
                                            }
                                        }
                                    )

                                    AddEditTemplateRoute(
                                        modifier = Modifier.padding(innerPadding),
                                        viewModel = addEditTemplateViewModel,
                                        authViewModel = sharedAuthViewModel,
                                        onBackClick = { navController.popBackStack() },
                                        onNavigateToAddRecipe = { day, mealType ->
                                            navController.navigate(
                                                Screen.RecipeSelection.createRoute(
                                                    date = day,
                                                    type = mealType
                                                )
                                            )
                                        },
                                        onRecipeClick = { recipeId ->
                                            if (recipeId.isNotEmpty()) {
                                                navController.navigate(Screen.RecipeDetails.createRoute(recipeId))
                                            }
                                        },
                                        onNavigateToProfile = { navController.navigate(Screen.EditBodyStatus.route) },
                                    )
                                }


                                composable(
                                    route = Screen.TemplateDetails.route,
                                    arguments = listOf(
                                        navArgument("planId") {
                                            type = NavType.StringType
                                        },
                                        navArgument("isMyTemplate") {
                                            type = NavType.BoolType
                                        }
                                    )
                                ) { backStackEntry ->
                                    val isMyTemplate = backStackEntry.arguments?.getBoolean("isMyTemplate") ?: false

                                    val planRepository = remember { PlanRepository() }
                                    val recipeRepository = remember { RecipeRepository(com.example.foodieheal.Recipe.local.RecipeDatabase.getDatabase(context).recipeDao()) }

                                    val currentUserIdFlow = remember(sharedAuthViewModel) {
                                        snapshotFlow { sharedAuthViewModel.currentUser?.id }
                                    }

                                    val templateViewModel: TemplateViewModel = viewModel(
                                        factory = remember(currentUserIdFlow) {
                                            object : ViewModelProvider.Factory {
                                                @Suppress("UNCHECKED_CAST")
                                                override fun <T : ViewModel> create(
                                                    modelClass: Class<T>,
                                                    extras: CreationExtras
                                                ): T {
                                                    val savedStateHandle = extras.createSavedStateHandle()
                                                    return TemplateViewModel(
                                                        savedStateHandle = savedStateHandle,
                                                        planRepository = planRepository,
                                                        recipeRepository = recipeRepository,
                                                        currentUserIdFlow = currentUserIdFlow
                                                    ) as T
                                                }
                                            }
                                        }
                                    )

                                    val selectedPlan by templateViewModel.selectedPlan.collectAsStateWithLifecycle()

                                    selectedPlan?.let { plan ->
                                        TemplateDetailsScreen(
                                            plan = plan,
                                            isMyTemplate = isMyTemplate,
                                            onApply = { startDate ->
                                                mealPlannerViewModel.applyTemplateToDate(
                                                    template = plan,
                                                    startDate = startDate
                                                )
                                                Toast.makeText(appContext, "Template applied to meal planner!", Toast.LENGTH_SHORT).show()
                                                navController.popBackStack()
                                            },
                                            onBack = { navController.popBackStack() },
                                            mealPlannerViewModel = mealPlannerViewModel,
                                            maxCalories = calculateSuggestedDailyCalories(sharedAuthViewModel.currentUser),
                                            onRecipeDetails = { id ->
                                                navController.navigate(Screen.RecipeDetails.createRoute(id))
                                            },
                                            onNavigateToProfile = { navController.navigate(Screen.EditBodyStatus.route) },
                                            onRecipeAdd = { date, mealType ->
                                                navController.navigate(Screen.RecipeSelection.createRoute(date = date, type = mealType))
                                            },
                                            onRecipeDelete = { recipeId ->
                                                templateViewModel.deleteRecipeFromTemplate(recipeId)
                                                Toast.makeText(appContext, "Recipe removed from template", Toast.LENGTH_SHORT).show()
                                            },
                                            onEdit = { navController.navigate(Screen.AddEditTemplate.createRoute(plan.planId)) },
                                            onDelete = {
                                                templateViewModel.deleteWeeklyPlan(
                                                    planId = plan.planId,
                                                    onSuccess = {
                                                        Toast.makeText(appContext, "Template deleted successfully!", Toast.LENGTH_SHORT).show()
                                                        navController.popBackStack()
                                                    }
                                                )
                                            },
                                            onAdd = {
                                                templateViewModel.duplicateTemplate(
                                                    sourcePlanId = plan.planId,
                                                    currentUserId = sharedAuthViewModel.currentUser?.id ?: "",
                                                    onSuccess = {
                                                        Toast.makeText(appContext, "Template saved to your collection!", Toast.LENGTH_SHORT).show()
                                                    },
                                                    onError = { error ->
                                                        Toast.makeText(appContext, error, Toast.LENGTH_SHORT).show()
                                                    }
                                                )
                                            }
                                        )
                                    } ?: Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }

                                composable(
                                    route = Screen.Profile.route,
                                    arguments = listOf(navArgument("customId") { 
                                        type = NavType.StringType
                                        nullable = true
                                        defaultValue = null
                                    })
                                ) { backStackEntry ->
                                    val customId = backStackEntry.arguments?.getString("customId")
                                    Box(modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())) {
                                        ProfileScreen(
                                            navController,
                                            sharedRecipeViewModel,
                                            sharedAuthViewModel,
                                            chefViewModel,
                                            targetCustomId = customId
                                        )
                                    }
                                }

                                composable(Screen.FollowRequests.route) {
                                    FollowRequestsScreen(
                                        navController,
                                        sharedAuthViewModel
                                    )
                                }

                                composable(
                                    route = Screen.FollowList.route,
                                    arguments = listOf(
                                        navArgument("userId") { type = NavType.StringType },
                                        navArgument("type") { type = NavType.StringType }
                                    )
                                ) { backStackEntry ->
                                    val userId = backStackEntry.arguments?.getString("userId") ?: ""
                                    val type = backStackEntry.arguments?.getString("type") ?: ""
                                    FollowListScreen(
                                        navController = navController,
                                        userId = userId,
                                        type = type,
                                        recipeViewModel = sharedRecipeViewModel
                                    )
                                }

                                // --- FEATURES (Full screen, instant swap) ---
                                composable(Screen.Hiring.route) {
                                    Box(modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())) {
                                        HiringScreen(
                                            chefListViewModel = chefListViewModel,
                                            userAppointmentViewModel = userAppointmentViewModel,
                                            authViewModel = sharedAuthViewModel,
                                            bookmarkViewModel = bookmarkViewModel,
                                            onChefClick = { chef ->
                                                chefListViewModel.selectChef(chef)
                                                bookingViewModel.selectChef(chef)
                                                val chefId = chef.chefId.ifEmpty { chef.id }
                                                navController.navigate("${Screen.HiringChefDetails.route}/$chefId")
                                            },
                                            onAppointmentClick = { appointment ->
                                                val id = appointment.AppointmentID.orEmpty()
                                               Log.d("HiringNav", "Click detected! ID: $id")
                                                navController.navigate(Screen.UserAppointmentDetail.createRoute(id))
                                            }
                                        )
                                    }
                                }

                                composable(
                                    route = "${Screen.HiringChefDetails.route}/{chefId}"
                                ) { backStackEntry ->
                                    val chefId = backStackEntry.arguments?.getString("chefId").orEmpty()
                                    val userId = sharedAuthViewModel.currentUser?.id.orEmpty()

                                    // Collect the StateFlow properly as Compose state
                                    val selectedChefState by bookingViewModel.selectedChef.collectAsStateWithLifecycle()

                                    // Match selected chef against the route argument
                                    val chef = selectedChefState?.takeIf { (it.chefId.ifEmpty { it.id }) == chefId }

                                    if (chef != null) {
                                        HiringChefDetails(
                                            chef = chef,
                                            userId = userId,
                                            viewModel = bookmarkViewModel,
                                            bookingViewModel = bookingViewModel,
                                            onBackClick = { navController.popBackStack() },
                                            onHireClick = {
                                                navController.navigate("${Screen.HiringAppointment.route}/$chefId")
                                            }
                                        )
                                    } else {
                                        LaunchedEffect(Unit) {
                                            navController.popBackStack()
                                        }
                                    }
                                }

                                composable(
                                    route = "${Screen.HiringAppointment.route}/{chefId}"
                                ) { backStackEntry ->
                                    val chefId = backStackEntry.arguments?.getString("chefId").orEmpty()

                                    // Collect the StateFlow properly as Compose state
                                    val selectedChefState by bookingViewModel.selectedChef.collectAsStateWithLifecycle()
                                    val chef = selectedChefState?.takeIf { (it.chefId.ifEmpty { it.id }) == chefId }

                                    if (chef != null) {
                                        HiringAppointment(
                                            chef = chef,
                                            bookingViewModel = bookingViewModel,
                                            onBackClick = { navController.popBackStack() },
                                            onAddAppointmentClick = { chosenDate ->
                                                bookingViewModel.updateSelectedDate(chosenDate)
                                                navController.navigate(Screen.AddHiringAppointment.route)
                                            }
                                        )
                                    } else {
                                        LaunchedEffect(Unit) {
                                            navController.popBackStack()
                                        }
                                    }
                                }

                                composable(Screen.AddHiringAppointment.route) {
                                    AddAppointmentFormScreen(
                                        viewModel = bookingViewModel,
                                        authViewModel = sharedAuthViewModel,
                                        recipeViewModel = sharedRecipeViewModel,
                                        onBackClick = { navController.popBackStack() },
                                        onSuccessConfirm = {
                                            navController.navigate(Screen.AppointmentReview.route)
                                        }
                                    )
                                }

                                composable(Screen.AppointmentReview.route) {
                                    AppointmentReviewScreen(
                                        viewModel = bookingViewModel,
                                        authViewModel = sharedAuthViewModel,
                                        onBackClick = { navController.popBackStack() },
                                        onFinalConfirm = {
                                            navController.popBackStack(Screen.Home.route, inclusive = false)
                                        }
                                    )
                                }

                                composable(
                                    route = "appointmentDetail/{appointmentId}",
                                    arguments = listOf(navArgument("appointmentId") { type = NavType.StringType })
                                ) { backStackEntry ->
                                    val appointmentId = backStackEntry.arguments?.getString("appointmentId").orEmpty()

                                    UserAppointmentDetailScreen(
                                        appointmentId = appointmentId,
                                        viewModel = userAppointmentViewModel,
                                        onBackClick = { navController.popBackStack() },
                                        onRescheduleClick = { appointment ->
                                            navController.navigate(Screen.RescheduleAppointment.createRoute(appointment.AppointmentID.orEmpty()))
                                        },
                                        onRatingClick = { targetAppointmentId ->
                                            navController.navigate(Screen.RateChef.createRoute(targetAppointmentId))
                                        },
                                        onPayClick = { appointment ->
                                            navController.navigate("payment_screen/${appointment.AppointmentID.orEmpty()}")
                                        }
                                    )
                                }

                                composable(
                                    route = "payment_screen/{appointmentId}",
                                    arguments = listOf(navArgument("appointmentId") { type = NavType.StringType })
                                ) { backStackEntry ->
                                    val context = LocalContext.current
                                    val appointmentId = backStackEntry.arguments?.getString("appointmentId").orEmpty()

                                    val paymentViewModel: PaymentViewModel = viewModel(
                                        factory = PaymentViewModel.Factory(
                                            client = SupabaseClient.client,
                                            networkMonitor = networkMonitor
                                        )
                                    )

                                    val database = remember { PayMethodDatabase.getDatabase(context) }
                                    val repository = remember {
                                        PaymentRepository(
                                            dao = database.paymentMethodDao(),
                                            supabaseClient = SupabaseClient.client
                                        )
                                    }

                                    val paymentMethodViewModel: PaymentMethodViewModel = viewModel(
                                        factory = PaymentMethodViewModel.Factory(
                                            repository = repository,
                                            networkMonitor = networkMonitor
                                        )
                                    )

                                    PaymentScreen(
                                        appointmentId = appointmentId,
                                        paymentViewModel = paymentViewModel,
                                        paymentMethodViewModel = paymentMethodViewModel,
                                        onBackClick = { navController.popBackStack() },
                                        onPaymentSuccess = { transactionId ->
                                            // Refresh main appointment list when payment completes
                                            userAppointmentViewModel.fetchAppointmentsForCurrentUser()
                                            navController.popBackStack()
                                        },
                                        onPaymentError = { error ->
                                            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                        }
                                    )
                                }

                                composable(
                                    route = Screen.RateChef.route,
                                    arguments = listOf(
                                        navArgument("appointmentId") { type = NavType.StringType }
                                    )
                                ) { backStackEntry ->
                                    val appointmentId = backStackEntry.arguments?.getString("appointmentId").orEmpty()

                                    RateChefScreen(
                                        appointmentId = appointmentId,
                                        userViewModel = userAppointmentViewModel,
                                        onSubmitSuccess = {
                                            navController.popBackStack()
                                        }
                                    )
                                }

                                composable(
                                    route = Screen.RescheduleAppointment.route,
                                    arguments = listOf(navArgument("appointmentId") { type = NavType.StringType })
                                ) { backStackEntry ->
                                    val appointmentId = backStackEntry.arguments?.getString("appointmentId").orEmpty()

                                    RescheduleAppointmentScreen(
                                        appointmentId = appointmentId,
                                        userViewModel = userAppointmentViewModel,
                                        bookingViewModel = bookingViewModel,
                                        onBackClick = { navController.popBackStack() },
                                        onRescheduleSuccess = {
                                            navController.popBackStack()
                                        }
                                    )
                                }


                                composable(
                                    route = Screen.AddRecipe.route,
                                    enterTransition = { slideInVertically(initialOffsetY = { it }, animationSpec = tween(400)) + fadeIn() },
                                    exitTransition = { slideOutVertically(targetOffsetY = { it }, animationSpec = tween(400)) + fadeOut() }
                                ) { 
                                    AddRecipeScreen(navController, sharedRecipeViewModel, sharedAuthViewModel) 
                                }
                                
                                composable(
                                    route = Screen.EditRecipe.route,
                                    enterTransition = { slideInVertically(initialOffsetY = { it }, animationSpec = tween(400)) + fadeIn() },
                                    exitTransition = { slideOutVertically(targetOffsetY = { it }, animationSpec = tween(400)) + fadeOut() }
                                ) { backStackEntry ->
                                    val recipeId = backStackEntry.arguments?.getString("recipeId") ?: ""
                                    EditRecipeScreen(navController, recipeId, sharedRecipeViewModel, sharedAuthViewModel)
                                }
                                composable(Screen.RecipeDetails.route) { backStackEntry ->
                                    val recipeId = backStackEntry.arguments?.getString("recipeId") ?: ""
                                    RecipeDetailsScreen(navController, recipeId, sharedRecipeViewModel, sharedAuthViewModel)
                                }
                                composable(Screen.EditProfile.route) { EditProfileScreen(navController) }

                                composable(Screen.AppoinmtmentHistory.route) {
                                    AppointmentHistoryScreen(
                                        viewModel = userAppointmentViewModel,
                                        onBackClick = { navController.popBackStack() },
                                        onAppointmentClick = { appointmentId ->
                                            navController.navigate(Screen.UserAppointmentDetail.createRoute(appointmentId))
                                        }
                                    )
                                }

                                composable(route = Screen.PaymentMethod.route) {
                                    val context = LocalContext.current
                                    val database = remember { PayMethodDatabase.getDatabase(context) }
                                    val repository = remember {
                                        PaymentRepository(
                                            dao = database.paymentMethodDao(),
                                            supabaseClient = SupabaseClient.client
                                        )
                                    }

                                    val paymentMethodViewModel: PaymentMethodViewModel = viewModel(
                                        factory = PaymentMethodViewModel.Factory(
                                            repository = repository,
                                            networkMonitor = networkMonitor
                                        )
                                    )

                                    // Retrieve current logged-in user ID from your Auth State / Session
                                    val currentUserId = sharedAuthViewModel.currentUser?.id.orEmpty()

                                    PaymentMethodScreen(
                                        userId = currentUserId,
                                        viewModel = paymentMethodViewModel,
                                        onBackClick = { navController.popBackStack() }
                                    )
                                }

                                composable(route = Screen.Wallet.route) {
                                    val context = LocalContext.current
                                    val database = remember { PayMethodDatabase.getDatabase(context) }
                                    val paymentRepo = remember {
                                        PaymentRepository(
                                            dao = database.paymentMethodDao(),
                                            supabaseClient = SupabaseClient.client
                                        )
                                    }
                                    val paymentMethodViewModel: PaymentMethodViewModel = viewModel(
                                        factory = PaymentMethodViewModel.Factory(
                                            repository = paymentRepo,
                                            networkMonitor = networkMonitor
                                        )
                                    )

                                    val walletDatabase = remember { com.example.foodieheal.wallet.local.WalletDatabase.getDatabase(context) }
                                    val walletRepo = remember {
                                        com.example.foodieheal.wallet.data.WalletRepository(
                                            dao = walletDatabase.walletDao(),
                                            supabaseClient = SupabaseClient.client
                                        )
                                    }
                                    val walletViewModel: com.example.foodieheal.wallet.viewmodel.WalletViewModel = viewModel(
                                        factory = com.example.foodieheal.wallet.viewmodel.WalletViewModel.Factory(
                                            repository = walletRepo,
                                            networkMonitor = networkMonitor
                                        )
                                    )

                                    val currentUserId = sharedAuthViewModel.currentUser?.id.orEmpty()

                                    WalletScreen(
                                        userId = currentUserId,
                                        viewModel = walletViewModel,
                                        paymentMethodViewModel = paymentMethodViewModel,
                                        onBackClick = { navController.popBackStack() },
                                        onTransactionClick = { transactionId ->
                                            navController.navigate(Screen.WalletTransactionDetail.createRoute(transactionId))
                                        }
                                    )
                                }

                                composable(
                                    route = Screen.WalletTransactionDetail.route,
                                    arguments = listOf(navArgument("transactionId") { type = NavType.StringType })
                                ) { backStackEntry ->
                                    val context = LocalContext.current
                                    val transactionId = backStackEntry.arguments?.getString("transactionId").orEmpty()
                                    val walletDatabase = remember { com.example.foodieheal.wallet.local.WalletDatabase.getDatabase(context) }
                                    val walletRepo = remember {
                                        com.example.foodieheal.wallet.data.WalletRepository(
                                            dao = walletDatabase.walletDao(),
                                            supabaseClient = SupabaseClient.client
                                        )
                                    }

                                    WalletTransactionDetailScreen(
                                        transactionId = transactionId,
                                        walletRepository = walletRepo,
                                        onBackClick = { navController.popBackStack() }
                                    )
                                }

                                composable(Screen.ChangePassword.route) { ChangePasswordScreen(navController) }
                                composable(
                                    route = Screen.EditBodyStatus.route + "?fromRegister={fromRegister}",
                                    arguments = listOf(navArgument("fromRegister") { defaultValue = false; type = NavType.BoolType })
                                ) { backStackEntry ->
                                    val fromRegister = backStackEntry.arguments?.getBoolean("fromRegister") ?: false
                                    EditBodyStatusScreen(navController, fromRegister = fromRegister)
                                }

                                // --- ADMIN & CHEF ---
                                composable(
                                    route = Screen.AdminChefScreen.route,
                                    arguments = listOf(navArgument("tab") { defaultValue = 0; type = NavType.IntType })
                                ) { backStackEntry ->
                                    val tab = backStackEntry.arguments?.getInt("tab") ?: 0
                                    AdminApprovalScreen(navController, authViewModel = sharedAuthViewModel, initialTab = tab)
                                }
                                composable(
                                    route = Screen.AdminIngredient.route,
                                    arguments = listOf(navArgument("tab") { defaultValue = -1; type = NavType.IntType })
                                ) { backStackEntry ->
                                    val tab = backStackEntry.arguments?.getInt("tab") ?: -1
                                    AdminIngredientsScreen(navController, initialTab = tab)
                                }
                                composable(
                                    route = Screen.AdminIngredientDetail.route,
                                    arguments = listOf(navArgument("id") { type = NavType.StringType })
                                ) { backStackEntry ->
                                    val id = backStackEntry.arguments?.getString("id") ?: ""
                                    AdminIngredientDetailScreen(navController, id)
                                }
                                composable(
                                    route = Screen.AdminIngredientReview.route,
                                    arguments = listOf(navArgument("id") { type = NavType.StringType })
                                ) { backStackEntry ->
                                    val id = backStackEntry.arguments?.getString("id") ?: ""
                                    AdminIngredientRequestFormScreen(navController, id)
                                }
                                composable(Screen.AdminAddIngredient.route) {
                                    AdminAddIngredientScreen(navController)
                                }
                                composable(Screen.ChefMain.route) { ChefMainScreen(navController, sharedAuthViewModel) }
                                composable(
                                    route = "chefDetail/{chefId}",
                                    arguments = listOf(navArgument("chefId") { type = NavType.StringType })
                                ) { backStackEntry ->
                                    val chefId = backStackEntry.arguments?.getString("chefId") ?: ""
                                    ChefDetailScreen(chefId, navController)
                                }
                                navigation(startDestination = Screen.Welcome.route, route = "chefRegisterRoute") {
                                    composable(Screen.Welcome.route) { ChefWelcomeScreen(navController, chefViewModel) }
                                    composable(Screen.BasicInfo.route) { basicInfo(navController, chefViewModel) }
                                    composable(Screen.Contact.route) { contactInfo(navController, chefViewModel) }
                                    composable(Screen.Address.route) { addressInfo(navController, chefViewModel) }
                                    composable(Screen.Description.route) { descriptionInfo(navController, chefViewModel) }
                                    composable(Screen.ChefPicture.route) { ChefPictureScreen(navController, chefViewModel) }
                                    composable(Screen.Review.route) { reviewInfo(navController, chefViewModel) }
                                }

                                // Ingredients module
                                composable(
                                    route = Screen.Ingredients.route,
                                    arguments = listOf(navArgument("tab") { defaultValue = -1; type = NavType.IntType })
                                ) { backStackEntry ->
                                    val tab = backStackEntry.arguments?.getInt("tab") ?: -1
                                    IngredientsMainScreen(navController, initialTab = tab)
                                }
                                composable(
                                    route = Screen.IngredientDetail.route,
                                    arguments = listOf(
                                        navArgument("id") { type = NavType.StringType },
                                        navArgument("isRequest") { type = NavType.BoolType },
                                        navArgument("showAddToCart") { type = NavType.BoolType; defaultValue = true }
                                    )
                                ) { backStackEntry ->
                                    val id = backStackEntry.arguments?.getString("id") ?: ""
                                    val isRequest = backStackEntry.arguments?.getBoolean("isRequest") ?: false
                                    val showAddToCart = backStackEntry.arguments?.getBoolean("showAddToCart") ?: true
                                    IngredientDetailScreen(navController, id, isRequest, showAddToCart)
                                }
                                composable(
                                    route = Screen.IngredientRequestForm.route,
                                    arguments = listOf(navArgument("id") {
                                        type = NavType.StringType
                                        nullable = true
                                        defaultValue = null
                                    })
                                ) { backStackEntry ->
                                    val id = backStackEntry.arguments?.getString("id")
                                    IngredientRequestFormScreen(navController, requestId = id)
                                }
                                composable(Screen.ShoppingListHome.route) {
                                    ShoppingListHomeScreen(navController)
                                }
                                composable(
                                    route = Screen.ShoppingList.route,
                                    arguments = listOf(navArgument("shoppingListId") {
                                        type = NavType.StringType
                                        nullable = true
                                        defaultValue = null
                                    })
                                ) { backStackEntry ->
                                    val shoppingListId = backStackEntry.arguments?.getString("shoppingListId")
                                    ShoppingListScreen(navController, shoppingListId = shoppingListId)
                                }
                                composable(
                                    route = Screen.AddShoppingListItem.route,
                                    arguments = listOf(navArgument("shoppingListId") {
                                        type = NavType.StringType
                                        nullable = true
                                        defaultValue = null
                                    })
                                ) { backStackEntry ->
                                    val shoppingListId = backStackEntry.arguments?.getString("shoppingListId")
                                    ShoppingListAddItemScreen(navController, targetShoppingListId = shoppingListId)
                                }
                                composable(
                                    route = Screen.ShoppingListAddFrom.route,
                                    arguments = listOf(navArgument("shoppingListId") {
                                        type = NavType.StringType
                                        nullable = true
                                        defaultValue = null
                                    })
                                ) { backStackEntry ->
                                    val shoppingListId = backStackEntry.arguments?.getString("shoppingListId")
                                    ShoppingListAddFromScreen(navController, targetShoppingListId = shoppingListId)
                                }
                            }
                        }
                    }

                    // 🌟 The Curtain: Fades away to reveal the Login screen already sitting underneath
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

                            // 🌟 Screens that are allowed WITHOUT login (Auth flow)
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
                            // 🌟 FIX: Use startsWith to handle routes with query parameters like ?fromRegister=true
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
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

data class NavigationItem(val route: String, val label: String, val icon: Int)


// PLEASE DON'T DELETE IT, IT CAN PREVENT THE SCREEN FLASH YOUR EYE
@Composable
fun SplashLogoOverlay() {
    val primaryColor = MaterialTheme.colorScheme.primary
    val backgroundColor = MaterialTheme.colorScheme.background
    val darkTheme = androidx.compose.foundation.isSystemInDarkTheme()
    val view = LocalView.current

    // 🌟 Sync Status Bar to background color
    SideEffect {
        val window = (view.context as Activity).window
        window.statusBarColor = backgroundColor.toArgb()
        androidx.core.view.WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor) // 🌟 Sync Background
    ) {
        // 🌟 Seamless Background Spacer
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
                    text = "Foodie Heal",
                    color = primaryColor,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Nourishing every bite.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, // 🌟 Themed Text
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}