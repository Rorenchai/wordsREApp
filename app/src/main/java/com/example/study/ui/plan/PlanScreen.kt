package com.example.study.ui.plan

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.study.data.plan.EbbinghausSchedule
import com.example.study.data.repo.SettingsRepository
import com.example.study.ui.components.DuoButton
import com.example.study.ui.components.DuoButtonStyle
import com.example.study.ui.components.ProgressTrack
import com.example.study.ui.theme.DuoBackground
import com.example.study.ui.theme.DuoBlue
import com.example.study.ui.theme.DuoBorder
import com.example.study.ui.theme.DuoCard
import com.example.study.ui.theme.DuoGreen
import kotlin.math.roundToInt
import com.example.study.ui.theme.DuoRed
import com.example.study.ui.theme.DuoSurface
import com.example.study.ui.theme.DuoTextPrimary
import com.example.study.ui.theme.DuoTextSecondary
import java.time.LocalDate

@Composable
fun PlanScreen(
    onBack: () -> Unit,
    onStartNew: () -> Unit,
    onStartReview: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: PlanViewModel = viewModel(factory = PlanViewModel.factory(context))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showFinishDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.refresh() }
    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearMessage()
    }

    Box(modifier = modifier.fillMaxSize().background(DuoBackground)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(top = 6.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = DuoTextPrimary
                    )
                }
                Text(
                    text = "学习计划",
                    style = MaterialTheme.typography.headlineSmall,
                    color = DuoTextPrimary
                )
            }

            if (state.loading) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(240.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = DuoGreen)
                }
                return@Column
            }

            val plan = state.activePlan
            if (plan == null) {
                PlanCreationForm(
                    state = state,
                    onLibrary = viewModel::setLibrary,
                    onDays = viewModel::setDays,
                    onDailyNew = viewModel::setDailyNewWords,
                    onReviewLimit = viewModel::setReviewLimit,
                    onName = viewModel::setName,
                    onCreate = viewModel::createPlan
                )
            } else {
                PlanOverview(
                    state = state,
                    onStartNew = onStartNew,
                    onStartReview = onStartReview,
                    onPrevMonth = { viewModel.moveCalendarMonth(-1) },
                    onNextMonth = { viewModel.moveCalendarMonth(1) },
                    onSelectDate = viewModel::selectDate,
                    onFinish = { showFinishDialog = true }
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 18.dp, vertical = 8.dp)
        )
    }

    if (showFinishDialog) {
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            title = { Text("结束当前计划？") },
            text = { Text("结束后将不再安排每日新词与复习任务，历史学习数据保留。") },
            confirmButton = {
                TextButton(onClick = {
                    showFinishDialog = false
                    viewModel.finishPlan()
                }) {
                    Text("结束计划", color = DuoRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinishDialog = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(DuoCard)
            .border(2.dp, DuoBorder, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = DuoTextPrimary
        )
        content()
    }
}

@Composable
private fun ChoiceChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(if (selected) DuoGreen else DuoCard)
            .border(2.dp, if (selected) DuoGreen else DuoBorder, RoundedCornerShape(percent = 50))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Color.White else DuoTextSecondary
        )
    }
}

