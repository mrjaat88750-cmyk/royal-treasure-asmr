package com.royaltreasure.asmr.state

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.royaltreasure.asmr.R
import com.royaltreasure.asmr.model.JewelleryItem
import com.royaltreasure.asmr.model.LevelConfig
import kotlin.math.hypot

class GameViewModel(context: Context) {
    private val prefs = context.getSharedPreferences("royal_treasure_progress", Context.MODE_PRIVATE)
    var currentLevelNumber by mutableStateOf(prefs.getInt("unlocked_level", 1).coerceIn(1, 10))
        private set
    var lives by mutableStateOf(5)
        private set
    var secondsLeft by mutableStateOf(180)
        private set
    var isGameOver by mutableStateOf(false)
        private set
    var wrongDrops by mutableStateOf(0)
        private set

    private fun makeItems(levelNo: Int): List<JewelleryItem> {
        val radius = (0.105f - (levelNo - 1) * 0.004f).coerceAtLeast(0.065f)
        return listOf(
            JewelleryItem("crown", "Royal Crown", R.drawable.crown, 0.500f, 0.135f, 0.340f, 0.125f, 0f, radius, 3f),
            JewelleryItem("hairpin", "Ruby Hairpin", R.drawable.hairpin, 0.500f, 0.235f, 0.145f, 0.095f, 0f, radius, 4f),
            JewelleryItem("earring_l", "Left Jhumka", R.drawable.earring_left, 0.405f, 0.335f, 0.075f, 0.065f, 0f, radius, 2f),
            JewelleryItem("earring_r", "Right Jhumka", R.drawable.earring_right, 0.620f, 0.330f, 0.080f, 0.085f, 0f, radius, 2f),
            JewelleryItem("necklace", "Bridal Choker", R.drawable.necklace, 0.535f, 0.440f, 0.280f, 0.130f, 0f, radius, 5f),
            JewelleryItem("waist_chain", "Royal Waist Chain", R.drawable.waist_chain, 0.530f, 0.590f, 0.240f, 0.105f, 0f, radius, 4.5f),
            JewelleryItem("bangle", "Kundan Bangle", R.drawable.bangle, 0.530f, 0.700f, 0.125f, 0.095f, 0f, radius, 5f),
            JewelleryItem("anklet", "Pearl Anklet", R.drawable.anklet, 0.530f, 0.845f, 0.205f, 0.090f, 0f, radius, 6f)
        )
    }

    private fun makeLevel(n: Int) = LevelConfig(
        id = n,
        title = when ((n - 1) % 3) {
            0 -> "ROYAL PRINCESS • LEVEL $n"
            1 -> "EMERALD PALACE • LEVEL $n"
            else -> "MOONLIGHT ROYALTY • LEVEL $n"
        },
        baseImageRes = R.drawable.girl_base,
        referenceImageRes = R.drawable.girl_reference,
        imageAspectRatio = 9f / 16f,
        items = makeItems(n)
    )

    var level by mutableStateOf(makeLevel(currentLevelNumber))
        private set
    var isPaused by mutableStateOf(false)
    var isSoundOn by mutableStateOf(true)
    var isDevMode by mutableStateOf(false)
    var showReferenceOverlay by mutableStateOf(false)
    var activeHintItemId by mutableStateOf<String?>(null)
    var lastSnapEffectPos by mutableStateOf<Offset?>(null)

    private val toneGen: ToneGenerator? = runCatching {
        ToneGenerator(AudioManager.STREAM_MUSIC, 80)
    }.getOrNull()

    val placedCount: Int get() = level.items.count { it.isPlaced }
    val totalCount: Int get() = level.items.size
    val isLevelCompleted: Boolean get() = placedCount == totalCount

    fun tickTimer() {
        if (!isPaused && !isGameOver && !isLevelCompleted && secondsLeft > 0) {
            secondsLeft -= 1
            if (secondsLeft <= 0) endGame()
        }
    }

    private fun endGame() {
        isGameOver = true
        isPaused = false
    }

    private fun loseLife() {
        wrongDrops += 1
        lives = (lives - 1).coerceAtLeast(0)
        if (lives == 0) endGame()
    }

    fun playAsmrSnapSound() {
        if (isSoundOn) runCatching { toneGen?.startTone(ToneGenerator.TONE_PROP_ACK, 120) }
    }
    fun playReturnSound() {
        if (isSoundOn) runCatching { toneGen?.startTone(ToneGenerator.TONE_PROP_NACK, 90) }
    }
    fun playWinSound() {
        if (isSoundOn) runCatching { toneGen?.startTone(ToneGenerator.TONE_CDMA_CONFIRM, 300) }
    }

    fun tryPlaceItem(itemId: String, dropScreenCenter: Offset, imageBounds: Rect): Boolean {
        if (imageBounds.width <= 0f || imageBounds.height <= 0f || isPaused || isGameOver) return false
        val item = level.items.find { it.id == itemId } ?: return false
        if (item.isPlaced) return false
        val normX = (dropScreenCenter.x - imageBounds.left) / imageBounds.width
        val normY = (dropScreenCenter.y - imageBounds.top) / imageBounds.height
        val distance = hypot(normX - item.targetX, normY - item.targetY)
        if (distance <= item.snapRadius) {
            level = level.copy(items = level.items.map { if (it.id == itemId) it.copy(isPlaced = true) else it })
            if (activeHintItemId == itemId) activeHintItemId = null
            lastSnapEffectPos = Offset(imageBounds.left + item.targetX * imageBounds.width, imageBounds.top + item.targetY * imageBounds.height)
            if (isLevelCompleted) playWinSound() else playAsmrSnapSound()
            return true
        } else {
            loseLife()
            playReturnSound()
            return false
        }
    }

    fun triggerHint() {
        if (!isPaused && !isGameOver) activeHintItemId = level.items.firstOrNull { !it.isPlaced }?.id
    }

    fun resetLevel() {
        level = makeLevel(currentLevelNumber)
        lives = 5
        secondsLeft = 180
        wrongDrops = 0
        isGameOver = false
        activeHintItemId = null
        lastSnapEffectPos = null
        isPaused = false
    }

    fun nextLevel() {
        if (!isLevelCompleted) return
        currentLevelNumber = if (currentLevelNumber >= 10) 1 else currentLevelNumber + 1
        prefs.edit().putInt("unlocked_level", maxOf(prefs.getInt("unlocked_level", 1), currentLevelNumber)).apply()
        resetLevel()
    }

    fun clearSnapEffect() { lastSnapEffectPos = null }
}
