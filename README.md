# VocalizzApp

**Leggi, non ascoltare.** App Android con un solo scopo: trasformare in testo i messaggi vocali di WhatsApp (e di altre app di messaggistica), per chi non ha voglia di ascoltare ore di vocali.

Condividi un vocale con VocalizzApp e dopo pochi secondi leggi cosa dice. Poi lo copi o lo giri a qualcun altro.

## Caratteristiche

- **Tutto sul telefono**: la trascrizione usa [Whisper](https://github.com/openai/whisper) in locale, tramite [sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx). L'audio e il testo non vengono mai inviati a nessun server.
- **Qualità a scelta**: al primo avvio scegli quale modello scaricare, da *Veloce* (161 MB) a *Preciso* (1,0 GB). Più è grande, più il testo è accurato e più spazio occupa. Puoi cambiarlo quando vuoi dall'app.
- **Veloce**: con il modello *Equilibrato* circa 16 secondi per 44 secondi di audio su un POCO X6 Pro; un vocale da 10 secondi richiede pochi secondi.
- **Testo progressivo**: i vocali lunghi sono divisi nei punti di silenzio e il testo compare man mano, con un'animazione che mostra che il lavoro è in corso. Si può annullare.
- **Copia e condividi** il testo con un tocco.
- **Continua in background**: un servizio in primo piano evita che Android rallenti la trascrizione se esci dall'app o blocchi lo schermo.
- Tema chiaro e scuro, in italiano.

## Requisiti del telefono

- Android **8.0 (API 26)** o successivo.
- Processore **ARM a 64 bit** (arm64-v8a): praticamente tutti i telefoni degli ultimi anni.
- **Spazio** per il modello scelto (da 161 MB a 1,0 GB) e **RAM**: 3 GB per *Veloce*, 4 GB per *Equilibrato*, 8 GB per *Preciso*. L'app ti avvisa se il telefono è al limite.
- Collegamento a internet **solo per scaricare il modello** (meglio con il Wi-Fi).

Testata finora su un solo dispositivo: POCO X6 Pro (HyperOS, Android 16).

## Come si usa

1. Al primo avvio scegli la **qualità** (vedi sotto) e tocca **Scarica e inizia**.
2. In WhatsApp tieni premuto su un vocale e scegli **Condividi** (in alcune versioni è nel menu ⋮ o in "Altro"), poi **VocalizzApp**.
3. Aspetta il testo. Con **Copia** lo copi, con **Condividi** lo invii a un'altra app.
4. In alternativa, dalla schermata iniziale tocca **Scegli file audio**.

Se condividi un vocale prima di aver scaricato un modello, l'app ti fa scegliere, lo scarica e poi trascrive da sola. Per cambiare qualità tocca il nome del modello in alto a destra: puoi usarne un altro, scaricarlo o eliminare quelli che non ti servono.

---

## Installare da sorgente

Le istruzioni valgono per **Windows**, **macOS** e **Linux**. I comandi sono dati sia per PowerShell/Windows sia per bash.

### 1. Strumenti necessari

| Strumento | Versione | Note |
|---|---|---|
| Git | qualsiasi | per clonare il progetto |
| JDK | **17 o superiore** (provato con 21) | oppure il JDK incluso in Android Studio |
| Android SDK | piattaforma **35**, build-tools **35.0.0**, platform-tools | il modo più semplice è installare [Android Studio](https://developer.android.com/studio) |
| Telefono Android | vedi requisiti | con cavo USB |

Senza Android Studio, con i soli strumenti da riga di comando ([cmdline-tools](https://developer.android.com/studio#command-line-tools-only)):

```bash
sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0"
```

### 2. Scaricare il progetto

```bash
git clone https://github.com/matteopelucco/vocalizzapp.git
cd vocalizzapp
```

### 3. Dire a Gradle dove sono SDK e JDK

Crea il file `local.properties` nella cartella del progetto (non è versionato). **Usa sempre la barra `/`, anche su Windows**:

```properties
# Windows
sdk.dir=C:/Users/IL_TUO_UTENTE/AppData/Local/Android/Sdk
# macOS
# sdk.dir=/Users/IL_TUO_UTENTE/Library/Android/sdk
# Linux
# sdk.dir=/home/IL_TUO_UTENTE/Android/Sdk
```

Se apri il progetto con Android Studio, questo file viene creato da solo.

Imposta `JAVA_HOME` se il comando `java -version` non mostra una versione 17 o superiore:

```powershell
# PowerShell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
```
```bash
# bash
export JAVA_HOME=/percorso/del/jdk-21
```

### 4. Scaricare le librerie native (una tantum)

Le librerie di sherpa-onnx (circa 27 MB) non sono nel repository: si scaricano con uno script che le mette in `app/src/main/jniLibs/`.

```powershell
# Windows (PowerShell)
.\scripts\setup-sherpa.ps1
```
```bash
# macOS / Linux / Git Bash
./scripts/setup-sherpa.sh
```

Senza questo passaggio l'app si compila ma, all'avvio della trascrizione, mostra un errore di libreria nativa.

### 5. Preparare il telefono

1. **Impostazioni → Info sul telefono** → tocca 7 volte **Numero build** per abilitare le Opzioni sviluppatore.
2. **Opzioni sviluppatore** → attiva **Debug USB**.
3. Sui telefoni **Xiaomi / POCO / Redmi (HyperOS, MIUI)** attiva anche **Installa via USB**. Può chiedere l'accesso all'account Xiaomi.
4. Collega il cavo e accetta la richiesta "Consenti debug USB".
5. Verifica che il telefono sia visto:

```powershell
# Windows
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" devices
```
```bash
# macOS / Linux
adb devices
```

Deve comparire una riga con la parola `device` (non `unauthorized`).

### 6. Compilare e installare (versione di sviluppo)

```powershell
# Windows
.\gradlew.bat installDebug
```
```bash
# macOS / Linux
./gradlew installDebug
```

La prima volta Gradle scarica sé stesso e le dipendenze: servono alcuni minuti. L'APK si trova in `app/build/outputs/apk/debug/app-debug.apk`.

Tieni il telefono **sbloccato e con lo schermo acceso**: sui telefoni Xiaomi/POCO l'installazione via USB viene annullata se lo schermo è spento, e a volte compare una finestra di conferma da accettare.

### 7. Primo avvio

Apri **VocalizzApp**: compare la scelta della qualità. Seleziona un modello e tocca **Scarica e inizia**. Il modello viene salvato nello spazio privato dell'app e da quel momento tutto funziona offline.

### 8. Versione di release firmata (facoltativo)

Serve per ottenere un APK "definitivo", firmato con la tua chiave e non debuggabile (circa 32 MB).

**a) Crea una chiave di firma, fuori dal repository.** Conservala con cura: senza di essa non potrai aggiornare l'app, ma solo disinstallarla e reinstallarla.

```bash
keytool -genkeypair -keystore ~/.vocalizzapp-keys/vocalizzapp-release.jks \
  -alias vocalizzapp -keyalg RSA -keysize 4096 -validity 36500 \
  -dname "CN=Il Tuo Nome, O=VocalizzApp, C=IT"
```

(Su Windows sostituisci il percorso con `%USERPROFILE%\.vocalizzapp-keys\vocalizzapp-release.jks`, dopo aver creato la cartella.) Ti chiederà una password.

**b) Crea `keystore.properties`** nella cartella del progetto (è ignorato da git):

```properties
storeFile=C:/Users/IL_TUO_UTENTE/.vocalizzapp-keys/vocalizzapp-release.jks
storePassword=LA_TUA_PASSWORD
keyAlias=vocalizzapp
keyPassword=LA_TUA_PASSWORD
```

**c) Compila:**

```powershell
.\gradlew.bat assembleRelease      # Windows
```
```bash
./gradlew assembleRelease          # macOS / Linux
```

L'APK firmato è in `app/build/outputs/apk/release/app-release.apk`. Se `keystore.properties` non esiste, la release viene compilata ma **non firmata**: il file si chiama `app-release-unsigned.apk` e Android non lo installa.

**d) Installa:**

