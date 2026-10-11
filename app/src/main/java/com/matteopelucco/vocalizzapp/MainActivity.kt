package com.matteopelucco.vocalizzapp

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.matteopelucco.vocalizzapp.ui.MainScreen
import com.matteopelucco.vocalizzapp.ui.ModelsUi
import com.matteopelucco.vocalizzapp.ui.SavedUi
import com.matteopelucco.vocalizzapp.ui.VocalizzAppTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) handleIntent(intent)
        setContent {
            VocalizzAppTheme {
                val audio by viewModel.audio.collectAsState()
                val state by viewModel.state.collectAsState()
                val installed by viewModel.installed.collectAsState()
                val activeModel by viewModel.activeModel.collectAsState()
                val device by viewModel.device.collectAsState()
                val download by viewModel.download.collectAsState()
                val chooserOpen by viewModel.chooserOpen.collectAsState()
                val recent by viewModel.recent.collectAsState()
                val totalSaved by viewModel.totalSaved.collectAsState()
                val history by viewModel.history.collectAsState()
                val picker = rememberLauncherForActivityResult(
                    ActivityResultContracts.GetContent()
                ) { uri -> uri?.let(viewModel::onAudioReceived) }

                MainScreen(
                    audio = audio,
                    state = state,
                    models = ModelsUi(
                        installed = installed,
                        active = activeModel,
                        download = download,
                        device = device,
                        busy = state is UiState.Decoding || state is UiState.Transcribing,
                    ),
                    chooserOpen = chooserOpen,
                    saved = SavedUi(recent = recent, total = totalSaved, history = history),
                    onPickFile = { picker.launch("audio/*") },
                    onCancel = viewModel::cancel,
                    onOpenChooser = viewModel::openChooser,
                    onCloseChooser = viewModel::closeChooser,
                    onDownloadModel = viewModel::downloadModel,
                    onUseModel = viewModel::selectModel,
                    onDeleteModel = viewModel::deleteModel,
                    onCancelDownload = viewModel::cancelDownload,
                    onOpenHistory = viewModel::openHistory,
                    onCloseHistory = viewModel::closeHistory,
                    onLoadMoreHistory = viewModel::loadMoreHistory,
                    onDeleteSaved = viewModel::deleteTranscript,
                    onDeleteAllSaved = viewModel::deleteAllTranscripts,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        // Solo build di sviluppo: avvia il download di un modello da adb (`--es download small`).
        val debuggable = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        val downloadId = if (debuggable) intent.getStringExtra("download") else null
        if (debuggable && intent.getBooleanExtra("chooser", false)) viewModel.openChooser()
        if (intent.action != Intent.ACTION_SEND) {
            downloadId?.let(viewModel::downloadModel)
            return
        }
        val uri: Uri? = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION") intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }
        // L'extra "model" serve solo per i test da adb: sceglie il modello Whisper da usare.
        uri?.let { viewModel.onAudioReceived(it, intent.getStringExtra("model")) }
        downloadId?.let(viewModel::downloadModel)
    }
}
