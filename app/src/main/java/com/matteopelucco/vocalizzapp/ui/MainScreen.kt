package com.matteopelucco.vocalizzapp.ui

import android.content.Intent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.matteopelucco.vocalizzapp.AudioInfo
import com.matteopelucco.vocalizzapp.R
import com.matteopelucco.vocalizzapp.UiState
import kotlinx.coroutines.delay

@Composable
fun MainScreen(
    audio: AudioInfo?,
    state: UiState,
    onPickFile: () -> Unit,
    onCancel: () -> Unit,
) {
    val working = state is UiState.Decoding || state is UiState.Transcribing
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding(),
    ) {
        Header()
        Box(Modifier.weight(1f).fillMaxWidth()) {
            Crossfade(targetState = audio == null && state is UiState.Idle, label = "content") { empty ->
                if (empty) EmptyState() else Content(audio, state)
            }
        }
        Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
            if (working) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) { Text(stringResource(R.string.cancel), style = MaterialTheme.typography.labelLarge) }
            } else {
                Button(
                    onClick = onPickFile,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(),
                    elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
                ) { Text(stringResource(R.string.pick_file), style = MaterialTheme.typography.labelLarge) }
            }
        }
    }
}

@Composable
private fun Header() {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(painterResource(R.drawable.ic_launcher_foreground), contentDescription = null, Modifier.size(40.dp))
        Spacer(Modifier.width(2.dp))
        Text(
            stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun EmptyState() {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 36.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(painterResource(R.drawable.ic_launcher_foreground), contentDescription = null, Modifier.size(168.dp))
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.empty_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun Content(audio: AudioInfo?, state: UiState) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        FileCard(audio?.name, statusText(state), state)
        when (state) {
            is UiState.ModelMissing -> MessageCard(stringResource(R.string.model_missing, state.path, state.found), isError = true)
            is UiState.Error -> MessageCard(stringResource(R.string.error, state.message), isError = true)
            UiState.Idle, UiState.Decoding -> Unit
            is UiState.Transcribing -> TranscriptCard(
                state.partialText,
                placeholder = stringResource(R.string.placeholder),
                inProgress = true,
            )
            is UiState.Cancelled -> TranscriptCard(state.partialText, placeholder = stringResource(R.string.cancelled))
            is UiState.Done -> TranscriptCard(state.text, placeholder = stringResource(R.string.no_speech))
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun statusText(state: UiState): String = when (state) {
    UiState.Idle -> ""
    UiState.Decoding -> stringResource(R.string.status_decoding)
    is UiState.Transcribing ->
        if (state.progress > 0f) {
            stringResource(R.string.status_transcribing, (state.progress * 100).toInt())
        } else {
            stringResource(R.string.status_transcribing_start)
        }
    is UiState.Done -> stringResource(R.string.status_done, state.audioSeconds.toInt(), state.elapsedSeconds.toInt())
    is UiState.Cancelled -> stringResource(R.string.status_cancelled)
    is UiState.ModelMissing, is UiState.Error -> ""
}

@Composable
private fun FileCard(name: String?, status: String, state: UiState) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp).animateContentSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                WaveGlyph(active = state is UiState.Decoding || state is UiState.Transcribing)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        name ?: stringResource(R.string.audio_generic),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (status.isNotEmpty()) {
                        Text(
                            status,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (state is UiState.Decoding || state is UiState.Transcribing) {
                Spacer(Modifier.height(14.dp))
                // Prima del primo blocco non c'e' un avanzamento reale: barra indeterminata.
                if (state is UiState.Transcribing && state.progress > 0f) {
                    val progress by animateFloatAsState(state.progress, label = "progress")
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primaryContainer,
                        strokeCap = StrokeCap.Round,
                        gapSize = 0.dp,
                        drawStopIndicator = {},
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primaryContainer,
                        strokeCap = StrokeCap.Round,
                    )
                }
            }
        }
    }
}

private val RestHeights = listOf(10, 18, 24, 14, 8)

/** Piccola icona a "onde sonore", coerente con il logo. Con [active] le barre si muovono: sta lavorando. */
@Composable
private fun WaveGlyph(active: Boolean) {
    Box(
        Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
            if (active) {
                // L'animazione infinita esiste solo mentre serve.
                val transition = rememberInfiniteTransition(label = "wave")
                RestHeights.forEachIndexed { i, rest ->
                    val level by transition.animateFloat(
                        initialValue = 0f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 480 + i * 90, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse,
                            initialStartOffset = StartOffset(i * 130),
                        ),
                        label = "bar$i",
                    )
                    WaveBar((6 + (maxOf(rest, 16) + 4 - 6) * level).dp)
                }
            } else {
                RestHeights.forEach { WaveBar(it.dp) }
            }
        }
    }
}

@Composable
private fun WaveBar(height: Dp) {
    Box(
        Modifier
            .width(3.dp)
            .height(height)
            .clip(RoundedCornerShape(2.dp))
            .background(MaterialTheme.colorScheme.primary),
    )
}

@Composable
private fun TranscriptCard(text: String, placeholder: String, inProgress: Boolean = false) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp).animateContentSize()) {
            if (text.isBlank()) {
                Text(
                    placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 2.dp),
                )
            } else {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                ) {
                    ShareButton(text)
                    CopyButton(text)
                }
                Spacer(Modifier.height(8.dp))
                SelectionContainer {
                    Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                }
            }
            // Finche' il testo non e' completo, i puntini avvisano che sta ancora arrivando altro.
            if (inProgress) {
                Spacer(Modifier.height(10.dp))
                TypingDots()
            }
        }
    }
}

/** Tre puntini colorati che pulsano in sequenza, in loop. */
@Composable
private fun TypingDots() {
    val transition = rememberInfiniteTransition(label = "dots")
    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.primary,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        colors.forEachIndexed { i, color ->
            val level by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 420, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(i * 160),
                ),
                label = "dot$i",
            )
            Box(
                Modifier
                    .size(8.dp)
                    .graphicsLayer {
                        val scale = 0.6f + 0.5f * level
                        scaleX = scale
                        scaleY = scale
                        alpha = 0.3f + 0.7f * level
                    }
                    .background(color, CircleShape),
            )
        }
    }
}

/** Copia il testo negli appunti; per un attimo conferma con "Copiato". */
@Composable
private fun CopyButton(text: String) {
    val clipboard = LocalClipboardManager.current
    var copied by remember(text) { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(2000)
            copied = false
        }
    }
    ActionPill(
        icon = if (copied) CheckIcon else CopyIcon,
        label = stringResource(if (copied) R.string.copied else R.string.copy),
        highlighted = copied,
    ) {
        clipboard.setText(AnnotatedString(text))
        copied = true
    }
}

/** Invia il testo a un'altra app (es. una chat) tramite il selettore di Android. */
@Composable
private fun ShareButton(text: String) {
    val context = LocalContext.current
    val title = stringResource(R.string.share)
    ActionPill(icon = ShareIcon, label = title, highlighted = false) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(send, title))
    }
}

@Composable
private fun ActionPill(icon: ImageVector, label: String, highlighted: Boolean, onClick: () -> Unit) {
    val container = if (highlighted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val content = if (highlighted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(container)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 12.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, Modifier.size(18.dp), tint = content)
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = content)
    }
}

@Composable
private fun MessageCard(message: String, isError: Boolean) {
    val container = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant
    val content = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(shape = MaterialTheme.shapes.large, color = container, modifier = Modifier.fillMaxWidth()) {
        Text(
            message,
            modifier = Modifier.padding(20.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = content,
        )
    }
}