```bash
adb install app/build/outputs/apk/release/app-release.apk
```

Se sul telefono c'è già la versione di sviluppo (firma diversa) devi prima disinstallarla con `adb uninstall com.matteopelucco.vocalizzapp`: così sparisce anche il modello scaricato, e l'app lo riscarica da sola.

**Installare senza cavo:** copia `app-release.apk` sul telefono (per esempio nella cartella Download), aprilo da "File" e tocca Installa. Android può chiedere di consentire le installazioni da quell'app.

---

## I modelli di trascrizione

Sono varianti di [Whisper](https://github.com/openai/whisper), in formato int8 per sherpa-onnx, prese da [csukuangfj su Hugging Face](https://huggingface.co/csukuangfj). **Non sono dentro l'APK**: si scaricano dall'app e si possono cambiare senza reinstallare.

| Nome nell'app | Modello | Spazio | Precisione | Velocità | Note |
|---|---|---|---|---|---|
| Veloce | `base` | 161 MB | ●●○○○ | ●●●●● | sbaglia più spesso, anche sul senso delle frasi |
| **Equilibrato** (consigliato) | `small` | 375 MB | ●●●○○ | ●●●○○ | buon compromesso |
| Preciso | `turbo` (large-v3-turbo) | 1,0 GB | ●●●●● | ●●○○○ | capisce meglio parole e nomi; nei vocali molto lunghi può omettere la punteggiatura |

I valori sono stati misurati su vocali WhatsApp in italiano (vedi [docs/DESIGN.md](docs/DESIGN.md), sezione 8): su un POCO X6 Pro, 44 s di audio richiedono circa 5 s con *Veloce*, 16 s con *Equilibrato* e 22 s con *Preciso*.

- I file stanno in `Android/data/com.matteopelucco.vocalizzapp/files/models/`, tre per modello: `<nome>-encoder.int8.onnx`, `<nome>-decoder.int8.onnx`, `<nome>-tokens.txt`.
- Il download riprende da dove si era fermato e verifica le dimensioni dei file.
- Se il download interno non è possibile, in sviluppo puoi copiare i file a mano con `adb push`. **Attenzione**: la cartella `models` deve essere creata dall'app (lo fa al primo avvio). Se la crei tu con adb appartiene all'utente "shell" e l'app non riesce a leggerla.

```bash
adb push small-encoder.int8.onnx small-decoder.int8.onnx small-tokens.txt \
  /sdcard/Android/data/com.matteopelucco.vocalizzapp/files/models/
```

## Sviluppo

### Struttura

```
app/src/main/java/com/matteopelucco/vocalizzapp/
  MainActivity.kt          punto d'ingresso, riceve i vocali condivisi
  MainViewModel.kt         stato dell'app, trascrizione, download del modello
  AudioDecoder.kt          audio qualsiasi -> PCM 16 kHz mono (MediaCodec)
  AudioChunker.kt          divide l'audio in blocchi tagliando nei silenzi
  Transcriber.kt           wrapper di sherpa-onnx / Whisper
  ModelCatalog.kt          i modelli offerti (nome, dimensioni, precisione, velocità)
  ModelManager.kt          modelli sul telefono, modello in uso, RAM e spazio
  ModelDownloader.kt       download del modello, con ripresa
  TranscriptionService.kt  servizio in primo piano contro il rallentamento in background
  ui/                      tema e schermate Jetpack Compose
app/src/main/java/com/k2fsa/sherpa/onnx/   API Kotlin ufficiale di sherpa-onnx (non rinominare)
docs/DESIGN.md             progettazione, benchmark, roadmap
scripts/                   setup delle librerie native
AGENTS.md                  istruzioni per gli agenti AI che lavorano sul progetto
```

Stack: Kotlin, Jetpack Compose (Material 3), Gradle con Kotlin DSL e version catalog (`gradle/libs.versions.toml`).

### Comandi utili

```bash
./gradlew testDebugUnitTest     # test automatici (Windows: .\gradlew.bat ...)
./gradlew assembleDebug         # solo compilazione, senza installare
```

### Provare senza WhatsApp

Con un'app di sviluppo installata, si può simulare la condivisione di un vocale da riga di comando. Il file audio deve stare in una cartella leggibile dall'app e va copiato con `adb push` (non `cp`):

```bash
adb push vocale.opus /sdcard/Android/data/com.matteopelucco.vocalizzapp/files/test.opus
adb shell am start -a android.intent.action.SEND -t audio/ogg \
  --eu android.intent.extra.STREAM file:///sdcard/Android/data/com.matteopelucco.vocalizzapp/files/test.opus \
  --es model small \
  -n com.matteopelucco.vocalizzapp/.MainActivity
adb logcat -d -s VocalizzApp:I      # tempi di decodifica, caricamento e trascrizione
```

Nelle build di sviluppo (debuggabili) ci sono anche due comandi per provare la scelta del modello senza toccare lo schermo: `--es download base` scarica un modello (`base`, `small` o `turbo`) e `--ez chooser true` apre la schermata di scelta.

Altri dettagli, benchmark e decisioni di progetto sono in [docs/DESIGN.md](docs/DESIGN.md).

## Risoluzione dei problemi

| Sintomo | Causa e rimedio |
|---|---|
| `INSTALL_FAILED_USER_RESTRICTED` | Il telefono ha rifiutato l'installazione via USB. Attiva **Installa via USB** nelle Opzioni sviluppatore, sblocca il telefono, tieni lo schermo acceso e tocca "Installa" nella finestra che compare. |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | C'è già installata una versione con firma diversa (debug o release). Disinstalla con `adb uninstall com.matteopelucco.vocalizzapp` e riprova. |
| `adb devices` non mostra il telefono | Controlla il cavo (deve trasferire dati), il Debug USB e di aver accettato la richiesta sul telefono. |
| `SDK location not found` | Manca `local.properties` o contiene un percorso sbagliato. Usa la barra `/` anche su Windows. |
| Errore sulla versione di Java (`Unsupported class file major version`, "requires Java 17") | Usa un JDK 17 o superiore e imposta `JAVA_HOME`. |
| L'app mostra un errore di libreria nativa (`dlopen failed`, `UnsatisfiedLinkError`) | Mancano le librerie di sherpa-onnx: esegui lo script `setup-sherpa`, poi ricompila. |
| Trascrizione molto lenta (tre volte più del solito) | Il telefono ha lo schermo spento o l'app era in secondo piano senza servizio, e Android ha limitato la CPU. Tieni acceso lo schermo; anche il surriscaldamento rallenta. |
| Compare la scelta della qualità a ogni avvio | Non c'è ancora nessun modello sul telefono: scegline uno e tocca **Scarica e inizia** (serve internet). |
| Download del modello interrotto | Riprova: riparte da dove si era fermato. Controlla lo spazio libero: serve un po' più della dimensione indicata sulla scheda. |
| VocalizzApp non compare tra le app di condivisione | Condividi proprio un messaggio **vocale** (non un file qualsiasi) e cerca l'app nell'elenco completo di Android. |

## Limiti noti

- Trascrive in **italiano** (la lingua è fissa).
- Testata solo con vocali WhatsApp (`.opus`) e su un solo telefono; con altre app e dispositivi potrebbero esserci differenze.
- Su telefoni di fascia bassa i modelli più grandi possono essere più lenti del parlato o richiedere troppa memoria: l'app avvisa quando RAM o spazio sono al limite.
- Solo `arm64-v8a`: non gira su emulatori x86 e su telefoni a 32 bit.

## Crediti e licenze

- [Whisper](https://github.com/openai/whisper) di OpenAI, licenza MIT.
- [sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx) di k2-fsa, licenza Apache 2.0, e [ONNX Runtime](https://github.com/microsoft/onnxruntime) di Microsoft, licenza MIT.
- I modelli Whisper convertiti per sherpa-onnx sono pubblicati da [csukuangfj](https://huggingface.co/csukuangfj).
- Jetpack Compose, AndroidX e Material Components di Google, licenza Apache 2.0.

Il codice di questo progetto non ha ancora una licenza: finché non ne viene scelta una, tutti i diritti sono riservati all'autore.

VocalizzApp non è affiliata né approvata da WhatsApp o da Meta.
