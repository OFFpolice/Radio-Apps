# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Optimized Proguard rules for WebRadioBot

# Keep line numbers for easier debugging of stack traces, but optimize names
-keepattributes SourceFile,LineNumberTable

# Retrofit & OkHttp
-keepattributes Signature, InnerClasses, EnclosingMethod, RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations, AnnotationDefault
-keep class retrofit2.** { *; }
-keepclassmembers class * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**

# Moshi rules (API JSON Parsing)
-keep class com.squareup.moshi.** { *; }
-keep class * { @com.squareup.moshi.JsonClass *; }
-keep class * { @com.squareup.moshi.Json *; }
-keep class *JsonAdapter { *; }
-keep class *JsonAdapterKt { *; }
-keep class com.example.data.** { *; } # Keep all database and api models intact

# Room database (Local favorites database)
-keep class * extends androidx.room.RoomDatabase
-keep class * extends androidx.room.Dao
-keep class * implements androidx.room.RoomDatabase$Callback
-keep class androidx.room.RoomDatabase
-dontwarn androidx.room.**

# Kotlin Coroutines and general library warnings
-dontwarn kotlinx.coroutines.**
-dontwarn org.codehaus.mojo.animalsniffer.**
-dontwarn javax.annotation.**

# Jetpack Compose and system classes
-keep class androidx.compose.** { *; }
-keep class androidx.lifecycle.** { *; }

# Media3/ExoPlayer components (ensuring audio player functions flawlessly)
-keep class androidx.media3.exoplayer.** { *; }
-keep class androidx.media3.session.** { *; }
-keep class androidx.media3.common.** { *; }
-dontwarn androidx.media3.**
