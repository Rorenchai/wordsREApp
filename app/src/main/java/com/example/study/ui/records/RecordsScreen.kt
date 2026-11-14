package com.example.study.ui.records

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
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
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.study.data.stats.LibraryStats
import com.example.study.data.stats.StudyDates
import com.example.study.data.stats.formatStudyDuration
import com.example.study.ui.components.DayBar
import com.example.study.ui.components.ProgressTrack
import com.example.study.ui.components.WeekBarChart
import java.time.LocalDate
import com.example.study.ui.theme.DuoBackground
import com.example.study.ui.theme.DuoBlue
import com.example.study.ui.theme.DuoBorder
import com.example.study.ui.theme.DuoCard
import com.example.study.ui.theme.DuoGold
import com.example.study.ui.theme.DuoGreen
import com.example.study.ui.theme.DuoRed
import com.example.study.ui.theme.DuoSurface
import com.example.study.ui.theme.DuoTextPrimary
import com.example.study.ui.theme.DuoTextSecondary

@Composable
fun RecordsScreen(onOpenWrongBook: () -> Unit, onOpenVocab: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val viewModel: RecordsViewModel = viewModel(factory = RecordsViewModel.factory(context))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var checkInReward by remember { mutableStateOf<CheckInReward?>(null) }

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
                CheckInCard(
                    stats = stats,
                    lastCheckInDate = state.lastCheckInDate,
                    onCheckIn = {
                        viewModel.checkIn()
                        checkInReward = CheckInReward(
                            streak = stats.streak,
                            energy = (10 + stats.streak * 2).coerceAtMost(50)
                        )
                    }
                )
                ProgressOverviewCard(stats)
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

    checkInReward?.let { reward ->
        CheckInRewardDialog(
            streak = reward.streak,
            energy = reward.energy,
            onDismiss = { checkInReward = null }
        )
    }
}

@Composable
private fun StatsGrid(stats: LibraryStats) {
    // 四个统计值合并进一张卡片，减少满屏独立圆角小卡，视觉更干净
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DuoSurface)
            .border(2.dp, DuoBorder, RoundedCornerShape(14.dp))
            .padding(vertical = 14.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            StatCell(
                value = stats.totalWords.toString(),
                label = "词库总数",
                accent = DuoTextPrimary,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(42.dp)
                    .background(DuoBorder)
            )
            StatCell(
                value = stats.startedWords.toString(),
                label = "已学",
                accent = DuoBlue,
                modifier = Modifier.weight(1f)
            )
        }
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .fillMaxWidth()
                .height(1.dp)
                .background(DuoBorder)
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            StatCell(
                value = stats.masteredWords.toString(),
                label = "已掌握",
                accent = DuoGreen,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(42.dp)
                    .background(DuoBorder)
            )
            StatCell(
                value = stats.untouchedWords.toString(),
                label = "待学",
                accent = DuoGold,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatCell(
    value: String,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = accent,
            textAlign = TextAlign.Center
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = DuoTextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ProgressOverviewCard(stats: LibraryStats) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DuoSurface)
            .border(2.dp, DuoBorder, RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("学习进度", style = MaterialTheme.typography.titleMedium, color = DuoTextPrimary)
            Text(
                text = String.format(java.util.Locale.US, "%.1f%%", stats.learnedPercent),
                style = MaterialTheme.typography.titleMedium,
                color = DuoBlue
            )
        }
        ProgressTrack(progress = stats.learnedPercent / 100f, modifier = Modifier.fillMaxWidth(), fillColor = DuoBlue)
        Text(
            text = "已接触 ${stats.startedWords} / ${stats.totalWords} 个单词",
            style = MaterialTheme.typography.bodySmall,
            color = DuoTextSecondary
        )
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("掌握率", style = MaterialTheme.typography.titleMedium, color = DuoTextPrimary)
            Text(
                text = String.format(java.util.Locale.US, "%.1f%%", stats.masteredPercent),
                style = MaterialTheme.typography.titleMedium,
                color = DuoGreen
            )
        }
        ProgressTrack(progress = stats.masteredPercent / 100f, modifier = Modifier.fillMaxWidth())
        Text(
            text = "已掌握 ${stats.masteredWords} / ${stats.totalWords} 个单词",
            style = MaterialTheme.typography.bodySmall,
            color = DuoTextSecondary
        )
    }
}

/** 今日打卡卡片的奖励数据（连续天数 + 学习能量）。 */
private data class CheckInReward(val streak: Int, val energy: Int)

