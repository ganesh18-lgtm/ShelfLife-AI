# Project: ShelfLife AI (Android 14)

## Architecture
- **Paradigm**: Clean Architecture + MVVM with Unidirectional Data Flow (UDF) via Kotlin Coroutines & StateFlow.
- **UI Framework**: 100% Jetpack Compose with Material Design 3 (Dark/Light theme support).
- **Navigation**: Navigation Compose (`NavHost`) integrated with Scaffold bottom navigation bar.
- **Dependency Injection**: Hilt (`@HiltAndroidApp`, `@HiltViewModel`, `@InstallIn`).
- **Data Layer**: Room Database with KSP annotation processing and reactive `Flow` observation.
- **Vision Engine**: CameraX bound to LifecycleOwner, Google ML Kit Text Recognition (OCR date regex) and Barcode Scanning (UPC/EAN) emitting to `SharedFlow<ScanResult>`.
- **Background Scheduling**: WorkManager `CoroutineWorker` querying items expiring in `< 48h` and triggering rich system notifications via `NotificationCompat.Builder` (handling Android 14 `POST_NOTIFICATIONS`).
- **Target SDK**: Android 14 (API level 34+), minSdk 26.
- **JDK Environment**: OpenJDK 21 pinned via `org.gradle.java.home=C\:/Users/acer/.jdks/jbr-21.0.11`.

---

## Feature Inventory
| # | Feature | Description | Milestone | Source |
|---|---------|-------------|-----------|--------|
| 1 | Gradle Build Toolchain | AGP 9.4.1, Kotlin 2.2.10, KSP 2.3.6, Version Catalog `libs.versions.toml`, JDK 21 pin | M1 | explorer_survey_1 |
| 2 | Application Manifest & Hilt Setup | `AndroidManifest.xml` (permissions: CAMERA, POST_NOTIFICATIONS), `ShelfLifeApp` (`@HiltAndroidApp`) | M1 | explorer_survey_1 |
| 3 | Pure Domain Models & Urgency | `PantryItem`, `Category`, `ExpiryUrgency` (Red <48h, Yellow <5d, Green Fresh), pure urgency calculation | M2 | explorer_survey_2 |
| 4 | Room Entity with SQLite Indexes | `PantryItemEntity` with indexes on `expiryTimestamp`, `barcode`, `category` | M2 | explorer_survey_2 |
| 5 | Room DAO Reactive & Suspend Queries | `PantryDao` with reactive `Flow` queries ordered by `expiryTimestamp ASC`, urgency filtered, & suspend query | M2 | explorer_survey_2 |
| 6 | Room Database & Mappers | `AppDatabase`, `PantryMapper` (entity <-> domain conversion) | M2 | explorer_survey_2 |
| 7 | Repository Layer | `PantryRepository` interface & `PantryRepositoryImpl` with `Dispatchers.IO` non-blocking Flow dispatch | M2 | explorer_survey_2 |
| 8 | Dependency Injection Modules | `DatabaseModule`, `RepositoryModule`, `AppModule` with `@IoDispatcher`, `@MainDispatcher`, `@DefaultDispatcher` | M2 | explorer_survey_2 |
| 9 | In-Memory DAO Unit Tests | `PantryDaoTest` verifying reactive flows, expiry timestamp ordering, and urgency threshold filtering | M2 | explorer_survey_2 |
| 10 | Date Regex Extraction Engine | `DateParser` supporting "EXP DD/MM/YYYY", "BEST BEFORE MM/YY", ISO 8601, and textual months | M3 | spec_miner_survey_3 |
| 11 | CameraX Preview & Lifecycle | `CameraPreview` wrapping `PreviewView` bound to LifecycleOwner via `AndroidView` | M3 | spec_miner_survey_3 |
| 12 | ML Kit Text Recognition Analyzer | `MlKitDateAnalyzer` (`ImageAnalysis.Analyzer`) passing frame buffers to ML Kit Text Recognition | M3 | spec_miner_survey_3 |
| 13 | ML Kit Barcode Analyzer | `BarcodeAnalyzer` extracting product UPC/EAN barcodes | M3 | spec_miner_survey_3 |
| 14 | Vision Scan Event Flow | `ScanResult` sealed hierarchy and thread-safe `SharedFlow<ScanResult>` | M3 | spec_miner_survey_3 |
| 15 | Vision Date Regex Unit Tests | `DateParserTest` / `MlKitDateAnalyzerTest` validating date pattern extractions and boundary dates | M3 | spec_miner_survey_3 |
| 16 | WorkManager Expiry Worker | `ExpiryNotificationWorker` extending `CoroutineWorker` querying items expiring in `< 48h` | M4 | explorer_survey_2 |
| 17 | Local System Notifications | Rich notifications via `NotificationCompat.Builder` (InboxStyle) with Android 14 `POST_NOTIFICATIONS` check | M4 | explorer_survey_2 |
| 18 | Worker 48h Query Unit Tests | `ExpiryNotificationWorkerTest` validating 48-hour threshold querying logic | M4 | explorer_survey_2 |
| 19 | Material 3 Theming & Semantic Colors | Color schemes (Light/Dark), Typography, and Urgency semantic color tokens | M5 | spec_miner_survey_3 |
| 20 | Navigation Compose & Scaffold | `NavHost` with Scaffold bottom navigation bar (Dashboard, Scanner, Recipes) | M5 | spec_miner_survey_3 |
| 21 | Dashboard Screen & Cards | `DashboardScreen` displaying pantry items in color-coded cards based on urgency | M5 | spec_miner_survey_3 |
| 22 | Dashboard Swipe-to-Delete & Quantity | SwipeToDismissBox and quick quantity adjustment stepper updating Room DB via ViewModel | M5 | spec_miner_survey_3 |
| 23 | Scanner Screen Overlay & Reticle | Camera preview with reticle bounding box and animated scanning guide | M5 | spec_miner_survey_3 |
| 24 | Scanner Pre-filled Bottom Sheet | `ModalBottomSheet` pre-filled with scanned product name and extracted expiration date | M5 | spec_miner_survey_3 |
| 25 | Zero-Waste AI Recipe Generation | `RecipeScreen` & `RecipeViewModel` prioritizing expiring items with smart local recipe engine fallback | M5 | spec_miner_survey_3 |
| 26 | ViewModel UDF Unit Tests | `DashboardViewModelTest`, `ScannerViewModelTest`, `RecipeViewModelTest` verifying state transitions | M5 | spec_miner_survey_3 |
| 27 | Full E2E Test Suite Pass | Complete automated unit & integration test suite (`./gradlew testDebugUnitTest`) passing at 100% | M6 | sentinel |
| 28 | Debug APK Build Verification | `./gradlew assembleDebug` compiles without errors | M6 | sentinel |

