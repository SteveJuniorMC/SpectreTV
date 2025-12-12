# Add project specific ProGuard rules here.

# Keep generic type information for Gson/Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# Keep Retrofit
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**

# Keep OkHttp
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# Keep Gson
-keep class com.google.gson.** { *; }
-keep class com.google.gson.stream.** { *; }
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep TypeToken for Gson generics (critical for ParameterizedType)
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# Keep all app data classes with their generic types
-keep class com.spectretv.app.data.remote.dto.** { *; }
-keep class com.spectretv.app.domain.model.** { *; }
-keep class com.spectretv.app.data.local.entity.** { *; }

# Keep fields for serialization
-keepclassmembers class com.spectretv.app.data.remote.dto.** { <fields>; }
-keepclassmembers class com.spectretv.app.domain.model.** { <fields>; }
-keepclassmembers class com.spectretv.app.data.local.entity.** { <fields>; }

# Keep Room entities
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel

# Kotlin
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**

# Coroutines
-keepclassmembers class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**
