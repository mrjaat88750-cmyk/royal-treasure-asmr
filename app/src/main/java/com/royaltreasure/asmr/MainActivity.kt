package com.royaltreasure.asmr

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.ads.MobileAds

private val Paper = Color(0xFFFFF2D9)
private val Brown = Color(0xFF70452E)
private val Gold = Color(0xFFD5A044)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        MobileAds.initialize(this) {}
        setContent { MaterialTheme { RoyalTreasureApp() } }
    }
}

@Composable
private fun RoyalTreasureApp() {
    val context = LocalContext.current
    val activity = context as? Activity
    val levels = remember { buildAllLevels(context) }
    var activeLevel by remember { mutableStateOf<Level?>(null) }
    var doneIds by remember { mutableStateOf(setOf<Int>()) }

    val current = activeLevel
    if (current != null) {
        GameScreen(
            level = current,
            activity = activity,
            onBack = { activeLevel = null },
            onWin = { doneIds = doneIds + current.id; activeLevel = null }
        )
    } else {
        Box(
            Modifier.fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFFFFE6D0), Paper, Color(0xFFFFF9EB))))
                .statusBarsPadding()
                .padding(14.dp)
        ) {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.Center) {
                    Image(painterResource(R.drawable.ui_ribbon), null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
                    Text("ROYAL TREASURE", color = Color(0xFFFFE9AD), fontSize = 13.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.height(10.dp))
                LazyVerticalGrid(
                    GridCells.Fixed(2), Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(levels) { lvl ->
                        val unlocked = lvl.id == 1 || (lvl.id - 1) in doneIds
                        Column(
                            Modifier.height(174.dp).shadow(4.dp, RoundedCornerShape(15.dp)).clip(RoundedCornerShape(15.dp))
                                .background(Color(0xFFE4C989)).border(2.dp, Color(0xFF9C713D), RoundedCornerShape(15.dp))
                                .clickable {
                                    if (unlocked) activeLevel = lvl
                                    else Toast.makeText(context, "🔒 Complete Level ${lvl.id - 1} first!", Toast.LENGTH_SHORT).show()
                                }.padding(7.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(lvl.title, color = Brown, fontSize = 10.sp, fontWeight = FontWeight.Black, maxLines = 1)
                            Box(Modifier.fillMaxWidth().weight(1f).padding(top = 5.dp).clip(RoundedCornerShape(9.dp))) {
                                Image(painterResource(lvl.girlRes), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                if (!unlocked) Box(Modifier.fillMaxSize().background(Color(0x882B1B12)), contentAlignment = Alignment.Center) { Text("🔒", fontSize = 34.sp) }
                                Text(if (lvl.id in doneIds) "✔" else if (unlocked) "▶" else "🔒", Modifier.align(Alignment.BottomEnd).padding(4.dp), fontSize = 20.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}