---

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| M1 | Project Setup & Build Infrastructure | Gradle wrapper, `gradle.properties`, `settings.gradle.kts`, `build.gradle.kts`, `app/build.gradle.kts`, `libs.versions.toml`, `AndroidManifest.xml`, `ShelfLifeApp` | none | DONE |
| M2 | Domain & Data Layer + Hilt DI | Domain models (`PantryItem`, `Category`, `ExpiryUrgency`), `PantryRepository`, `PantryItemEntity`, `PantryDao`, `AppDatabase`, `PantryRepositoryImpl`, Hilt modules, `PantryDaoTest` | M1 | DONE |
| M3 | Vision Engine (CameraX & ML Kit) | `DateParser`, `MlKitDateAnalyzer`, `BarcodeAnalyzer`, `ScanResult`, `CameraPreview`, `DateParserTest` | M1 | IN_PROGRESS |
| M4 | WorkManager & Notification Engine | `ExpiryNotificationWorker`, Notification helper (Android 14 `POST_NOTIFICATIONS`), `ExpiryNotificationWorkerTest` | M2 | PLANNED |
| M5 | Presentation Layer (Compose UI & ViewModels) | M3 theme, `DashboardScreen`, `ScannerScreen`, `RecipeScreen`, Navigation `NavHost`, ViewModels, UI unit tests | M2, M3, M4 | PLANNED |
| M6 | Final Integration & Test Hardening | Full build (`assembleDebug`), full unit test suite (`testDebugUnitTest`), 100% acceptance verification | M1–M5, E2E Track | PLANNED |

---

## Interface Contracts

