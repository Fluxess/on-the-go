package ru.routealert.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.routealert.app.R
import ru.routealert.app.data.LegalDocs
import ru.routealert.app.ui.theme.Glass
import ru.routealert.app.ui.theme.GlassBlue
import ru.routealert.app.ui.theme.LiquidGlassPanel
import ru.routealert.app.ui.theme.liquidGlass

@Composable
fun AboutScreen(
    onBack: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenTerms: () -> Unit,
    onOpenSecretAdmin: () -> Unit = {}
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    var versionTapCount by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .padding(bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .liquidGlass(shape = CircleShape, colors = Glass.MapFillStrong)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Text("←", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(14.dp))
            Text(
                "О программе",
                style = MaterialTheme.typography.headlineMedium,
                color = colors.onBackground,
                fontWeight = FontWeight.Bold
            )
        }

        // Header app banner
        LiquidGlassPanel(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = Glass.PanelFill
        ) {
            Column(
                modifier = Modifier
                    .background(colors.surface.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .liquidGlass(shape = CircleShape, colors = Glass.MapFillStrong),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        modifier = Modifier.size(56.dp)
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "on-the-go",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Версия ${LegalDocs.APP_VERSION}",
                    modifier = Modifier.clickable {
                        versionTapCount++
                        if (versionTapCount >= 5) {
                            versionTapCount = 0
                            onOpenSecretAdmin()
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurface.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Умный GPS-будильник и оповещения по точкам на карте во время поездок.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurface.copy(alpha = 0.85f),
                    lineHeight = 20.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        // Contacts block
        LiquidGlassPanel(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = Glass.PanelFill
        ) {
            Column(
                modifier = Modifier
                    .background(colors.surface.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Связь с разработчиком",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                ContactItem(
                    label = "Telegram",
                    value = LegalDocs.TELEGRAM_HANDLE,
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(LegalDocs.TELEGRAM_URL))
                        runCatching { context.startActivity(intent) }
                    }
                )
                ContactItem(
                    label = "Email поддержки",
                    value = LegalDocs.SUPPORT_EMAIL,
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${LegalDocs.SUPPORT_EMAIL}"))
                        runCatching { context.startActivity(intent) }
                    }
                )
            }
        }

        // Legal block
        LiquidGlassPanel(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = Glass.PanelFill
        ) {
            Column(
                modifier = Modifier
                    .background(colors.surface.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Правовая информация",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface,
                    fontWeight = FontWeight.SemiBold
                )

                AboutActionButton(
                    label = "Политика конфиденциальности",
                    onClick = onOpenPrivacy
                )

                AboutActionButton(
                    label = "Условия использования и отказ от ответственности",
                    onClick = onOpenTerms
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "© 2026 on-the-go. Все права защищены.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
private fun ContactItem(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = GlassBlue,
            fontWeight = FontWeight.Medium,
            softWrap = false,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AboutActionButton(
    label: String,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(shape = RoundedCornerShape(16.dp), colors = Glass.MapFillStrong)
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = colors.primary),
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Text("→", color = Color.White.copy(alpha = 0.7f), fontSize = 16.sp)
        }
    }
}
