package com.royaltreasure.asmr.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.royaltreasure.asmr.model.JewelleryItem
import com.royaltreasure.asmr.state.GameViewModel
import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.math.roundToInt

private val GoldAccent = Color(0xFFD4AF37)
private val DeepBrown = Color(0xFF4A2C18)
private val CreamBg = Color(0xFFFFF6EB)

@Composable
fun GameScreen(viewModel: GameViewModel) {
    val level = viewModel.level
    val density = LocalDensity.current

    var canvasRect by remember { mutableStateOf(Rect.Zero) }
    var draggingItem by remember { mutableStateOf<JewelleryItem?>(null) }
    var fingerRootPos by remember { mutableStateOf(Offset.Zero) }

    val liftOffsetPx = with(density) { 48.dp.toPx() }

    // Calculate exact rendered image rectangle inside ContentScale.Fit container (accounts for letterboxing)
    val imageBounds = remember(canvasRect, level.imageAspectRatio) {
        if (canvasRect.width <= 0f || canvasRect.height <= 0f) Rect.Zero
        else {
            val scale = min(
                canvasRect.width / level.imageAspectRatio,
                canvasRect.height / 1f
            )
            val drawnW = level.imageAspectRatio * scale
            val drawnH = 1f * scale
            val left = canvasRect.left + (canvasRect.width - drawnW) / 2f
            val top = canvasRect.top + (canvasRect.height - drawnH) / 2f
            Rect(left, top, left + drawnW, top + drawnH)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            viewModel.tickTimer()
        }
    }

    LaunchedEffect(viewModel.lastSnapEffectPos) {
        if (viewModel.lastSnapEffectPos != null) {
            delay(650)
            viewModel.clearSnapEffect()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CreamBg)
    ) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            // Top Bar: Progress, Reference Preview, Hint, Sound, Pause, Dev Mode
            TopControlBar(
                viewModel = viewModel,
                onToggleRef = { viewModel.showReferenceOverlay = !viewModel.showReferenceOverlay }
            )

            // Center Princess Canvas
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .onGloballyPositioned { canvasRect = it.boundsInRoot() },
                contentAlignment = Alignment.Center
            ) {
                // Base Princess Image
                Image(
                    painter = painterResource(
                        if (viewModel.showReferenceOverlay) level.referenceImageRes else level.baseImageRes
                    ),
                    contentDescription = "Princess",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )

                // Placed Jewellery Items rendered at exact normalized coordinates
                if (imageBounds != Rect.Zero) {
                    level.items.filter { it.isPlaced }.sortedBy { it.zIndex }.forEach { item ->
                        val itemW = imageBounds.width * item.widthRatio
                        val itemH = imageBounds.height * item.heightRatio
                        val leftPx = (imageBounds.left - canvasRect.left) + (item.targetX * imageBounds.width) - (itemW / 2f)
                        val topPx = (imageBounds.top - canvasRect.top) + (item.targetY * imageBounds.height) - (itemH / 2f)

                        Image(
                            painter = painterResource(item.resId),
                            contentDescription = item.name,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .offset { IntOffset(leftPx.roundToInt(), topPx.roundToInt()) }
                                .size(
                                    width = with(density) { itemW.toDp() },
                                    height = with(density) { itemH.toDp() }
                                )
                                .rotate(item.rotation)
                                .zIndex(item.zIndex),
                            contentScale = ContentScale.Fit
                        )
                    }

                    // Hint Pulse Indicator
                    val hintId = viewModel.activeHintItemId
                    if (hintId != null) {
                        val hintItem = level.items.find { it.id == hintId }
                        if (hintItem != null) {
                            val hx = (imageBounds.left - canvasRect.left) + (hintItem.targetX * imageBounds.width)
                            val hy = (imageBounds.top - canvasRect.top) + (hintItem.targetY * imageBounds.height)
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .offset { IntOffset((hx - 22.dp.toPx()).roundToInt(), (hy - 22.dp.toPx()).roundToInt()) }
                                    .size(44.dp)
                                    .background(Color(0x66FFD700), CircleShape)
                                    .border(2.5.dp, GoldAccent, CircleShape)
                                    .zIndex(20f)
                            )
                        }
                    }

                    // Developer / Test Mode Overlay (Target Anchor Points & Coordinates)
                    if (viewModel.isDevMode) {
                        Canvas(modifier = Modifier.fillMaxSize().zIndex(30f)) {
                            level.items.forEach { item ->
                                val cx = (imageBounds.left - canvasRect.left) + (item.targetX * imageBounds.width)
                                val cy = (imageBounds.top - canvasRect.top) + (item.targetY * imageBounds.height)
                                val radiusPx = item.snapRadius * imageBounds.width
                                drawCircle(
                                    color = if (item.isPlaced) Color.Green.copy(alpha = 0.35f) else Color.Red.copy(alpha = 0.25f),
                                    radius = radiusPx,
                                    center = Offset(cx, cy)
                                )
                                drawCircle(
                                    color = Color.Blue,
                                    radius = 6f,
                                    center = Offset(cx, cy)
                                )
                            }
                        }
                        level.items.forEach { item ->
                            val cx = (imageBounds.left - canvasRect.left) + (item.targetX * imageBounds.width)
                            val cy = (imageBounds.top - canvasRect.top) + (item.targetY * imageBounds.height)
                            Text(
                                text = "${item.id}\n(${"%.2f".format(item.targetX)}, ${"%.2f".format(item.targetY)})",
                                color = Color.White,
                                fontSize = 9.sp,
                                lineHeight = 10.sp,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .offset { IntOffset(cx.roundToInt(), cy.roundToInt()) }
                                    .background(Color(0xAA000000), RoundedCornerShape(4.dp))
                                    .padding(3.dp)
                                    .zIndex(31f)
                            )
                        }
                    }
                }
            }

            // Bottom Jewellery Tray
            val remainingItems = level.items.filter { !it.isPlaced }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF3E2C7))
                    .border(1.dp, GoldAccent)
                    .padding(vertical = 10.dp, horizontal = 12.dp)
            ) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                    userScrollEnabled = draggingItem == null
                ) {
                    items(remainingItems, key = { it.id }) { item ->
                        var slotTopLeft by remember { mutableStateOf(Offset.Zero) }
                        val isBeingDragged = draggingItem?.id == item.id

                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .shadow(3.dp, RoundedCornerShape(14.dp))
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White)
                                .border(2.dp, GoldAccent, RoundedCornerShape(14.dp))
                                .onGloballyPositioned { slotTopLeft = it.boundsInRoot().topLeft }
                                .pointerInput(item.id, viewModel.isPaused) {
                                    if (!viewModel.isPaused && !viewModel.isGameOver) {
                                        detectDragGestures(
                                            onDragStart = { localOffset ->
                                                draggingItem = item
                                                fingerRootPos = slotTopLeft + localOffset
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                fingerRootPos += dragAmount
                                            },
                                            onDragEnd = {
                                                val dropCenter = Offset(fingerRootPos.x, fingerRootPos.y - liftOffsetPx)
                                                viewModel.tryPlaceItem(item.id, dropCenter, imageBounds)
                                                draggingItem = null
                                            },
                                            onDragCancel = { draggingItem = null }
                                        )
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (!isBeingDragged) {
                                Image(
                                    painter = painterResource(item.resId),
                                    contentDescription = item.name,
                                    modifier = Modifier.fillMaxSize().padding(8.dp),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }
                }
            }
        }

        // Smooth Floating Dragged Jewellery Item following finger
        val activeDrag = draggingItem
        if (activeDrag != null && imageBounds != Rect.Zero) {
            val dragW = (imageBounds.width * activeDrag.widthRatio).coerceAtLeast(with(density) { 64.dp.toPx() })
            val dragH = (imageBounds.height * activeDrag.heightRatio).coerceAtLeast(with(density) { 64.dp.toPx() })

            Image(
                painter = painterResource(activeDrag.resId),
                contentDescription = activeDrag.name,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset {
                        IntOffset(
                            (fingerRootPos.x - dragW / 2f).roundToInt(),
                            (fingerRootPos.y - liftOffsetPx - dragH / 2f).roundToInt()
                        )
                    }
                    .size(
                        width = with(density) { dragW.toDp() },
                        height = with(density) { dragH.toDp() }
                    )
                    .graphicsLayer { scaleX = 1.08f; scaleY = 1.08f }
                    .zIndex(100f),
                contentScale = ContentScale.Fit
            )
        }

        // ASMR Snap Checkmark Sparkle
        viewModel.lastSnapEffectPos?.let { pos ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset { IntOffset((pos.x - 22.dp.toPx()).roundToInt(), (pos.y - 22.dp.toPx()).roundToInt()) }
                    .size(44.dp)
                    .background(Color(0xFF2E7D32), CircleShape)
                    .border(2.dp, Color.White, CircleShape)
                    .zIndex(90f),
                contentAlignment = Alignment.Center
            ) {
                Text("✔", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Pause Overlay
        if (viewModel.isPaused) {
            AlertDialog(
                onDismissRequest = { viewModel.isPaused = false },
                title = { Text("GAME PAUSED", color = DeepBrown, fontWeight = FontWeight.Bold) },
                text = { Text("Take a break or reset the princess makeover.", color = DeepBrown) },
                confirmButton = {
                    Button(onClick = { viewModel.isPaused = false }) { Text("Resume") }
                },
                dismissButton = {
                    OutlinedButton(onClick = { viewModel.resetLevel() }) { Text("Reset") }
                }
            )
        }

        // Game over: retry keeps the current unlocked level.
        if (viewModel.isGameOver) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("👑 TRY AGAIN", color = Color(0xFFB3261E), fontWeight = FontWeight.Black) },
                text = { Text("Time is up or all 5 hearts were used. Your level progress is ready for another attempt.", color = DeepBrown) },
                confirmButton = {
                    Button(onClick = { viewModel.resetLevel() }, colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)) {
                        Text("RETRY LEVEL", color = DeepBrown, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // Level Completion Celebration Dialog
        if (viewModel.isLevelCompleted) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("✨ ROYAL MAKEOVER COMPLETE!", color = Color(0xFF2E7D32), fontWeight = FontWeight.Black) },
                text = { Text("All ${viewModel.totalCount}/${viewModel.totalCount} jewellery pieces matched the royal portrait!", color = DeepBrown) },
                confirmButton = {
                    Button(
                        onClick = { viewModel.nextLevel() },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
                    ) {
                        Text(if (viewModel.currentLevelNumber >= 10) "RESTART WORLD" else "NEXT LEVEL  ➜", color = DeepBrown, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}

@Composable
private fun TopControlBar(viewModel: GameViewModel, onToggleRef: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Progress Badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(DeepBrown)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = "👑 ${viewModel.placedCount}/${viewModel.totalCount}",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SmallActionChip(if (viewModel.showReferenceOverlay) "👁 Hide" else "👁 Ref") { onToggleRef() }
            SmallActionChip("💡 Hint") { viewModel.triggerHint() }
            SmallActionChip("↻ Reset") { viewModel.resetLevel() }
            SmallActionChip(if (viewModel.isSoundOn) "🔊" else "🔇") { viewModel.isSoundOn = !viewModel.isSoundOn }
            SmallActionChip("⏸") { viewModel.isPaused = true }
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("❤️ ${viewModel.lives}/5", color = Color(0xFFB3261E), fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("⏱ %02d:%02d".format(viewModel.secondsLeft / 60, viewModel.secondsLeft % 60),
            color = if (viewModel.secondsLeft <= 30) Color(0xFFB3261E) else DeepBrown,
            fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
        Text("LEVEL ${viewModel.currentLevelNumber}/10", color = DeepBrown, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun SmallActionChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .border(1.dp, GoldAccent, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = DeepBrown, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
