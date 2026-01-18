# Comparo - Price Comparison App

Comparo is an Android application designed to help users compare prices across multiple quick commerce platforms in India. The app functions like a browser with tabbed interface, allowing users to view and compare prices from **Zepto**, **Swiggy Instamart**, and **Blinkit** simultaneously.

## Features

### 🌐 Multi-Platform Browser
- Browse three major quick commerce platforms in one app
- Tabbed interface for easy switching between platforms
- Full-featured web browser with navigation controls

### 🛒 Price Comparison
- View product prices from Zepto, Swiggy Instamart, and Blinkit side-by-side
- Quick access to each platform's website
- Search and browse products across all platforms

### 📱 Browser Features
- **Navigation Controls**: Back, Forward, Refresh, and Home buttons
- **URL Bar**: Direct URL input with search functionality
- **WebView**: Full JavaScript support for complete platform functionality
- **Progress Indicator**: Visual feedback during page loading
- **Tab System**: Easy switching between different platforms

## Platforms Supported

1. **Zepto** - Quick commerce delivery
2. **Swiggy Instamart** - Grocery delivery by Swiggy
3. **Blinkit** - Instant grocery delivery

## Technical Details

### Built With
- **Language**: Kotlin
- **UI Framework**: Android SDK with Material Design Components
- **Web Rendering**: WebView with JavaScript support
- **Architecture**: Fragment-based with ViewPager2 for tab navigation

### Requirements
- **Minimum SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)
- **Compile SDK**: 34

### Key Dependencies
- AndroidX Core KTX
- AppCompat
- Material Design Components
- ConstraintLayout
- WebKit
- ViewPager2

## Installation

### Prerequisites
- Android Studio (latest version recommended)
- Android SDK with API level 34
- Gradle 8.1.0+
- Kotlin 1.9.0+

### Build Steps

1. Clone the repository:
```bash
git clone https://github.com/sohan-a11y/Comparo.git
cd Comparo
```

2. Open the project in Android Studio

3. Sync Gradle files:
   - Click on "File" → "Sync Project with Gradle Files"
   - Wait for dependencies to download

4. Build the project:
   - Select "Build" → "Make Project" or press `Ctrl+F9` (Windows/Linux) / `Cmd+F9` (Mac)

5. Run on device/emulator:
   - Connect an Android device or start an emulator
   - Click "Run" → "Run 'app'" or press `Shift+F10`

### Command Line Build

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install on connected device
./gradlew installDebug
```

## Usage

1. **Launch the App**: Open Comparo from your app drawer
2. **Select Platform**: Use the tabs at the top to switch between Zepto, Swiggy Instamart, and Blinkit
3. **Browse Products**: Navigate each platform as you would in a regular browser
4. **Compare Prices**: Switch between tabs to compare prices of the same product across platforms
5. **Search**: Use the URL bar to search for specific products or navigate to different pages

### Navigation Tips
- **Back Button**: Navigate to previous page within the current platform
- **Forward Button**: Move forward in browsing history
- **Refresh Button**: Reload the current page
- **Home Button**: Return to the platform's home page
- **URL Bar**: Enter product names or URLs directly

## App Structure

```
Comparo/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/comparo/app/
│   │       │   ├── MainActivity.kt              # Main activity with navigation
│   │       │   ├── PlatformFragment.kt          # WebView fragment for each platform
│   │       │   └── PlatformPagerAdapter.kt      # ViewPager adapter
│   │       ├── res/
│   │       │   ├── layout/
│   │       │   │   ├── activity_main.xml        # Main activity layout
│   │       │   │   └── fragment_platform.xml    # Platform fragment layout
│   │       │   ├── values/
│   │       │   │   ├── colors.xml               # App colors
│   │       │   │   ├── strings.xml              # String resources
│   │       │   │   └── themes.xml               # App themes
│   │       │   └── drawable/                    # App icons and graphics
│   │       └── AndroidManifest.xml              # App manifest
│   └── build.gradle                             # App-level Gradle config
├── build.gradle                                 # Project-level Gradle config
├── settings.gradle                              # Gradle settings
└── README.md                                    # This file
```

## Features in Detail

### WebView Configuration
- JavaScript enabled for full platform functionality
- DOM storage and database support
- Zoom controls with gesture support
- Wide viewport for better mobile experience
- Automatic page loading progress tracking

### UI Components
- **Material Design**: Modern and clean interface
- **TabLayout**: Intuitive platform switching
- **ViewPager2**: Smooth transitions between platforms
- **Progress Indicators**: Visual feedback for loading states
- **Responsive Layout**: Adapts to different screen sizes

## Privacy & Permissions

The app requires the following permissions:
- **INTERNET**: To load and display platform websites
- **ACCESS_NETWORK_STATE**: To check network connectivity

No personal data is collected or stored by the app. All browsing is done directly through the platform websites.

## Known Limitations

- Prices are displayed as shown on the respective platform websites
- Real-time price updates depend on platform website performance
- Some platform features may require login
- Internet connection required for all functionality

## Future Enhancements

Potential features for future versions:
- Side-by-side price comparison view
- Product search across all platforms simultaneously
- Price history tracking
- Favorites and bookmarks
- Notification for price drops
- Offline mode for previously viewed products

## Contributing

Contributions are welcome! Please feel free to submit issues or pull requests.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## License

This project is open source and available for educational and personal use.

## Disclaimer

This app is an independent project and is not affiliated with, endorsed by, or connected to Zepto, Swiggy, or Blinkit. All trademarks and brand names belong to their respective owners.

## Support

For issues, questions, or suggestions, please open an issue on the GitHub repository.

---

**Developed with ❤️ for savvy shoppers who love comparing prices!**