@Composable
private fun PlanCreationForm(
    state: PlanUiState,
    onLibrary: (String) -> Unit,
    onDays: (Int) -> Unit,
    onDailyNew: (Int) -> Unit,
    onReviewLimit: (Int) -> Unit,
    onName: (String) -> Unit,
    onCreate: () -> Unit
) {
    Text(
        text = "按艾宾浩斯遗忘曲线安排每日新词与复习，考试前完成多轮巩固。",
        style = MaterialTheme.typography.bodySmall,
        color = DuoTextSecondary
    )

    SectionCard(title = "目标词库") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            state.libraries.forEach { item ->
                ChoiceChip(
                    text = "${item.library}（${formatCount(item.count)} 词）",
                    selected = state.formLibrary == item.library,
                    onClick = { onLibrary(item.library) }
                )
            }
        }
    }

    SectionCard(title = "备考时长") {
        val examDate = LocalDate.now().plusDays(state.formDays.toLong())
        // 用滑动条替代固定档位：7–365 天连续可调，拖动时同步更新考试日期与建议每日量
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "备考 ${state.formDays} 天",
                style = MaterialTheme.typography.titleMedium,
                color = DuoTextPrimary
            )
            Text(
                text = "考试日期约 $examDate",
                style = MaterialTheme.typography.bodySmall,
                color = DuoTextSecondary
            )
        }
        Slider(
            value = state.formDays.toFloat(),
            onValueChange = { onDays(it.roundToInt()) },
            valueRange = 7f..365f,
            colors = SliderDefaults.colors(
                thumbColor = DuoGreen,
                activeTrackColor = DuoGreen,
                inactiveTrackColor = DuoBorder
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("7 天", style = MaterialTheme.typography.bodySmall, color = DuoTextSecondary)
            Text("365 天", style = MaterialTheme.typography.bodySmall, color = DuoTextSecondary)
        }
    }

    SectionCard(title = "每日新词") {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(onClick = {
                onDailyNew(state.formDailyNewWords - 5)
            }) { Text("−5", color = DuoBlue) }
            Text(
                text = "${state.formDailyNewWords} 词",
                style = MaterialTheme.typography.titleMedium,
                color = DuoTextPrimary
            )
            TextButton(onClick = {
                onDailyNew(state.formDailyNewWords + 5)
            }) { Text("+5", color = DuoBlue) }
            Text(
                text = "建议 ${state.suggestion} 词",
                style = MaterialTheme.typography.bodySmall,
                color = DuoTextSecondary
            )
        }
        Text(
            text = "艾宾浩斯法建议新词与复习量约 1:2，单日新词不超过 ${EbbinghausSchedule.MAX_DAILY_NEW_WORDS} 个。",
            style = MaterialTheme.typography.bodySmall,
            color = DuoTextSecondary
        )
    }

    SectionCard(title = "每日复习上限") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(60, 120, 180, 240).forEach { limit ->
                ChoiceChip(
                    text = "$limit 词",
                    selected = state.formReviewLimit == limit,
                    onClick = { onReviewLimit(limit) }
                )
            }
        }
    }

    SectionCard(title = "计划名称") {
        OutlinedTextField(
            value = state.formName,
            onValueChange = onName,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("${state.formLibrary} 备考计划") },
            shape = RoundedCornerShape(14.dp)
        )
    }

    SectionCard(title = "计划预览") {
        val words = state.libraryCounts[state.formLibrary] ?: 0
        val target = minOf(state.formDays * state.formDailyNewWords, words)
        Text(
            text = "目标词数：$target / $words 词\n预计每日复习量：约 ${state.formDailyNewWords * 2} 词（新词:复习 ≈ 1:2）",
            style = MaterialTheme.typography.bodyMedium,
            color = DuoTextPrimary
        )
        if (!state.feasible) {
            Text(
                text = "按每日 ${state.formDailyNewWords} 词，${state.formDays} 天无法覆盖全部 ${formatCount(words)} 词：建议延长备考时长或减少目标词数。",
                style = MaterialTheme.typography.bodySmall,
                color = DuoRed
            )
        }
    }

    DuoButton(
        text = if (state.creating) "创建中…" else "创建计划",
        onClick = onCreate,
        modifier = Modifier.fillMaxWidth(),
        enabled = !state.creating
    )
}

