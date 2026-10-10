# VocalizzApp — Progettazione

## 1. Problema
Ricevere molti vocali WhatsApp (e simili) e non voler ascoltarli. Serve leggerli.

## 2. Flusso utente (unico)
1. In WhatsApp: tieni premuto sul vocale → **Condividi/Inoltra** → scegli **VocalizzApp**.
2. L'app si apre, mostra "Trascrizione in corso…" con progresso.
3. Appare il testo. Pulsanti: **Copia**, **Condividi testo**.
4. In alternativa, dalla home: **Scegli file audio**.

## 3. Architettura
```
Intent SEND (audio/*)  ──►  MainActivity ──► TranscriptionViewModel
                                                │
                       AudioDecoder (MediaExtractor/MediaCodec)
                                                │  FloatArray 16kHz mono
                                          Transcriber (sherpa-onnx, Whisper)
                                                │
                                         StateFlow<UiState> ──► Compose UI
```
- `UiState`: `Idle | Decoding | Transcribing(progress) | Done(text) | Error(msg)`.
- Audio lunghi: spezzare in chunk ≤ 30 s (limite Whisper) con piccola sovrapposizione, trascrivere in sequenza e concatenare.
- Il file condiviso è letto via `ContentResolver.openInputStream/AssetFileDescriptor`; nessun permesso storage.

## 4. Scelte tecniche e motivazioni
| Tema | Scelta | Motivo |
|---|---|---|
| ASR | sherpa-onnx + Whisper multilingua | Offline, privato, binari precompilati (no NDK/CMake) |
| Modello | Whisper `small` int8 (qualità) o `base` int8 (velocità) — da testare sul telefono | Italiano richiede almeno `base`; `small` molto meglio |
| Decodifica | MediaCodec | WhatsApp usa Opus in OGG (`.opus`), supportato da Android ≥ 5 |
| UI | Compose Material 3 | Semplice, moderna |
| Lingua | Auto-detect, default `it` selezionabile | I vocali sono per lo più italiani |

## 5. Rischi
- Dimensione APK: il modello pesa 100–400 MB. Opzioni: includerlo negli assets (APK grande, ok per uso personale) o scaricarlo al primo avvio.
- Velocità: su telefoni medi `small` può essere più lento del tempo reale → mostrare progresso, eseguire in foreground service se > ~1 min.
- Memoria: decodificare in streaming/chunk per vocali lunghi.

## 6. Roadmap
- [x] F0 Setup: AGENTS.md, DESIGN.md (questo)
- [x] F1 Scheletro Gradle + Compose, build `assembleDebug`, install sul telefono (hello world)
- [ ] F2 Intent filter SEND audio/* + schermata di ricezione (mostra nome/durata file)
- [ ] F3 AudioDecoder: URI → PCM 16 kHz (verificato con vocale WhatsApp reale)
- [ ] F4 Integrazione sherpa-onnx + modello, trascrizione di file di test
- [ ] F5 Chunking audio lunghi, progresso, cancellazione
- [ ] F6 UI finale: copia/condividi, gestione errori, icona
- [ ] F7 APK release firmato per uso personale + installazione definitiva

## 7. Preparazione dispositivo Android
1. Impostazioni → Info telefono → tocca 7 volte "Numero build" → abilita Opzioni sviluppatore.
2. Opzioni sviluppatore → attiva **Debug USB**.
3. Collega via USB, accetta il prompt "Consenti debug USB" sul telefono.
4. Verifica: `adb devices` deve mostrare il dispositivo come `device`.
