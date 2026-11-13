package com.example.study.ui.records

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.study.data.stats.LibraryStats
import com.example.study.data.stats.StudyDates
import com.example.study.data.stats.formatStudyDuration
import com.example.study.ui.components.DayBar
import com.example.study.ui.components.ProgressTrack
import com.example.study.ui.components.StatCard
import com.example.study.ui.components.WeekBarChart
import java.time.LocalDate
import com.example.study.ui.theme.DuoBackground
import com.example.study.ui.theme.DuoBlue
import com.example.study.ui.theme.DuoBorder
import com.example.study.ui.theme.DuoCard
import com.example.study.ui.theme.DuoGold
import com.example.study.ui.theme.DuoGreen
import com.example.study.ui.theme.DuoSurface
import com.example.study.ui.theme.DuoTextPrimary
import com.example.study.ui.theme.DuoTextSecondary

@Composable
fun RecordsScreen(onOpenWrongBook: () -> Unit, onOpenVocab: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val viewModel: RecordsViewModel = viewModel(factory = RecordsViewModel.factory(context))
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.refresh() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DuoBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(top = 14.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "学习记录",
                    style = MaterialTheme.typography.headlineMedium,
                    color = DuoTextPrimary
                )
                Text(
                    text = state.library,
                    style = MaterialTheme.typography.labelMedium,
                    color = DuoTextSecondary
                )
            }

            val stats = state.stats
            if (state.loading && stats == null) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 60.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = DuoGreen)
                    Spacer(Modifier.height(12.dp))
                    Text("统计加载中…", color = DuoTextSecondary)
                }
            } else if (stats != null) {
                StatsGrid(stats)
                RateCard(
                    title = "学习进度",
                    rate = stats.learnedPercent,
                    caption = "已接触 ${stats.startedWords} / ${stats.totalWords} 个单词",
                    color = DuoBlue
                )
                RateCard(
                    title = "掌握率",
                    rate = stats.masteredPercent,
                    caption = "已掌握 ${stats.masteredWords} / ${stats.totalWords} 个单词",
                    color = DuoGreen
                )
                QuizStatsCard(stats)
                WeekCard(stats)
                LearningTimeCard(stats)
                WrongBookCard(stats.wrongBookCount, onOpenWrongBook)
                VocabCard(
                    estimate = state.vocabEstimate,
                    date = state.vocabDate,
                    onOpen = onOpenVocab
                )
                CheckInCalendarCard(stats)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StreakCard(stats.streak, Modifier.weight(1f))
                    TotalCard(stats.totalStudyEvents, Modifier.weight(1f))
                }
                Text(
                    text = "统计口径：今日目标按「不同单词」去重；复习会累计学习次数；streak 按有学习记录的连续自然日计算。",
                    style = MaterialTheme.typography.bodySmall,
                    color = DuoTextSecondary
                )
            }
        }
    }
}



@Composable
private fun StatsGrid(stats: LibraryStats) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                value = stats.totalWords.toString(),
                label = "词库总数",
                modifier = Modifier.weight(1f),
                accent = DuoTextPrimary
            )
            StatCard(
                value = stats.startedWords.toString(),
                label = "已学",
                modifier = Modifier.weight(1f),
                accent = DuoBlue
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                value = stats.masteredWords.toString(),
                label = "已掌握",
                modifier = Modifier.weight(1f),
                accent = DuoGreen
            )
            StatCard(
                value = stats.untouchedWords.toString(),
                label = "待学",
                modifier = Modifier.weight(1f),
                accent = DuoGold
            )
        }
    }
}

@Composable
private fun RateCard(
    title: String,
    rate: Float,
    caption: String,
    color: androidx.compose.ui.graphics.Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(DuoSurface)
            .border(2.dp, DuoBorder, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = DuoTextPrimary)
            Text(
                text = String.format(java.util.Locale.US, "%.1f%%", rate),
                style = MaterialTheme.typography.titleMedium,
                color = color
            )
        }
        ProgressTrack(
            progress = rate / 100f,
            modifier = Modifier.fillMaxWidth(),
            fillColor = color
        )
        Text(caption, style = MaterialTheme.typography.bodySmall, color = DuoTextSecondary)
    }
}

@Composable
private fun QuizStatsCard(stats: LibraryStats) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(DuoSurface)
            .border(2.dp, DuoBorder, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "答题统计（答题模式）",
            style = MaterialTheme.typography.titleMedium,
            color = DuoTextPrimary
        )
        Text(
            text = "已答 ${stats.quizAnswered} 题 · 答对 ${stats.quizCorrect} 题 · 正确率 ${stats.quizAccuracyPercent}%",
            style = MaterialTheme.typography.bodyMedium,
            color = DuoTextSecondary
        )
    }
}

@Composable
private fun LearningTimeCard(stats: LibraryStats) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(DuoSurface)
            .border(2.dp, DuoBorder, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "学习时长",
            style = MaterialTheme.typography.titleMedium,
            color = DuoTextPrimary
        )
        Text(
            text = "今日 ${formatStudyDuration(stats.todayStudyMs)} · 累计 ${formatStudyDuration(stats.totalStudyMs)}",
            style = MaterialTheme.typography.bodyMedium,
            color = DuoTextSecondary
        )
    }
}

