# VaultGuard

[![Build Android APK](https://github.com/VenZ9/vaultguard/actions/workflows/android-build.yml/badge.svg)](https://github.com/VenZ9/vaultguard/actions/workflows/android-build.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026%2B)-brightgreen.svg)](https://developer.android.com)

**VaultGuard** is a modern, high-security, open-source password manager and secret vault for Android. Built with Jetpack Compose, Kotlin 2.0, Room + SQLCipher, and modern Material 3 Expressive design, VaultGuard empowers you to manage passwords, credentials, and API keys with military-grade encryption and native Android Autofill integration.

---

## Screenshots Placeholder

```
+------------------------+  +------------------------+  +------------------------+
|       VaultGuard       |  |      All Secrets       |  |   Password Generator   |
|                        |  |                        |  |                        |
|        [ SHIELD ]      |  | [Q Search vault...] [=]|  |   K#9xP$2mQ!vL&8wZ   [C|
|       Master Lock      |  |                        |  |   =================    |
|                        |  | (All)(Login)(API)(Apps)|  |   Strength: Very Strong|
| [••••••••••••••••••••] |  |                        |  |                        |
|                        |  | [*] Gemini        [=>] |  | Length: 18 chars       |
|  [  UNLOCK VAULT  ]    |  | [*] OpenAI API    [=>] |  | [x] Uppercase (A-Z)    |
|  [  (Fingerprint)  ]   |  | [*] GitHub        [=>] |  | [x] Lowercase (a-z)    |
+------------------------+  +------------------------+  +------------------------+
```

---

## Key Features

### 1. Encrypted Local Vault
- **Multi-Type Secrets**: Store `LOGIN` (username & password), `API_KEY` (tokens, secrets & custom field rules), and `APP_PASSWORD` (native app credentials & PINs).
- **Zero-Knowledge Encryption**: Local database powered by **Room + SQLCipher (256-bit AES)**.
- **Argon2id Key Derivation**: High-memory, GPU-resistant Argon2id derivation hashes your master password and derives the database key.
- **Biometric Unlock**: Securely unlock using fingerprint or face unlock via AndroidX `BiometricPrompt` backed by hardware-protected Android Keystore key wrapping.
- **Configurable Auto-Lock**: Automatically locks the vault when backgrounded or after an inactivity timer (default 60 seconds).
- **Screenshot Protection**: Hardware-enforced `FLAG_SECURE` active across all screens.
- **Clipboard Auto-Clear**: Automatically purges copied secrets after 30 seconds and tags copies as sensitive on Android 13+.

### 2. Native Android AutofillService
- **Intelligent Form Detection**: Automatically detects login forms, signup forms, and input hints (`AUTOFILL_HINT_USERNAME`, `AUTOFILL_HINT_PASSWORD`, `AUTOFILL_HINT_EMAIL_ADDRESS`, `AUTOFILL_HINT_NEW_PASSWORD`).
- **Domain & Package Matching**: Matches credentials by Android application package name or browser web domain.
- **Signup Prompt**: Detects new accounts created in apps or browsers and prompts you to save the credential into VaultGuard.

### 3. API Key Custom Field Mapping
- Teach Autofill custom fields: `"This field in this app = this API key"`.
- Store field signatures (viewId, hint, inputType).
- AutofillService matches the signature and injects your API keys automatically, with manual copy fallback.

### 4. Smart Icon Resolution
- **Installed App Matching**: Checks if an app is installed on your device and displays its native icon.
- **Curated Service Mapping**: Built-in directory of 40+ popular services (Gemini, OpenAI, Claude, GitHub, AWS, Stripe, Slack, Discord, etc.).
- **Favicon Fetching**: Optional high-resolution favicon resolution from official service domains.
- **Monogram Fallback**: Elegant colored circle monogram generated deterministically from the service name.

### 5. High-Entropy Password Generator
- Standalone tab and built-in drawer generator for quick secret creation.
- Configurable length (6–64 characters).
- Uppercase, lowercase, numbers, and special symbols toggles.
- Real-time entropy calculation and visual strength meter.

### 6. Encrypted Backup & Restore
- Encrypted export format: Entire vault exported as AES-256-GCM encrypted payload derived with user passphrase.
- Safely migrate credentials between devices.

---

## How to Enable Autofill Service

To allow VaultGuard to autofill passwords and API keys across apps and browsers:

1. Open **Android Settings** on your device.
2. Navigate to **System** > **Languages & input** > **Autofill service** (or search "Autofill service" in Settings).
3. Select **VaultGuard Autofill**.
4. Confirm the prompt to activate VaultGuard as your default autofill provider.

---

## Security Architecture

| Security Layer | Implementation | Details |
|---|---|---|
| **Database Encryption** | SQLCipher for Android | 256-bit AES cipher in CBC mode with HMAC-SHA1 |
| **Key Derivation** | Argon2id (RFC 9106) | 3 iterations, 32 MB memory, 1 parallelism thread |
| **Biometric Security** | Android Keystore | Hardware-backed AES-256-GCM key wraps the database passphrase |
| **Screenshot Defense** | `WindowManager.FLAG_SECURE` | Enforced on `MainActivity` to prevent screen capture & recents preview |
| **Network Privacy** | Zero Internet by default | Internet permission only used if user opts in to favicon fetching |
| **Memory Sanitation** | Immediate In-Memory Zeroing | Database encryption keys are wiped (`ByteArray.fill(0)`) on lock |

---

## Build Instructions

### Prerequisites
- Android Studio Ladybug / Meerkat (or JDK 17+)
- Android SDK 35 (Android 15)
- Gradle 8.11+

### Build via Command Line

Clone the repository and build using Gradle:

```bash
# Clone the repository
git clone https://github.com/VenZ9/vaultguard.git
cd vaultguard

# Grant executable permission to the gradle wrapper
chmod +x gradlew

# Run unit tests
./gradlew test

# Build Debug APK
./gradlew assembleDebug

# Build Release APK (unsigned)
./gradlew assembleRelease
```

Generated APKs will be available at:
- `app/build/outputs/apk/debug/app-debug.apk`
- `app/build/outputs/apk/release/app-release-unsigned.apk`

---

## Tech Stack

- **Language**: Kotlin 2.0+
- **UI Framework**: Jetpack Compose (Material 3 Expressive)
- **Architecture**: MVVM + Clean Architecture (Data / Domain / UI)
- **Dependency Injection**: Dagger Hilt
- **Database**: Room + SQLCipher
- **Biometrics**: AndroidX Biometric
- **Preferences**: Jetpack DataStore Preferences
- **Serialization**: Kotlinx Serialization JSON
- **Cryptography**: Bouncy Castle (`bcprov-jdk18on`) & Android KeyStore
- **Image Loading**: Coil Compose
- **Logging**: Timber (Debug only)

---

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
