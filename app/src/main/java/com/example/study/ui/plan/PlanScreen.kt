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
            SettingsRepository.LIBRARIES.forEach { library ->
                ChoiceChip(
                    text = "$library（${formatCount(state.libraryCounts[library] ?: 0)} 词）",
                    selected = state.formLibrary == library,
                    onClick = { onLibrary(library) }
                )
            }
        }
    }

    SectionCard(title = "备考时长") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(30, 45, 60, 90, 120).forEach { days ->
                ChoiceChip(
                    text = "$days 天",
                    selected = state.formDays == days,
                    onClick = { onDays(days) }
                )
            }
        }
        val examDate = LocalDate.now().plusDays(state.formDays.toLong())
        Text(
            text = "考试日期约为 $examDate · 剩余 ${state.formDays} 天",
            style = MaterialTheme.typography.bodySmall,
            color = DuoTextSecondary
        )
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

    TextButton(onClick = onFinish, modifier = Modifier.fillMaxWidth()) {
        Text("结束当前计划", color = DuoRed)
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
