package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Modern 3D Tactile Pressable Modifier
 * Provides physical depth press feedback with spring physics and scale compression.
 */
@Composable
fun Modifier.threeDPressable(
    enabled: Boolean = true,
    pressedScale: Float = 0.96f,
    onClick: (() -> Unit)? = null
): Modifier {
    if (onClick == null) return this
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed && enabled) pressedScale else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "3d_press_scale"
    )

    val animatedTranslationY by animateDpAsState(
        targetValue = if (isPressed && enabled) 2.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "3d_press_translation_y"
    )

    return this
        .graphicsLayer {
            scaleX = animatedScale
            scaleY = animatedScale
            translationY = animatedTranslationY.toPx()
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = { onClick() }
        )
}

/**
 * Modern 3D Spatial Depth Modifier
 * Applies perspective camera depth, subtle 3D tilt, and dynamic drop shadow.
 */
fun Modifier.threeDDepthCard(
    elevation: Dp = 6.dp,
    shape: Shape = RoundedCornerShape(16.dp),
    ambientColor: Color = Color.Black.copy(alpha = 0.15f),
    spotColor: Color = Color.Black.copy(alpha = 0.25f),
    tiltX: Float = 0f,
    tiltY: Float = 0f
): Modifier {
    return this
        .shadow(
            elevation = elevation,
            shape = shape,
            clip = false,
            ambientColor = ambientColor,
            spotColor = spotColor
        )
        .graphicsLayer {
            cameraDistance = 16f * density
            rotationX = tiltX
            rotationY = tiltY
        }
}

/**
 * Modern 3D Glassmorphic Sheen Modifier
 * Adds subtle gradient rim light and surface sheen to simulate 3D polished glass.
 */
fun Modifier.glassmorphic3D(
    shape: Shape = RoundedCornerShape(16.dp),
    backgroundColor: Color = Color.White.copy(alpha = 0.85f),
    borderColor: Color = Color.White.copy(alpha = 0.6f)
): Modifier {
    return this
        .clip(shape)
        .background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    backgroundColor,
                    backgroundColor.copy(alpha = backgroundColor.alpha * 0.92f)
                )
            ),
            shape = shape
        )
        .border(
            width = 1.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    borderColor,
                    borderColor.copy(alpha = 0.15f)
                )
            ),
            shape = shape
        )
}
