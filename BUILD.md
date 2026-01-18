# Build Instructions

## Prerequisites
Before building the Comparo app, ensure you have:
- Java Development Kit (JDK) 8 or higher
- Android SDK with API level 34
- Android Studio (recommended) or command-line tools

## First Time Setup

1. **Clone the repository** (if not already done):
   ```bash
   git clone https://github.com/sohan-a11y/Comparo.git
   cd Comparo
   ```

2. **Generate Gradle Wrapper JAR** (if missing):
   The gradle-wrapper.jar file is required but is a binary file. To generate it:
   ```bash
   gradle wrapper --gradle-version 8.0
   ```
   
   Or if you have Android Studio:
   - Open the project in Android Studio
   - Android Studio will automatically download and setup the Gradle wrapper

## Building the App

### Using Android Studio (Recommended)
1. Open Android Studio
2. Select "Open an Existing Project"
3. Navigate to the Comparo directory and select it
4. Wait for Gradle sync to complete
5. Click "Build" → "Build Bundle(s) / APK(s)" → "Build APK(s)"
6. The APK will be generated in `app/build/outputs/apk/debug/`

### Using Command Line
```bash
# Make sure gradlew is executable
chmod +x gradlew

# Build debug APK
./gradlew assembleDebug

# Build release APK (requires signing configuration)
./gradlew assembleRelease

# Install directly on connected device
./gradlew installDebug
```

## Running the App

### On Physical Device
1. Enable Developer Options on your Android device
2. Enable USB Debugging
3. Connect device via USB
4. Run: `./gradlew installDebug`
5. Open the Comparo app from your device

### On Emulator
1. Create an Android Virtual Device (AVD) in Android Studio
2. Start the emulator
3. Run: `./gradlew installDebug`
4. The app will launch automatically

## Troubleshooting

### Gradle Sync Failed
- Ensure you have an active internet connection
- Check that your Android SDK is properly installed
- Verify ANDROID_HOME environment variable is set

### Build Failed
- Clean the project: `./gradlew clean`
- Invalidate caches in Android Studio: File → Invalidate Caches / Restart
- Check that you have the correct SDK version (34) installed

### Missing gradle-wrapper.jar
- Run: `gradle wrapper --gradle-version 8.0`
- Or open project in Android Studio which will auto-download it

## Project Structure Check
Verify your project has this structure:
```
Comparo/
├── app/
│   ├── build.gradle
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/comparo/app/
│   │   └── res/
├── build.gradle
├── settings.gradle
├── gradle.properties
├── gradlew
└── gradlew.bat
```

## Additional Resources
- [Android Developer Documentation](https://developer.android.com/)
- [Gradle Build Tool](https://gradle.org/)
- [Android Studio Download](https://developer.android.com/studio)
