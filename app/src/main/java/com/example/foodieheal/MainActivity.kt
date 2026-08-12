package com.example.foodieheal

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.foodieheal.Admin.AdminApprovalScreen
import com.example.foodieheal.Admin.AdminIngredientDetailScreen
import com.example.foodieheal.Admin.AdminIngredientRequestFormScreen
import com.example.foodieheal.Admin.AdminIngredientsScreen
import com.example.foodieheal.Admin.ChefDetailScreen
import com.example.foodieheal.Chef.ChefMainScreen
import com.example.foodieheal.Chef.Register.*
import com.example.foodieheal.Chef.ViewModel.chefRegisterViewModel
import com.example.foodieheal.Hiring.Screen.AddAppointmentFormScreen
import com.example.foodieheal.Hiring.Screen.AppointmentReviewScreen
import com.example.foodieheal.Hiring.Screen.HiringAppointment
import com.example.foodieheal.Hiring.Screen.HiringChefDetails
import com.example.foodieheal.Hiring.Screen.HiringScreen
import com.example.foodieheal.Hiring.Screen.RateChefScreen
import com.example.foodieheal.Hiring.Screen.RescheduleAppointmentScreen
import com.example.foodieheal.Hiring.Screen.UserAppointmentDetailScreen
import com.example.foodieheal.Hiring.ViewModel.BookmarkViewModel
import com.example.foodieheal.Hiring.ViewModel.HiringViewModel
import com.example.foodieheal.meal_planner.data.MealPlannerRepository
import com.example.foodieheal.meal_planner.screen.AddRecipeToPlanScreen
import com.example.foodieheal.meal_planner.screen.MealPlannerScreen
import com.example.foodieheal.meal_planner.viewModel.MealPlannerViewModel
import com.example.foodieheal.navigation.Screen
import com.example.foodieheal.repository.RecipeRepository
import com.example.foodieheal.ui.theme.FoodieHealTheme
import com.example.foodieheal.view.*
import com.example.foodieheal.viewmodel.AuthViewModel
import com.example.foodieheal.viewmodel.RecipeViewModel
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.datetime.LocalDate

class MainActivity : ComponentActivity() {

