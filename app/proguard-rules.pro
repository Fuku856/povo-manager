# kotlinx-serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class com.fuku856.povomanager.** {
    *** Companion;
}
-keepclasseswithmembers class com.fuku856.povomanager.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# DataStore Preferences: protobuf 内部フィールド strings_ が R8 に削除されるのを防ぐ
-keep class androidx.datastore.preferences.protobuf.** { *; }
