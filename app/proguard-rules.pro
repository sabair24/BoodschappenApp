-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes SourceFile,LineNumberTable

# App models used by Retrofit/Gson
-keep class com.boodschappen.app.data.remote.** { *; }

# ML Kit
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# Firebase + Google Play Services
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# gRPC (used internally by Firebase Firestore)
-keep class io.grpc.** { *; }
-dontwarn io.grpc.**
-keep class io.perfmark.** { *; }
-dontwarn io.perfmark.**

# Protobuf (required by gRPC)
-keep class com.google.protobuf.** { *; }
-dontwarn com.google.protobuf.**

# TLS providers for gRPC
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn sun.misc.Unsafe

# Kotlin coroutines
-keepclassmembernames class kotlinx.** { volatile <fields>; }
-dontwarn kotlinx.**

# OkHttp / Okio
-dontwarn okhttp3.**
-dontwarn okio.**

# Gson
-keep class com.google.gson.** { *; }
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Retrofit
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# Paho MQTT (legacy)
-keep class org.eclipse.paho.** { *; }
-dontwarn org.eclipse.paho.**
