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
- Dimensione modello: `small` int8 ≈ 375 MB (encoder 112 + decoder 262), `base` int8 molto meno. **Decisione: il modello è esterno all'APK** (aggiornabile senza reinstallare). Cartella: `getExternalFilesDir("models")/<nome>/`. Download in-app da Hugging Face (permesso INTERNET solo per questo), implementato; in sviluppo anche `adb push`. L'APK resta piccolo (~50 MB, per le librerie native sherpa-onnx).
- Velocità: su telefoni medi `small` può essere più lento del tempo reale → mostrare progresso, eseguire in foreground service se > ~1 min.
- Memoria: decodificare in streaming/chunk per vocali lunghi.

## 6. Roadmap
- [x] F0 Setup: AGENTS.md, DESIGN.md (questo)
- [x] F1 Scheletro Gradle + Compose, build `assembleDebug`, install sul telefono (hello world)
- [x] F2 Intent filter SEND audio/* + schermata di ricezione (mostra nome/durata file)
- [x] F3 AudioDecoder: URI → PCM 16 kHz (verificato con vocale WhatsApp reale)
- [x] F4 Integrazione sherpa-onnx + modello, trascrizione di file di test (vedi benchmark in sez. 8)
- [x] F5 Chunking audio lunghi, progresso, cancellazione
- [x] F6 UI finale: copia, condividi, animazioni, download del modello in-app, servizio in primo piano, gestione errori, icona
- [x] F7 APK release firmato (v1.0) installato sul POCO X6 Pro

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

## 10. Stile UI
- Tema in `ui/Theme.kt` (palette dal logo: blu #1659F0 + accenti ciano; chiaro e scuro, segue il sistema), schermata in `ui/MainScreen.kt`. `MainActivity` si limita a collegare ViewModel e UI.
- Linee guida: minimalismo, card bianche con bordo sottile e angoli 24 dp, testo trascritto a 17 sp con interlinea ampia, un solo pulsante principale a larghezza piena (Scegli file / Annulla).

## 11. Prestazioni in background (importante)
Con schermo bloccato o app non in primo piano HyperOS limita la CPU: lo stesso vocale da 44 s con `small` passa da ~17 s (schermo acceso) a ~48 s. Se l'utente condivide un vocale e poi torna a WhatsApp, la trascrizione rallenta o puo' essere sospesa.
Possibile soluzione (da valutare in F6): servizio in primo piano (`FOREGROUND_SERVICE_DATA_SYNC` + notifica) per le trascrizioni lunghe; oppure semplicemente avvisare l'utente di restare nell'app.

## 12. Qualità del testo: confronto tra modelli (esperimento sul PC, ottobre 2026)
Motivazione: un'app concorrente trascriveva meglio lo stesso vocale. Banco di prova: `bench.py` con la stessa versione di sherpa-onnx dell'app (1.13.8), stesso taglio dei blocchi, vocale WhatsApp di 12 s ("ufficio postale"). Gli errori del telefono si riproducono sul PC, quindi il confronto è affidabile.

| Modello | Spazio | Esito sul vocale di 12 s |
|---|---|---|
| `base` int8 | 161 MB | errori sul senso ("assurre" per "stasera", frasi spezzate) |
| `small` int8 | 375 MB | "ruffigio postale", "so" per "tesoro", "noi tre" per "inoltre" |
| `small` fp32 | 970 MB | corregge "inoltre", restano "rufficio" e "so" |
| `medium` int8 | 945 MB | meglio, restano "ruffigio" e "zoro"; molto lento (37 s sul PC) |
| `turbo` int8 | 1,03 GB | solo "rufficio" sbagliato; "tesoro" giusto, punteggiatura corretta |

Conclusioni:
- La causa principale è **la taglia del modello**. La quantizzazione int8 pesa poco; il ricampionamento non c'entra (WhatsApp registra già a 16 kHz: risultato identico con un ricampionatore di alta qualità).
- `turbo` su POCO X6 Pro: 12 s di audio in ~7 s (`small` ~5 s), 44 s in ~22 s (`small` ~17 s); RAM ~1,8 GB contro ~1,2 GB.
- **Limite di `turbo`**: sul vocale di 44 s il primo blocco di 28 s è uscito senza punteggiatura né maiuscole; con blocchi da 20 s ha ripetuto una frase. I blocchi più corti (10-15 s) non risolvono. Per questo la scheda dice che "nei vocali molto lunghi può omettere la punteggiatura". Possibili rimedi da valutare: post-elaborazione della punteggiatura, blocchi basati su VAD, modelli successivi.
- Il modello `distil-large-v3` è solo inglese: non adatto.
- L'app concorrente (installata dal Play Store) pesa ~29 MB, quindi non contiene il modello; non è stato analizzato il suo funzionamento. Se la trascrizione avviene su un server, l'audio lascia il telefono: differenza di principio con VocalizzApp.

## 13. Scelta del modello (schermata)
- Decisione utente: al primo avvio si sceglie cosa scaricare, spiegando con poche parole il rapporto tra qualità, velocità e spazio. Si può cambiare in seguito dal nome del modello in alto a destra.
- Tre modelli nel catalogo (`ModelCatalog.kt`): **Veloce** (`base`), **Equilibrato** (`small`, consigliato), **Preciso** (`turbo`). Ogni scheda mostra nome, dimensione, una riga di descrizione, due indicatori a 5 tacche (precisione, velocità), eventuale avvertenza e stato (In uso / Scaricato).
- Il telefono è controllato prima di scaricare: spazio libero (serve il 30% in più della dimensione) e RAM (soglie 3/4/8 GB, con tolleranza perché `totalMem` è sotto il valore nominale). Spazio insufficiente blocca il pulsante; RAM scarsa mostra solo un avviso.
- Download in primo piano (servizio), con ripresa; un solo download alla volta; annullabile. Al termine il modello diventa quello in uso e un eventuale audio in attesa viene trascritto.
- Eliminazione dei modelli non in uso con conferma, non durante una trascrizione.
- Test automatici: `ModelCatalogTest` (formati di dimensione, completezza del catalogo, soglie RAM/spazio).

## 14. Android 16 (API 36) e pubblicazione
- **Passaggio a API 36** (ottobre 2026): `compileSdk`/`targetSdk` 36, AGP 8.13.2, Gradle 8.14.6, Kotlin 2.3.21, Compose BOM 2026.06.01, AndroidX aggiornato. Migrazioni necessarie: `kotlinOptions` → `kotlin { compilerOptions }`, `LocalClipboardManager` → `LocalClipboard`. Collaudo sul POCO X6 Pro (Android 16): trascrizione e interfaccia invariate. Con target 36 l'app non può bloccare orientamento e ridimensionamento sugli schermi grandi (non lo faceva) e il "indietro" predittivo è attivo (si usa `BackHandler`).
- **Decisione utente: l'app non va su Google Play.** Motivo: diventerà inutile quando WhatsApp offrirà la trascrizione in italiano. Distribuzione = APK firmato installato a mano.
- Se in futuro si volesse pubblicare, requisiti verificati a ottobre 2026: target API 36 obbligatorio dal 31 agosto 2026 ([Google](https://developer.android.com/google/play/requirements/target-sdk)); nuovi account personali: test chiuso con almeno 12 tester per 14 giorni continuativi ([Google](https://support.google.com/googleplay/android-developer/answer/14151465)); dichiarazione con video del servizio in primo piano in "Contenuti dell'app" ([Google](https://support.google.com/googleplay/android-developer/answer/13392821)). Il tipo `dataSync` è appropriato per elaborazione di file locali e download ([documentazione Android](https://developer.android.com/develop/background-work/services/fgs/service-types)). Servono inoltre: AAB, privacy policy pubblica, modulo Data safety, schermata crediti/licenze, scheda dello store, e la licenza del progetto.

## 15. Versione dell'app
- Mostrata all'utente sopra il pulsante principale: `Versione 0.1.0 (20261010_222537)` = `major.minor.patch` + data/ora della build. Letta da `BuildConfig.VERSION_NAME` e `BuildConfig.BUILD_STAMP`.
- Sorgente dei numeri: `version.properties`. Ogni build che firma (`assembleRelease`, `bundleRelease`, `installRelease`) usa i valori del file e poi incrementa `patch` e `code` (`versionCode`, necessario ad Android per riconoscere un aggiornamento). `major`/`minor` solo su richiesta dell'utente. Il rilevamento della firma si basa sui nomi dei task richiesti (`gradle.startParameter.taskNames`): una build avviata da IDE con "Generate Signed APK" usa gli stessi task.
- Build di sviluppo: versione con suffisso `-dev`, file invariato.
- Limite noto: se una build di release fallisce dopo l'incremento, il numero resta "consumato" (accettabile: i numeri non devono essere consecutivi).
