package com.example.study.ui.my

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.study.data.repo.SettingsRepository
import com.example.study.ui.components.AvatarFrames
import com.example.study.ui.components.UserAvatar
import com.example.study.ui.records.RecordsContent
import com.example.study.ui.settings.SettingsViewModel
import com.example.study.ui.theme.DuoBackground
import com.example.study.ui.theme.DuoBorder
import com.example.study.ui.theme.DuoCard
import com.example.study.ui.theme.DuoChocolate
import com.example.study.ui.theme.DuoGold
import com.example.study.ui.theme.DuoGreen
import com.example.study.ui.theme.DuoSurface
import com.example.study.ui.theme.DuoTextPrimary
import com.example.study.ui.theme.DuoTextSecondary

/**
 * 「我的」页：个人资料 + 积分 + 原记录页内容；右上角齿轮进入独立设置页。
 */
@Composable
fun MyScreen(
    onOpenSettings: () -> Unit,
    onOpenWrongBook: () -> Unit,
    onOpenVocab: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(context))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showProfileDialog by rememberSaveable { mutableStateOf(false) }
    var showFrameShop by rememberSaveable { mutableStateOf(false) }

    val avatarLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            viewModel.setAvatarUri(uri.toString())
        }
    }

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
                .padding(top = 14.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "我的",
                    style = MaterialTheme.typography.headlineMedium,
                    color = DuoTextPrimary
                )
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "设置",
                        tint = DuoTextSecondary
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(DuoCard)
                    .border(2.dp, DuoBorder, RoundedCornerShape(18.dp))
                    .clickable { showProfileDialog = true }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UserAvatar(
                    uri = state.avatarUri,
                    size = 72.dp,
                    frameId = state.equippedFrame
                )
                Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
                    Text(
                        text = state.nickname,
                        style = MaterialTheme.typography.headlineSmall,
                        color = DuoTextPrimary
                    )
                    Text(
                        text = state.bio.ifBlank { SettingsRepository.DEFAULT_BIO },
                        style = MaterialTheme.typography.bodySmall,
                        color = DuoTextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "编辑资料",
                    tint = DuoTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DuoSurface)
                    .border(2.dp, DuoBorder, RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("学习积分", style = MaterialTheme.typography.titleMedium, color = DuoTextPrimary)
                    Text(
                        text = "${state.points} 分 · 头像框 ${AvatarFrames.byId(state.equippedFrame).label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = DuoTextSecondary
                    )
                }
                TextButton(onClick = { showFrameShop = true }) {
                    Text("头像框商店", color = DuoGreen)
                }
            }

            RecordsContent(
                onOpenWrongBook = onOpenWrongBook,
                onOpenVocab = onOpenVocab,
                showHeader = false
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 18.dp, vertical = 8.dp)
        )
    }

    if (showProfileDialog) {
        ProfileDialog(
            nickname = state.nickname,
            bio = state.bio,
            avatarUri = state.avatarUri,
            equippedFrame = state.equippedFrame,
            ownedFrames = state.ownedFrames,
            onPickAvatar = { avatarLauncher.launch(arrayOf("image/*")) },
            onEquipFrame = viewModel::equipFrame,
            onDismiss = { showProfileDialog = false },
            onSave = { nickname, bio ->
                viewModel.setNickname(nickname)
                viewModel.setBio(bio)
                showProfileDialog = false
            }
        )
    }

    if (showFrameShop) {
        FrameShopDialog(
            points = state.points,
            ownedFrames = state.ownedFrames,
            equippedFrame = state.equippedFrame,
            onPurchase = { id -> viewModel.purchaseFrame(id) },
            onEquip = viewModel::equipFrame,
            onDismiss = { showFrameShop = false }
        )
    }
}

@Composable
private fun ProfileDialog(
    nickname: String,
    bio: String,
    avatarUri: String,
    equippedFrame: String,
    ownedFrames: Set<String>,
    onPickAvatar: () -> Unit,
    onEquipFrame: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var draftName by remember(nickname) { mutableStateOf(nickname) }
    var draftBio by remember(bio) { mutableStateOf(bio.ifBlank { SettingsRepository.DEFAULT_BIO }) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑个人资料") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    UserAvatar(uri = avatarUri, size = 64.dp, frameId = equippedFrame)
                    TextButton(onClick = onPickAvatar, modifier = Modifier.padding(start = 8.dp)) {
                        Text("更换头像")
                    }
                }
                OutlinedTextField(
                    value = draftName,
                    onValueChange = { if (it.length <= 20) draftName = it },
                    label = { Text("昵称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = draftBio,
                    onValueChange = { if (it.length <= SettingsRepository.MAX_BIO_LENGTH) draftBio = it },
                    label = { Text("简介") },
                    supportingText = { Text("${draftBio.length}/${SettingsRepository.MAX_BIO_LENGTH}") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("头像框", style = MaterialTheme.typography.labelMedium, color = DuoTextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AvatarFrames.all.filter { it.id in ownedFrames }.forEach { frame ->
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(frame.colors.first())
                                .border(
                                    width = if (frame.id == equippedFrame) 3.dp else 1.dp,
                                    color = if (frame.id == equippedFrame) DuoGold else DuoBorder,
                                    shape = CircleShape
                                )
                                .clickable { onEquipFrame(frame.id) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(draftName, draftBio) }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun FrameShopDialog(
    points: Int,
    ownedFrames: Set<String>,
    equippedFrame: String,
    onPurchase: (String) -> Unit,
    onEquip: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("头像框商店") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("当前积分：$points · 每个头像框 500 分，积分不足时不可兑换", color = DuoGold)
                AvatarFrames.all.forEach { frame ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(frame.colors.first())
                                .border(2.dp, frame.colors.last(), CircleShape)
                        )
                        Text(
                            text = frame.label,
                            modifier = Modifier.weight(1f).padding(start = 10.dp),
                            color = DuoTextPrimary
                        )
                        when {
                            frame.id == equippedFrame -> Text("佩戴中", color = DuoGreen)
                            frame.id in ownedFrames -> TextButton(onClick = { onEquip(frame.id) }) { Text("佩戴") }
                            else -> TextButton(
                                onClick = { onPurchase(frame.id) },
                                enabled = points >= frame.cost
                            ) { Text("${frame.cost} 分兑换") }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("完成") } }
    )
}
