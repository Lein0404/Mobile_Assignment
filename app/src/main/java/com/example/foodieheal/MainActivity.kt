package com.example.foodieheal

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.foodieheal.Admin.AdminApprovalScreen
import com.example.foodieheal.Admin.ChefDetailScreen
import com.example.foodieheal.Chef.ChefMainScreen
import com.example.foodieheal.Chef.Register.*
import com.example.foodieheal.Chef.ViewModel.chefRegisterViewModel
import com.example.foodieheal.Hiring.Screen.HiringAppointment
import com.example.foodieheal.Hiring.Screen.HiringChefDetails
import com.example.foodieheal.Hiring.Screen.HiringScreen
import com.example.foodieheal.Hiring.ViewModel.BookmarkViewModel
import com.example.foodieheal.Hiring.ViewModel.HiringViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.foodieheal.Admin.AdminIngredientDetailScreen
import com.example.foodieheal.Admin.AdminIngredientRequestFormScreen
import com.example.foodieheal.Admin.AdminIngredientsScreen
import com.example.foodieheal.ingredients.view.AddShoppingListItemScreen
import com.example.foodieheal.ingredients.view.IngredientDetailScreen
import com.example.foodieheal.ingredients.view.IngredientRequestFormScreen
import com.example.foodieheal.ingredients.view.IngredientsMainScreen
import com.example.foodieheal.ingredients.view.ShoppingListScreen
import com.example.foodieheal.meal_planner.model.MealType
import com.example.foodieheal.meal_planner.screen.AddRecipeToPlanScreen
import com.example.foodieheal.meal_planner.screen.MealPlannerScreen
import com.example.foodieheal.meal_planner.screen.RecipesSelectingScreen
import com.example.foodieheal.meal_planner.viewModel.MealPlannerViewModel
import com.example.foodieheal.meal_planner.viewModel.MealPlannerViewModelFactory
import com.example.foodieheal.view.AddRecipeScreen
import com.example.foodieheal.navigation.Screen
import com.example.foodieheal.repository.RecipeRepository
import com.example.foodieheal.ui.theme.FoodieHealTheme
import com.example.foodieheal.view.ChangePasswordScreen
import com.example.foodieheal.view.EditBodyStatusScreen
import com.example.foodieheal.view.EditProfileScreen
import com.example.foodieheal.view.HomeScreen
import com.example.foodieheal.view.LoginScreen
import com.example.foodieheal.view.ProfileScreen
import com.example.foodieheal.view.RecipesScreen
import com.example.foodieheal.view.RegisterScreen
import com.example.foodieheal.viewmodel.AuthViewModel
import com.example.foodieheal.viewmodel.RecipeViewModel
import kotlinx.coroutines.delay
import java.time.LocalDate
import kotlin.time.Duration.Companion.milliseconds

