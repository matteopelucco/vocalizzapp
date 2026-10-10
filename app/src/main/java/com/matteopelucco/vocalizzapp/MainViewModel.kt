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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface UiState {
    data object Idle : UiState
    data object Decoding : UiState
    data class Transcribing(val progress: Float, val partialText: String) : UiState
    data class Done(val text: String, val audioSeconds: Float, val elapsedSeconds: Float) : UiState
    data class Cancelled(val partialText: String) : UiState
    data object ModelMissing : UiState
    data class Error(val message: String) : UiState
}

private const val TAG = "VocalizzApp"

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val modelManager = ModelManager(app)

    private val _audio = MutableStateFlow<AudioInfo?>(null)
    val audio: StateFlow<AudioInfo?> = _audio.asStateFlow()

    private val _state = MutableStateFlow<UiState>(UiState.Idle)
    val state: StateFlow<UiState> = _state.asStateFlow()

    // Modelli: quali sono sul telefono, quale e' in uso, quanta RAM/spazio ha il telefono.
    private val _installed = MutableStateFlow(modelManager.installedIds())
    val installed: StateFlow<Set<String>> = _installed.asStateFlow()

    private val _activeModel = MutableStateFlow(modelManager.findModel()?.name)
    val activeModel: StateFlow<String?> = _activeModel.asStateFlow()

    private val _device = MutableStateFlow(modelManager.deviceInfo())
    val device: StateFlow<DeviceInfo> = _device.asStateFlow()

    private val _download = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val download: StateFlow<DownloadState> = _download.asStateFlow()

    private val _chooserOpen = MutableStateFlow(false)
    val chooserOpen: StateFlow<Boolean> = _chooserOpen.asStateFlow()

    private var job: Job? = null
    private var downloadJob: Job? = null

    // Audio arrivato quando il modello mancava: parte da solo appena il download finisce.
    private var pendingUri: Uri? = null

    // Il caricamento del modello e' costoso: lo teniamo in memoria tra un vocale e l'altro.
    private var transcriber: Transcriber? = null
    private var transcriberModel: String? = null

    // Lavori attivi (compreso uno annullato che sta finendo il blocco in corso): il servizio resta finche' > 0.
    private var activeJobs = 0

    override fun onCleared() {
        // Un servizio ancora attivo viene fermato dal "finally" del lavoro; qui basta liberare il modello.
        transcriber?.close()
        transcriber = null
    }

    /** Il servizio in primo piano evita che Android rallenti la CPU se l'utente lascia l'app. */
    private fun serviceStarted() {
        TranscriptionService.start(getApplication())
        activeJobs++
    }

    private fun serviceFinished() {
        if (--activeJobs == 0) TranscriptionService.stop(getApplication())
    }

    private fun refreshModels() {
        _installed.value = modelManager.installedIds()
        _activeModel.value = modelManager.findModel()?.name
        _device.value = modelManager.deviceInfo()
    }

    // --- Scelta del modello ---------------------------------------------------------------------

    fun openChooser() {
        refreshModels()
        _chooserOpen.value = true
    }

    fun closeChooser() {
        _chooserOpen.value = false
    }

    /** Usa un modello gia' scaricato. */
    fun selectModel(id: String) {
        if (id !in modelManager.installedIds()) return
        modelManager.activeId = id
        refreshModels()
        _chooserOpen.value = false
    }

    /** Elimina un modello per liberare spazio (non durante una trascrizione). */
    fun deleteModel(id: String) {
        if (activeJobs > 0) return
        if (transcriberModel == id) {
            transcriber?.close()
            transcriber = null
            transcriberModel = null
        }
        modelManager.delete(id)
        refreshModels()
    }

    /** Scarica un modello e lo rende quello in uso; se c'era un audio in attesa, lo trascrive subito dopo. */
    fun downloadModel(id: String) {
        val option = ModelCatalog.byId(id) ?: return
        if (_download.value is DownloadState.Running) return
        _download.value = DownloadState.Running(id, 0f)
        downloadJob = viewModelScope.launch {
            serviceStarted()
            try {
                ModelDownloader.download(option, modelManager.modelsDir) {
                    _download.value = DownloadState.Running(id, it)
                }
                modelManager.activeId = id
                refreshModels()
                _download.value = DownloadState.Idle
                _chooserOpen.value = false
                pendingUri?.let { uri ->
                    pendingUri = null
                    onAudioReceived(uri)
                }
            } catch (e: CancellationException) {
                _download.value = DownloadState.Idle
                throw e
            } catch (e: Throwable) {
                _download.value = DownloadState.Failed(id, e.message ?: e.javaClass.simpleName)
            } finally {
                serviceFinished()
            }
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
    }

    // --- Trascrizione ---------------------------------------------------------------------------

    /** Interrompe la trascrizione in corso, mantenendo il testo parziale gia' ottenuto. */
    fun cancel() {
        val current = _state.value
        if (current is UiState.Decoding || current is UiState.Transcribing) {
            job?.cancel()
            _state.value = UiState.Cancelled((current as? UiState.Transcribing)?.partialText.orEmpty())
        }
    }

    /** [modelName]: modello da usare solo per questa trascrizione (test da adb); di norma quello scelto. */
    fun onAudioReceived(uri: Uri, modelName: String? = null) {
        _audio.value = readAudioInfo(getApplication(), uri)
        job?.cancel()
        job = viewModelScope.launch {
            var started = false
            try {
                val model = modelManager.findModel(modelName)
                if (model == null) {
                    pendingUri = uri
                    refreshModels()
                    _state.value = UiState.ModelMissing
                    return@launch
                }
                serviceStarted()
                started = true
                _state.value = UiState.Decoding
                val t0 = SystemClock.elapsedRealtime()
                val pcm = AudioDecoder.decode(getApplication(), uri)
                val audioSeconds = pcm.size / AudioDecoder.TARGET_RATE.toFloat()
                val t1 = SystemClock.elapsedRealtime()

                _state.value = UiState.Transcribing(0f, "")
                if (transcriberModel != model.name) {
                    transcriber?.close()
                    transcriber = null
                    transcriber = withContext(Dispatchers.Default) { Transcriber(model) }
                    transcriberModel = model.name
                }
                val transcriber = transcriber!!
                val t2 = SystemClock.elapsedRealtime()
                val text = transcriber.transcribe(pcm) { progress, partial ->
                    _state.value = UiState.Transcribing(progress, partial)
                }
                val t3 = SystemClock.elapsedRealtime()
                Log.i(TAG, "model=${model.name} audio=${audioSeconds}s decode=${t1 - t0}ms load=${t2 - t1}ms transcribe=${t3 - t2}ms")
                _state.value = UiState.Done(text, audioSeconds, (t3 - t1) / 1000f)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _state.value = UiState.Error(e.message ?: e.javaClass.simpleName)
            } finally {
                if (started) serviceFinished()
            }
        }
    }
}
