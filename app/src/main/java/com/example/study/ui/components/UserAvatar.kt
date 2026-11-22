package com.example.study.ui.components

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.study.ui.theme.CreamGold
import com.example.study.ui.theme.DuoCard
import com.example.study.ui.theme.DuoChocolate
import com.example.study.ui.theme.PeachPink
import com.example.study.ui.theme.Sage
import com.example.study.ui.theme.SageDark

/** 可兑换/佩戴的头像框。 */
data class AvatarFrame(
    val id: String,
    val label: String,
    val colors: List<Color>,
    val cost: Int = 0
)

object AvatarFrames {
    const val NONE = "none"

    private val DefaultBorder = Color(0xFFEADCBF)

    val all = listOf(
        AvatarFrame(NONE, "默认", listOf(DefaultBorder, DefaultBorder)),
        AvatarFrame("gold", "奶油金", listOf(CreamGold, Color(0xFFF7EBC4)), 500),
        AvatarFrame("peach", "蜜桃粉", listOf(PeachPink, Color(0xFFE7A58F)), 500),
        AvatarFrame("sage", "鼠尾草", listOf(Sage, SageDark), 500),
        AvatarFrame("lavender", "薰衣草", listOf(Color(0xFFB49AC8), Color(0xFF9278A8)), 500)
    )

    fun byId(id: String): AvatarFrame = all.firstOrNull { it.id == id } ?: all.first()
}

/** 头像 + 可选头像框；自定义图片从持久化 URI 读取，失败回退默认人像。 */
@Composable
fun UserAvatar(
    uri: String,
    size: Dp,
    frameId: String = AvatarFrames.NONE,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var bitmap by remember(uri) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(uri) {
        bitmap = if (uri.isBlank()) {
            null
        } else {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(Uri.parse(uri))?.use { input ->
                        BitmapFactory.decodeStream(input)?.asImageBitmap()
                    }
                }.getOrNull()
            }
        }
    }
    val frame = AvatarFrames.byId(frameId)
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(DuoCard)
            .border(
                width = if (frameId == AvatarFrames.NONE) 2.dp else 4.dp,
                brush = Brush.sweepGradient(frame.colors),
                shape = CircleShape
            )
            .padding(if (frameId == AvatarFrames.NONE) 2.dp else 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(PeachPink.copy(alpha = 0.45f)),
            contentAlignment = Alignment.Center
        ) {
            val image = bitmap
            if (image != null) {
                Image(
                    bitmap = image,
                    contentDescription = "用户头像",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = "默认头像",
                    tint = DuoChocolate,
                    modifier = Modifier.size(size * 0.5f)
                )
            }
        }
    }
}
