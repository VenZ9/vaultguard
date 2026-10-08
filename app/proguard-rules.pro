# SQLCipher ProGuard rules
-keep class net.zetetic.** { *; }
-dontwarn net.zetetic.**

# Bouncy Castle ProGuard rules
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# Kotlinx Serialization
-keepattributes *Annotation*,InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
