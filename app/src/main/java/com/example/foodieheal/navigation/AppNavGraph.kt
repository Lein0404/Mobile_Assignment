package com.example.foodieheal.navigation

import android.widget.Toast
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.foodieheal.Admin.*
import com.example.foodieheal.Chef.ChefMainScreen
import com.example.foodieheal.Chef.Register.*
import com.example.foodieheal.Chef.ViewModel.Register.ChefRegisterViewModel
import com.example.foodieheal.MainActivity
import com.example.foodieheal.Payment.Screen.PaymentMethodScreen
import com.example.foodieheal.Payment.Screen.PaymentScreen
import com.example.foodieheal.Payment.ViewModel.PaymentMethodViewModel
import com.example.foodieheal.Payment.ViewModel.PaymentViewModel
import com.example.foodieheal.Payment.local.PayMethodDatabase
import com.example.foodieheal.Payment.repo.PaymentRepository
import com.example.foodieheal.R
import com.example.foodieheal.Recipe.Repo.RecipeRepository
import com.example.foodieheal.Recipe.View.AddRecipeScreen
import com.example.foodieheal.Recipe.View.EditRecipeScreen
import com.example.foodieheal.Recipe.View.RecipeDetailsScreen
import com.example.foodieheal.Recipe.View.RecipesScreen
import com.example.foodieheal.Recipe.local.RecipeDatabase
import com.example.foodieheal.Recipe.viewModel.RecipeViewModel
import com.example.foodieheal.SupabaseClient
import com.example.foodieheal.User.View.*
import com.example.foodieheal.User.viewModel.AuthViewModel
import com.example.foodieheal.hiring.screen.*
import com.example.foodieheal.hiring.viewmodel.AppointmentBookingViewModel
import com.example.foodieheal.hiring.viewmodel.BookmarkViewModel
import com.example.foodieheal.hiring.viewmodel.ChefListViewModel
import com.example.foodieheal.hiring.viewmodel.UserAppointmentViewModel
import com.example.foodieheal.ingredients.view.*
import com.example.foodieheal.meal_planner.data.PlanRepository
import com.example.foodieheal.meal_planner.model.MealType
import com.example.foodieheal.meal_planner.screen.*
import com.example.foodieheal.meal_planner.viewModel.AddEditTemplateViewModel
import com.example.foodieheal.meal_planner.viewModel.MealPlannerViewModel
import com.example.foodieheal.meal_planner.viewModel.NetworkMonitor
import com.example.foodieheal.meal_planner.viewModel.TemplateViewModel
import com.example.foodieheal.wallet.screen.WalletScreen
import com.example.foodieheal.wallet.screen.WalletTransactionDetailScreen
import es.dmoral.toasty.Toasty
import kotlinx.datetime.DayOfWeek
import java.time.LocalDate

