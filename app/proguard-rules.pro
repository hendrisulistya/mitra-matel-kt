# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
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

# Keep Kotlin metadata
-keep class kotlin.Metadata { *; }

# Keep Compose related classes
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep Ktor client classes
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# Keep gRPC classes
-keep class io.grpc.** { *; }
-dontwarn io.grpc.**

# Keep Protobuf classes
-keep class com.google.protobuf.** { *; }
-dontwarn com.google.protobuf.**

# Keep serialization classes
-keep class kotlinx.serialization.** { *; }
-dontwarn kotlinx.serialization.**

# Keep data classes used for serialization
-keep @kotlinx.serialization.Serializable class * {
    *;
}

# Keep enum classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Parcelable implementations
-keep class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Keep BuildConfig
-keep class **.BuildConfig { *; }

# Keep R class
-keep class **.R
-keep class **.R$* {
    <fields>;
}

# SLF4J Logger - ignore missing implementation
-dontwarn org.slf4j.**
-keep class org.slf4j.** { *; }

# Ignore missing SLF4J implementation classes
-dontwarn org.slf4j.impl.StaticLoggerBinder