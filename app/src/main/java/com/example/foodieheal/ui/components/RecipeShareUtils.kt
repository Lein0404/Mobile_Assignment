package com.example.foodieheal.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.foodieheal.R
import com.example.foodieheal.Recipe.Model.Recipe
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@Composable
fun ShareableRecipeCard(
    recipe: Recipe,
    authorName: String?,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .width(400.dp) 
            .wrapContentHeight() 
    ) {
        Column(
            modifier = Modifier
                .background(Color.White)
                .padding(bottom = 24.dp)
        ) {
            if (!recipe.recipeImageUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = recipe.recipeImageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_recipe),
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                }
            }

            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = recipe.recipeName,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black
                )
                Text(
                    text = "by ${authorName ?: "Chef"}",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    ShareStatItem(R.drawable.ic_fire, "${recipe.calories} kcal", Modifier.weight(1f))
                    ShareStatItem(R.drawable.ic_clock, "${recipe.time} mins", Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    ShareStatItem(R.drawable.skill, recipe.cookingSkill, Modifier.weight(1f))
                    ShareStatItem(R.drawable.dollar_symbol, "RM ${recipe.estimatedBudget}", Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(20.dp))

                if (recipe.recipeDescription.isNotBlank()) {
                    Text("Description", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                    Text(
                        text = recipe.recipeDescription,
                        fontSize = 13.sp,
                        color = Color.DarkGray,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }

                Text("Ingredients", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                recipe.ingredients.forEach { ingredient ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "• ${ingredient.name}", fontSize = 13.sp, color = Color.DarkGray)
                        Text(text = "${ingredient.displayQuantity} ${ingredient.unit}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text("Recipe Steps", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                recipe.recipeStep.split("\n").forEachIndexed { index, step ->
                    if (step.isNotBlank()) {
                        Row(modifier = Modifier.padding(vertical = 6.dp)) {
                            Text(text = "${index + 1}. ", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            Text(text = step.trim(), fontSize = 13.sp, color = Color.DarkGray)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
                
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "SHARED VIA FOODIE HEAL",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ShareStatItem(icon: Int, label: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = Color(0XFFEC5E3A)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, fontSize = 12.sp, color = Color.DarkGray)
    }
}

/**
 * 🌟 Reusable Dialog for Sharing Recipes as Images
 */
@Composable
fun ShareRecipeDialog(
    recipe: Recipe,
    authorName: String?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val graphicsLayer = rememberGraphicsLayer()
    var isSharing by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isSharing) onDismiss() },
        title = { Text("Share Recipe Image") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Box(
                    modifier = Modifier
                        .wrapContentSize()
                        .drawWithContent {
                            graphicsLayer.record {
                                this@drawWithContent.drawContent()
                            }
                            drawContent()
                        }
                ) {
                    ShareableRecipeCard(
                        recipe = recipe,
                        authorName = authorName ?: "Chef"
                    )
                }
                if (isSharing) {
                    Spacer(modifier = Modifier.height(16.dp))
                    CircularProgressIndicator()
                    Text("Generating image...", fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isSharing = true
                    coroutineScope.launch {
                        try {
                            shareRecipeAsImage(
                                context = context,
                                graphicsLayer = graphicsLayer,
                                recipeName = recipe.recipeName
                            )
                        } catch (_: Exception) {
                        } finally {
                            isSharing = false
                            onDismiss()
                        }
                    }
                },
                enabled = !isSharing
            ) {
                Text("Share Now")
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }, enabled = !isSharing) {
                Text("Cancel")
            }
        }
    )
}

suspend fun shareRecipeAsImage(
    context: Context,
    graphicsLayer: GraphicsLayer,
    recipeName: String
) {
    val bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
    
    val cachePath = File(context.cacheDir, "shares")
    cachePath.mkdirs()
    val file = File(cachePath, "recipe_share_${System.currentTimeMillis()}.png")
    val stream = FileOutputStream(file)
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
    stream.close()

    val contentUri: Uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )

    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_STREAM, contentUri)
        putExtra(Intent.EXTRA_TEXT, "Check out this recipe for $recipeName on Foodie Heal!")
        type = "image/png"
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share Recipe Image"))
}
