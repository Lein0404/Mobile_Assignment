package com.example.foodieheal.Recipe.View

import androidx.annotation.DrawableRes
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
        modifier = modifier.height(260.dp).clickable { onClick() } // 🌟 Reduced height to feel compact
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween // 🌟 Distributes space naturally
        ) {
            Column {
                Box(modifier = Modifier.fillMaxWidth().height(135.dp).background(MaterialTheme.colorScheme.surfaceVariant)) { // 🌟 Shorter image
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
                                modifier = Modifier.size(50.dp),
                                alpha = 0.3f,
                                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onTertiaryContainer)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(26.dp)
                    ) {
                        IconButton(onClick = onBookmarkClick) {
                            Image(
                                painter = painterResource(
                                    id = if (isBookmarked) R.drawable.bookmark_fill else R.drawable.bookmark
                                ),
                                contentDescription = "Bookmark",
                                modifier = Modifier.size(14.dp),
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
                        modifier = Modifier.align(Alignment.TopStart).padding(8.dp).size(26.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_recipe),
                            contentDescription = "Add to Planner",
                            modifier = Modifier.padding(5.dp).clickable { onAddClick() },
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = recipe.recipeName,
                            fontSize = 13.sp, // 🌟 Slightly smaller font for tighter look
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (showMenu) {
                            IconButton(onClick = { expanded = true }, modifier = Modifier.size(18.dp)) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_vertical_more),
                                    contentDescription = "Menu",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                    DropdownMenuItem(
                                        text = { Text("Edit Recipe", fontSize = 12.sp) },
                                        onClick = { expanded = false; onEditClick() },
                                        leadingIcon = { Icon(painterResource(id = R.drawable.ic_square_edit), null, modifier = Modifier.size(16.dp)) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Share Recipe", fontSize = 12.sp) },
                                        onClick = { expanded = false; onShareClick(recipe) },
                                        leadingIcon = { Icon(painterResource(id = R.drawable.ic_share), null, modifier = Modifier.size(16.dp)) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Delete Recipe", color = MaterialTheme.colorScheme.error, fontSize = 12.sp) },
                                        onClick = { expanded = false; onDeleteClick() },
                                        leadingIcon = { Icon(painterResource(id = R.drawable.ic_delete), null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp)) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(start = 10.dp, end = 10.dp, bottom = 10.dp)) {
                // 🌟 Compact Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_fire),
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${recipe.calories} kcal",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_clock),
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${recipe.time} mins",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 🌟 Author Row
                val authorToDisplay = if (recipe.author_id == currentUser?.customId && currentUser != null) {
                    currentUser.name
                } else {
                    recipe.authorName ?: recipe.authorInfo?.name ?: "Chef"
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
                                modifier = Modifier.size(10.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = authorToDisplay,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
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
