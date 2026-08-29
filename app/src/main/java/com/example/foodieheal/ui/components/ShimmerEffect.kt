package com.example.foodieheal.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun Modifier.shimmerEffect(): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val shimmerColors = listOf(
        Color(0xFFE0E0E0),
        Color(0xFFF5F5F5),
        Color(0xFFE0E0E0)
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 300f, translateAnim - 300f),
        end   = Offset(translateAnim + 300f, translateAnim + 300f)
    )

    return this.background(brush)
}

@Composable
fun ShimmerLine(
    modifier: Modifier = Modifier,
    height: Dp = 14.dp,
    widthFraction: Float = 1f,
    cornerRadius: Dp = 6.dp
) {
    Box(
        modifier = modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
            .shimmerEffect()
    )
}

@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    height: Dp = 100.dp,
    cornerRadius: Dp = 12.dp
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
            .shimmerEffect()
    )
}

@Composable
fun WalletScreenSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ShimmerBox(height = 160.dp, cornerRadius = 20.dp)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ShimmerBox(modifier = Modifier.weight(1f), height = 44.dp, cornerRadius = 12.dp)
            ShimmerBox(modifier = Modifier.weight(1f), height = 44.dp, cornerRadius = 12.dp)
        }

        ShimmerLine(height = 16.dp, widthFraction = 0.4f)

        repeat(4) { TransactionRowSkeleton() }
    }
}

@Composable
private fun TransactionRowSkeleton(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(modifier = Modifier.size(44.dp).clip(CircleShape).shimmerEffect())
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ShimmerLine(height = 14.dp, widthFraction = 0.6f)
            ShimmerLine(height = 11.dp, widthFraction = 0.4f)
        }
        ShimmerLine(modifier = Modifier.width(56.dp), height = 14.dp)
    }
}

@Composable
fun PaymentScreenSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ShimmerBox(height = 180.dp, cornerRadius = 16.dp)

        ShimmerLine(height = 16.dp, widthFraction = 0.45f)

        repeat(2) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(modifier = Modifier.size(20.dp).clip(CircleShape).shimmerEffect())
                Box(modifier = Modifier.size(24.dp).clip(CircleShape).shimmerEffect())
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ShimmerLine(height = 14.dp, widthFraction = 0.55f)
                    ShimmerLine(height = 11.dp, widthFraction = 0.35f)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        ShimmerBox(height = 50.dp, cornerRadius = 14.dp)
    }
}

@Composable
fun HiringChefListSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ShimmerBox(height = 48.dp, cornerRadius = 24.dp)

        repeat(3) { ChefCardSkeleton() }
    }
}

@Composable
private fun ChefCardSkeleton(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)).shimmerEffect())
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ShimmerLine(height = 16.dp, widthFraction = 0.7f)
            ShimmerLine(height = 12.dp, widthFraction = 0.5f)
            ShimmerLine(height = 12.dp, widthFraction = 0.4f)
        }
    }
}

@Composable
fun AppointmentListSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(3) { ShimmerBox(height = 110.dp, cornerRadius = 16.dp) }
    }
}