class MainActivity : ComponentActivity() {
    private val mealPlannerViewModel: MealPlannerViewModel by viewModels {
        MealPlannerViewModelFactory(application)
    }
    companion object {
        var appContext: Context? = null
            private set
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appContext = applicationContext
        enableEdgeToEdge()
        intent?.data?.let { uri ->
            processDeepLink(uri)
        }
        setContent {
            FoodieHealTheme(dynamicColor = false) {
                val navController = rememberNavController()
                val lifecycleOwner = LocalLifecycleOwner.current

                LaunchedEffect(lifecycleOwner) {
                    lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                        mealPlannerViewModel.navigationEvent.collect { route ->
                            // 1. Prevent the crash: If NavHost isn't ready, wait until the graph is attached
                            while (runCatching { navController.graph }.isFailure) {
                                delay(50.milliseconds)
                            }

                            val currentDest = navController.currentBackStackEntry?.destination?.route
                            val hasLoginOnStack = currentDest == Screen.Login.route

                            navController.navigate(route) {
                                if (hasLoginOnStack) {
                                    // Cold start handling: wipe the placeholder/login screens out completely
                                    popUpTo(0) { inclusive = true }
                                } else {
                                    // Warm start handling: preserve user stack state safely
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                }
                val sharedAuthViewModel: AuthViewModel = viewModel()
                val sharedRecipeViewModel: RecipeViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return RecipeViewModel(RecipeRepository(SupabaseClient.client)) as T
                        }
                    }
                )
                val hiringViewModel: HiringViewModel = viewModel()
                val chefViewModel: chefRegisterViewModel = viewModel()

                if (sharedAuthViewModel.isInitializing) {
                    SplashLogoOverlay()
                } else {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination

                    // Check if we are on one of the main tabs
                    val tabRoutes = listOf(Screen.Home.route, Screen.Recipes.route, Screen.Planner.route, Screen.Hiring.route, Screen.Profile.route)
                    val isTabScreen = currentDestination?.route in tabRoutes
                    val shouldShowBottomBar = isTabScreen && sharedAuthViewModel.loginSuccess

                    Scaffold(
                        // 🌟 contentWindowInsets=0 ensures the orange header reaches the top without gaps
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                        bottomBar = {
                            if (shouldShowBottomBar) {
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
                                                navController.navigate(item.route) {
                                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        val startRoute = when {
                            sharedAuthViewModel.loginSuccess && sharedAuthViewModel.isAdmin -> Screen.AdminChefScreen.route
                            sharedAuthViewModel.loginSuccess && sharedAuthViewModel.isChef -> Screen.ChefMain.route
                            sharedAuthViewModel.loginSuccess -> Screen.Home.route
                            else -> Screen.Login.route
                        }

                        // Use a Box to keep the NavHost full screen and avoid resizing lag
                        Box(modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)) {
                            NavHost(
                                navController = navController,
                                startDestination = startRoute,
                                modifier = Modifier.fillMaxSize(),
                                // 🌟 FADE Transition only for initialization to prevent flashing after slogan
                                enterTransition = { fadeIn(animationSpec = tween(400)) },
                                exitTransition = { fadeOut(animationSpec = tween(400)) }
                            ) {
                                // --- AUTH ---
                                composable(Screen.Login.route) { LoginScreen(navController, sharedAuthViewModel) }
                                composable(Screen.Register.route) { RegisterScreen(navController, sharedAuthViewModel) }

                                // --- TABS ---
                                composable(Screen.Home.route) {
                                    Box(modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())) { HomeScreen(navController, sharedAuthViewModel) }
                                }
                                composable(Screen.Recipes.route) {
                                    Box(modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())) { RecipesScreen(navController, sharedRecipeViewModel, sharedAuthViewModel) }
                                }

                                composable(Screen.Planner.route) {
                                    MealPlannerScreen(
                                        mealPlannerViewModel = mealPlannerViewModel,
                                        authViewModel = sharedAuthViewModel,
                                        onNavigateToProfile = { navController.navigate(Screen.EditBodyStatus.route) },
                                        onRecipeDetails = {recipeId -> navController.navigate(navController.navigate(
                                            Screen.RecipeDetails.createRoute(recipeId)))},
                                        onAddMeal = { date, type ->
                                            navController.navigate(Screen.RecipeSelection.createRoute(date = date,type = type))
                                        }
                                    )
                                }

                                composable(Screen.AddRecipeToPlanner.route) { backStackEntry ->
                                    val recipeId = backStackEntry.arguments?.getString("recipeId")
                                    // Trigger fetch only if the ID is valid
                                    LaunchedEffect(recipeId) {
                                        if (!recipeId.isNullOrEmpty()) {
                                            sharedRecipeViewModel.fetchRecipeById(recipeId)
                                        }
                                    }

                                    val recipe = sharedRecipeViewModel.selectedRecipe

                                    if (recipeId.isNullOrEmpty()) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                                    route = Screen.RecipeSelection.route, // matches "recipe_selection/{date}/{type}"
                                    arguments = listOf(
                                        navArgument("date") { type = NavType.StringType },
                                        navArgument("type") { type = NavType.StringType }
                                    )
                                ) { backStackEntry ->
                                    // 🌟 Extract strings from navigation and parse them into their native types
                                    val dateString = backStackEntry.arguments?.getString("date") ?: ""
                                    val typeString = backStackEntry.arguments?.getString("type") ?: ""

                                    // 🌟 Convert strings safely to LocalDate and MealType enum
                                    val date: LocalDate = if (dateString.isNotEmpty()) LocalDate.parse(dateString) else LocalDate.now()
                                    val type: MealType = try {
                                        MealType.valueOf(typeString)
                                    } catch (e: IllegalArgumentException) {
                                        MealType.BREAKFAST // Fallback default if parsing fails
                                    }
                                    RecipesSelectingScreen(
                                        recipeViewModel = sharedRecipeViewModel,
                                        authViewModel = sharedAuthViewModel,
                                        onSave = { selectedIds ->
                                            selectedIds.forEach { recipeId ->
                                                val recipe = sharedRecipeViewModel.recipeList.find { it.recipe_id == recipeId }
                                                    ?: sharedRecipeViewModel.myRecipes.find { it.recipe_id == recipeId }
                                                    ?: sharedRecipeViewModel.bookmarkedRecipes.find { it.recipe_id == recipeId }

                                                if (recipe != null) {
                                                    mealPlannerViewModel.addRecipeToMeal(
                                                        date = date,
                                                        mealType = type,
                                                        recipe = recipe
                                                    )
                                                }
                                            }
                                            navController.popBackStack()
                                        },
                                        onBackClick = {navController.popBackStack()}
                                    )
                                }

                                composable(Screen.Hiring.route) {
                                    Box(modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())) {
                                        HiringScreen(onChefClick = { chef ->
                                            hiringViewModel.selectChef(chef)
                                            navController.navigate(Screen.HiringChefDetails.route)
                                        })
                                    }
                                }
                                composable(Screen.Profile.route) {
                                    Box(modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())) { ProfileScreen(navController, sharedRecipeViewModel, sharedAuthViewModel) }
                                }

                                // --- FEATURES (Full screen, instant swap) ---
                                composable(Screen.HiringChefDetails.route) {
                                    val userId = sharedAuthViewModel.currentUser?.id.orEmpty()
                                    val profileVM: BookmarkViewModel = viewModel()
                                    hiringViewModel.selectedChef?.let { chef ->
                                        HiringChefDetails(
                                            chef = chef,
                                            userId = userId,
                                            viewModel = profileVM,
                                            onBackClick = { navController.popBackStack() },
                                            onHireClick = { navController.navigate(Screen.HiringAppointment.route) })
                                    }
                                }
                                composable(Screen.HiringAppointment.route) { backStackEntry ->
                                    val parentEntry = remember(backStackEntry) {
                                        navController.getBackStackEntry(Screen.HiringChefDetails.route)
                                    }
                                    val chef = hiringViewModel.selectedChef
                                    if (chef != null) {
                                        HiringAppointment(
                                            chef = chef,
                                            onBackClick = { navController.popBackStack() },
                                            onAddAppointmentClick = { chosenDate ->
                                                hiringViewModel.updateSelectedDate(chosenDate) // Update selected date (passing data)
                                                navController.navigate(Screen.AddHiringAppointment.route)
                                            }
                                        )
                                    }
                                    }

                                composable(Screen.AddRecipe.route) { AddRecipeScreen(navController, sharedRecipeViewModel, sharedAuthViewModel) }
                                composable(Screen.EditProfile.route) { EditProfileScreen(navController) }
                                composable(Screen.ChangePassword.route) { ChangePasswordScreen(navController) }
                                composable(
                                    route = Screen.EditBodyStatus.route + "?fromRegister={fromRegister}",
                                    arguments = listOf(navArgument("fromRegister") { defaultValue = false; type = NavType.BoolType })
                                ) { backStackEntry ->
                                    val fromRegister = backStackEntry.arguments?.getBoolean("fromRegister") ?: false
                                    EditBodyStatusScreen(navController, fromRegister = fromRegister)
                                }

                                // --- ADMIN & CHEF ---
                                composable(Screen.AdminChefScreen.route) {
                                    AdminApprovalScreen(navController, authViewModel = sharedAuthViewModel)
                                }
                                composable(Screen.AdminIngredient.route){
                                    AdminIngredientsScreen(navController)
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
                                composable(Screen.ChefMain.route) { ChefMainScreen(navController, sharedAuthViewModel) }
                                composable("chefDetail/{chefId}") {
                                    ChefDetailScreen(it.arguments?.getString("chefId") ?: "", navController)
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
                                composable(Screen.Ingredients.route) {
                                    IngredientsMainScreen(navController)
                                }
                                composable(
                                    route = Screen.IngredientDetail.route,
                                    arguments = listOf(
                                        navArgument("id") { type = NavType.StringType },
                                        navArgument("isRequest") { type = NavType.BoolType }
                                    )
                                ) { backStackEntry ->
                                    val id = backStackEntry.arguments?.getString("id") ?: ""
                                    val isRequest = backStackEntry.arguments?.getBoolean("isRequest") ?: false
                                    IngredientDetailScreen(navController, id, isRequest)
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
                                composable(Screen.ShoppingList.route) {
                                    ShoppingListScreen(navController)
                                }
                                composable(Screen.AddShoppingListItem.route) {
                                    AddShoppingListItemScreen(navController)
                                }
                            }
                        }
                    }

                    // Global Logout Logic
                    LaunchedEffect(sharedAuthViewModel.loginSuccess) {
                        if (!sharedAuthViewModel.loginSuccess && !sharedAuthViewModel.isInitializing) {
                            navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // Update activity intent
        intent.data?.let { uri ->
            processDeepLink(uri)
        }
    }

    private fun processDeepLink(uri: Uri) {
        Log.d("DeepLink", "Processing URI: $uri")

        val isHttpsLink = uri.scheme == "https" && uri.host == "tzh652.github.io" && uri.path?.startsWith("/share") == true
        val isCustomScheme = uri.scheme == "foodieheal" && uri.host == "share"

        if (isHttpsLink || isCustomScheme) {
            uri.getQueryParameter("sourceStart")?.let { dateStr ->
                runCatching {
                    LocalDate.parse(dateStr)
                }.onSuccess { startDate ->
                    mealPlannerViewModel.prepareSharedWeeklyPlan(startDate)
                }.onFailure { e ->
                    Log.e("DeepLink", "Failed to parse date: $dateStr", e)
                }
            }
        }
    }
}

data class NavigationItem(val route: String, val label: String, val icon: Int)

@Composable
fun SplashLogoOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Foodie Heal",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Nourishing every bite.",
                color = Color.Gray,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}