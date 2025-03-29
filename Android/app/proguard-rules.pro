# Keep all classes and members in your app's package
-keep class com.example.barkhordha.** { *; }
-dontwarn com.example.barkhordha.**

# Keep AndroidX libraries used
-keep class androidx.appcompat.** { *; }
-keep class androidx.activity.** { *; }
-keep class androidx.core.content.FileProvider { *; }
-dontwarn androidx.**

# Preserve annotations
-keepattributes *Annotation*
-keepattributes Signature

# Optimize aggressively
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification

-keep class org.jetbrains.annotations.** { *; }
-keep class kotlinx.coroutines.** { *; }

# Suppress warnings for third-party libraries
-dontwarn java.lang.invoke.**
-dontwarn org.apache.**

# Keep Bitmap, Canvas, and MediaStore classes
-keep class android.graphics.Bitmap { *; }
-keep class android.graphics.Canvas { *; }
-keep class android.provider.MediaStore { *; }

# Suppress Kotlin warnings (in case any remain)
-dontwarn kotlin.**