package com.example.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch

fun Modifier.zoomableOnPinchReset(): Modifier = composed {
    val scaleAnim = remember { Animatable(1f) }
    val translationXAnim = remember { Animatable(0f) }
    val translationYAnim = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    this.pointerInput(Unit) {
        awaitEachGesture {
            var active = false
            awaitFirstDown(requireUnconsumed = false)
            do {
                val event = awaitPointerEvent()
                val changes = event.changes
                
                if (changes.size >= 2) {
                    active = true
                    val zoom = event.calculateZoom()
                    val pan = event.calculatePan()
                    
                    changes.forEach { it.consume() }
                    
                    coroutineScope.launch {
                        val newScale = (scaleAnim.value * zoom).coerceIn(1f, 5f)
                        scaleAnim.snapTo(newScale)
                        translationXAnim.snapTo(translationXAnim.value + pan.x * newScale)
                        translationYAnim.snapTo(translationYAnim.value + pan.y * newScale)
                    }
                } else if (active && changes.any { it.pressed }) {
                    changes.forEach { it.consume() }
                }
                
                val anyPressed = changes.any { it.pressed }
            } while (anyPressed)
            
            coroutineScope.launch {
                scaleAnim.animateTo(1f)
                translationXAnim.animateTo(0f)
                translationYAnim.animateTo(0f)
            }
        }
    }
    .graphicsLayer {
        scaleX = scaleAnim.value
        scaleY = scaleAnim.value
        translationX = translationXAnim.value
        translationY = translationYAnim.value
    }
}
