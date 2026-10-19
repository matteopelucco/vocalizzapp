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
- **Modello esternalizzato (decisione utente)**: il modello Whisper NON va nell'APK, così può essere aggiornato/sostituito senza reinstallare l'app. Vive in `getExternalFilesDir("models")/` come terna `<nome>-encoder.int8.onnx`, `<nome>-decoder.int8.onnx`, `<nome>-tokens.txt`. `ModelManager` lo individua a runtime; se manca, l'app mostra la scelta e scarica in-app (`ModelDownloader`, da Hugging Face `csukuangfj/sherpa-onnx-whisper-<nome>`, con ripresa e verifica delle dimensioni). In sviluppo si può anche usare `adb push`, ma la cartella `models` deve essere creata dall'app (vedi DESIGN.md §9).
- **Scelta del modello (decisione utente)**: al primo avvio l'utente sceglie cosa scaricare tra `base` (Veloce, 161 MB), `small` (Equilibrato, 375 MB), `turbo` (Preciso, 1,0 GB) e `parakeet` (Rapido e preciso, 0,7 GB, NVIDIA Parakeet TDT v3 transducer, ~10x più veloce di turbo: consigliato); poi può cambiare, scaricarne altri o eliminarli. Il catalogo è in `ModelCatalog.kt` (file, dimensioni esatte, precisione e velocità misurate, RAM minima) e la scheda avvisa se RAM o spazio sono al limite. Per aggiungere un modello: nuova voce nel catalogo + stringhe in `strings.xml` + benchmark in `docs/DESIGN.md`.
- **Cronologia (decisione utente)**: i testi trascritti restano sul telefono, in SQLite privato (`TranscriptStore`, `transcripts.db`) con il `SQLiteOpenHelper` di Android: nessuna libreria (niente Room), perché serve una sola tabella. Schermata iniziale: gli ultimi 3 sempre visibili + "Vedi tutti" (schermata `HistoryScreen`, 10 alla volta con scorrimento infinito, paginazione a cursore sull'id). Una riga = prima riga del testo con ellissi + data/ora ai minuti. Toccare un testo apre la sua card **sul posto** (accordion: una sola aperta, si richiude toccando l'intestazione) con Elimina/Condividi/Copia, sia tra i recenti sia nell'elenco completo: **niente navigazione verso un'altra vista** (decisione utente: disorientava). Salvati solo i testi completati e non vuoti. **Eliminazione (decisione utente)**: singola (cestino sulla riga, o "Elimina" nella card aperta e nel risultato appena trascritto) e totale ("Elimina tutto"), sempre con una finestra di conferma (`ConfirmDeleteDialog`): non eliminare mai senza conferma esplicita. Il database usa `PRAGMA secure_delete` e `deleteAll()` fa `VACUUM`, cosi' il testo cancellato non resta nel file. **Mai scrivere il contenuto dei testi nei log.** Se serve cambiare lo schema: incrementare `DB_VERSION` e scrivere la migrazione in `onUpgrade`.
- **Servizio in primo piano**: `TranscriptionService` non fa lavoro, serve solo a evitare che HyperOS rallenti la CPU in background (48 s → 16 s su 44 s di audio). Parte con ogni trascrizione/download.
- **Privacy**: l'audio non lascia mai il dispositivo. Il permesso INTERNET serve solo per scaricare/aggiornare il modello; mai per inviare audio o testo.
- minSdk 26, **targetSdk/compileSdk 36** (Android 16), package `com.matteopelucco.vocalizzapp`. Strumenti: AGP 8.13.2, Gradle 8.14.6, Kotlin 2.3.21 (le versioni 9.x di AGP/Gradle e Kotlin 2.4 hanno cambiamenti incompatibili: aggiornare solo con un test dedicato).
- **Distribuzione**: uso personale tramite APK firmato (decisione utente). Niente Google Play: l'app diventerà inutile quando WhatsApp offrirà la trascrizione in italiano. Il percorso di pubblicazione (target API 36, test chiuso con 12 tester per 14 giorni, AAB, privacy policy, dichiarazione del servizio `dataSync` con video) è descritto in `docs/DESIGN.md` §14 per chi volesse riprenderlo.