    private val mealPlannerViewModel: MealPlannerViewModel by viewModels {
        MealPlannerViewModelFactory(application)
    }
    companion object {
        var appContext: android.content.Context? = null
            private set
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appContext = applicationContext
        enableEdgeToEdge()
        setContent {
            FoodieHealTheme(dynamicColor = false) {
                val navController = rememberNavController()
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
                val chefviewModel: chefRegisterViewModel = viewModel()

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
                        Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF8F8F8))) {
                            NavHost(
                                navController = navController,
                                startDestination = startRoute,
                                modifier = Modifier.fillMaxSize(),
                                // 🌟 FADE Transition only for initialization to prevent flashing after slogan
                                enterTransition = { fadeIn(animationSpec = tween(400)) },
                                exitTransition = { fadeOut(animationSpec = tween(400)) }
                            ) {
                                // --- AUTH ---
                                composable(Screen.Login.route) { LoginScreen(navController) }
                                composable(Screen.Register.route) { RegisterScreen(navController) }

                                // --- TABS (Manually apply innerPadding to avoid NavHost-wide resize shifts) ---

                                composable(Screen.Home.route) {
                                    HomeScreen(
                                        navController = navController,
                                        chefViewModel = hiringViewModel,
                                        onChefClick = { chef ->
                                            hiringViewModel.selectChef(chef)
                                            navController.navigate(Screen.HiringChefDetails.route) {
                                                launchSingleTop = true
                                            }
                                        }
                                    )
                                }
                                composable(Screen.Recipes.route) {
                                    Box(modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())) { RecipesScreen(navController) }
                                }
                                composable(Screen.Planner.route) {
                                    val mv: MealPlannerViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                                        @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                            return MealPlannerViewModel(application, MealPlannerRepository(SupabaseClient.client.postgrest, SupabaseClient.client)) as T
                                        }
                                    })
                                    Box(modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())) { MealPlannerScreen(viewModel = mv) }
                                }
                                composable(Screen.Hiring.route) {
                                    Box(modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())) {
                                        HiringScreen(
                                            onChefClick = { chef ->
                                                hiringViewModel.selectChef(chef)
                                                navController.navigate(Screen.HiringChefDetails.route)
                                            },
                                            onAppointmentClick = { appointment ->
                                                // Handle appointment click / navigation here
                                                // e.g., navController.navigate(Screen.AppointmentDetails.createRoute(appointment.id))
                                            }
                                        )
                                    }
                                }
                                composable(Screen.Profile.route) {
                                    Box(modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())) { ProfileScreen(navController) }
                                }

                                // --- FEATURES (Full screen, instant swap) ---
                                composable(Screen.HiringChefDetails.route) {
                                    val userId = sharedAuthViewModel.currentUser?.id.orEmpty()
                                    val profileVM: BookmarkViewModel = viewModel()
                                    hiringViewModel.selectedChef?.let { chef ->
                                        HiringChefDetails(chef = chef, userId = userId, viewModel = profileVM, onBackClick = { navController.popBackStack() }, onHireClick = { navController.navigate(Screen.HiringAppointment.route) })
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

                                composable(Screen.AddHiringAppointment.route) {
                                    AddAppointmentFormScreen(
                                        viewModel = hiringViewModel, // PASS SHARED VIEWMODEL
                                        onBackClick = { navController.popBackStack() },
                                        onSuccessConfirm = {
                                            navController.navigate(Screen.AppointmentReview.route)
                                        }
                                    )
                                }

                                composable(Screen.AppointmentReview.route) {
                                    AppointmentReviewScreen(
                                        viewModel = hiringViewModel,
                                        onBackClick = { navController.popBackStack() },
                                        onFinalConfirm = {
                                            navController.popBackStack(Screen.Home.route, inclusive = false)
                                        }
                                    )
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

                                composable(
                                    route = "appointmentDetail/{appointmentId}",
                                    arguments = listOf(navArgument("appointmentId") { type = NavType.StringType })
                                ) { backStackEntry ->
                                    val appointmentId = backStackEntry.arguments?.getString("appointmentId").orEmpty()

                                    UserAppointmentDetailScreen(
                                        appointmentId = appointmentId,
                                        viewModel = hiringViewModel,
                                        onBackClick = { navController.popBackStack() },
                                        onRescheduleClick = { appointment ->
                                            navController.navigate("rescheduleAppointment/${appointment.AppointmentID}")
                                        },
                                        onRatingClick = { targetAppointmentId ->
                                            navController.navigate(Screen.RateChef.createRoute(targetAppointmentId))
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
                                        viewModel = hiringViewModel,
                                        onSubmitSuccess = {
                                            navController.popBackStack()
                                        }
                                    )
                                }

                                composable(
                                    route = "rescheduleAppointment/{appointmentId}",
                                    arguments = listOf(navArgument("appointmentId") { type = NavType.StringType })
                                ) { backStackEntry ->
                                    val appointmentId = backStackEntry.arguments?.getString("appointmentId").orEmpty()

                                    RescheduleAppointmentScreen(
                                        appointmentId = appointmentId,
                                        viewModel = hiringViewModel,
                                        onBackClick = { navController.popBackStack() },
                                        onRescheduleSuccess = {
                                            navController.popBackStack()
                                        }
                                    )
                                }


                                composable(Screen.AddRecipe.route) { AddRecipeScreen(navController) }
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

                                composable(Screen.AdminChefScreen.route) { AdminApprovalScreen(navController) }
                                composable(Screen.ChefMain.route) { ChefMainScreen(navController, sharedAuthViewModel) }
                                composable("chefDetail/{chefId}") {
                                    ChefDetailScreen(it.arguments?.getString("chefId") ?: "", navController)
                                }
                                navigation(startDestination = Screen.Welcome.route, route = "chefRegisterRoute") {
                                    composable(Screen.Welcome.route) { ChefWelcomeScreen(navController, chefviewModel) }
                                    composable(Screen.BasicInfo.route) { basicInfo(navController, chefviewModel) }
                                    composable(Screen.Contact.route) { contactInfo(navController, chefviewModel) }
                                    composable(Screen.Address.route) { addressInfo(navController, chefviewModel) }
                                    composable(Screen.Description.route) { descriptionInfo(navController, chefviewModel) }
                                    composable(Screen.ChefPicture.route) { ChefPictureScreen(navController, chefviewModel) }
                                    composable(Screen.Review.route) { reviewInfo(navController, chefviewModel) }
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