package com.matteopelucco.vocalizzapp.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.matteopelucco.vocalizzapp.HistoryUi
import com.matteopelucco.vocalizzapp.R
import com.matteopelucco.vocalizzapp.SavedTranscript
import com.matteopelucco.vocalizzapp.formatTranscriptDate
import com.matteopelucco.vocalizzapp.singleLinePreview

/** Cio' che serve alla UI della cronologia: gli ultimi testi, quanti ce ne sono in tutto e la lista completa. */
data class SavedUi(
    val recent: List<SavedTranscript>,
    val total: Int,
    val history: HistoryUi,
)

/** Richieste di eliminazione in attesa di conferma: un singolo testo oppure tutta la cronologia. */
@Stable
internal class DeleteRequests {
    var item by mutableStateOf<SavedTranscript?>(null)
    var all by mutableStateOf(false)
}

@Composable
internal fun rememberDeleteRequests(): DeleteRequests = remember { DeleteRequests() }

/** Le finestre di conferma per le richieste di [requests]: niente viene eliminato senza un "Elimina" esplicito. */
@Composable
internal fun DeleteDialogs(
    requests: DeleteRequests,
    total: Int,
    onDeleteItem: (SavedTranscript) -> Unit,
    onDeleteAll: () -> Unit,
) {
    requests.item?.let { item ->
        ConfirmDeleteDialog(
            title = stringResource(R.string.delete_text_title),
            text = stringResource(R.string.delete_text_body, previewForDialog(item.text)),
            onConfirm = {
                requests.item = null
                onDeleteItem(item)
            },
            onDismiss = { requests.item = null },
        )
    }
    if (requests.all) {
        ConfirmDeleteDialog(
            title = stringResource(R.string.delete_all_title),
            text = pluralStringResource(R.plurals.delete_all_body, total, total),
            confirmLabel = stringResource(R.string.delete_all_confirm),
            onConfirm = {
                requests.all = false
                onDeleteAll()
            },
            onDismiss = { requests.all = false },
        )
    }
}

/** Finestra di conferma con "Elimina" in rosso e "Annulla". */
@Composable
internal fun ConfirmDeleteDialog(
    title: String,
    text: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmLabel: String = stringResource(R.string.delete),
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmLabel, color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

/** Le prime parole del testo, per ricordare all'utente quale sta per eliminare. */
internal fun previewForDialog(text: String): String {
    val line = singleLinePreview(text)
    return if (line.length <= 70) line else line.take(70).trimEnd() + "…"
}

/** Gli ultimi testi, sempre visibili nella schermata principale, con "Vedi tutti" se ce ne sono altri. */
@Composable
internal fun RecentSection(
    items: List<SavedTranscript>,
    total: Int,
    onSeeAll: () -> Unit,
    onDeleteItem: (Long) -> Unit,
    onDeleteAll: () -> Unit,
) {
    val requests = rememberDeleteRequests()
    // Una sola card aperta alla volta; toccarla di nuovo la richiude.
    var expandedId by rememberSaveable { mutableStateOf<Long?>(null) }
    Column(Modifier.fillMaxWidth().padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.recent_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { requests.all = true }) {
                Text(
                    stringResource(R.string.delete_all),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        items.forEach { item ->
            TranscriptRow(
                item = item,
                expanded = expandedId == item.id,
                onToggle = { expandedId = if (expandedId == item.id) null else item.id },
                onDelete = { requests.item = it },
            )
        }
        if (total > items.size) {
            OutlinedButton(
                onClick = onSeeAll,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) { Text(stringResource(R.string.see_all), style = MaterialTheme.typography.labelLarge) }
        }
    }
    DeleteDialogs(requests, total, onDeleteItem = { onDeleteItem(it.id) }, onDeleteAll = onDeleteAll)
}

/**
 * Un testo della cronologia, come card che si apre e si chiude sul posto.
 * Chiusa: la prima riga (con i puntini se continua), la data e il cestino.
 * Aperta: il testo intero con Elimina, Condividi e Copia. Si richiude toccando l'intestazione.
 * L'eliminazione chiede [onDelete]; la conferma e' a carico di chi usa la card.
 */
@Composable
internal fun TranscriptRow(
    item: SavedTranscript,
    expanded: Boolean,
    onToggle: () -> Unit,
    onDelete: (SavedTranscript) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = scheme.surface,
        border = BorderStroke(
            width = if (expanded) 1.5.dp else 1.dp,
            color = if (expanded) scheme.primary.copy(alpha = 0.6f) else scheme.outlineVariant,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.animateContentSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button, onClick = onToggle)
                    .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (expanded) {
                        Text(
                            stringResource(
                                R.string.status_saved,
                                item.audioSeconds.toInt(),
                                formatTranscriptDate(item.createdAtMillis),
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            color = scheme.onSurface,
                        )
                    } else {
                        Text(
                            singleLinePreview(item.text),
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            formatTranscriptDate(item.createdAtMillis),
                            style = MaterialTheme.typography.labelMedium,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                }
                if (expanded) {
                    // Solo un'indicazione: la chiusura la gestisce l'intera intestazione.
                    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                        Icon(
                            ChevronUpIcon,
                            contentDescription = stringResource(R.string.collapse),
                            modifier = Modifier.size(22.dp),
                            tint = scheme.onSurfaceVariant,
                        )
                    }
                } else {
                    IconButton(onClick = { onDelete(item) }) {
                        Icon(
                            TrashIcon,
                            contentDescription = stringResource(R.string.delete_text_desc),
                            modifier = Modifier.size(20.dp),
                            tint = scheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (expanded) {
                Column(
                    Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SelectionContainer {
                        Text(item.text, style = MaterialTheme.typography.bodyLarge, color = scheme.onSurface)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                        ActionPill(icon = TrashIcon, label = stringResource(R.string.delete), highlighted = false) {
                            onDelete(item)
                        }
                        ShareButton(item.text)
                        CopyButton(item.text)
                    }
                }
            }
        }
    }
}

/** Tutti i testi salvati, dal piu' recente; scorrendo verso il fondo se ne caricano altri 10. */
@Composable
fun HistoryScreen(
    history: HistoryUi,
    total: Int,
    onBack: () -> Unit,
    onLoadMore: () -> Unit,
    onDeleteItem: (Long) -> Unit,
    onDeleteAll: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val requests = rememberDeleteRequests()
    var expandedId by rememberSaveable { mutableStateOf<Long?>(null) }
    val listState = rememberLazyListState()
    // Vero quando mancano pochi elementi alla fine di cio' che e' gia' caricato.
    val nearEnd by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            last >= info.totalItemsCount - 3
        }
    }
    LaunchedEffect(nearEnd, history.items.size, history.loading, history.endReached) {
        if (nearEnd) onLoadMore()
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding(),
    ) {
        BackBar(onBack)
        Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.history_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { requests.all = true }) {
                Text(
                    stringResource(R.string.delete_all),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(history.items, key = { it.id }) { item ->
                TranscriptRow(
                    item = item,
                    expanded = expandedId == item.id,
                    onToggle = { expandedId = if (expandedId == item.id) null else item.id },
                    onDelete = { requests.item = it },
                )
            }
            if (history.loading) {
                item(key = "loading") {
                    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
                    }
                }
            }
        }
    }
    DeleteDialogs(requests, total, onDeleteItem = { onDeleteItem(it.id) }, onDeleteAll = onDeleteAll)
}