@Composable
private fun WrongBookCard(count: Int, onOpen: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(DuoSurface)
            .border(2.dp, DuoBorder, RoundedCornerShape(20.dp))
            .clickable(onClick = onOpen)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "错题本",
                style = MaterialTheme.typography.titleMedium,
                color = DuoTextPrimary
            )
            Text(
                text = if (count > 0) "$count 个最近答错的词，点击进入复习" else "暂无错题",
                style = MaterialTheme.typography.bodySmall,
                color = DuoTextSecondary
            )
        }
        if (count > 0) {
            Text(
                text = "去复习 ›",
                style = MaterialTheme.typography.titleMedium,
                color = DuoGreen
            )
        }
    }
}

@Composable
private fun VocabCard(estimate: Int, date: String, onOpen: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(DuoSurface)
            .border(2.dp, DuoBorder, RoundedCornerShape(20.dp))
            .clickable(onClick = onOpen)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "词汇量评测",
                style = MaterialTheme.typography.titleMedium,
                color = DuoTextPrimary
            )
            Text(
                text = if (estimate > 0) "上次估算 ≈ $estimate 词 · $date" else "抽样答题，估算你的词汇量",
                style = MaterialTheme.typography.bodySmall,
                color = DuoTextSecondary
            )
        }
        Text(
            text = if (estimate > 0) "再测一次 ›" else "去评测 ›",
            style = MaterialTheme.typography.titleMedium,
            color = DuoGreen
        )
    }
}

@Composable
private fun CheckInCalendarCard(stats: LibraryStats) {
    val today = LocalDate.now()
    val studiedDates = stats.monthStudyDates.toSet()
    val daysInMonth = today.lengthOfMonth()
    val firstOffset = today.withDayOfMonth(1).dayOfWeek.value - 1

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(DuoSurface)
            .border(2.dp, DuoBorder, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "打卡日历",
            style = MaterialTheme.typography.titleMedium,
            color = DuoTextPrimary
        )
        Text(
            text = "${today.year} 年 ${today.monthValue} 月 · 本月打卡 ${studiedDates.size} 天",
            style = MaterialTheme.typography.bodySmall,
            color = DuoTextSecondary
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("一", "二", "三", "四", "五", "六", "日").forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = DuoTextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        var day = 1
        val totalCells = firstOffset + daysInMonth
        var weekStart = 0
        while (weekStart < totalCells) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (column in 0 until 7) {
                    val cellIndex = weekStart + column
                    if (cellIndex < firstOffset || day > daysInMonth) {
                        Spacer(Modifier.weight(1f))
                    } else {
                        val date = today.withDayOfMonth(day)
                        val dateText = StudyDates.format(date)
                        val isStudied = dateText in studiedDates
                        val isToday = day == today.dayOfMonth
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isStudied) DuoGreen else DuoCard)
                                .border(
                                    width = 2.dp,
                                    color = if (isToday) DuoBlue else if (isStudied) DuoGreen else DuoBorder,
                                    shape = RoundedCornerShape(10.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$day",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isStudied) Color.White else DuoTextSecondary
                            )
                        }
                        day++
                    }
                }
            }
            weekStart += 7
        }
    }
}

@Composable
private fun WeekCard(stats: LibraryStats) {
    val today = StudyDates.today()
    val countByDate = stats.weekCounts.associate { it.date to it.count }
    val bars = (6 downTo 0).map { offset ->
        val date = today.minusDays(offset.toLong())
        DayBar(label = weekdayLabel(date, offset), count = countByDate[StudyDates.format(date)] ?: 0)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(DuoSurface)
            .border(2.dp, DuoBorder, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("最近 7 天", style = MaterialTheme.typography.titleMedium, color = DuoTextPrimary)
            Text(
                "每日不同单词数",
                style = MaterialTheme.typography.bodySmall,
                color = DuoTextSecondary
            )
        }
        WeekBarChart(bars = bars, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun StreakCard(streak: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(DuoSurface)
            .border(2.dp, DuoBorder, RoundedCornerShape(20.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.LocalFireDepartment,
                contentDescription = null,
                tint = DuoGold,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "$streak 天",
                style = MaterialTheme.typography.headlineSmall,
                color = DuoTextPrimary
            )
        }
        Text("连续学习", style = MaterialTheme.typography.bodySmall, color = DuoTextSecondary)
    }
}

@Composable
private fun TotalCard(totalEvents: Long, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(DuoSurface)
            .border(2.dp, DuoBorder, RoundedCornerShape(20.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$totalEvents 次",
            style = MaterialTheme.typography.headlineSmall,
            color = DuoTextPrimary
        )
        Text("累计学习", style = MaterialTheme.typography.bodySmall, color = DuoTextSecondary)
    }
}

private fun weekdayLabel(date: LocalDate, offset: Int): String {
    if (offset == 0) return "今天"
    return when (date.dayOfWeek.value) {
        1 -> "一"
        2 -> "二"
        3 -> "三"
        4 -> "四"
        5 -> "五"
        6 -> "六"
        else -> "日"
    }
}