@Composable
private fun CheckInCard(
    stats: LibraryStats,
    lastCheckInDate: String,
    onCheckIn: () -> Unit
) {
    val checkedToday = lastCheckInDate == LocalDate.now().toString()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DuoSurface)
            .border(2.dp, DuoBorder, RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("每日打卡", style = MaterialTheme.typography.titleMedium, color = DuoTextPrimary)
            Text(
                text = if (checkedToday) "今日已打卡" else "待打卡",
                style = MaterialTheme.typography.labelMedium,
                color = if (checkedToday) DuoGreen else DuoGold
            )
        }
        Text(
            text = when {
                checkedToday -> "今天已打卡 · 连续 ${stats.streak} 天，明天继续保持"
                stats.todayStudied > 0 -> "今天已学 ${stats.todayStudied} 个单词，完成打卡可领取学习能量"
                else -> "今天还没有学习记录，先学几个单词再打卡吧"
            },
            style = MaterialTheme.typography.bodySmall,
            color = DuoTextSecondary
        )
        if (!checkedToday && stats.todayStudied > 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DuoGreen)
                    .clickable(onClick = onCheckIn)
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "立即打卡 · 领取奖励",
                    style = MaterialTheme.typography.labelLarge,
                    color = DuoCard
                )
            }
        }
    }
}

@Composable
private fun CheckInRewardDialog(streak: Int, energy: Int, onDismiss: () -> Unit) {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    val confettiProgress by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(durationMillis = 1200),
        label = "check-in-confetti"
    )
    val medalScale by animateFloatAsState(
        targetValue = if (started) 1f else 0.4f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "check-in-medal"
    )
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(DuoCard)
                .border(2.dp, DuoBorder, RoundedCornerShape(24.dp))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            // 简单彩带粒子：按进度从顶部飘落，形成轻量奖励反馈
            Canvas(modifier = Modifier.matchParentSize()) {
                val colors = listOf(DuoGreen, DuoGold, DuoBlue, DuoRed)
                repeat(40) { index ->
                    val seed = (index * 53) % 97
                    val delay = (seed % 35) / 100f
                    val progress = ((confettiProgress - delay) / (1f - delay)).coerceIn(0f, 1f)
                    if (progress <= 0f) return@repeat
                    val x = size.width * ((seed % 100) / 100f) +
                        (index % 2 * 10 - 5).dp.toPx() * progress
                    val y = -16.dp.toPx() + (size.height + 32.dp.toPx()) * progress
                    drawCircle(
                        color = colors[index % colors.size].copy(alpha = 1f - progress * 0.5f),
                        radius = (2 + index % 3).dp.toPx(),
                        center = Offset(x, y)
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "🎉",
                    fontSize = 48.sp,
                    modifier = Modifier.graphicsLayer {
                        scaleX = medalScale
                        scaleY = medalScale
                    }
                )
                Text("打卡成功！", style = MaterialTheme.typography.headlineSmall, color = DuoGreen)
                Text("连续打卡 $streak 天", style = MaterialTheme.typography.titleMedium, color = DuoTextPrimary)
                Text("获得 $energy 学习能量", style = MaterialTheme.typography.bodyMedium, color = DuoGold)
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DuoGreen)
                        .clickable(onClick = onDismiss)
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("收下奖励", style = MaterialTheme.typography.labelLarge, color = DuoCard)
                }
            }
        }
    }
}

@Composable
private fun QuizStatsCard(stats: LibraryStats) {    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DuoSurface)
            .border(2.dp, DuoBorder, RoundedCornerShape(14.dp))
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
            .clip(RoundedCornerShape(14.dp))
            .background(DuoSurface)
            .border(2.dp, DuoBorder, RoundedCornerShape(14.dp))
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
            .clip(RoundedCornerShape(14.dp))
            .background(DuoSurface)
            .border(2.dp, DuoBorder, RoundedCornerShape(14.dp))
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
            .clip(RoundedCornerShape(14.dp))
            .background(DuoSurface)
            .border(2.dp, DuoBorder, RoundedCornerShape(14.dp))
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
            .clip(RoundedCornerShape(14.dp))
            .background(DuoSurface)
            .border(2.dp, DuoBorder, RoundedCornerShape(14.dp))
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
            .clip(RoundedCornerShape(14.dp))
            .background(DuoSurface)
            .border(2.dp, DuoBorder, RoundedCornerShape(14.dp))
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
            .clip(RoundedCornerShape(14.dp))
            .background(DuoSurface)
            .border(2.dp, DuoBorder, RoundedCornerShape(14.dp))
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
            .clip(RoundedCornerShape(14.dp))
            .background(DuoSurface)
            .border(2.dp, DuoBorder, RoundedCornerShape(14.dp))
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