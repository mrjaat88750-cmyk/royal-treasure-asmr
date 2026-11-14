package com.royaltreasure.asmr

import android.content.Context
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class Slot(val id: Int, val resId: Int, val ax: Float, val ay: Float, val w: Dp)
data class Level(val id: Int, val title: String, val girlRes: Int, val slots: List<Slot>, val tray: List<Int>)

fun buildAllLevels(context: Context): List<Level> {
    val lvl1Slots = listOf(
        Slot(1, R.drawable.lvl1_real_1, -0.09f, -0.82f, 118.dp),
        Slot(2, R.drawable.lvl1_real_2, -0.35f, -0.53f, 40.dp),
        Slot(3, R.drawable.lvl1_real_3, 0.08f, -0.54f, 46.dp),
        Slot(4, R.drawable.lvl1_real_4, -0.02f, -0.32f, 94.dp),
        Slot(5, R.drawable.lvl1_real_5, -0.05f, -0.14f, 84.dp),
        Slot(6, R.drawable.lvl1_real_6, 0.72f, -0.04f, 56.dp)
    )
    val lvl1Tray = listOf(
        R.drawable.lvl1_real_1, R.drawable.lvl1_real_2, R.drawable.lvl1_real_3,
        R.drawable.lvl1_real_4, R.drawable.lvl1_real_5, R.drawable.lvl1_real_6,
        R.drawable.lvl1_fake_1, R.drawable.lvl1_fake_2, R.drawable.lvl1_fake_3,
        R.drawable.lvl1_fake_4, R.drawable.lvl1_fake_5, R.drawable.lvl1_fake_6
    ).shuffled()

    val lvl2Slots = listOf(
        Slot(1, R.drawable.lvl2_real_1, 0.01f, -0.78f, 120.dp),
        Slot(2, R.drawable.lvl2_real_2, 0.02f, -0.35f, 114.dp),
        Slot(3, R.drawable.lvl2_real_3, 0.02f, -0.19f, 80.dp),
        Slot(4, R.drawable.lvl2_real_4, 0.02f, 0.42f, 46.dp)
    )
    val lvl2Tray = listOf(
        R.drawable.lvl2_real_1, R.drawable.lvl2_real_2,
        R.drawable.lvl2_real_3, R.drawable.lvl2_real_4,
        R.drawable.lvl2_fake_1, R.drawable.lvl2_fake_2,
        R.drawable.lvl2_fake_3, R.drawable.lvl2_fake_4
    ).shuffled()

    return (1..100).map { i ->
        when (i) {
            1 -> Level(1, "CLOUD VEILED EYES", R.drawable.lvl1_girl, lvl1Slots, lvl1Tray)
            2 -> Level(2, "ROYAL RAJASTHANI BRIDE", R.drawable.lvl2_girl, lvl2Slots, lvl2Tray)
            else -> Level(i, "ROYAL CHAPTER $i", R.drawable.lvl2_girl, lvl2Slots, lvl2Tray)
        }
    }
}
