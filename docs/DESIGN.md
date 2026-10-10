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
- Dimensione modello: `small` int8 ≈ 375 MB (encoder 112 + decoder 262), `base` int8 molto meno. **Decisione: il modello è esterno all'APK** (aggiornabile senza reinstallare). Cartella: `getExternalFilesDir("models")/<nome>/`. Dev: `adb push`; release: download in-app da Hugging Face (permesso INTERNET solo per questo) — da implementare in F6/F7. L'APK resta piccolo (~50 MB, per le librerie native sherpa-onnx).
- Velocità: su telefoni medi `small` può essere più lento del tempo reale → mostrare progresso, eseguire in foreground service se > ~1 min.
- Memoria: decodificare in streaming/chunk per vocali lunghi.

## 6. Roadmap
- [x] F0 Setup: AGENTS.md, DESIGN.md (questo)
- [x] F1 Scheletro Gradle + Compose, build `assembleDebug`, install sul telefono (hello world)
- [x] F2 Intent filter SEND audio/* + schermata di ricezione (mostra nome/durata file)
- [x] F3 AudioDecoder: URI → PCM 16 kHz (verificato con vocale WhatsApp reale)
- [x] F4 Integrazione sherpa-onnx + modello, trascrizione di file di test (vedi benchmark in sez. 8)
- [ ] F5 Chunking audio lunghi, progresso, cancellazione
- [ ] F6 UI finale: copia/condividi, gestione errori, icona
- [ ] F7 APK release firmato per uso personale + installazione definitiva

## 7. Preparazione dispositivo Android
1. Impostazioni → Info telefono → tocca 7 volte "Numero build" → abilita Opzioni sviluppatore.
2. Opzioni sviluppatore → attiva **Debug USB**.
3. Collega via USB, accetta il prompt "Consenti debug USB" sul telefono.
4. Verifica: `adb devices` deve mostrare il dispositivo come `device`.

Su HyperOS/MIUI (POCO) serve anche **Installa via USB** (Opzioni sviluppatore), altrimenti `INSTALL_FAILED_USER_RESTRICTED`.

## 8. Benchmark Whisper (POCO X6 Pro, Dimensity 8300 Ultra, 4 thread, vocale WhatsApp di 44 s)
| Modello (int8) | Peso | Trascrizione | Qualità sul vocale di prova |
|---|---|---|---|
| `base` | 160 MB | ~4,9 s | Buona ma con errori di senso ("ho detto tutto" invece di "ho letto tutto", frase omessa) |
| `small` | 375 MB | ~16,3 s | Molto buona (errori solo su termini rari: "Deplian", "proloco") |

- Decodifica audio ~1,2 s; caricamento modello 0,7 s (`base`) / 2 s (`small`), fatto una volta sola (modello tenuto in memoria).
- **Thread**: 3–4 ottimali (16,6 / 16,8 s con `small`); 2 → 19 s; 8 → molto peggio (i core "little" rallentano tutto).
- **Throttling termico**: dopo molte prove ravvicinate `small` è salito a ~47 s. Prove di velocità vanno fatte a telefono freddo.
- Whisper elabora finestre fisse da 30 s: il costo non scala linearmente con la durata.
- **Decisione**: default `small` (qualità prima di tutto, si legge al posto di ascoltare); `base` come opzione "veloce" selezionabile (F6).

## 9. Dev: setup e test da riga di comando
- Librerie native: `.\scripts\setup-sherpa.ps1` (scarica sherpa-onnx in `app/src/main/jniLibs`, non versionato).
- Modello: `adb push <file> /sdcard/Android/data/com.matteopelucco.vocalizzapp/files/models/`. La cartella `models` **deve essere creata dall'app** (lo fa al primo avvio): una cartella creata da adb appartiene a `shell` e l'app non la legge. I modelli stanno direttamente in `models/` (`small-encoder.int8.onnx`, `small-decoder.int8.onnx`, `small-tokens.txt`, idem `base-*`).
- Test senza WhatsApp: `adb push test.opus .../files/test.opus` (con push, non cp), poi
  `adb shell am start -a android.intent.action.SEND -t audio/ogg --eu android.intent.extra.STREAM file:///sdcard/Android/data/com.matteopelucco.vocalizzapp/files/test.opus --es model small -n com.matteopelucco.vocalizzapp/.MainActivity`
  e leggere i tempi con `adb logcat -d -s VocalizzApp:I`.
