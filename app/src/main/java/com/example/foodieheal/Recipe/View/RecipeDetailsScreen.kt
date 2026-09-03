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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.foodieheal.R
import com.example.foodieheal.Recipe.Model.Recipe
import com.example.foodieheal.User.viewModel.AuthViewModel
import com.example.foodieheal.Recipe.viewModel.RecipeViewModel
import com.example.foodieheal.User.viewModel.FollowViewModel
import com.example.foodieheal.navigation.Screen
import androidx.compose.ui.graphics.ColorFilter
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodieheal.ui.components.ShareRecipeDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailsScreen(
    navController: NavController,
    recipeId: String,
    viewModel: RecipeViewModel,
    authViewModel: AuthViewModel,
    followViewModel: FollowViewModel = viewModel()
) {
    val user = authViewModel.currentUser
    val recipe = viewModel.selectedRecipe
    val view = LocalView.current
    val isBookmarked = viewModel.bookmarkedRecipeIds.contains(recipeId)
    val isMyRecipe = recipe?.author_id == user?.id

    LaunchedEffect(recipeId, user?.id) {
        viewModel.fetchRecipeLocalFirst(recipeId)
        user?.id?.let { uid ->
            viewModel.fetchBookmarkIds(uid)
        }
    }

    LaunchedEffect(recipe?.author_id, user?.id) {
        val aid = recipe?.author_id
        val uid = user?.id
        if (aid != null && uid != null && aid != uid) {
            followViewModel.fetchFollowStatus(uid, aid)
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    var expanded by remember { mutableStateOf(false) }
    var recipeToDelete by remember { mutableStateOf<Recipe?>(null) }

    // Share logic
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
                FollowViewModel.FollowEvent.Error -> view.context.getString(R.string.error_network_try_again)
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
                title = { Text(stringResource(R.string.title_view_recipe), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = { 
                        // Safety check to prevent spam-clicks from causing navigation crashes or "blank screens"
                        val currentRoute = navController.currentDestination?.route
                        if (currentRoute?.contains(Screen.RecipeDetails.route.substringBefore("/{")) == true) {
                            navController.popBackStack() 
                        }
                    }) {
                        Icon(painterResource(id = R.drawable.ic_arrowback), stringResource(R.string.back_button), tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        user?.id?.let { uid ->
                            recipe?.recipe_id?.takeIf { it.isNotBlank() }?.let { rid ->
                                viewModel.toggleBookmark(uid, rid, recipe?.recipeName ?: "")
                            }
                        }
                    }) {
                        Image(
                            painter = painterResource(id = if (isBookmarked) R.drawable.bookmark_fill else R.drawable.bookmark),
                            contentDescription = stringResource(R.string.bookmark_chef),
                            modifier = Modifier.size(22.dp),
                            colorFilter = ColorFilter.tint(Color.White)
                        )
                    }

                    Box {
                        IconButton(onClick = { expanded = true }) {
                            Icon(painterResource(id = R.drawable.ic_vertical_more), stringResource(R.string.more_options), tint = Color.White)
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            if (isMyRecipe) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.menu_edit_recipe)) },
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
                                    text = { Text(stringResource(R.string.menu_delete_recipe), color = MaterialTheme.colorScheme.error) },
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
                                text = { Text(stringResource(R.string.menu_share_recipe)) },
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
                                text = { Text(stringResource(R.string.menu_add_to_meal_planner)) },
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
        if (viewModel.isNotFound) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(id = if (viewModel.isNetworkAvailable) R.drawable.ic_image else R.drawable.wifi_off),
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (viewModel.isNetworkAvailable)
                            stringResource(R.string.label_recipe_not_found)
                        else
                            stringResource(R.string.label_recipe_offline_unavailable),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray
                    )
                    TextButton(onClick = { navController.popBackStack() }) {
                        Text(stringResource(R.string.back_button))
                    }
                }
            }
        } else if (recipe == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .background(MaterialTheme.colorScheme.surface)
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
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
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
                        
                        val courseLabel = when(recipe.recipeCourse) {
                            "Breakfast" -> stringResource(R.string.breakfast)
                            "Lunch" -> stringResource(R.string.lunch)
                            "Dinner" -> stringResource(R.string.dinner)
                            "Snack" -> stringResource(R.string.snack)
                            else -> recipe.recipeCourse
                        }
                        Text(text = courseLabel, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        
                        // Show Last Updated Time on the same line, right next to the course
                        if (!recipe.lastUpdated.isNullOrBlank()) {
                            val displayTime = recipe.lastUpdated.split("T").firstOrNull() ?: ""
                            Text(
                                text = " • " + stringResource(R.string.label_updated_at_format, displayTime), 
                                fontSize = 13.sp, // Sized closer to course
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }

                    // Show Visibility Status on its own line
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        val (iconRes, visibilityText) = when(recipe.visibility.lowercase()) {
                            "followers" -> R.drawable.follower to stringResource(R.string.visibility_followers)
                            "private" -> R.drawable.privatevis to stringResource(R.string.visibility_private)
                            else -> R.drawable.publicvis to stringResource(R.string.visibility_public)
                        }

                        Icon(
                            painter = painterResource(id = iconRes),
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = visibilityText,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Stats Grid
                    Row(modifier = Modifier.fillMaxWidth()) {
                        StatItem(R.drawable.ic_fire, "${recipe.calories} ${stringResource(R.string.label_kcal_suffix)}", Modifier.weight(1f))
                        StatItem(R.drawable.ic_clock, "${recipe.time} ${stringResource(R.string.label_mins_suffix)}", Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        val skillLabel = when(recipe.cookingSkill) {
                            "Beginner" -> stringResource(R.string.skill_beginner)
                            "Intermediate" -> stringResource(R.string.skill_intermediate)
                            "Master/Expert" -> stringResource(R.string.skill_expert)
                            else -> recipe.cookingSkill
                        }
                        StatItem(R.drawable.skill, skillLabel, Modifier.weight(1f)) 
                        StatItem(R.drawable.dollar_symbol, stringResource(R.string.currency_prefix) + recipe.estimatedBudget, Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Author Section
                    val author = viewModel.recipeAuthor
                    // Prioritize recipe's own cached info, then live session, then join result
                    val displayAuthorName = (recipe.authorName ?: (if (isMyRecipe && user != null) user.name else (author?.name ?: recipe.authorInfo?.name))) ?: stringResource(R.string.unknown_author)
                    val displayAuthorImage = (recipe.authorImageUrl ?: (if (isMyRecipe && user != null) user.profilePicUrl else (author?.profilePicUrl ?: recipe.authorInfo?.profile_pic_url)))

                    val authorBitmap = remember(displayAuthorImage) {
                        if (!displayAuthorImage.isNullOrBlank() && !displayAuthorImage.startsWith("http")) {
                            try {
                                val imageBytes = android.util.Base64.decode(displayAuthorImage, android.util.Base64.DEFAULT)
                                android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                            } catch (e: Exception) { null }
                        } else null
                    }

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
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (authorBitmap != null) {
                                Image(
                                    bitmap = authorBitmap.asImageBitmap(),
                                    contentDescription = "Author Profile",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else if (!displayAuthorImage.isNullOrBlank()) {
                                AsyncImage(
                                    model = displayAuthorImage,
                                    contentDescription = "Author Profile",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_outline_account_circle),
                                    contentDescription = "Default Author Profile",
                                    modifier = Modifier.fillMaxSize(),
                                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                        }
                        
                        Column(
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .weight(1f)
                        ) {
                            Text(
                                text = stringResource(R.string.label_recipe_by),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = displayAuthorName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (!isMyRecipe && user != null) {
                            Spacer(modifier = Modifier.width(12.dp))
                            val status = followViewModel.followStatus
                            val buttonText = when (status) {
                                null -> stringResource(R.string.btn_follow)
                                "PENDING" -> stringResource(R.string.btn_request_sent)
                                "ACCEPTED" -> stringResource(R.string.btn_unfollow)
                                else -> stringResource(R.string.btn_follow)
                            }
                            val buttonColor = if (status == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary

                            Button(
                                onClick = {
                                    recipe.author_id?.let { aid ->
                                        user.id?.let { uid ->
                                            followViewModel.toggleFollow(uid, aid)
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                                modifier = Modifier
                                    .height(32.dp)
                                    .padding(end = 4.dp)
                            ) {
                                Text(buttonText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(20.dp))

                    // Description (Hidden if empty)
                    if (recipe.recipeDescription.isNotBlank()) {
                        Text(text = stringResource(R.string.label_description_optional), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        var isExpanded by remember { mutableStateOf(false) }
                        Text(
                            text = recipe.recipeDescription,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp,
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .animateContentSize()
                                .clickable { isExpanded = !isExpanded },
                            maxLines = if (isExpanded) Int.MAX_VALUE else 5,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!isExpanded && (recipe.recipeDescription.length > 150 || recipe.recipeDescription.count { it == '\n' } >= 5)) {
                            Text(
                                text = stringResource(R.string.msg_more),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
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
                        Text(text = stringResource(R.string.label_ingredients_list), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
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
                    Text(text = stringResource(R.string.label_recipe_steps), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
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

    // Offline Share Dialog
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
                    Text(stringResource(R.string.title_no_internet))
                }
            },
            text = { Text(stringResource(R.string.msg_no_internet_delete_warning)) }, // Adjust string if needed
            confirmButton = {
                TextButton(onClick = { showOfflineShareDialog = false }) {
                    Text(stringResource(R.string.ok), fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Offline Planner Dialog
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
                    Text(stringResource(R.string.title_no_internet))
                }
            },
            text = { Text(stringResource(R.string.msg_wifi_required_planner)) },
            confirmButton = {
                TextButton(onClick = { showOfflinePlannerDialog = false }) {
                    Text(stringResource(R.string.ok), fontWeight = FontWeight.Bold)
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
                        Text(stringResource(R.string.title_no_internet))
                    }
                },
                text = { Text(stringResource(R.string.msg_no_internet_delete_warning)) },
                confirmButton = {
                    TextButton(onClick = { recipeToDelete = null }) {
                        Text(stringResource(R.string.ok), fontWeight = FontWeight.Bold)
                    }
                }
            )
        } else {
            AlertDialog(
                onDismissRequest = { recipeToDelete = null },
                title = { Text(stringResource(R.string.dialog_delete_recipe_title)) },
                text = { Text(stringResource(R.string.dialog_delete_recipe_msg, recipeToDelete?.recipeName ?: "")) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val rid = recipeToDelete?.recipe_id
                            val uid = user?.id
                            if (rid != null && uid != null) {
                                viewModel.deleteRecipe(rid, uid)
                                navController.popBackStack() // Go back after successful deletion
                            }
                            recipeToDelete = null
                        }
                    ) {
                        Text(stringResource(R.string.dialog_delete_recipe_confirm), color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { recipeToDelete = null }) {
                        Text(stringResource(R.string.dialog_cancel), color = Color.Gray)
                    }
                }
            )
        }
    }

    // Share Preview Dialog
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
            tint = MaterialTheme.colorScheme.primary // Consistent Icon Color
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