@Composable
private fun PlanOverview(
    state: PlanUiState,
    onStartNew: () -> Unit,
    onStartReview: () -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDate: (String) -> Unit,
    onFinish: () -> Unit
) {
    val plan = state.activePlan ?: return
    val summary = state.summary

    SectionCard(title = plan.name) {
        Text(
            text = "考试日期：${plan.examDate} · 剩余 ${summary?.daysUntilExam ?: 0} 天 · 每日新词 ${plan.dailyNewWords}",
            style = MaterialTheme.typography.bodySmall,
            color = DuoTextSecondary
        )
        val target = summary?.targetWords ?: 0
        val learned = summary?.learnedWords ?: 0
        val progress = if (target <= 0) 0f else (learned.toFloat() / target).coerceIn(0f, 1f)
        Text(
            text = "已学 $learned / 目标 $target 词",
            style = MaterialTheme.typography.bodyMedium,
            color = DuoTextPrimary
        )
        ProgressTrack(progress = progress, modifier = Modifier.fillMaxWidth())
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("已加入 ${summary?.addedWords ?: 0}", style = MaterialTheme.typography.bodySmall, color = DuoTextSecondary)
            Text("已掌握 ${summary?.masteredWords ?: 0}", style = MaterialTheme.typography.bodySmall, color = DuoGreen)
        }
    }

    SectionCard(title = "今日任务") {
        Text(
            text = "新词 ${summary?.todayNewLearned ?: 0} / ${summary?.todayNewTotal ?: 0} 个 · 到期复习 ${summary?.dueReviewCount ?: 0} 个",
            style = MaterialTheme.typography.titleMedium,
            color = DuoTextPrimary
        )
        Text(
            text = "按艾宾浩斯复习间隔（1/2/4/7/15 天）自动安排，答对推进、答错次日重试；越过考试日期自动收口。",
            style = MaterialTheme.typography.bodySmall,
            color = DuoTextSecondary
        )
        val remainingNew = (summary?.todayNewTotal ?: 0) - (summary?.todayNewLearned ?: 0)
        val dueReviews = summary?.dueReviewCount ?: 0
        DuoButton(
            text = "开始学习今日新词（剩余 $remainingNew）",
            onClick = onStartNew,
            modifier = Modifier.fillMaxWidth(),
            enabled = remainingNew > 0
        )
        DuoButton(
            text = "开始复习（$dueReviews）",
            onClick = onStartReview,
            modifier = Modifier.fillMaxWidth(),
            style = DuoButtonStyle.Learning,
            enabled = dueReviews > 0
        )
    }

    if (state.todayNew.isNotEmpty()) {
        SectionCard(title = "今日新词（${state.todayNew.size}）") {
            state.todayNew.take(8).forEach { detail ->
                PlanWordLine(word = detail.word.word, meaning = detail.word.meaning)
            }
            if (state.todayNew.size > 8) {
                Text(
                    text = "还有 ${state.todayNew.size - 8} 个…",
                    style = MaterialTheme.typography.bodySmall,
                    color = DuoTextSecondary
                )
            }
        }
    }

    if (state.dueReviews.isNotEmpty()) {
        SectionCard(title = "到期复习（${state.dueReviews.size}）") {
            state.dueReviews.take(8).forEach { detail ->
                PlanWordLine(
                    word = detail.word.word,
                    meaning = detail.word.meaning,
                    stage = "第 ${detail.stage + 1} 轮"
                )
            }
            if (state.dueReviews.size > 8) {
                Text(
                    text = "还有 ${state.dueReviews.size - 8} 个…",
                    style = MaterialTheme.typography.bodySmall,
                    color = DuoTextSecondary
                )
            }
        }
    }

    SectionCard(title = "计划日历") {
        PlanCalendarContent(
            state = state,
            onPrevMonth = onPrevMonth,
            onNextMonth = onNextMonth,
            onSelectDate = onSelectDate
        )
    }

    TextButton(onClick = onFinish, modifier = Modifier.fillMaxWidth()) {
        Text("结束当前计划", color = DuoRed)
    }
}

