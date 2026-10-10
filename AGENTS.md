# AGENTS.md — VocalizzApp

Istruzioni per gli agenti AI (Claude Code, ecc.) che lavorano su questo repository.
Lingua di lavoro con l'utente: **italiano**. Codice, identificatori e commit: inglese per il codice, italiano per testi UI.

## Scopo del progetto
App Android minimale con **un unico scopo**: trascrivere in testo i messaggi vocali (WhatsApp e altre app di messaggistica) per chi non vuole ascoltarli.
Non aggiungere funzionalità fuori scopo (no chat, no account, no cloud, no analytics).

## Decisioni di progetto (vedi docs/DESIGN.md per il dettaglio)
- **Stack**: Kotlin + Jetpack Compose, Material 3, single-module Gradle (Kotlin DSL, version catalog).
- **Trascrizione**: on-device, offline, via **sherpa-onnx** (librerie native precompilate, niente NDK) con modello **Whisper multilingua** int8.
- **Input**: l'app è destinataria di `ACTION_SEND` / `ACTION_SEND_MULTIPLE` con `audio/*` (Condividi da WhatsApp) + pulsante "scegli file".
- **Decodifica audio**: `MediaExtractor` + `MediaCodec` (Opus/OGG, m4a, mp3...) → PCM 16 kHz mono float. Nessun ffmpeg.
- **Modello esternalizzato (decisione utente)**: il modello Whisper NON va nell'APK, così può essere aggiornato/sostituito senza reinstallare l'app. Vive in `getExternalFilesDir("models")/<nome-modello>/` (`*-encoder.int8.onnx`, `*-decoder.int8.onnx`, `*-tokens.txt`). `ModelManager` lo individua a runtime; se manca, l'app lo dice e propone il download (da Hugging Face `csukuangfj/sherpa-onnx-whisper-*`). In sviluppo: `adb push` nella stessa cartella.
- **Privacy**: l'audio non lascia mai il dispositivo. Il permesso INTERNET serve solo per scaricare/aggiornare il modello; mai per inviare audio o testo.
- minSdk 26, targetSdk/compileSdk 35, package `com.matteopelucco.vocalizzapp`.

## Ambiente di sviluppo (Windows 11)
- JDK: `C:\Program Files\Java\jdk-21.0.12` (JAVA_HOME). Alternativa: JBR di Android Studio.
- Android SDK: `%LOCALAPPDATA%\Android\Sdk` (platforms 34/35, build-tools 34/35, platform-tools). `local.properties` (non versionato) deve contenere `sdk.dir`.
- adb: `%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe` (non è nel PATH).
- Shell primaria: PowerShell. Usare `.\gradlew.bat`.

## Comandi
```powershell
.\gradlew.bat assembleDebug          # build APK debug -> app\build\outputs\apk\debug\
.\gradlew.bat installDebug           # build + install sul dispositivo collegato
.\gradlew.bat test                   # unit test JVM
.\gradlew.bat lintDebug              # lint
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" devices
```

## Struttura prevista
```
app/src/main/java/com/matteopelucco/vocalizzapp/
  MainActivity.kt        # entry point, gestisce intent SEND
  ui/                    # schermate Compose (stato, risultato, copia/condividi)
  audio/AudioDecoder.kt  # URI -> FloatArray 16 kHz mono
  asr/Transcriber.kt     # wrapper sherpa-onnx (interfaccia + impl)
  asr/ModelManager.kt    # localizzazione/estrazione del modello
docs/DESIGN.md           # progettazione e roadmap
```

## Convenzioni
- Kotlin idiomatico, coroutines per il lavoro pesante (`Dispatchers.Default`/`IO`), mai bloccare il main thread.
- Stato UI con `StateFlow` + `ViewModel`; UI Compose stateless dove possibile.
- Testi UI in `res/values/strings.xml` (it come default).
- Dipendenze solo via `gradle/libs.versions.toml`. Non aggiungere librerie senza motivarlo.
- Niente segreti nel repo. `local.properties`, `*.jks`, `build/` in `.gitignore`.
- Commit piccoli e atomici; non committare o pushare senza richiesta esplicita dell'utente.

## Workflow agentico
1. Prima di cambiare codice, leggere `docs/DESIGN.md` e lo stato della roadmap.
2. Dopo ogni modifica significativa: `assembleDebug` deve passare; eseguire `test` se esistono test toccati.
3. Aggiornare la checklist di roadmap in `docs/DESIGN.md` quando una fase è completata.
4. Per azioni sul dispositivo (install, adb) verificare prima `adb devices`; chiedere all'utente di collegare/autorizzare il telefono se assente.
5. Per decisioni architetturali nuove, proporle all'utente e annotarle in `docs/DESIGN.md`.

## Cosa NON fare
- Non inviare audio o testo trascritto a servizi esterni.
- Non richiedere permessi superflui (no microfono, no storage ampio: usare gli URI condivisi).
- Non aggiungere funzioni "extra" (riassunti, traduzioni) finché l'utente non le chiede.
