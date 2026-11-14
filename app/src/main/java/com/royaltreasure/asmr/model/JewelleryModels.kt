package com.royaltreasure.asmr.model

import androidx.annotation.DrawableRes

data class JewelleryItem(
    val id: String,
    val name: String,
    @DrawableRes val resId: Int,
    val targetX: Float,      // Normalized X (0.0f to 1.0f relative to girl image)
    val targetY: Float,      // Normalized Y (0.0f to 1.0f relative to girl image)
    val widthRatio: Float,   // Width relative to displayed girl image width
    val heightRatio: Float,  // Height relative to displayed girl image height
    val rotation: Float = 0f,
    val snapRadius: Float = 0.08f, // Normalized snap threshold
    val zIndex: Float = 1f,
    val isPlaced: Boolean = false
)

data class LevelConfig(
    val id: Int,
    val title: String,
    @DrawableRes val baseImageRes: Int,
    @DrawableRes val referenceImageRes: Int,
    val imageAspectRatio: Float, // width / height (e.g., 9f / 16f)
    val items: List<JewelleryItem>
)
