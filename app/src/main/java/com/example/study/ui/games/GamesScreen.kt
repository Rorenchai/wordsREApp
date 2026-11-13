package com.example.study.ui.games

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.study.ui.theme.DuoBlue
import com.example.study.ui.theme.DuoBorder
import com.example.study.ui.theme.DuoGold
import com.example.study.ui.theme.DuoGreen
import com.example.study.ui.theme.DuoSurface
import com.example.study.ui.theme.DuoTextPrimary
import com.example.study.ui.theme.DuoTextSecondary

private data class GameEntry(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val tint: Color
)

private val gameEntries = listOf(
    GameEntry("单词消消乐", "词义配对，消除全部卡片", Icons.Filled.Extension, DuoGreen),
    GameEntry("看词选译", "四选一快速答题", Icons.Filled.Translate, DuoBlue),
    GameEntry("听音辨词", "听发音选正确单词", Icons.Filled.Headphones, DuoGold),
    GameEntry("单词拼写", "根据释义拼出单词", Icons.Filled.Keyboard, Color(0xFFA855F7))
)

@Composable
fun GamesScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(top = 14.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "小游戏",
            style = MaterialTheme.typography.headlineMedium,
            color = DuoTextPrimary
        )
        Text(
            text = "核心学习闭环已上线，以下趣味模式将在第二期开放。",
            style = MaterialTheme.typography.bodyMedium,
            color = DuoTextSecondary
        )
        gameEntries.chunked(2).forEach { rowEntries ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowEntries.forEach { entry ->
                    GameCard(entry = entry, modifier = Modifier.weight(1f))
                }
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun GameCard(entry: GameEntry, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(DuoSurface)
            .border(2.dp, DuoBorder, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(entry.tint.copy(alpha = 0.14f))
                .padding(10.dp)
        ) {
            Icon(
                imageVector = entry.icon,
                contentDescription = null,
                tint = entry.tint,
                modifier = Modifier.size(26.dp)
            )
        }
        Text(
            text = entry.title,
            style = MaterialTheme.typography.titleMedium,
            color = DuoTextPrimary
        )
        Text(
            text = entry.description,
            style = MaterialTheme.typography.bodySmall,
            color = DuoTextSecondary
        )
        Text(
            text = "第二期开放",
            style = MaterialTheme.typography.labelSmall,
            color = DuoGold
        )
    }
}
