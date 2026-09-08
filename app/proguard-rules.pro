# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
# Scanner diagnostics must not survive into a release build: they print recognized text, which
# carries the user's addresses and logins. Every call is already behind BuildConfig.DEBUG, a
# compile-time constant; this strips the calls outright as a second line of defence.
# NOTE: -assumenosideeffects only takes effect when R8 optimization runs, and this module builds
# release with isMinifyEnabled = false, so today the BuildConfig.DEBUG gate is what does the work.
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
}