### Domain ↔ Data Layer
- **`PantryItem`**: `id: Long = 0L, name: String, barcode: String?, expiryTimestamp: Long, category: Category, quantity: Int`
- **`Category`**: `PRODUCE, DAIRY, MEAT, PANTRY, BAKERY, BEVERAGES, FROZEN, OTHER`
- **`ExpiryUrgency`**: `CRITICAL` (Red < 48h), `WARNING` (Yellow < 5d), `FRESH` (Green >= 5d), `EXPIRED` (< 0)
- **`PantryRepository`**:
  ```kotlin
  interface PantryRepository {
      fun getAllItems(): Flow<List<PantryItem>>
      fun getItemsByUrgencyThreshold(thresholdTimestamp: Long): Flow<List<PantryItem>>
      suspend fun getItemsExpiringWithinSync(thresholdTimestamp: Long): List<PantryItem>
      suspend fun getItemById(id: Long): PantryItem?
      suspend fun getItemByBarcode(barcode: String): PantryItem?
      suspend fun insertItem(item: PantryItem): Long
      suspend fun updateItem(item: PantryItem)
      suspend fun updateQuantity(id: Long, quantity: Int)
      suspend fun deleteItem(item: PantryItem)
      suspend fun deleteItemById(id: Long)
  }
  ```

### Vision Engine ↔ Presentation Layer
- **`ScanResult`**:
  ```kotlin
  sealed interface ScanResult {
      data class BarcodeDetected(val barcode: String, val format: Int) : ScanResult
      data class ExpiryDateDetected(val rawText: String, val parsedTimestamp: Long) : ScanResult
      data class ScanError(val throwable: Throwable) : ScanResult
  }
  ```
- **`DateParser`**:
  ```kotlin
  object DateParser {
      fun parseExpiryDate(text: String, nowTimestamp: Long = System.currentTimeMillis()): Long?
  }
  ```

### WorkManager ↔ Notification Engine
- **`ExpiryNotificationWorker`**:
  - Worker queries `repository.getItemsExpiringWithinSync(System.currentTimeMillis() + 48 * 3600 * 1000)`.
  - Fires notification using channel ID `"shelf_life_expiry_channel"` with `NotificationCompat.InboxStyle`.
  - Checks `NotificationManagerCompat.from(context).areNotificationsEnabled()`.

---

## Code Layout
```
d:\My Projects\Shell-Life\
├── gradle/
│   ├── wrapper/
│   │   ├── gradle-wrapper.jar
│   │   └── gradle-wrapper.properties
│   └── libs.versions.toml
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
└── app/
    ├── build.gradle.kts
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   └── java/com/shelflife/ai/
        │       ├── ShelfLifeApp.kt
        │       ├── MainActivity.kt
        │       ├── domain/
        │       │   ├── model/ (PantryItem.kt, Category.kt, ExpiryUrgency.kt)
        │       │   └── repository/ (PantryRepository.kt)
        │       ├── data/
        │       │   ├── local/ (PantryItemEntity.kt, PantryDao.kt, AppDatabase.kt, PantryMapper.kt)
        │       │   ├── repository/ (PantryRepositoryImpl.kt)
        │       │   └── vision/ (DateParser.kt, MlKitDateAnalyzer.kt, BarcodeAnalyzer.kt, ScanResult.kt)
        │       ├── di/ (AppModule.kt, DatabaseModule.kt, RepositoryModule.kt)
        │       ├── worker/ (ExpiryNotificationWorker.kt, WorkManagerHelper.kt)
        │       └── presentation/
        │           ├── navigation/ (ShelfLifeNavigation.kt, Screen.kt)
        │           ├── theme/ (Color.kt, Theme.kt, Type.kt)
        │           ├── dashboard/ (DashboardScreen.kt, DashboardViewModel.kt, PantryUiState.kt, PantryItemCard.kt)
        │           ├── scanner/ (ScannerScreen.kt, ScannerViewModel.kt, CameraPreview.kt, ScannerReticle.kt)
        │           └── recipe/ (RecipeScreen.kt, RecipeViewModel.kt, RecipeUiState.kt, RecipeGenerator.kt)
        └── test/
            └── java/com/shelflife/ai/
                ├── data/
                │   ├── local/PantryDaoTest.kt
                │   └── vision/DateParserTest.kt
                ├── worker/ExpiryNotificationWorkerTest.kt
                └── presentation/
                    ├── dashboard/DashboardViewModelTest.kt
                    ├── scanner/ScannerViewModelTest.kt
                    └── recipe/RecipeViewModelTest.kt
```