@Composable
private fun PlanCalendarContent(
    state: PlanUiState,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDate: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onPrevMonth) { Text("‹", color = DuoBlue, style = MaterialTheme.typography.titleLarge) }
        Text(
            text = "${state.calendarYear} 年 ${state.calendarMonth} 月",
            style = MaterialTheme.typography.titleMedium,
            color = DuoTextPrimary
        )
        TextButton(onClick = onNextMonth) { Text("›", color = DuoBlue, style = MaterialTheme.typography.titleLarge) }
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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

    val firstDate = LocalDate.of(state.calendarYear, state.calendarMonth, 1)
    val offset = firstDate.dayOfWeek.value - 1
    val cells: List<com.example.study.data.plan.PlanCalendarDay?> =
        List(offset) { null } + state.calendarDays.map { it }
    var startIndex = 0
    while (startIndex < cells.size) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (column in 0 until 7) {
                val day = cells.getOrNull(startIndex + column)
                if (day == null) {
                    Spacer(Modifier.weight(1f))
                } else {
                    PlanCalendarCell(
                        day = day,
                        selected = state.selectedDate == day.date,
                        onClick = { onSelectDate(day.date) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        startIndex += 7
    }

    val selected = state.calendarDays.firstOrNull { it.date == state.selectedDate }
    if (selected != null) {
        val newText = if (selected.isPast || selected.isToday) {
            "新词 ${selected.newLearned}/${selected.newTotal}"
        } else {
            "预计新词 ${selected.newTotal}"
        }
        val reviewText = if (selected.isToday) {
            "复习完成 ${selected.reviewDone} · 今日剩余 ${selected.reviewRemaining}"
        } else if (selected.isPast) {
            "复习完成 ${selected.reviewDone}"
        } else {
            "已排期复习 ${selected.reviewScheduled}"
        }
        Text(
            text = "${selected.dayOfMonth} 日 · $newText · $reviewText" +
                if (selected.isExamDay) " · 考试日" else "",
            style = MaterialTheme.typography.bodySmall,
            color = DuoTextPrimary
        )
    }

    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("● 新词任务", style = MaterialTheme.typography.labelSmall, color = DuoGreen)
        Text("● 复习任务", style = MaterialTheme.typography.labelSmall, color = DuoBlue)
        Text("红框 = 考试日", style = MaterialTheme.typography.labelSmall, color = DuoRed)
    }
}

@Composable
private fun PlanCalendarCell(
    day: com.example.study.data.plan.PlanCalendarDay,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasNew = day.newTotal > 0 || day.newLearned > 0
    val hasReview = day.reviewScheduled > 0 || day.reviewDone > 0 || day.reviewRemaining > 0
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier = modifier
            .height(48.dp)
            .clip(shape)
            .background(if (selected) DuoBlue.copy(alpha = 0.14f) else DuoCard)
            .border(
                width = 2.dp,
                color = when {
                    selected -> DuoBlue
                    day.isExamDay -> DuoRed
                    day.isToday -> DuoBlue
                    else -> DuoBorder
                },
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = "${day.dayOfMonth}",
            style = MaterialTheme.typography.labelSmall,
            color = if (day.isPast && !day.isToday) DuoTextSecondary else DuoTextPrimary
        )
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            if (hasNew) {
                Box(modifier = Modifier.clip(RoundedCornerShape(percent = 50)).background(DuoGreen).height(5.dp).width(5.dp))
            }
            if (hasReview) {
                Box(modifier = Modifier.clip(RoundedCornerShape(percent = 50)).background(DuoBlue).height(5.dp).width(5.dp))
            }
        }
    }
}

@Composable
private fun PlanWordLine(word: String, meaning: String, stage: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = word,
            style = MaterialTheme.typography.titleSmall,
            color = DuoTextPrimary
        )
        Text(
            text = meaning,
            style = MaterialTheme.typography.bodySmall,
            color = DuoTextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (stage != null) {
            Text(stage, style = MaterialTheme.typography.labelSmall, color = DuoBlue)
        }
    }
}

private fun formatCount(count: Int): String {
    return if (count >= 1000) "%,d".format(count) else count.toString()
}
