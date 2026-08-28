package com.example.foodieheal.Recipe.View

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.foodieheal.R
import com.example.foodieheal.Recipe.Model.Recipe
import com.example.foodieheal.User.Model.User
import com.example.foodieheal.User.viewModel.AuthViewModel
import com.example.foodieheal.Recipe.viewModel.RecipeViewModel
import com.example.foodieheal.User.viewModel.FollowViewModel
import com.example.foodieheal.navigation.Screen
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.draw.drawWithContent
import androidx.annotation.DrawableRes
import com.example.foodieheal.ui.components.ShareRecipeDialog
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailsScreen(
    navController: NavController,
    recipeId: String,
    viewModel: RecipeViewModel,
    authViewModel: AuthViewModel,
    followViewModel: FollowViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    // 1. Fetch data if needed (Smart fetching)
    LaunchedEffect(recipeId) {
        viewModel.fetchRecipeById(recipeId)
    }



    val recipe = viewModel.selectedRecipe
    val user = authViewModel.currentUser
    val view = androidx.compose.ui.platform.LocalView.current
    val isBookmarked = viewModel.bookmarkedRecipeIds.contains(recipeId)
    // 🌟 FIX: Use customId for ownership check to match database logic
    val isMyRecipe = recipe?.author_id == user?.customId

    LaunchedEffect(recipe?.author_id, user?.customId) {
        val aid = recipe?.author_id
        val uid = user?.customId
        if (aid != null && uid != null && aid != uid) {
            followViewModel.fetchFollowStatus(uid, aid)
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    var expanded by remember { mutableStateOf(false) }
    var recipeToDelete by remember { mutableStateOf<Recipe?>(null) }
    
    // 🌟 Share logic
    var showSharePreview by remember { mutableStateOf(false) }
    var showOfflineShareDialog by remember { mutableStateOf(false) }
    var showOfflinePlannerDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.bookmarkMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
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

    Scaffold(
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
            CenterAlignedTopAppBar(
                title = { Text("View Recipe", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(painterResource(id = R.drawable.ic_arrowback), "Back", tint = Color.White)
                    }
                },
                actions = {
                    // 🌟 Bookmark is now enabled for all recipes, including your own
                    IconButton(onClick = {
                        user?.customId?.let { cid ->
                            recipe?.let { r -> viewModel.toggleBookmark(cid, r.recipe_id ?: "", r.recipeName) }
                        }
                    }) {
                        // 🌟 FIX: Use Image with specific size and tint for PNG icons to keep them sharp
                        Image(
                            painter = painterResource(id = if (isBookmarked) R.drawable.bookmark_fill else R.drawable.bookmark),
                            contentDescription = "Bookmark",
                            modifier = Modifier.size(22.dp),
                            colorFilter = ColorFilter.tint(Color.White)
                        )
                    }

                    Box {
                        IconButton(onClick = { expanded = true }) {
                            Icon(painterResource(id = R.drawable.ic_vertical_more), "More", tint = Color.White)
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            if (isMyRecipe) {
                                DropdownMenuItem(
                                    text = { Text("Edit Recipe") },
                                    onClick = {
                                        expanded = false
                                        navController.navigate(Screen.EditRecipe.createRoute(recipeId))
                                    },
                                    leadingIcon = {
                                        Icon(
                                            painterResource(id = R.drawable.ic_square_edit),
                                            null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete Recipe", color = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        expanded = false
                                        recipeToDelete = recipe
                                    },
                                    leadingIcon = {
                                        Icon(
                                            painterResource(id = R.drawable.ic_delete),
                                            null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                )
                            }

                            DropdownMenuItem(
                                text = { Text("Share Recipe") },
                                onClick = {
                                    expanded = false
                                    if (viewModel.isNetworkAvailable) {
                                        showSharePreview = true
                                    } else {
                                        showOfflineShareDialog = true
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        painterResource(id = R.drawable.ic_share),
                                        null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Add to Planner") },
                                onClick = {
                                    expanded = false
                                    if (viewModel.isNetworkAvailable) {
                                        recipe?.recipe_id?.let { id ->
                                            navController.navigate(Screen.AddRecipeToPlanner.createRoute(id))
                                        }
                                    } else {
                                        showOfflinePlannerDialog = true
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        painterResource(id = R.drawable.ic_recipe),
                                        null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        }
    ) { paddingValues ->
        if (recipe == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .background(MaterialTheme.colorScheme.surface) // 🌟 Themed Background
            ) {
                // Recipe Image or Artistic Placeholder
                if (!recipe.recipeImageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = recipe.recipeImageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().height(250.dp),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // 🌟 Option 3: Artistic "Food Sketch" Placeholder
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)), // 🌟 Themed Background
                        contentAlignment = Alignment.Center
                    ) {
                        val iconRes = when (recipe.recipeCourse.lowercase()) {
                            "breakfast" -> R.drawable.ic_breakfast
                            "lunch" -> R.drawable.ic_lunch
                            "dinner" -> R.drawable.ic_dinner
                            else -> R.drawable.ic_snack
                        }
                        Image(
                            painter = painterResource(id = iconRes),
                            contentDescription = null,
                            modifier = Modifier.size(100.dp),
                            alpha = 0.4f // Light "sketch" effect
                        )
                    }
                }

                Column(modifier = Modifier.padding(20.dp)) {
                    // Title and Course + Last Updated
                    Text(text = recipe.recipeName, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        Icon(painterResource(id = R.drawable.recipe_category), null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = recipe.recipeCourse, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        
                        // 🌟 Show Last Updated Time on the same line, right next to the course
                        if (!recipe.lastUpdated.isNullOrBlank()) {
                            val displayTime = recipe.lastUpdated.split("T").firstOrNull() ?: ""
                            Text(
                                text = "  •  Updated: $displayTime", 
                                fontSize = 13.sp, // Sized closer to course
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Stats Grid
                    Row(modifier = Modifier.fillMaxWidth()) {
                        StatItem(R.drawable.ic_fire, "${recipe.calories} kcal", Modifier.weight(1f))
                        StatItem(R.drawable.ic_clock, "${recipe.time} minutes", Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        StatItem(R.drawable.skill, recipe.cookingSkill, Modifier.weight(1f)) 
                        StatItem(R.drawable.dollar_symbol, "RM ${recipe.estimatedBudget}", Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) // 🌟 Themed Divider
                    Spacer(modifier = Modifier.height(16.dp))

                    // 🌟 Author Section
                    val author = viewModel.recipeAuthor
                    // 🌟 FIX: Prioritize recipe's own cached info, then live session, then join result
                    val displayAuthorName = (recipe.authorName ?: (if (isMyRecipe && user != null) user.name else (author?.name ?: recipe.authorInfo?.name))) ?: "Unknown Author"
                    val displayAuthorImage = (recipe.authorImageUrl ?: (if (isMyRecipe && user != null) user.profilePicUrl else (author?.profilePicUrl ?: recipe.authorInfo?.profile_pic_url)))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                recipe.author_id?.let { aid ->
                                    navController.navigate(Screen.Profile.createRoute(aid))
                                }
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        if (!displayAuthorImage.isNullOrBlank()) {
                            AsyncImage(
                                model = displayAuthorImage,
                                contentDescription = "Author Profile",
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.ic_outline_account_circle),
                                contentDescription = "Default Author Profile",
                                modifier = Modifier.size(40.dp).clip(CircleShape),
                                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                        
                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            Text(
                                text = "Recipe by",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = displayAuthorName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (!isMyRecipe && user != null) {
                            Spacer(modifier = Modifier.weight(1f))
                            val status = followViewModel.followStatus
                            val buttonText = when (status) {
                                null -> "Follow"
                                "PENDING" -> "Request Sent"
                                "ACCEPTED" -> "Unfollow"
                                else -> "Follow"
                            }
                            val buttonColor = if (status == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                            
                            Button(
                                onClick = {
                                    recipe.author_id?.let { aid ->
                                        user.customId?.let { uid ->
                                            followViewModel.toggleFollow(uid, aid)
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(buttonText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(20.dp))

                    // 🌟 Description (Hidden if empty)
                    if (recipe.recipeDescription.isNotBlank()) {
                        Text(text = "Description", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        var isExpanded by remember { mutableStateOf(false) }
                        Text(
                            text = recipe.recipeDescription,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .animateContentSize()
                                .clickable { isExpanded = !isExpanded },
                            maxLines = if (isExpanded) Int.MAX_VALUE else 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!isExpanded && recipe.recipeDescription.length > 100) {
                            Text(
                                text = "... See More",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary, // 🌟 Use primary for action text
                                modifier = Modifier.clickable { isExpanded = true }
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // Ingredients
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Ingredients", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        IconButton(onClick = {
                            recipe.let {
                                navController.navigate(Screen.ShoppingListAddFrom.createRoute(recipeId = it.recipe_id ?: recipeId))
                            }
                        }) {
                            Icon(painterResource(id = R.drawable.ic_add_to_shopping_cart), null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    recipe.ingredients.forEach { ingredient ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = ingredient.name, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = "${ingredient.displayQuantity} ${ingredient.unit}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Steps
                    Text(text = "Recipe Steps", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    recipe.recipeStep.split("\n").forEachIndexed { index, step ->
                        if (step.isNotBlank()) {
                            Row(modifier = Modifier.padding(vertical = 8.dp)) {
                                Text(text = "${index + 1}. ", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text(text = step.trim(), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }

    // 🌟 Offline Share Dialog
    if (showOfflineShareDialog) {
        AlertDialog(
            onDismissRequest = { showOfflineShareDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.wifi_off),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("No Internet Connection")
                }
            },
            text = { Text("You cannot share recipes while offline. Please check your network settings.") },
            confirmButton = {
                TextButton(onClick = { showOfflineShareDialog = false }) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // 🌟 Offline Planner Dialog
    if (showOfflinePlannerDialog) {
        AlertDialog(
            onDismissRequest = { showOfflinePlannerDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.wifi_off),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("No Internet Connection")
                }
            },
            text = { Text("You cannot add recipes to your planner while offline. Please check your network settings.") },
            confirmButton = {
                TextButton(onClick = { showOfflinePlannerDialog = false }) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (recipeToDelete != null) {
        if (!viewModel.isNetworkAvailable) {
            AlertDialog(
                onDismissRequest = { recipeToDelete = null },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.wifi_off),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("No Internet Connection")
                    }
                },
                text = { Text("You cannot delete recipes while offline. Please check your network settings.") },
                confirmButton = {
                    TextButton(onClick = { recipeToDelete = null }) {
                        Text("OK", fontWeight = FontWeight.Bold)
                    }
                }
            )
        } else {
            AlertDialog(
                onDismissRequest = { recipeToDelete = null },
                title = { Text("Delete Recipe") },
                text = { Text("Are you sure you want to delete '${recipeToDelete?.recipeName}'?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val rid = recipeToDelete?.recipe_id
                            val uid = user?.customId
                            if (rid != null && uid != null) {
                                viewModel.deleteRecipe(rid, uid)
                                navController.popBackStack() // Go back after successful deletion
                            }
                            recipeToDelete = null
                        }
                    ) {
                        Text("Delete", color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { recipeToDelete = null }) {
                        Text("Cancel", color = Color.Gray)
                    }
                }
            )
        }
    }

    // 🌟 Share Preview Dialog
    if (showSharePreview && recipe != null) {
        val author = viewModel.recipeAuthor
        val displayAuthorName = (if (isMyRecipe && user != null) user.name else (author?.name ?: recipe.authorName ?: recipe.authorInfo?.name)) ?: "Unknown Author"

        ShareRecipeDialog(
            recipe = recipe,
            authorName = displayAuthorName,
            onDismiss = { showSharePreview = false }
        )
    }
}

@Composable
fun StatItem(@DrawableRes icon: Int, label: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier, 
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Icon(
            painter = painterResource(id = icon), 
            null, 
            modifier = Modifier.size(18.dp), 
            tint = MaterialTheme.colorScheme.primary // 🌟 Consistent Icon Color
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label, 
            fontSize = 14.sp, 
            fontWeight = FontWeight.Medium, 
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
