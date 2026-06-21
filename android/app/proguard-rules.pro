# TeleFlow ProGuard Rules

# Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.teleflow.**$$serializer { *; }
-keepclassmembers class com.teleflow.** {
    *** Companion;
}
-keepclasseswithmembers class com.teleflow.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# VPN
-keep class com.teleflow.vpn.** { *; }