@Composable
fun AppNavGraph(
    navController: NavHostController,
    innerPadding: PaddingValues,
    sharedAuthViewModel: AuthViewModel,
    sharedRecipeViewModel: RecipeViewModel,
    mealPlannerViewModel: MealPlannerViewModel,
    chefListViewModel: ChefListViewModel,
    bookingViewModel: AppointmentBookingViewModel,
    userAppointmentViewModel: UserAppointmentViewModel,
    bookmarkViewModel: BookmarkViewModel,
    chefViewModel: ChefRegisterViewModel,
    networkMonitor: NetworkMonitor,
    startDestination: String = Screen.Login.route
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
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
                Box(modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())) {
                    HomeScreen(
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
                    onEdit = { id -> navController.navigate(Screen.AddEditTemplate.createRoute(id)) },
                    onAppointmentClick = { appointmentId ->
                        navController.navigate(Screen.UserAppointmentDetail.createRoute(appointmentId))
                    }
                )
            }

            composable(
                route = Screen.AddRecipeToPlanner.route,
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

                RecipesScreen(
                    parentNavController = navController,
                    viewModel = sharedRecipeViewModel,
                    authViewModel = sharedAuthViewModel,
                    isSelectionMode = true,
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
                val recipeRepository = remember { RecipeRepository(RecipeDatabase.getDatabase(context).recipeDao()) }

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
                            MainActivity.appContext?.let { ctx ->
                                Toasty.custom(ctx, "Template applied to meal planner!", R.drawable.foodieheallogo_removebg_and_word, R.color.black, Toast.LENGTH_SHORT, true, true).show()
                            }
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
                            MainActivity.appContext?.let { ctx ->
                                Toasty.custom(ctx, "Recipe removed from template", R.drawable.foodieheallogo_removebg_and_word, R.color.black, Toast.LENGTH_SHORT, true, true).show()
                            }
                        },
                        onEdit = { navController.navigate(Screen.AddEditTemplate.createRoute(plan.planId)) },
                        onDelete = {
                            templateViewModel.deleteWeeklyPlan(
                                planId = plan.planId,
                                onSuccess = {
                                    MainActivity.appContext?.let { ctx ->
                                        Toasty.custom(ctx, "Template deleted successfully!", R.drawable.foodieheallogo_removebg_and_word, R.color.black, Toast.LENGTH_SHORT, true, true).show()
                                    }
                                    navController.popBackStack()
                                }
                            )
                        },
                        onAdd = {
                            templateViewModel.duplicateTemplate(
                                sourcePlanId = plan.planId,
                                currentUserId = sharedAuthViewModel.currentUser?.id ?: "",
                                onSuccess = {
                                    MainActivity.appContext?.let { ctx ->
                                        Toasty.custom(ctx, "Template saved to your collection!", R.drawable.foodieheallogo_removebg_and_word, R.color.black, Toast.LENGTH_SHORT, true, true).show()
                                    }
                                },
                                onError = { error ->
                                    MainActivity.appContext?.let { ctx ->
                                        Toasty.custom(ctx, error, R.drawable.foodieheallogo_removebg_and_word, R.color.black, Toast.LENGTH_SHORT, true, true).show()
                                    }
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
                        bookingViewModel = bookingViewModel,
                        bookmarkViewModel = bookmarkViewModel,
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
                        userAppointmentViewModel.fetchAppointmentsForCurrentUser()
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
                        userAppointmentViewModel.fetchAppointmentsForCurrentUser()
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
                    recipeViewModel = sharedRecipeViewModel,
                    authViewModel = sharedAuthViewModel,
                    onBackClick = { navController.popBackStack() },
                    onRescheduleSuccess = {
                        userAppointmentViewModel.fetchAppointmentsForCurrentUser()
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
                val rebookingAppointmentId by bookingViewModel.isRebooking.collectAsStateWithLifecycle()

                AppointmentHistoryScreen(
                    viewModel = userAppointmentViewModel,
                    rebookingAppointmentId = rebookingAppointmentId,
                    onBackClick = { navController.popBackStack() },
                    onAppointmentClick = { appointmentId ->
                        navController.navigate(Screen.UserAppointmentDetail.createRoute(appointmentId))
                    },
                    onRebookClick = { appointment ->
                        bookingViewModel.prepareRebook(
                            appointment = appointment,
                            onSuccess = { chefId ->
                                navController.navigate("${Screen.HiringAppointment.route}/$chefId")
                            },
                            onError = { errorMsg ->
                                Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                )
            }

            composable(route = Screen.PaymentMethod.route) {
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
                AdminMainScreen(navController, authViewModel = sharedAuthViewModel, initialTab = tab)
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
                arguments = listOf(
                    navArgument("shoppingListId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("recipeId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val shoppingListId = backStackEntry.arguments?.getString("shoppingListId")
                val recipeId = backStackEntry.arguments?.getString("recipeId")
                ShoppingListAddFromScreen(
                    navController = navController,
                    targetShoppingListId = shoppingListId,
                    recipeId = recipeId,
                    recipeViewModel = sharedRecipeViewModel
                )
            }
        }
    }
}
