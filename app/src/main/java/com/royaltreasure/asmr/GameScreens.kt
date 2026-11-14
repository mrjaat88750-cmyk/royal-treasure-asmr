package com.royaltreasure.asmr

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
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
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.delay
import kotlin.math.hypot
import kotlin.math.roundToInt

private val Paper = Color(0xFFFFF2D9)
private val Brown = Color(0xFF70452E)
private val Green = Color(0xFF258B68)
private val Red = Color(0xFFD63B47)
private const val AD_ID = "ca-app-pub-3940256099942544/5224354917"

fun playRewardAd(activity: Activity?, onReward: () -> Unit) {
    if (activity == null) { onReward(); return }
    Toast.makeText(activity, "Loading Video Ad...", Toast.LENGTH_SHORT).show()
    RewardedAd.load(activity, AD_ID, AdRequest.Builder().build(), object : RewardedAdLoadCallback() {
        override fun onAdLoaded(ad: RewardedAd) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdFailedToShowFullScreenContent(e: AdError) { onReward() }
            }
            ad.show(activity) { onReward() }
        }
        override fun onAdFailedToLoad(e: LoadAdError) { onReward() }
    })
}

@Composable
fun GameScreen(level: Level, activity: Activity?, onBack: () -> Unit, onWin: () -> Unit) {
    var placed by remember(level.id) { mutableStateOf(setOf<Int>()) }
    var usedIdx by remember(level.id) { mutableStateOf(setOf<Int>()) }
    var lives by remember(level.id) { mutableIntStateOf(5) }
    var seconds by remember(level.id) { mutableIntStateOf(240) }
    var hintId by remember(level.id) { mutableIntStateOf(0) }
    var gameOver by remember(level.id) { mutableStateOf(false) }
    var won by remember(level.id) { mutableStateOf(false) }

    var draggingIdx by remember(level.id) { mutableStateOf<Int?>(null) }
    var fingerRootPos by remember(level.id) { mutableStateOf(Offset.Zero) }
    val slotCenters = remember(level.id) { mutableStateMapOf<Int, Offset>() }
    var checkMarkPos by remember(level.id) { mutableStateOf<Offset?>(null) }
    var trayTopY by remember(level.id) { mutableFloatStateOf(1500f) }

    val density = LocalDensity.current
    val itemHalfPx = with(density) { 45.dp.toPx() }
    val liftAboveFingerPx = with(density) { 55.dp.toPx() }

    LaunchedEffect(seconds, gameOver, won) {
        if (seconds > 0 && !gameOver && !won) { delay(1000); seconds-- }
        else if (seconds <= 0 && !won) gameOver = true
    }

    LaunchedEffect(checkMarkPos) {
        if (checkMarkPos != null) {
            delay(700)
            checkMarkPos = null
        }
    }

    fun handleDrop(trayIdx: Int, dropRootPoint: Offset) {
        val resId = level.tray.getOrNull(trayIdx) ?: return
        // आभूषण जहाँ दिख रहा है (उँगली से थोड़ा ऊपर), वहाँ का पॉइंट चेक करें
        val visualCenter = Offset(dropRootPoint.x, dropRootPoint.y - liftAboveFingerPx)

        var matchedSlot: Slot? = null
        var minDistance = Float.MAX_VALUE

        for (slot in level.slots) {
            if (slot.id !in placed) {
                val center = slotCenters[slot.id] ?: continue
                val dist = hypot(visualCenter.x - center.x, visualCenter.y - center.y)
                if (dist < 190f && dist < minDistance) {
                    minDistance = dist
                    matchedSlot = slot
                }
            }
        }

        if (matchedSlot != null && matchedSlot.resId == resId) {
            placed = placed + matchedSlot.id
            usedIdx = usedIdx + trayIdx
            checkMarkPos = slotCenters[matchedSlot.id]
            if (hintId == matchedSlot.id) hintId = 0
            if (placed.size == level.slots.size) won = true
        } else if (dropRootPoint.y < trayTopY - 80f) {
            // सिर्फ तभी लाइफ कटेगी जब खिलाड़ी सच में राजकुमारी के ऊपर गलत जगह छोड़े
            lives--
            if (lives <= 0) gameOver = true
        }
    }

    Box(
        Modifier.fillMaxSize().background(Color(0xFFFFE9EE)),
        contentAlignment = Alignment.Center
    ) {
        // राजकुमारी की मुख्य इमेज और स्लॉट्स
        Box(Modifier.fillMaxHeight().aspectRatio(9f / 16f)) {
            Image(painterResource(level.girlRes), null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)

            level.slots.forEach { s ->
                Box(
                    Modifier
                        .align(BiasAlignment(s.ax, s.ay))
                        .width(s.w)
                        .height(s.w)
                        .onGloballyPositioned { coords ->
                            slotCenters[s.id] = coords.boundsInRoot().center
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (s.id in placed) {
                        Image(painterResource(s.resId), null, Modifier.fillMaxWidth(), contentScale = ContentScale.FillWidth)
                    } else if (hintId == s.id) {
                        Box(
                            Modifier.size(50.dp)
                                .background(Color(0x66FFD54F), CircleShape)
                                .border(2.5.dp, Color(0xFFFFB300), CircleShape)
                        )
                    }
                }
            }
        }

        // सही जगह फिट होने पर हरा ✔ निशान
        checkMarkPos?.let { pos ->
            Box(
                Modifier
                    .offset { IntOffset((pos.x - 24.dp.toPx()).roundToInt(), (pos.y - 24.dp.toPx()).roundToInt()) }
                    .size(48.dp)
                    .shadow(6.dp, CircleShape)
                    .background(Color(0xFF4CAF50), CircleShape)
                    .border(2.dp, Color.White, CircleShape)
                    .zIndex(50f),
                contentAlignment = Alignment.Center
            ) {
                Text("✔", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
            }
        }

        // Top HUD
        Column(Modifier.align(Alignment.TopCenter).fillMaxWidth().statusBarsPadding().padding(14.dp, 8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                    Image(painterResource(R.drawable.ui_circle_btn), null, Modifier.fillMaxSize())
                    Text("‹", color = Brown, fontSize = 20.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.weight(1f))
                Box(Modifier.clip(RoundedCornerShape(50)).background(Color(0x993E2D28)).padding(18.dp, 6.dp)) {
                    Text(level.title, color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp)
                }
                Spacer(Modifier.weight(1f)); Spacer(Modifier.size(42.dp))
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("⏱ %02d:%02d".format(seconds / 60, seconds % 60), Modifier.clip(RoundedCornerShape(50)).background(Color(0xCCFFFFFF)).padding(12.dp, 5.dp), color = Brown, fontWeight = FontWeight.Black, fontSize = 13.sp)
                Text("❤️ $lives/5", Modifier.clip(RoundedCornerShape(50)).background(Color(0xCCFFFFFF)).padding(12.dp, 5.dp), color = if (lives <= 2) Red else Brown, fontWeight = FontWeight.Black, fontSize = 13.sp)
            }
        }

        // Bottom Tray
        Column(
            Modifier.align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(10.dp, 8.dp)
                .onGloballyPositioned { coords ->
                    trayTopY = coords.boundsInRoot().top
                }
        ) {
            val rem = level.tray.indices.filterNot { it in usedIdx }

            LazyRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                userScrollEnabled = draggingIdx == null // ड्रैग करते समय ट्रे स्क्रॉल लॉक रहेगी
            ) {
                items(rem, key = { "${level.id}_$it" }) { idx ->
                    var itemTopLeftRoot by remember { mutableStateOf(Offset.Zero) }
                    val isBeingDragged = draggingIdx == idx

                    Box(
                        Modifier.size(76.dp)
                            .shadow(4.dp, RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFAFFF8EB))
                            .border(2.5.dp, Color(0xFFD9B372), RoundedCornerShape(16.dp))
                            .onGloballyPositioned { coords ->
                                itemTopLeftRoot = coords.boundsInRoot().topLeft
                            }
                            .pointerInput(idx, level.id) {
                                detectDragGestures(
                                    onDragStart = { localOffset ->
                                        draggingIdx = idx
                                        fingerRootPos = itemTopLeftRoot + localOffset
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        fingerRootPos += dragAmount
                                    },
                                    onDragEnd = {
                                        val finalPos = fingerRootPos
                                        draggingIdx = null
                                        handleDrop(idx, finalPos)
                                    },
                                    onDragCancel = {
                                        draggingIdx = null
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (!isBeingDragged) {
                            Image(painterResource(level.tray[idx]), null, Modifier.fillMaxSize().padding(8.dp), contentScale = ContentScale.Fit)
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button({ playRewardAd(activity) { seconds += 60 } }, Modifier.weight(1f).height(46.dp), shape = RoundedCornerShape(13.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3FA0F2))) { Text("🎬 +60s", fontSize = 11.sp, fontWeight = FontWeight.Black) }
                Button({ playRewardAd(activity) { hintId = level.slots.firstOrNull { it.id !in placed }?.id ?: 0 } }, Modifier.weight(1f).height(46.dp), shape = RoundedCornerShape(13.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF6A52E))) { Text("🎬 Tips", fontSize = 11.sp, fontWeight = FontWeight.Black) }
                Button({ playRewardAd(activity) { lives = (lives + 1).coerceAtMost(5) } }, Modifier.weight(1f).height(46.dp), shape = RoundedCornerShape(13.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF68C943))) { Text("🎬 +1 Life", fontSize = 11.sp, fontWeight = FontWeight.Black) }
            }
        }

        // ठीक उँगली के ऊपर उड़ने वाला आभूषण
        val activeDragIdx = draggingIdx
        if (activeDragIdx != null) {
            val res = level.tray.getOrNull(activeDragIdx)
            if (res != null) {
                Image(
                    painter = painterResource(res),
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset {
                            IntOffset(
                                (fingerRootPos.x - itemHalfPx).roundToInt(),
                                (fingerRootPos.y - itemHalfPx - liftAboveFingerPx).roundToInt()
                            )
                        }
                        .size(90.dp)
                        .graphicsLayer { scaleX = 1.2f; scaleY = 1.2f }
                        .zIndex(100f),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }

    if (won) {
        AlertDialog(
            onDismissRequest = onWin,
            title = { Text("LEVEL ${level.id} COMPLETE!", color = Green, fontWeight = FontWeight.Black) },
            text = { Text("Shabash! Sabhi aabhushan ekdam sahi jagah fit ho gaye!", color = Brown, fontWeight = FontWeight.Bold) },
            confirmButton = { Button(onClick = onWin, colors = ButtonDefaults.buttonColors(containerColor = Green)) { Text("NEXT LEVEL ✔") } },
            containerColor = Paper
        )
    }

    if (gameOver) {
        AlertDialog(
            onDismissRequest = onBack,
            title = { Text("GAME OVER", color = Red, fontWeight = FontWeight.Black) },
            text = { Text("Lives khatam ho gayi! Ad dekh kar Revive karein.", color = Brown, fontWeight = FontWeight.Bold) },
            confirmButton = { Button({ playRewardAd(activity) { gameOver = false; lives = 3; seconds += 60 } }, colors = ButtonDefaults.buttonColors(containerColor = Green)) { Text("🎬 Revive") } },
            dismissButton = { Button({ gameOver = false; lives = 5; seconds = 240; placed = emptySet(); usedIdx = emptySet() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CA8F5))) { Text("↻ Restart") } },
            containerColor = Paper
        )
    }
}
