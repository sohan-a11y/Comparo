# Comparo - Android Price Comparison App

A complete Android application that compares grocery prices across Swiggy Instamart, Zepto, and Blinkit using headless WebViews to intercept API calls.

## Download

### Latest APK
You can download the latest compiled APK without needing Android Studio:

**Option 1: From GitHub Actions (Latest Build)**
1. Go to [Actions tab](https://github.com/sohan-a11y/Comparo/actions)
2. Click on the latest successful "Build Android APK" workflow run
3. Scroll down to "Artifacts" section
4. Download `Comparo-APK-build-XXX`
5. Extract the ZIP file to get the APK

**Option 2: From Releases (Stable Versions)**
1. Go to [Releases page](https://github.com/sohan-a11y/Comparo/releases)
2. Download the latest `Comparo-vX.X.X.apk` file

### Installation
1. Download the APK file
2. Enable "Install from Unknown Sources" in your Android settings
3. Open the APK file and install
4. Grant necessary permissions when prompted

**Note:** This is a debug APK for testing purposes. You may see a warning about installing apps from unknown sources - this is normal for APKs not downloaded from the Play Store.

---

## Features

- **Multi-Platform Price Comparison**: Compare prices across Swiggy, Zepto, and Blinkit simultaneously
- **Headless WebView Architecture**: Uses hidden WebViews to intercept API calls without breaking platform functionality
- **Modern UI**: Built with Jetpack Compose and Material 3 design
- **Smart Product Grouping**: Groups similar products from different platforms for easy comparison
- **Price Highlighting**: Automatically highlights the cheapest option with visual indicators
- **Login Management**: Secure login flow for each platform with cookie persistence
- **Real-time Search**: Search across all platforms with a single query
- **Delivery Time Display**: Shows estimated delivery time for each platform

## Architecture

### Core Components

1. **NetworkInterceptor** (`interceptor/NetworkInterceptor.kt`)
   - Extends `WebViewClient` to intercept API requests
   - Clones requests using OkHttp with proper headers
   - Returns null to allow original WebView requests
   - Parses JSON responses and broadcasts to MainActivity

2. **PlatformParser** (`parser/PlatformParser.kt`)
   - Parses JSON responses from all three platforms
   - Extracts product information (name, price, ETA, stock status)
   - Handles platform-specific data structures
   - Filters out-of-stock items

3. **MainActivity** (`MainActivity.kt`)
   - Manages three headless WebViews (one per platform)
   - Handles platform login dialogs
   - Coordinates search across all platforms
   - Updates UI state with search results

4. **Jetpack Compose UI** (`ui/ComparoApp.kt`)
   - **SetupScreen**: Platform login interface
   - **HomeScreen**: Search interface and results display
   - **ComparisonCard**: Individual product price comparison

## Technical Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material 3
- **Networking**: OkHttp for API request cloning
- **WebView**: AndroidX WebKit for modern WebView support
- **Async**: Kotlin Coroutines for background operations
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)

## Dependencies

```kotlin
- androidx.core:core-ktx:1.12.0
- androidx.compose:compose-bom:2024.01.00
- androidx.compose.material3:material3
- androidx.activity:activity-compose:1.8.2
- com.squareup.okhttp3:okhttp:4.12.0
- kotlinx-coroutines-android:1.7.3
- androidx.webkit:webkit:1.9.0
```

## How It Works

1. **Login Phase**: Users log into each platform (Swiggy, Zepto, Blinkit) through WebView dialogs. Cookies are saved for session persistence.

2. **Search Phase**: When a user searches for a product:
   - The query is sent to all three headless WebViews simultaneously
   - Each WebView loads the respective platform's search page
   - NetworkInterceptor detects and clones API calls
   - JSON responses are parsed by PlatformParser
   - Results are aggregated and displayed in the UI

3. **Comparison**: Products are grouped by name, sorted by price, and displayed with the cheapest option highlighted.

## File Structure

```
app/src/main/
├── AndroidManifest.xml
├── java/com/sohan/comparo/
│   ├── MainActivity.kt
│   ├── interceptor/
│   │   └── NetworkInterceptor.kt
│   ├── parser/
│   │   └── PlatformParser.kt
│   └── ui/
│       ├── ComparoApp.kt
│       └── Theme.kt
└── res/
    └── values/
        └── strings.xml
```

## Building

```bash
./gradlew assembleDebug
```

## Running

```bash
./gradlew installDebug
```

## Security & Privacy

- Uses cleartext traffic for HTTP debugging (can be disabled for production)
- WebView debugging enabled for development
- Cookies stored locally on device
- No data sent to external servers

## Future Enhancements

- Price history tracking
- Push notifications for price drops
- Favorites/wishlist functionality
- Share comparison results
- Dark mode support
- Product image display
- Additional platform support

## License

This is a demonstration project for educational purposes.
