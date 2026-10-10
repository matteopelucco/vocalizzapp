package com.matteopelucco.vocalizzapp

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handleIntent(intent)
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    val audio by viewModel.audio.collectAsState()
                    val state by viewModel.state.collectAsState()
                    val picker = rememberLauncherForActivityResult(
                        ActivityResultContracts.GetContent()
                    ) { uri -> uri?.let(viewModel::onAudioReceived) }

                    Column(
                        Modifier.fillMaxSize().statusBarsPadding().padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        val a = audio
                        Text(
                            a?.name ?: stringResource(R.string.empty_hint),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        when (val s = state) {
                            UiState.Idle -> {}
                            UiState.Decoding -> Text(stringResource(R.string.decoding))
                            is UiState.Transcribing -> {
                                Text(stringResource(R.string.transcribing))
                                LinearProgressIndicator(progress = { s.progress }, modifier = Modifier.fillMaxWidth())
                            }
                            is UiState.ModelMissing -> Text(stringResource(R.string.model_missing, s.path, s.found))
                            is UiState.Error -> Text(stringResource(R.string.error, s.message))
                            is UiState.Done -> {
                                Text(
                                    stringResource(
                                        R.string.done_stats, "%.1f".format(s.audioSeconds), "%.1f".format(s.elapsedSeconds)
                                    ),
                                    style = MaterialTheme.typography.labelMedium,
                                )
                                SelectionContainer(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                                    Text(s.text.ifBlank { stringResource(R.string.no_speech) })
                                }
                            }
                        }
                        Spacer(Modifier.weight(if (state is UiState.Done) 0.0001f else 1f))
                        Button(onClick = { picker.launch("audio/*") }) {
                            Text(stringResource(R.string.pick_file))
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        if (intent.action != Intent.ACTION_SEND) return
        val uri: Uri? = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION") intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }
        // L'extra "model" serve solo per i test da adb: sceglie il modello Whisper da usare.
        uri?.let { viewModel.onAudioReceived(it, intent.getStringExtra("model")) }
    }
}
