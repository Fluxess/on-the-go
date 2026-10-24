package ru.routealert.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.routealert.app.data.RoutePoint
import ru.routealert.app.share.PointShare
import ru.routealert.app.ui.theme.Glass
import ru.routealert.app.ui.theme.LiquidGlassChip
import ru.routealert.app.ui.theme.LiquidGlassPanel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharePointsSheet(
    points: List<RoutePoint>,
    isPro: Boolean,
    onImportFromText: (String) -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val enabled = points.filter { it.enabled }.ifEmpty { points }
    val qrLink = remember(enabled) { PointShare.toDeepLink(enabled).toString() }
    val qr = remember(qrLink) { PointShare.qrBitmap(qrLink, 640) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xF2161618),
        scrimColor = Color.Black.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Поделиться точками · Pro",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Покажите QR-код или отправьте ссылку. На устройстве получателя должно быть установлено on-the-go.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.72f),
                modifier = Modifier.padding(top = 6.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(14.dp))

            if (!isPro) {
                LiquidGlassPanel(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = Glass.PanelFill
                ) {
                    Text(
                        "Функция доступна в Pro. Включите Pro в разделе «Помощь» для теста.",
                        modifier = Modifier.padding(14.dp),
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
                Spacer(Modifier.height(12.dp))
                LiquidGlassChip("Закрыть", selected = false, onClick = onDismiss)
                return@Column
            }

            if (enabled.isEmpty()) {
                Text("Нет сохранённых точек для шаринга", color = Color.White.copy(alpha = 0.8f))
                Spacer(Modifier.height(14.dp))
                LiquidGlassChip("Закрыть", selected = false, onClick = onDismiss)
                return@Column
            }

            LiquidGlassPanel(
                shape = RoundedCornerShape(20.dp),
                colors = Glass.PanelFill
            ) {
                Image(
                    bitmap = qr.asImageBitmap(),
                    contentDescription = "QR код точек",
                    modifier = Modifier
                        .padding(16.dp)
                        .size(230.dp)
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "${enabled.size} точ. · ${enabled.joinToString { it.name }}",
                color = Color.White.copy(alpha = 0.82f),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(16.dp))

            // Action buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                LiquidGlassChip("Поделиться", selected = false) {
                    val shareText = PointShare.toShareText(enabled)
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(Intent.createChooser(send, "Поделиться маршрутом"))
                }

                LiquidGlassChip("Копировать", selected = false) {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    if (clipboard != null) {
                        val shareText = PointShare.toShareText(enabled)
                        clipboard.setPrimaryClip(ClipData.newPlainText("Маршрут on-the-go", shareText))
                        Toast.makeText(context, "Ссылка скопирована в буфер", Toast.LENGTH_SHORT).show()
                    }
                }

                LiquidGlassChip("Вставить", selected = false) {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    val clip = clipboard?.primaryClip?.getItemAt(0)?.text?.toString()
                    if (!clip.isNullOrBlank()) {
                        val parsed = PointShare.parse(clip)
                        if (parsed.isNotEmpty()) {
                            onImportFromText(clip)
                            onDismiss()
                        } else {
                            Toast.makeText(context, "В буфере нет данных точек", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Буфер обмена пуст", Toast.LENGTH_SHORT).show()
                    }
                }

                LiquidGlassChip("Закрыть", selected = false, onClick = onDismiss)
            }
        }
    }
}
