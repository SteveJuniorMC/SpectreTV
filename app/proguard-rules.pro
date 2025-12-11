# Add project specific ProGuard rules here.

# Keep Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Keep Gson
-keep class com.spectretv.app.data.remote.dto.** { *; }
-keep class com.spectretv.app.domain.model.** { *; }

# Keep Room entities
-keep class com.spectretv.app.data.local.entity.** { *; }

# ExoPlayer
-keep class androidx.media3.** { *; }
