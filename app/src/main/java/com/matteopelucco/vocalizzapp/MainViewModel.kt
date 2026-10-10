package com.matteopelucco.vocalizzapp

import android.app.Application
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface UiState {
    data object Idle : UiState
    data object Decoding : UiState
    data class Transcribing(val progress: Float) : UiState
    data class Done(val text: String, val audioSeconds: Float, val elapsedSeconds: Float) : UiState
    data class ModelMissing(val path: String, val found: String) : UiState
    data class Error(val message: String) : UiState
}

private const val TAG = "VocalizzApp"

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val modelManager = ModelManager(app)

    private val _audio = MutableStateFlow<AudioInfo?>(null)
    val audio: StateFlow<AudioInfo?> = _audio.asStateFlow()

    private val _state = MutableStateFlow<UiState>(UiState.Idle)
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var job: Job? = null

    // Il caricamento del modello e' costoso: lo teniamo in memoria tra un vocale e l'altro.
    private var transcriber: Transcriber? = null
    private var transcriberModel: String? = null

    override fun onCleared() {
        transcriber?.close()
        transcriber = null
    }

    /** [modelName]: modello da usare (default: il primo disponibile). */
    fun onAudioReceived(uri: Uri, modelName: String? = null) {
        _audio.value = readAudioInfo(getApplication(), uri)
        job?.cancel()
        job = viewModelScope.launch {
            try {
                val model = modelManager.findModel(modelName)
                if (model == null) {
                    _state.value = UiState.ModelMissing(modelManager.modelsDir.absolutePath, modelManager.describe())
                    return@launch
                }
                _state.value = UiState.Decoding
                val t0 = SystemClock.elapsedRealtime()
                val pcm = AudioDecoder.decode(getApplication(), uri)
                val audioSeconds = pcm.size / AudioDecoder.TARGET_RATE.toFloat()
                val t1 = SystemClock.elapsedRealtime()

                _state.value = UiState.Transcribing(0f)
                if (transcriberModel != model.name) {
                    transcriber?.close()
                    transcriber = null
                    transcriber = withContext(Dispatchers.Default) { Transcriber(model) }
                    transcriberModel = model.name
                }
                val transcriber = transcriber!!
                val t2 = SystemClock.elapsedRealtime()
                val text = transcriber.transcribe(pcm) { _state.value = UiState.Transcribing(it) }
                val t3 = SystemClock.elapsedRealtime()
                Log.i(TAG, "model=${model.name} audio=${audioSeconds}s decode=${t1 - t0}ms load=${t2 - t1}ms transcribe=${t3 - t2}ms")
                Log.i(TAG, "text[${model.name}]: $text")
                _state.value = UiState.Done(text, audioSeconds, (t3 - t1) / 1000f)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _state.value = UiState.Error(e.message ?: e.javaClass.simpleName)
            }
        }
    }
}
