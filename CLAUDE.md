# CLAUDE.md

Este arquivo fornece orientações ao Claude Code (claude.ai/code) ao trabalhar com o código neste repositório.

## Comandos de Build

```bash
./gradlew assembleDebug          # Gera APK de debug
./gradlew assembleRelease        # Gera APK de release
./gradlew installDebug           # Instala no dispositivo conectado
./gradlew test                   # Executa testes unitários
./gradlew connectedAndroidTest   # Executa testes instrumentados
./gradlew lint                   # Executa verificações de lint
./gradlew clean                  # Limpa artefatos de build
```

## Visão Geral da Arquitetura

**App Android de módulo único** (`com.oitickets.cebola26`) — sistema de credenciamento para eventos.

### Stack de Tecnologias
- **Kotlin 2.0.21** + Coroutines + Flow
- **Jetpack Compose** (Material 3) para toda a UI
- **MVVM** com `RegistrationViewModel` e `QrCodeViewModel`
- **Firebase Firestore** (banco de dados) + **Firebase Storage** (fotos)
- **CameraX** + **ML Kit** (detecção facial para prova de vida, leitura de QR codes)
- **WorkManager** para fila de upload offline
- **Gradle version catalog** em `gradle/libs.versions.toml`

### Fluxo do App (Máquina de Estados)

`RegistrationUiState` é uma sealed class que controla toda a navegação. Estados e transições:

```
Login → StepQr → StepData → Camera → StepPhoto → Uploading → Success
                  ↕
              QrScanner
              PendingUploads
              Error
```

`RegistrationViewModel` detém esse estado e toda a lógica de navegação. Não há Jetpack Navigation — as telas são renderizadas via `when(uiState)` na `MainActivity`.

### Estrutura de Fontes

```
app/src/main/java/com/oitickets/cebola26/
├── MainActivity.kt              # Activity única, hospeda todas as telas Compose
├── data/
│   ├── model/                   # Participant, Staff, RegistrationRules
│   ├── repository/              # RegistrationRepository, QrCodeRepository (Firebase)
│   └── worker/                  # UploadWorker (WorkManager, uploads offline)
├── domain/
│   ├── FaceQualityAnalyzer.kt   # ML Kit: detecção facial + prova de vida (piscar/sorrir)
│   ├── QrCodeAnalyzer.kt        # ML Kit: leitura de QR code pela câmera
│   └── LivenessAction.kt        # Enum: SMILE, BLINK
└── ui/
    ├── screens/                 # Um arquivo por tela (Login, QrStep, DataStep, Camera, etc.)
    ├── components/              # Componentes Compose reutilizáveis
    ├── viewmodel/               # RegistrationViewModel, QrCodeViewModel
    └── theme/                   # Tema Material 3
```

### Decisões de Design Importantes

- **Sem Jetpack Navigation**: o roteamento de telas é feito via sealed class em `RegistrationViewModel.uiState`
- **Upload offline-first**: fotos são salvas no cache do app como JPEG; o JSON do participante é enfileirado via WorkManager; `UploadWorker` executa quando há rede disponível
- **Prova de vida**: `FaceQualityAnalyzer` valida centralização do rosto, pose da cabeça e uma ação de prova de vida (piscar ou sorrir) antes de aceitar a foto
- **Login de staff**: senha hardcoded `"Cebol@26"` — dados do staff são buscados no Firestore pelo nome de usuário
- **Regras de cadastro**: documento `RegistrationRules` no Firestore controla feature flags (requireName, requireCpf, requirePhoto, requireQrCode, allowDuplicateQrCode)
- **Min SDK 31** (Android 12)
