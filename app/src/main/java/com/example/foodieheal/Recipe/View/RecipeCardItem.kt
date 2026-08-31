package com.example.foodieheal.Recipe.View

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import coil.compose.AsyncImage
import com.example.foodieheal.R
import com.example.foodieheal.Recipe.Model.Recipe
import com.example.foodieheal.User.Model.User

@Composable
fun RecipeCardItem(
    recipe: Recipe,
    modifier: Modifier = Modifier,
    currentUser: User? = null, // 🌟 Added for live name sync
    showMenu: Boolean = false,
    isBookmarked: Boolean = false,
    isSelected: Boolean = false, // 🌟 Added for Selection Mode
    isSelectionMode: Boolean = false, // 🌟 Added for Selection Mode
    onBookmarkClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onEditClick: () -> Unit = {},
    onShareClick: (Recipe) -> Unit = {}, // 🌟 Added share callback
    onAddClick: () -> Unit = {},
    onClick: () -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = if (isSelectionMode && isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = modifier.height(310.dp).clickable { onClick() }
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(150.dp).background(MaterialTheme.colorScheme.surfaceVariant)) {
                if (!recipe.recipeImageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = recipe.recipeImageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)),
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
                            modifier = Modifier.size(60.dp),
                            alpha = 0.3f,
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onTertiaryContainer)
                        )
                    }
                }

                // 🌟 Selection Check Badge
                if (isSelectionMode && isSelected) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                        modifier = Modifier
                            .padding(10.dp)
                            .size(24.dp)
                            .align(Alignment.TopEnd)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_check),
                            contentDescription = stringResource(R.string.desc_checkbox_checked),
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                }

                if (!isSelectionMode) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                        modifier = Modifier.align(Alignment.TopEnd).padding(10.dp).size(28.dp)
                    ) {
                        IconButton(onClick = onBookmarkClick) {
                            Image(
                                painter = painterResource(
                                    id = if (isBookmarked) R.drawable.bookmark_fill else R.drawable.bookmark
                                ),
                                contentDescription = stringResource(R.string.bookmark_chef),
                                modifier = Modifier.size(16.dp),
                                colorFilter = ColorFilter.tint(
                                    if (isBookmarked) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                        modifier = Modifier.align(Alignment.TopStart).padding(10.dp).size(28.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_recipe),
                            contentDescription = stringResource(R.string.menu_add_to_planner),
                            modifier = Modifier.padding(6.dp).clickable { onAddClick() },
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = recipe.recipeName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (showMenu) {
                        Box {
                            IconButton(onClick = { expanded = true }, modifier = Modifier.size(20.dp)) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_vertical_more),
                                    contentDescription = stringResource(R.string.more_options),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.menu_edit_recipe)) },
                                    onClick = { expanded = false; onEditClick() },
                                    leadingIcon = { Icon(painterResource(id = R.drawable.ic_square_edit), null, modifier = Modifier.size(18.dp)) }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.menu_share_recipe)) },
                                    onClick = { expanded = false; onShareClick(recipe) },
                                    leadingIcon = { Icon(painterResource(id = R.drawable.ic_share), null, modifier = Modifier.size(18.dp)) }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.menu_delete_recipe), color = MaterialTheme.colorScheme.error) },
                                    onClick = { expanded = false; onDeleteClick() },
                                    leadingIcon = { Icon(painterResource(id = R.drawable.ic_delete), null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)) }
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_fire),
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.format_recipe_calories, recipe.calories),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_clock),
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.format_recipe_duration, recipe.time),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }

                    val authorToDisplay = if (recipe.author_id == currentUser?.customId && currentUser != null) {
                        currentUser.name
                    } else {
                        recipe.authorName ?: recipe.authorInfo?.name ?: stringResource(R.string.default_chef_name)
                    }

                    val authorImageToDisplay = if (recipe.author_id == currentUser?.customId && currentUser != null) {
                        currentUser.profilePicUrl
                    } else {
                        recipe.authorImageUrl ?: recipe.authorInfo?.profile_pic_url
                    }

                    if (!authorToDisplay.isNullOrEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!authorImageToDisplay.isNullOrEmpty()) {
                                AsyncImage(
                                    model = authorImageToDisplay,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    painter = painterResource(id = R.drawable.author),
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                            Text(
                                text = authorToDisplay,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(start = 4.dp).weight(1f, fill = false)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilterSectionHeader(@DrawableRes icon: Int, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
