# Weather Map Lithuania (Android Application)

[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Language](https://img.shields.io/badge/Language-Kotlin-purple.svg)](https://kotlinlang.org)
[![Architecture](https://img.shields.io/badge/Architecture-MVVM-blue.svg)](https://developer.android.com/topic/architecture)
[![API](https://img.shields.io/badge/Min%20SDK-24-orange.svg)](https://developer.android.com)
[![Target SDK](https://img.shields.io/badge/Target%20SDK-34-brightgreen.svg)](https://developer.android.com)

**Course:** Mobile Application Development Technologies (MADT) — Lab 6  
**Package:** `com.madt.lab6.weathermap`  
**Target Region:** Lithuania (`55.1694° N, 23.8813° E`)  

---

## Table of Contents

1. [Project Title and Overview](#1-project-title-and-overview)
2. [Main Objectives](#2-main-objectives)
3. [Technologies Used](#3-technologies-used)
4. [Main Application Features](#4-main-application-features)
5. [Application Architecture and Project Structure](#5-application-architecture-and-project-structure)
6. [Authentication Workflow](#6-authentication-workflow)
7. [Weather API and JSON Data Handling](#7-weather-api-and-json-data-handling)
8. [Map Markers, Search and Filtering](#8-map-markers-search-and-filtering)
9. [Setup Instructions](#9-setup-instructions)
10. [Firebase Configuration (`google-services.json`)](#10-firebase-configuration-google-servicesjson)
11. [Google Maps API Key Configuration (`local.properties`)](#11-google-maps-api-key-configuration-localproperties)
12. [Build and Run Instructions](#12-build-and-run-instructions)
13. [Testing and Verification Status](#13-testing-and-verification-status)
14. [Known Limitations](#14-known-limitations)
15. [AI Usage Declaration](#15-ai-usage-declaration)

---

## 1. Project Title and Overview

**Weather Map Lithuania** is a native Android application engineered for the Mobile Application Development Technologies (MADT) laboratory curriculum. The app delivers a georeferenced weather visualization across all administrative regions, municipalities, and towns in the Republic of Lithuania.

By bridging cloud authentication (**Firebase Authentication**), cloud-hosted cartography (**Google Maps SDK for Android**), and real-time open environmental telemetry (**Meteo.lt Web API**), the app enables users to securely log in, inspect locations on a high-resolution interactive map, filter municipalities dynamically by current meteorological conditions or text search, and drill down into comprehensive weather parameters via an expandable bottom sheet.

---

## 2. Main Objectives

* **User Authentication & Session Management:** Implement secure registration and login flows using Firebase Authentication, maintaining persistent session state and safeguarding data queries.
* **Geospatial Mapping & Visualization:** Integrate the Google Maps SDK to plot Lithuanian settlements accurately with custom, color-coded condition markers and interactive camera controls.
* **RESTful API Integration & JSON Deserialization:** Consume the official Meteo.lt API using Retrofit, OkHttp, and Gson to parse complex nested JSON forecasts into typed Kotlin data structures.
* **Asynchronous Concurrency & Rate Limiting:** Manage background synchronization using Kotlin Coroutines and rate-limiting semaphores to batch network requests without exceeding API thresholds or freezing the UI.
* **Architecture & Clean Code:** Employ the **Model-View-ViewModel (MVVM)** design pattern and Repository layer with LiveData and ViewBinding, strictly decoupling UI rendering from network and domain logic.

---

## 3. Technologies Used

The project relies on a modern Android native tech stack:

* **Kotlin:** Modern, concise programming language configured for JVM target 17.
* **Android XML Views & Material Design Components:** Material 3 UI layouts (`CoordinatorLayout`, `MaterialToolbar`, `MaterialCardView`, `LinearProgressIndicator`, `BottomSheetBehavior`) powered by Android **ViewBinding**.
* **Firebase Authentication (BOM 33.7.0):** Cloud identity service providing email/password registration, login, token management, and session invalidation.
* **Meteo.lt Web API:** Open REST service operated by the Lithuanian Hydrometeorological Service (LHMT), providing geographical coordinates and Numerical Weather Prediction (NWP) model forecasts.
* **Google Maps SDK for Android (v19.0.0):** Hardware-accelerated vector mapping engine providing marker placement, custom hues, camera animations, and tap gestures.
* **Retrofit 2 (v2.11.0) & Gson (v2.11.0):** Type-safe HTTP client with automatic JSON deserialization and OkHttp 4 logging interceptors.
* **Kotlin Coroutines & Flow (v1.8.1):** Asynchronous structured concurrency utilizing `viewModelScope`, `Dispatchers.IO`, `SupervisorJob`, and `Semaphore` constructs.
* **MVVM Architecture & Android Jetpack:** `ViewModel`, `LiveData`, and `Lifecycle` components ensuring robust lifecycle awareness across device rotations and configuration shifts.

| Technology | Artifact / Version | Purpose |
| :--- | :--- | :--- |
| **Language** | Kotlin 2.x (Java 17 target) | Application business and UI logic |
| **Build Tool** | Android Gradle Plugin 9.4.1 / Gradle 9.6 | Dependency management and compilation |
| **UI Framework** | Android XML Views & ViewBinding | Layout declaration and view binding |
| **Material Components** | `com.google.android.material:material:1.14.0` | Cards, progress indicators, bottom sheets |
| **Authentication** | `com.google.firebase:firebase-auth` (BOM 33.7.0) | User authentication and session control |
| **Mapping Engine** | `com.google.android.gms:play-services-maps:19.0.0` | Interactive map and spatial markers |
| **REST Networking** | `com.squareup.retrofit2:retrofit:2.11.0` | HTTP communications with Meteo.lt |
| **JSON Parser** | `com.squareup.retrofit2:converter-gson:2.11.0` | Deserializing API responses |
| **Concurrency** | `kotlinx-coroutines-android:1.8.1` | Non-blocking background worker pipelines |
| **Architecture** | `androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7` | Reactive MVVM state delivery |

---

## 4. Main Application Features

1. **Two-Stage Data Initialization:**
   * **Stage 1 (Instant Placement):** Instantly queries all Lithuanian place names and coordinates (`/v1/places`) and displays geographic pins on the map without blocking.
   * **Stage 2 (Throttled Background Sync):** Seamlessly syncs long-term forecast timestamps in controlled batches (concurrency factor of 3) with a real-time linear progress indicator (`Syncing weather data: X/Y locations…`).
2. **Dynamic Color-Coded Map Markers:**
   * Map pins change color dynamically based on real-time weather condition codes:
     * 🟠 **Orange:** Clear sky
     * 🟡 **Yellow:** Isolated / Scattered clouds
     * 🔵 **Azure:** Overcast
     * 🔷 **Blue:** Light, moderate, or heavy rain
     * 🧊 **Cyan:** Snow, sleet, or winter mix
     * 🟣 **Violet:** Fog or mist
     * 🔴 **Red:** Initial pending state / fallback
3. **Interactive Weather Detail Bottom Sheet:**
   * Tapping any map marker opens an expandable modal card displaying:
     * Location Name & Administrative Division
     * Exact GPS Coordinates (Latitude / Longitude)
     * Air Temperature (°C) and "Feels Like" Temperature (°C)
     * Wind Speed (m/s) and Wind Direction (degrees)
     * Relative Humidity (%) & Total Precipitation (mm)
     * Atmospheric Pressure (hPa) & Cloud Cover (%)
     * Forecast validation timestamp
4. **Real-Time Live Search:**
   * Integrated search bar instantly filters visible markers as the user types, querying against place name and municipality/division.
5. **Weather Condition Dropdown Filter:**
   * Dedicated condition filter allows isolating settlements by weather type (e.g., viewing only towns currently experiencing snow or clear skies).
6. **Client-Side Authentication Gating:**
   * All network repository requests verify Firebase authentication status prior to dispatching network calls, ensuring unauthenticated sessions cannot consume weather endpoints.
7. **In-Memory Caching:**
   * Thread-safe cache with a 20-minute Time-To-Live (TTL) prevents duplicate network calls when re-inspecting locations.
8. **Clean Session Teardown:**
   * Logging out terminates active background coroutine sync jobs, wipes the memory cache, signs out of Firebase, and resets navigation back to the authentication screen.

---

## 5. Application Architecture and Project Structure

The project strictly follows the recommended Android **Model-View-ViewModel (MVVM)** pattern combined with the **Repository Pattern**.

```
                   ┌──────────────────────────────┐
                   │          UI Layer            │
                   │   AuthActivity, MapActivity  │
                   │   (XML Views + ViewBinding)  │
                   └──────────────▲───────────────┘
                                  │ Observes LiveData / Dispatches Events
                   ┌──────────────┴───────────────┐
                   │       ViewModel Layer        │
                   │ AuthViewModel, WeatherMapVM  │
                   └──────────────▲───────────────┘
                                  │ Calls suspend functions
                   ┌──────────────┴───────────────┐
                   │       Repository Layer       │
                   │  AuthRepo, WeatherRepository │
                   │    (In-Memory TTL Cache)     │
                   └───────▲──────────────▲───────┘
                           │              │
      ┌────────────────────┴───┐      ┌───┴────────────────────┐
      │     Firebase Auth      │      │       Meteo.lt         │
      │  (Google Play Services)│      │  (Retrofit + OkHttp)   │
      └────────────────────────┘      └────────────────────────┘
```

### Directory Structure

```text
app/src/main/
├── AndroidManifest.xml
├── java/com/madt/lab6/weathermap/
│   ├── data/
│   │   ├── api/
│   │   │   ├── MeteoApiService.kt          # Retrofit interface (places & forecasts endpoints)
│   │   │   └── RetrofitClient.kt           # OkHttpClient & Retrofit singleton builder
│   │   ├── model/
│   │   │   ├── ForecastResponse.kt         # Forecast timestamps and meteorological metrics
│   │   │   ├── LocationWeather.kt          # UI presentation model (filtering & search logic)
│   │   │   └── Place.kt                    # Place name, code, division, and coordinates
│   │   └── repository/
│   │       ├── AuthRepository.kt           # Firebase Auth wrapper (signIn, signUp, signOut)
│   │       └── WeatherRepository.kt        # Network fetching, semaphore throttling, 20-min cache
│   └── ui/
│       ├── auth/
│       │   ├── AuthActivity.kt             # Form validation, login/register UI toggle
│       │   └── AuthViewModel.kt            # LiveData auth state machine
│       └── map/
│           ├── MapActivity.kt              # Google Map fragment, markers, search, bottom sheet
│           └── WeatherMapViewModel.kt      # Two-phase loading, sync progress, filter coordinator
└── res/
    ├── layout/
    │   ├── activity_auth.xml               # Clean card-based login & registration form
    │   ├── activity_map.xml                # Map view with search/filter overlay & progress bar
    │   └── bottom_sheet_weather_detail.xml # Expandable card with weather metrics grid
    ├── menu/
    │   └── menu_map.xml                    # Action bar items (User Email, Refresh, Logout)
    └── values/
        ├── colors.xml                      # Palette (Teal, Navy, Slate, Condition hues)
        ├── strings.xml                     # Localized string resources & formatted templates
        └── themes.xml                      # Material3 Day/Night theme configurations
```

---

## 6. Authentication Workflow

Authentication is enforced using **Firebase Authentication** with an email/password credential provider:

```
[Launch App] 
     │
     ▼
[AuthActivity.onCreate] ──► [AuthViewModel.checkExistingSession]
                                   │
                   ┌───────────────┴───────────────┐
            User != null                    User == null
                   │                               │
                   ▼                               ▼
          [Navigate MapActivity]            [Show Auth Form]
                                                   │
                                      ┌────────────┴────────────┐
                                      │ Enter Email & Password  │
                                      └────────────┬────────────┘
                                                   │
                                      [Validate Input locally]
                                      (Email regex, min 6 chars, match)
                                                   │
                                      [Submit: signIn / signUp]
                                                   │
                                   ┌───────────────┴───────────────┐
                                Success                         Failure
                                   │                               │
                                   ▼                               ▼
                         [Navigate MapActivity]           [Display Snackbar Error]
```

* **Session Persistence:** When the application launches, `AuthViewModel` evaluates `FirebaseAuth.getInstance().currentUser`. If an active token exists, the user skips the login screen and lands directly on `MapActivity`.
* **Client-Side Form Validation:**
  * Checks for empty fields and formats using `Patterns.EMAIL_ADDRESS`.
  * Enforces a minimum password length of 6 characters.
  * Ensures password confirmation match during registration mode.
* **Logout & Security Teardown:**
  * Invoking **Sign Out** from the toolbar menu clears the Firebase authentication token, cancels background coroutines via `WeatherRepository.onUserSignOut()`, purges cached weather data, and clears the backstack to return to `AuthActivity`.

---

## 7. Weather API and JSON Data Handling

### 7.1 Meteo.lt REST Endpoints

Weather data is sourced from the open API provided by the Lithuanian Hydrometeorological Service:

* **Places Endpoint:**
  ```http
  GET https://api.meteo.lt/v1/places
  ```
  Returns a JSON array of all registered Lithuanian administrative places with their coordinates (`latitude`, `longitude`), `name`, `code`, and `administrativeDivision`.

* **Long-Term Forecast Endpoint:**
  ```http
  GET https://api.meteo.lt/v1/places/{placeCode}/forecasts/long-term
  ```
  Returns detailed Numerical Weather Prediction (NWP) model timestamps containing:
  * `airTemperature` (°C), `feelsLikeTemperature` (°C)
  * `windSpeed` (m/s), `windGust` (m/s), `windDirection` (degrees)
  * `cloudCover` (%), `seaLevelPressure` (hPa), `relativeHumidity` (%)
  * `totalPrecipitation` (mm)
  * `conditionCode` (e.g. `clear`, `overcast`, `light-rain`, `heavy-snow`, `fog`)

> **Note on Forecast Data:** Meteo.lt generates numerical predictions based on ECMWF/HIRLAM model cycles that recalculate 2–4 times per day. The data reflects model predictions rather than instantaneous minute-by-minute sensor readings.

### 7.2 Rate-Limiting & Memory Caching

To operate responsibly with the public API and preserve device network bandwidth:
* **Controlled Concurrency:** Requesting forecasts for hundreds of Lithuanian settlements simultaneously would trigger rate limits. The app utilizes `kotlinx.coroutines.sync.Semaphore(3)` alongside Kotlin's `chunked(3)` operator, guaranteeing no more than 3 simultaneous HTTP requests.
* **In-Memory Cache (TTL: 20 Minutes):** Forecast responses are cached in a `ConcurrentHashMap` with a 20-minute validity window (`CACHE_TTL_MILLIS = 1,200,000`). If a user revisits a place or searches within 20 minutes, data is served instantly from memory.

---

## 8. Map Markers, Search and Filtering

### 8.1 Map & Camera Setup

The Google Map fragment (`SupportMapFragment`) initializes centered precisely over Lithuania:
* **Coordinates:** `55.1694° N, 23.8813° E`
* **Default Zoom Level:** `7.2f` (encompassing the entire territory from Klaipėda to Vilnius).
* **UI Controls:** Zoom controls and compass enabled.

### 8.2 Differential Marker Rendering

To maintain smooth 60 FPS map performance without memory leaks:
* Markers are tracked in an internal lookup table (`activeMarkers: HashMap<String, Marker>`).
* When search queries or filter conditions update, the map dynamically removes obsolete pins and updates existing pin snippets or icons without clearing the entire map surface.

### 8.3 Search and Filter Operations

* **Live Query Filtering:** The `SearchView` widget triggers `onSearchQueryChanged()`, performing case-insensitive matching against both `place.name` (e.g., "Kaunas") and `place.administrativeDivision` (e.g., "Kauno miesto savivaldybė").
* **Condition Filter Spinner:** A dropdown filter categorizes weather states into user-friendly groups:
  * *All Conditions*
  * *Clear*
  * *Isolated Clouds*
  * *Scattered Clouds*
  * *Overcast*
  * *Light Rain*
  * *Moderate / Heavy Rain*
  * *Snow / Sleet*
  * *Fog / Mist*

---

## 9. Setup Instructions

### Prerequisites

* **Operating System:** Windows 10/11, macOS, or Linux.
* **Android Studio:** Android Studio Ladybug (2024.2.1+) or newer.
* **Java Development Kit (JDK):** Version 17 (recommended: Android Studio bundled JBR 17 or higher).
* **Android SDK:** 
  * `compileSdk`: 35 / release 37
  * `targetSdk`: 34
  * `minSdk`: 24 (compatible with Android 7.0 Nougat and above)
* **Device / Emulator:** An Android Virtual Device (AVD) running system image with **Google Play Services** enabled (API 28–34).

### Repository Cloning

```bash
git clone <repository-url>
cd MADT_Lab6_WebAPI_Map
```

---

## 10. Firebase Configuration (`google-services.json`)

Firebase Authentication requires a registered project configuration file:

1. Navigate to the [Firebase Console](https://console.firebase.google.com/).
2. Create a new Firebase project (e.g., `weather-map-lithuania`).
3. Add an **Android Application** to your project:
   * **Android package name:** `com.madt.lab6.weathermap` *(must match exactly)*.
   * **App nickname:** Weather Map Lithuania.
   * Debug signing certificate SHA-1 (optional for email/password, recommended for debugging).
4. Download the generated `google-services.json` file.
5. Place `google-services.json` into the project's `app/` directory:
   ```text
   MADT_Lab6_WebAPI_Map/
   └── app/
       ├── build.gradle.kts
       └── google-services.json   <--- Place file here
   ```
6. In Firebase Console, go to **Build > Authentication > Sign-in method**, and enable the **Email/Password** provider.

> [!IMPORTANT]
> The `app/google-services.json` file contains project credentials and is registered in `.gitignore`. Never commit private production keys to public GitHub repositories.

---

## 11. Google Maps API Key Configuration (`local.properties`)

The application consumes the Google Maps SDK via an API key injected at build time from `local.properties`.

1. Obtain an API key from the [Google Cloud Console](https://console.cloud.google.com/).
2. Ensure the **Maps SDK for Android** API is enabled for your project.
3. Open (or create) the `local.properties` file in the root of the project.
4. Add your key using the property name `MAPS_API_KEY`:

```properties
## This file must NOT be checked into Version Control Systems.
sdk.dir=C\:\\Users\\<YourUser>\\AppData\\Local\\Android\\Sdk
MAPS_API_KEY=AIzaSyYourActualGoogleMapsApiKeyHere
```

5. How it works in Gradle:
   In `app/build.gradle.kts`, the key is read securely and passed as a manifest placeholder:
   ```kotlin
   val mapsApiKey: String = localProperties.getProperty("MAPS_API_KEY") ?: "YOUR_GOOGLE_MAPS_API_KEY_HERE"
   
   android {
       defaultConfig {
           manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
       }
   }
   ```
   This populates the `<meta-data android:name="com.google.android.geo.API_KEY" android:value="${MAPS_API_KEY}" />` tag in `AndroidManifest.xml` during compilation.

> [!NOTE]
> A template file `local.properties.example` is provided in the repository root for reference.

---

## 12. Build and Run Instructions

### Building via Android Studio

1. Launch Android Studio and choose **File > Open**, selecting the `MADT_Lab6_WebAPI_Map` folder.
2. Allow Gradle to perform the initial project sync and dependency download.
3. Ensure an AVD with **Google Play** is created and selected in the device selector.
4. Click **Run** (`Shift + F10`) or **Debug** (`Shift + F9`).

### Building via Command Line (Gradle Wrapper)

From the project root:

* **Compile Debug APK:**
  ```bash
  # Windows
  .\gradlew.bat assembleDebug

  # macOS / Linux
  ./gradlew assembleDebug
  ```
  The compiled APK is generated at: `app/build/outputs/apk/debug/app-debug.apk`.

* **Run Unit Tests:**
  ```bash
  # Windows
  .\gradlew.bat test

  # macOS / Linux
  ./gradlew test
  ```

* **Install Directly on Connected Device:**
  ```bash
  .\gradlew.bat installDebug
  ```

---

## 13. Testing and Verification Status

The following matrix documents the verification and testing status of application modules for this lab assignment:

| Component / Task | Verification Scope | Status | Remarks |
| :--- | :--- | :---: | :--- |
| **Gradle Compilation** | `assembleDebug` build task | ✅ **VERIFIED** | Clean build completed without compilation or resource errors. |
| **Unit Test Execution** | Gradle `test` tasks | ✅ **VERIFIED** | Unit test tasks execute and complete successfully. |
| **Firebase Initialization** | Firebase App bootstrap | ✅ **VERIFIED** | Firebase SDK initializes properly in the app process. |
| **Auth Screen Runtime** | UI rendering, validation, mode switch | ✅ **VERIFIED** | AuthActivity launches; field validation and toggles work on emulator. |
| **Firebase End-to-End Auth** | Live user registration and login | ⚠️ **NOT TESTED** | Requires active network and authenticated Firebase backend at test time. |
| **Google Maps Runtime** | Map tile rendering & Play Services runtime | ⚠️ **NOT TESTED** | Requires active, authorized Google Cloud key & Play Services on test AVD. |
| **Live Meteo.lt API Sync** | Live HTTP requests & continuous sync | ⚠️ **NOT TESTED** | Requires active external internet connectivity from the test runtime. |

---

## 14. Known Limitations

* **Client-Enforced Security Barrier:** Meteo.lt is an open public REST API and does not validate Firebase JWT authorization tokens on its server. The Firebase authentication check is enforced within the client-side repository layer (`ensureAuthenticated()`).
* **Initial Place Pin Status:** During Phase 1 of data loading, places are plotted before individual forecast timestamps arrive. Markers initially appear in red/pending state and progressively update to their condition-specific hue as the background sync batches complete.
* **Network Disconnection Behavior:** While unexpired forecast data is retained in memory for 20 minutes, fully offline launches without a prior cache will display an error banner with a manual "Retry" action.
* **Model Update Latency:** Meteorological forecasts reflect numerical prediction model runs published 2 to 4 times per day by LHMT, which means rapid local shower variations may exhibit short reporting delays.

---

## 15. AI Usage Declaration

### University Academic Integrity Declaration

This project was developed as part of the **Mobile Application Development Technologies (MADT)** curriculum. In compliance with university guidelines regarding academic honesty and artificial intelligence assistance:

* **Tool Utilized:** Google DeepMind AI Assistant (Antigravity).
* **Scope of Assistance:**
  1. Structuring the MVVM architecture and Kotlin Coroutine rate-limiting strategies (`Semaphore` and batching).
  2. Providing boilerplate guidance for Retrofit client construction and Gson deserialization mapping.
  3. Assisting with Android Material 3 XML layouts, including the `BottomSheetBehavior` configuration and custom marker hue logic.
  4. Generating technical documentation, Markdown structure, and setup instructions.
* **Student Verification & Responsibility:**
  * All architecture decisions, source code files, and configuration setups were reviewed, analyzed, debugged, and verified by the student.
  * The student understands the operational mechanics of all components, including Firebase authentication flows, Retrofit network dispatchers, and Google Maps lifecycle events.

---

*Authored for MADT Lab 6 — Vilnius, 2026.*
