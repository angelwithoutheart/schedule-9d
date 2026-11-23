# Keep line numbers for readable crash reports.
-keepattributes SourceFile,LineNumberTable

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class ru.school9d.schedule.**$$serializer { *; }
-keepclassmembers class ru.school9d.schedule.** {
    *** Companion;
}
-keepclasseswithmembers class ru.school9d.schedule.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Glance widget and its reflection-based action callbacks
-keep class ru.school9d.schedule.widget.** { *; }
-keep class * implements androidx.glance.appwidget.action.ActionCallback { *; }
-keep class androidx.glance.appwidget.** { *; }

# Serialized schedule models and OpenRouter payloads
-keep class ru.school9d.schedule.data.** { *; }
-keep class ru.school9d.schedule.ai.** { *; }
