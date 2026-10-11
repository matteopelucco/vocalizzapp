package com.matteopelucco.vocalizzapp.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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

/** Gli ultimi testi, sempre visibili nella schermata principale, con "Vedi tutti" se ce ne sono altri. */
@Composable
internal fun RecentSection(
    items: List<SavedTranscript>,
    total: Int,
    enabled: Boolean,
    onOpen: (SavedTranscript) -> Unit,
    onSeeAll: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            stringResource(R.string.recent_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        items.forEach { TranscriptRow(it, enabled, onOpen) }
        if (total > items.size) {
            OutlinedButton(
                onClick = onSeeAll,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) { Text(stringResource(R.string.see_all), style = MaterialTheme.typography.labelLarge) }
        }
    }
}

/** Una riga della cronologia: la prima riga del testo (con i puntini se continua) e data e ora. */
@Composable
internal fun TranscriptRow(item: SavedTranscript, enabled: Boolean, onClick: (SavedTranscript) -> Unit) {
    Surface(
        onClick = { onClick(item) },
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                singleLinePreview(item.text),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                formatTranscriptDate(item.createdAtMillis),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Tutti i testi salvati, dal piu' recente; scorrendo verso il fondo se ne caricano altri 10. */
@Composable
fun HistoryScreen(
    history: HistoryUi,
    enabled: Boolean,
    onBack: () -> Unit,
    onLoadMore: () -> Unit,
    onOpen: (SavedTranscript) -> Unit,
) {
    BackHandler(onBack = onBack)
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
        Text(
            stringResource(R.string.history_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(history.items, key = { it.id }) { TranscriptRow(it, enabled, onOpen) }
            if (history.loading) {
                item(key = "loading") {
                    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
                    }
                }
            }
        }
    }
}
