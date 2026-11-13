package com.example.study.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.study.ui.theme.DuoBorder
import com.example.study.ui.theme.DuoGreen
import com.example.study.ui.theme.DuoTextSecondary

/** 学习页筛选选项：-1 全部 / 0 未学习 / 1 学习中 / 2 已掌握。 */
val StudyFilterOptions: List<Pair<Int, String>> = listOf(
    -1 to "全部",
    0 to "未学习",
    1 to "学习中",
    2 to "已掌握"
)

@Composable
fun FilterChips(
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StudyFilterOptions.forEach { (value, label) ->
            val isSelected = selected == value
            Surface(
                shape = RoundedCornerShape(percent = 50),
                color = if (isSelected) DuoGreen else Color.White,
                border = BorderStroke(2.dp, if (isSelected) DuoGreen else DuoBorder),
                modifier = Modifier.clickable { onSelect(value) }
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) Color.White else DuoTextSecondary,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}
