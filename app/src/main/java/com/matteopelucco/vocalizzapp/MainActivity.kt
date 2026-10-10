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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
                    val decode by viewModel.decode.collectAsState()
                    val picker = rememberLauncherForActivityResult(
                        ActivityResultContracts.GetContent()
                    ) { uri -> uri?.let(viewModel::onAudioReceived) }

                    Column(
                        Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        val a = audio
                        if (a == null) {
                            Text(stringResource(R.string.empty_hint))
                        } else {
                            Text(stringResource(R.string.audio_received), style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(R.string.audio_name, a.name ?: "?"))
                            Text(stringResource(R.string.audio_size, ((a.sizeBytes ?: 0) / 1024).toString()))
                            Text(stringResource(R.string.audio_type, a.mimeType ?: "?"))
                            Text(
                                when (val d = decode) {
                                    DecodeState.Idle -> ""
                                    DecodeState.Decoding -> stringResource(R.string.decoding)
                                    is DecodeState.Done -> stringResource(
                                        R.string.decoded, "%.1f".format(d.seconds), "%.2f".format(d.peak)
                                    )
                                    is DecodeState.Error -> stringResource(R.string.decode_error, d.message)
                                }
                            )
                        }
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
        uri?.let(viewModel::onAudioReceived)
    }
}
