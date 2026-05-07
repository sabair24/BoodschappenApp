package com.boodschappen.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.boodschappen.app.viewmodel.ShareUiState
import com.boodschappen.app.viewmodel.ShoppingViewModel
import com.boodschappen.app.viewmodel.SyncMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareSheet(
    viewModel: ShoppingViewModel,
    onDismiss: () -> Unit
) {
    val shareUiState by viewModel.shareUiState.collectAsState()
    val syncMode by viewModel.syncMode.collectAsState()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var joinCode by remember { mutableStateOf("") }
    var showJoinInput by remember { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current

    ModalBottomSheet(
        onDismissRequest = {
            viewModel.resetShareError()
            onDismiss()
        },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.PeopleAlt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text("Lijst delen", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        when (syncMode) {
                            is SyncMode.Shared -> "Live gesynchroniseerd ✓"
                            else -> "Deel je lijst met je partner"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (syncMode is SyncMode.Shared)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(24.dp))

            when (val state = shareUiState) {

                // ── Niet actief ────────────────────────────────────────────────
                is ShareUiState.Idle -> {
                    if (!showJoinInput) {
                        // Create button
                        ListShareButton(
                            icon = Icons.Outlined.Add,
                            title = "Nieuwe gedeelde lijst maken",
                            subtitle = "Genereer een code en deel die met je partner",
                            color = MaterialTheme.colorScheme.primary,
                            onClick = { viewModel.createSharedList() }
                        )
                        Spacer(Modifier.height(12.dp))
                        // Join button
                        ListShareButton(
                            icon = Icons.Outlined.GroupAdd,
                            title = "Meedoen met bestaande lijst",
                            subtitle = "Voer de code in die je partner heeft gedeeld",
                            color = MaterialTheme.colorScheme.secondary,
                            onClick = { showJoinInput = true }
                        )
                    } else {
                        JoinInputField(
                            value = joinCode,
                            onValueChange = { joinCode = it.uppercase().take(6) },
                            onJoin = {
                                keyboard?.hide()
                                viewModel.joinSharedList(joinCode)
                            },
                            onBack = { showJoinInput = false; joinCode = "" }
                        )
                    }
                }

                // ── Laden ──────────────────────────────────────────────────────
                is ShareUiState.Loading -> {
                    Spacer(Modifier.height(16.dp))
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(16.dp))
                    Text("Even geduld...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))
                }

                // ── Actief: code tonen ─────────────────────────────────────────
                is ShareUiState.Active -> {
                    ActiveSyncCard(
                        code = state.code,
                        onCopy = {
                            clipboard.setText(AnnotatedString(state.code))
                        },
                        onShare = { viewModel.shareList(context) },
                        onStop = {
                            viewModel.stopSharing()
                            onDismiss()
                        }
                    )
                }

                // ── Fout ───────────────────────────────────────────────────────
                is ShareUiState.Error -> {
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.ErrorOutline,
                                null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    state.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                if (state.message.contains("mislukt") || state.message.contains("aanmaken")) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "Controleer je internetverbinding en probeer opnieuw.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = { viewModel.resetShareError(); showJoinInput = false },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Opnieuw proberen") }
                }
            }
        }
    }
}

@Composable
private fun ListShareButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Row(
            Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .background(color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, null, tint = color)
        }
    }
}

@Composable
private fun JoinInputField(
    value: String,
    onValueChange: (String) -> Unit,
    onJoin: () -> Unit,
    onBack: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "Voer de lijstcode in",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Vraag de code aan je partner",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text("ABC123", fontFamily = FontFamily.Monospace, fontSize = 24.sp) },
            textStyle = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                letterSpacing = 8.sp
            ),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { if (value.length == 6) onJoin() })
        )

        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                Text("Terug")
            }
            Button(
                onClick = onJoin,
                modifier = Modifier.weight(1f),
                enabled = value.length == 6
            ) {
                Icon(Icons.Default.Link, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Verbinden")
            }
        }
    }
}

@Composable
private fun ActiveSyncCard(
    code: String,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onStop: () -> Unit
) {
    var copied by remember { mutableStateOf(false) }

    // Live indicator pulsing
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(900),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {

        // Live indicator
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                Modifier
                    .size(10.dp)
                    .background(Color(0xFF4CAF50).copy(alpha = alpha), CircleShape)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Live gesynchroniseerd",
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFF4CAF50),
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(20.dp))

        // Code display
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Jouw lijstcode",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    code,
                    style = MaterialTheme.typography.displaySmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    letterSpacing = 8.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Deel deze code met je partner",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Action buttons
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = { onCopy(); copied = true },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    if (copied) Icons.Default.Check else Icons.Outlined.ContentCopy,
                    null, Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(if (copied) "Gekopieerd!" else "Kopiëren")
            }
            Button(
                onClick = onShare,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Outlined.Share, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Versturen")
            }
        }

        Spacer(Modifier.height(20.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        TextButton(
            onClick = onStop,
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.Outlined.LinkOff, null, Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Stoppen met delen")
        }
    }
}