## Ambiente di sviluppo (Windows 11)
- JDK: `C:\Program Files\Java\jdk-21.0.12` (JAVA_HOME). Alternativa: JBR di Android Studio.
- Android SDK: `%LOCALAPPDATA%\Android\Sdk` (platforms 34/35/36, build-tools 34/35, platform-tools). `local.properties` (non versionato) deve contenere `sdk.dir`.
- adb: `%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe` (non è nel PATH).
- Shell primaria: PowerShell. Usare `.\gradlew.bat`.

## Comandi
```powershell
.\scripts\setup-sherpa.ps1           # (una tantum) scarica le librerie native sherpa-onnx in app\src\main\jniLibs
.\gradlew.bat assembleDebug          # build APK debug -> app\build\outputs\apk\debug\
.\gradlew.bat installDebug           # build + install sul dispositivo collegato
.\gradlew.bat testDebugUnitTest      # unit test JVM
.\gradlew.bat assembleRelease        # APK firmato -> app\build\outputs\apk\release\app-release.apk
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" devices
```

### Release firmata
- La chiave sta **fuori dal repo**: `%USERPROFILE%\.vocalizzapp-keys\vocalizzapp-release.jks`; i riferimenti sono in `keystore.properties` (radice del progetto, ignorato da git). **Mai committarli né stamparne le password.** Senza la chiave l'app non si può più aggiornare, solo reinstallare: ricordare all'utente di tenerne un backup.
- Una release ha firma diversa dalla debug: per passare dall'una all'altra serve `adb uninstall` (si perde il modello scaricato, che l'app riscarica da sola).
- Su HyperOS l'installazione via USB richiede **telefono sbloccato e schermo acceso** e un tocco su "Installa"; se è bloccato fallisce con `INSTALL_FAILED_USER_RESTRICTED`. In alternativa: `adb push app-release.apk /sdcard/Download/` e installarlo dal telefono.
- **Versione (decisione utente)**: l'app mostra "Versione `major.minor.patch` (`aaaammgg_hhmmss`)" sopra il pulsante "Scegli file audio"; la data e l'ora sono quelle della build. I numeri stanno in `version.properties` (radice del progetto) e indicano la versione del **prossimo** APK firmato. A ogni firma (`assembleRelease`/`bundleRelease`/`installRelease`) la build usa i valori correnti e poi aumenta da sola `patch` e `code`: dopo ogni firma **committare `version.properties`**. `major` e `minor` si cambiano a mano e **solo su richiesta dell'utente** (mai da soli). Le build di sviluppo mostrano la stessa versione con il suffisso `-dev` e non toccano il file. Prima versione firmata: 0.1.0.

## Struttura
```
app/src/main/java/com/matteopelucco/vocalizzapp/
  MainActivity.kt          # entry point, intent SEND, collega ViewModel e UI
  MainViewModel.kt         # stato (UiState), trascrizione, download modello
  AudioDecoder.kt          # URI -> FloatArray 16 kHz mono (MediaCodec)
  AudioChunker.kt          # divide l'audio in blocchi <= 28 s tagliando nei silenzi
  Transcriber.kt           # wrapper sherpa-onnx/Whisper (blocchi, parziali, chiusura sicura)
  ModelCatalog.kt          # modelli offerti: id, file, dimensioni, precisione/velocità
  ModelManager.kt          # modelli installati, modello in uso (SharedPreferences), RAM/spazio, eliminazione
  ModelDownloader.kt       # download del modello con ripresa
  TranscriptionService.kt  # servizio in primo piano anti-throttling
  TranscriptStore.kt       # cronologia dei testi (SQLite privato, paginazione a cursore)
  TranscriptFormat.kt      # data "10 ott 2026, 21:34" e anteprima su una riga
  ui/Theme.kt, MainScreen.kt, ModelChooser.kt, History.kt, Icons.kt
app/src/main/java/com/k2fsa/sherpa/onnx/   # API Kotlin ufficiale di sherpa-onnx (NON rinominare: JNI)
docs/DESIGN.md             # progettazione, benchmark, roadmap
scripts/setup-sherpa.ps1
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
