# ProGuard rules for MADT_Lab6_WebAPI_Map
# Retrofit & Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.madt.lab6.weathermap.data.model.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
