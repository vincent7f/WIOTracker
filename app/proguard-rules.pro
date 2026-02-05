# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep Room classes
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep class * extends androidx.room.RoomDatabase$Callback

# Keep WorkManager classes
-keep class androidx.work.** { *; }
-dontwarn androidx.work.**

# Keep WorkManager notification resources
-keep class androidx.work.impl.foreground.** { *; }
-dontwarn androidx.work.impl.foreground.**

# Keep notification resources (needed for WorkManager)
-keep class android.support.v4.app.NotificationCompat { *; }
-keep class androidx.core.app.NotificationCompat { *; }
-dontwarn android.support.v4.app.NotificationCompat
-dontwarn androidx.core.app.NotificationCompat

# Keep Compose classes (but allow optimization)
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.foundation.** { *; }
-keep class androidx.compose.material3.** { *; }
-keep class kotlin.coroutines.** { *; }
-keep class kotlinx.coroutines.** { *; }
-dontwarn androidx.compose.**

# Keep ViewModel classes
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class * extends androidx.lifecycle.ViewModelFactory { *; }

# Keep data classes used in Room
-keep class com.wiotracker.data.database.entity.** { *; }
-keep class com.wiotracker.domain.model.** { *; }

# Keep WorkManager Worker classes
-keep class com.wiotracker.service.WifiScanWorker { *; }

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep Parcelable implementations
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}
