# kotlinx-serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class com.fuku856.povomanager.** {
    *** Companion;
}
-keepclasseswithmembers class com.fuku856.povomanager.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# DataStore Preferences: protobuf-lite はシリアライズ時に生成クラスの
# フィールド(strings_ 等)をリフレクションで名前解決するため、R8 による
# 削除/リネームを防ぐ必要がある。
# 生成クラス(PreferencesProto$StringSet 等)は androidx.datastore.preferences
# パッケージ直下にあり、shade 化された GeneratedMessageLite を継承している。
# 旧ルールは .protobuf. サブパッケージ限定でこれらに一致せず、効いていなかった。
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}
