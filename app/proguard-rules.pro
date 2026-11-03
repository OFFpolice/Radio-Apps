# Keep line numbers for easier debugging of stack traces, but optimize names
-keepattributes SourceFile,LineNumberTable,Signature,InnerClasses,EnclosingMethod,RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,AnnotationDefault

# App Models & JSON serialization
-keep class com.offpolice.webradiobot.data.** { *; }
-keep class * { @com.squareup.moshi.JsonClass *; }
-keep class * { @com.squareup.moshi.Json *; }
-keep class *JsonAdapter { *; }
-keep class *JsonAdapterKt { *; }

# Retrofit service interface methods
-keepclassmembers class * {
    @retrofit2.http.* <methods>;
}

# Room database
-keep class * extends androidx.room.RoomDatabase
-keep class * extends androidx.room.Dao
-dontwarn androidx.room.**

# Suppress harmless warnings from dependencies
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn kotlinx.coroutines.**
-dontwarn androidx.media3.**
-dontwarn org.codehaus.mojo.animalsniffer.**
-dontwarn javax.annotation.**
