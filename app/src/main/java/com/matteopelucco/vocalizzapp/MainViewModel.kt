package com.matteopelucco.vocalizzapp

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlin.math.abs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface DecodeState {
    data object Idle : DecodeState
    data object Decoding : DecodeState
    data class Done(val seconds: Float, val peak: Float) : DecodeState
    data class Error(val message: String) : DecodeState
}

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val _audio = MutableStateFlow<AudioInfo?>(null)
    val audio: StateFlow<AudioInfo?> = _audio.asStateFlow()

    private val _decode = MutableStateFlow<DecodeState>(DecodeState.Idle)
    val decode: StateFlow<DecodeState> = _decode.asStateFlow()

    private var job: Job? = null

    fun onAudioReceived(uri: Uri) {
        _audio.value = readAudioInfo(getApplication(), uri)
        job?.cancel()
        _decode.value = DecodeState.Decoding
        job = viewModelScope.launch {
            _decode.value = try {
                val pcm = AudioDecoder.decode(getApplication(), uri)
                DecodeState.Done(
                    seconds = pcm.size / AudioDecoder.TARGET_RATE.toFloat(),
                    peak = pcm.maxOfOrNull { abs(it) } ?: 0f,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                DecodeState.Error(e.message ?: e.javaClass.simpleName)
            }
        }
    }
}
