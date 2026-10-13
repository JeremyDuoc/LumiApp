# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# --- Lumi: Preservación de DTOs y modelos de datos para Gson, Room y TFLite ---
-keepattributes Signature
-keepattributes *Annotation*

# DTOs de respaldo (Gson) y Room Entities
-keep class com.jeremy.lumi.data.backup.** { *; }
-keep class com.jeremy.lumi.data.local.entity.** { *; }
-keep class com.jeremy.lumi.domain.model.** { *; }

# TensorFlow Lite / LiteRT (16 KB compatible)
-keep class org.tensorflow.lite.** { *; }
-keepclassmembers class org.tensorflow.lite.** { *; }
-keep class com.google.ai.edge.litert.** { *; }
-keepclassmembers class com.google.ai.edge.litert.** { *; }