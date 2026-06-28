# Add project specific ProGuard rules here.
# MPAndroidChart
-keep class com.github.mikephil.charting.** { *; }
# Room
-keep class * extends androidx.room.RoomDatabase { *; }
-dontwarn androidx.room.**
# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
