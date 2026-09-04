# DocEditor (Note-Writer)

DocEditor is an Android document editor built with Kotlin and Jetpack Compose.  
The app supports rich text-style editing, local persistence, importing existing files, and exporting documents to multiple formats.

## Overview

This project is a single-module Android app (`:app`) targeting Android API 24+ with:
- Jetpack Compose UI
- Room database storage for recent/saved documents
- Document import support (DOCX, Markdown, HTML, JSON/docedit, plain text)
- Document export support (DOCX, PDF, HTML, TXT, Markdown)

## Features Present in the Codebase

- Rich editing blocks: paragraphs, tables, images, horizontal rules, page breaks
- Text formatting controls: bold, italic, underline, strike, headings, alignment, lists, indentation, font, size, colors, line spacing
- Undo/redo history
- Search and replace
- Auto-save and manual save/save-as
- Word/character/page/paragraph metrics
- Template-based document creation (`letter`, `minutes`, `resume`)
- Recent documents list (Room-backed)
- File import and export/share flows
- Print support (PDF-backed)

## Technologies Used

- Kotlin
- Android Gradle Plugin
- Jetpack Compose (Material 3)
- AndroidX Lifecycle + ViewModel
- Room (with KSP)
- Coroutines
- Firebase BOM (`firebase-ai`, App Check debug/recaptcha)
- Retrofit, OkHttp, Moshi
- Coil
- Robolectric + Roborazzi (tests)

## Project Structure

```text
.
├── app/
│   ├── src/main/java/com/example/
│   │   ├── data/        # Room database, DAO, repository, entities
│   │   ├── export/      # Import/export/print pipelines
│   │   ├── model/       # Document block model + serializer
│   │   ├── ui/          # Editor screen, dialogs, Compose components
│   │   └── viewmodel/   # DocumentViewModel + state/actions
│   └── src/test/        # Unit/Robolectric/Roborazzi tests
├── gradle/libs.versions.toml
├── build.gradle.kts
└── settings.gradle.kts
```

## Setup

### Prerequisites

- JDK 11
- Android SDK (compile SDK 36)

### Build

```bash
./gradlew assembleDebug
```

### Run Tests

```bash
./gradlew testDebugUnitTest
```

### Install on a connected device/emulator

```bash
./gradlew installDebug
```

## Usage Notes

- Launch the app and edit the active document from the main editor screen.
- Use the menu/toolbar to create, open, save, import, export, print, and apply templates.
- Export output is generated through `ExportManager` and shared via Android share sheet.

## Configuration Notes

- The app applies the Secrets Gradle Plugin with:
  - `propertiesFileName = ".env"`
  - `defaultPropertiesFileName = ".env.example"`
- `google-services` plugin is enabled and configured to warn if `google-services.json` is missing.
- Release signing expects environment variables:
  - `KEYSTORE_PATH` (optional fallback: `my-upload-key.jks`)
  - `STORE_PASSWORD`
  - `KEY_PASSWORD`
