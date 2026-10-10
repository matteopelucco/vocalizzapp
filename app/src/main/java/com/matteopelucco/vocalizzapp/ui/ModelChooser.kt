package com.matteopelucco.vocalizzapp.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.matteopelucco.vocalizzapp.DeviceInfo
import com.matteopelucco.vocalizzapp.DownloadState
import com.matteopelucco.vocalizzapp.ModelCatalog
import com.matteopelucco.vocalizzapp.ModelOption
import com.matteopelucco.vocalizzapp.R
import com.matteopelucco.vocalizzapp.formatSize
import kotlin.math.ceil

/** Tutto cio' che la schermata di scelta deve sapere sui modelli. */
data class ModelsUi(
    val installed: Set<String>,
    val active: String?,
    val download: DownloadState,
    val device: DeviceInfo,
    /** Una trascrizione e' in corso: niente eliminazioni. */
    val busy: Boolean,
)

/**
 * Scelta della qualita' della trascrizione. Al primo avvio ([firstRun]) e' la prima schermata;
 * in seguito si apre dal nome del modello in alto e permette di cambiare, scaricare o eliminare.
 */
@Composable
fun ModelChooserScreen(
    models: ModelsUi,
    firstRun: Boolean,
    onDownload: (String) -> Unit,
    onUse: (String) -> Unit,
    onDelete: (String) -> Unit,
    onCancelDownload: () -> Unit,
    onBack: () -> Unit,
) {
    val running = models.download as? DownloadState.Running
    var selectedId by rememberSaveable { mutableStateOf(models.active ?: ModelCatalog.recommended.id) }
    LaunchedEffect(running?.modelId) { running?.let { selectedId = it.modelId } }
    var pendingDelete by remember { mutableStateOf<ModelOption?>(null) }
    if (!firstRun) BackHandler(onBack = onBack)

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding(),
    ) {
        if (firstRun) Header() else BackBar(onBack)

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(if (firstRun) R.string.chooser_title_first else R.string.chooser_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                stringResource(R.string.chooser_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))

            ModelCatalog.options.forEach { option ->
                val installed = option.id in models.installed
                val inUse = option.id == models.active
                ModelCard(
                    option = option,
                    selected = option.id == selectedId,
                    installed = installed,
                    inUse = inUse,
                    device = models.device,
                    enabled = running == null,
                    canDelete = installed && !inUse && !models.busy && running == null,
                    onSelect = { selectedId = option.id },
                    onDelete = { pendingDelete = option },
                )
            }

            Text(
                stringResource(
                    R.string.device_line,
                    "${ceil(models.device.totalRamBytes / 1_000_000_000.0).toInt()} GB",
                    formatSize(models.device.freeBytes),
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
        }

        BottomAction(models, selectedId, firstRun, onDownload, onUse, onCancelDownload)
    }

    pendingDelete?.let { option ->
        val name = stringResource(option.title)
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.delete_title, name)) },
            text = { Text(stringResource(R.string.delete_body, formatSize(option.totalBytes))) },
            confirmButton = {
                TextButton(onClick = { onDelete(option.id); pendingDelete = null }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun BackBar(onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
        Row(
            Modifier
                .clip(RoundedCornerShape(50))
                .clickable(role = Role.Button, onClick = onBack)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(ArrowBackIcon, contentDescription = null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.width(6.dp))
            Text(
                stringResource(R.string.back),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@Composable
private fun ModelCard(
    option: ModelOption,
    selected: Boolean,
    installed: Boolean,
    inUse: Boolean,
    device: DeviceInfo,
    enabled: Boolean,
    canDelete: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val enoughSpace = installed || device.hasEnoughSpaceFor(option)
    val enoughRam = device.hasEnoughRamFor(option)
    val container by animateColorAsState(
        if (selected) scheme.primaryContainer.copy(alpha = 0.4f) else scheme.surface,
        label = "card",
    )

    Surface(
        shape = MaterialTheme.shapes.large,
        color = container,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) scheme.primary else scheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onSelect),
    ) {
        Row(Modifier.padding(16.dp).animateContentSize(), verticalAlignment = Alignment.Top) {
            RadioDot(selected)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(option.title),
                        style = MaterialTheme.typography.titleMedium,
                        color = scheme.onSurface,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        formatSize(option.totalBytes),
                        style = MaterialTheme.typography.labelLarge,
                        color = scheme.onSurface,
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (inUse) {
                        Pill(stringResource(R.string.badge_in_use), scheme.primary, scheme.onPrimary)
                    } else if (installed) {
                        Pill(stringResource(R.string.badge_installed), scheme.surfaceVariant, scheme.onSurfaceVariant)
                    }
                    if (option.recommended) {
                        Pill(stringResource(R.string.badge_recommended), scheme.primaryContainer, scheme.onPrimaryContainer)
                    }
                }

                Text(
                    stringResource(option.tagline),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                )

                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Meter(stringResource(R.string.meter_precision), option.precision)
                    Meter(stringResource(R.string.meter_speed), option.speed)
                }

                option.note?.let {
                    Text(stringResource(it), style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
                }
                if (!enoughSpace) {
                    Text(
                        stringResource(R.string.warn_space, formatSize(option.totalBytes)),
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.error,
                    )
                } else if (!enoughRam) {
                    Text(
                        stringResource(R.string.warn_ram),
                        style = MaterialTheme.typography.labelMedium,
                        color = warningColor(),
                    )
                }
                if (canDelete) {
                    Text(
                        stringResource(R.string.delete),
                        style = MaterialTheme.typography.labelLarge,
                        color = scheme.error,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(role = Role.Button, onClick = onDelete)
                            .padding(vertical = 4.dp, horizontal = 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun RadioDot(selected: Boolean) {
    val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    Box(
        Modifier.padding(top = 2.dp).size(22.dp).border(2.dp, color, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) Box(Modifier.size(10.dp).clip(CircleShape).background(color))
    }
}

/** Indicatore a 5 tacche (precisione o velocita'). */
@Composable
private fun Meter(label: String, value: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(84.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(5) { i ->
                Box(
                    Modifier
                        .width(18.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (i < value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                )
            }
        }
    }
}

@Composable
private fun Pill(text: String, container: Color, content: Color) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = content,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(container)
            .padding(horizontal = 10.dp, vertical = 3.dp),
    )
}

@Composable
private fun BottomAction(
    models: ModelsUi,
    selectedId: String,
    firstRun: Boolean,
    onDownload: (String) -> Unit,
    onUse: (String) -> Unit,
    onCancelDownload: () -> Unit,
) {
    val option = ModelCatalog.byId(selectedId) ?: return
    val running = models.download as? DownloadState.Running
    val failed = models.download as? DownloadState.Failed
    val installed = selectedId in models.installed
    val inUse = selectedId == models.active

    Column(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp).animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (running != null) {
            val progress by animateFloatAsState(running.progress, label = "download")
            val name = ModelCatalog.byId(running.modelId)?.let { stringResource(it.title) }.orEmpty()
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer,
                strokeCap = StrokeCap.Round,
                gapSize = 0.dp,
                drawStopIndicator = {},
            )
            Text(
                stringResource(R.string.model_downloading, name, (running.progress * 100).toInt()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = onCancelDownload,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) { Text(stringResource(R.string.cancel), style = MaterialTheme.typography.labelLarge) }
        } else {
            if (failed != null && !installed) {
                Text(
                    stringResource(R.string.model_failed, failed.message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            val size = formatSize(option.totalBytes)
            val enoughSpace = installed || models.device.hasEnoughSpaceFor(option)
            val (label, enabled, action) = when {
                inUse -> Triple(stringResource(R.string.action_in_use), false) {}
                installed -> Triple(stringResource(R.string.action_use), true) { onUse(selectedId) }
                failed != null -> Triple(stringResource(R.string.action_retry, size), enoughSpace) { onDownload(selectedId) }
                firstRun -> Triple(stringResource(R.string.action_download_start, size), enoughSpace) { onDownload(selectedId) }
                else -> Triple(stringResource(R.string.action_download, size), enoughSpace) { onDownload(selectedId) }
            }
            Button(
                onClick = action,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = MaterialTheme.shapes.medium,
                elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
            ) { Text(label, style = MaterialTheme.typography.labelLarge) }
            if (firstRun) {
                Text(
                    stringResource(R.string.privacy_note),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
