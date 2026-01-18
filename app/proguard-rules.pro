# Add project specific ProGuard rules here.
-keep class com.comparo.app.** { *; }
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
