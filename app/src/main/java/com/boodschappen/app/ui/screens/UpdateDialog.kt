package com.boodschappen.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.boodschappen.app.ui.theme.*
import com.boodschappen.app.data.remote.AppVersion
import com.boodschappen.app.viewmodel.UpdateState

@Composable
fun UpdateDialog(
    updateState : UpdateState,
    isDark      : Boolean,
    onDownload  : (String) -> Unit,
    onDismiss   : () -> Unit
) {
    if (updateState is UpdateState.Idle) return

    Dialog(
        onDismissRequest = {
            if (updateState !is UpdateState.Downloading) onDismiss()
        },
        properties = DialogProperties(dismissOnBackPress = updateState !is UpdateState.Downloading)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(if (isDark) Dark700 else Color.White)
                .border(
                    1.dp,
                    if (isDark) Color.White.copy(0.12f) else Violet90,
                    RoundedCornerShape(28.dp)
                )
                .padding(24.dp)
        ) {
            when (updateState) {

                // ── Update beschikbaar ────────────────────────────────────────
                is UpdateState.Available -> AvailableContent(
                    version  = updateState.version,
                    isDark   = isDark,
                    onDownload = { onDownload(updateState.version.downloadUrl) },
                    onDismiss  = onDismiss
                )

                // ── Downloaden ────────────────────────────────────────────────
                is UpdateState.Downloading -> DownloadingContent(
                    progress = updateState.progress,
                    isDark   = isDark
                )

                // ── Klaar om te installeren ───────────────────────────────────
                is UpdateState.ReadyToInstall -> ReadyContent(isDark = isDark)

                // ── Fout ──────────────────────────────────────────────────────
                is UpdateState.Error -> ErrorContent(
                    message  = updateState.message,
                    isDark   = isDark,
                    onDismiss = onDismiss
                )

                else -> Unit
            }
        }
    }
}

// ── Update beschikbaar ────────────────────────────────────────────────────────

@Composable
private fun AvailableContent(
    version: AppVersion,
    isDark: Boolean,
    onDownload: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {

        // Icon met glow
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            if (isDark) Violet30.copy(0.8f) else Violet90,
                            Color.Transparent
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text("🚀", fontSize = 36.sp)
        }

        Spacer(Modifier.height(16.dp))

        Text(
            "Update beschikbaar!",
            style      = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color      = if (isDark) Color.White else Color(0xFF1A1040)
        )

        Spacer(Modifier.height(6.dp))

        // Versie badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            if (isDark) Violet80.copy(0.25f) else Violet90,
                            if (isDark) Emerald80.copy(0.25f) else Emerald90
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Text(
                "v${version.versionName}",
                style      = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color      = if (isDark) Violet80 else Violet40
            )
        }

        if (version.releaseNotes.isNotBlank()) {
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isDark) Dark800.copy(0.6f) else Violet95
                    )
                    .padding(14.dp)
            ) {
                Column {
                    Text(
                        "Wat is er nieuw:",
                        style      = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(4.dp))
                    version.releaseNotes.split("\n").forEach { line ->
                        if (line.isNotBlank()) {
                            Row(verticalAlignment = Alignment.Top) {
                                Text("• ", color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodySmall)
                                Text(
                                    line.trimStart('-', ' '),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Download knop
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            if (isDark) Violet80 else Violet40,
                            if (isDark) Emerald80 else Emerald40
                        )
                    )
                )
                .clickable(onClick = onDownload),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Download, null, tint = Color.White,
                    modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Downloaden & installeren",
                    color      = Color.White,
                    fontWeight = FontWeight.Bold,
                    style      = MaterialTheme.typography.titleSmall)
            }
        }

        Spacer(Modifier.height(10.dp))

        TextButton(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "Later herinneren",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ── Downloadvoortgang ─────────────────────────────────────────────────────────

@Composable
private fun DownloadingContent(progress: Int, isDark: Boolean) {
    val animProgress by animateFloatAsState(
        targetValue   = progress / 100f,
        animationSpec = tween(300),
        label         = "dl_progress"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Animated download icon
        val infiniteTransition = rememberInfiniteTransition(label = "bounce")
        val offsetY by infiniteTransition.animateFloat(
            initialValue = 0f, targetValue = 6f,
            animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
            label = "bounce_y"
        )

        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(listOf(
                        if (isDark) Violet30.copy(0.8f) else Violet90,
                        Color.Transparent
                    ))
                )
                .offset(y = offsetY.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("⬇️", fontSize = 34.sp)
        }

        Spacer(Modifier.height(20.dp))

        Text(
            "Downloaden...",
            style      = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color      = if (isDark) Color.White else Color(0xFF1A1040)
        )

        Spacer(Modifier.height(6.dp))

        Text(
            "$progress%",
            style      = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color      = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(12.dp))

        // Gradient progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(if (isDark) Dark600 else Violet90)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animProgress)
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                if (isDark) Violet80 else Violet40,
                                if (isDark) Emerald80 else Emerald40
                            )
                        )
                    )
            )
        }

        Spacer(Modifier.height(10.dp))

        Text(
            "Even geduld, installatie start automatisch",
            style  = MaterialTheme.typography.bodySmall,
            color  = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// ── Klaar om te installeren ───────────────────────────────────────────────────

@Composable
private fun ReadyContent(isDark: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(listOf(
                        if (isDark) Emerald20.copy(0.8f) else Emerald90,
                        Color.Transparent
                    ))
                ),
            contentAlignment = Alignment.Center
        ) { Text("✅", fontSize = 36.sp) }

        Spacer(Modifier.height(16.dp))

        Text("Download klaar!",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = if (isDark) Color.White else Color(0xFF1A1040))

        Spacer(Modifier.height(8.dp))

        Text("De installatie start nu automatisch...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center)

        Spacer(Modifier.height(12.dp))
        CircularProgressIndicator(
            color = if (isDark) Emerald80 else Emerald40,
            modifier = Modifier.size(32.dp),
            strokeWidth = 3.dp,
            strokeCap = StrokeCap.Round
        )
    }
}

// ── Fout ──────────────────────────────────────────────────────────────────────

@Composable
private fun ErrorContent(message: String, isDark: Boolean, onDismiss: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("❌", fontSize = 40.sp)
        Spacer(Modifier.height(12.dp))
        Text("Download mislukt",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isDark) Color.White else Color(0xFF1A1040))
        Spacer(Modifier.height(8.dp))
        Text(message, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text("Sluiten")
        }
    }
}
