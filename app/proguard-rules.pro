# kotlinx-serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class com.fuku856.povomanager.** {
    *** Companion;
}
-keepclasseswithmembers class com.fuku856.povomanager.** {
    kotlinx.serialization.KSerializer serializer(...);
}
