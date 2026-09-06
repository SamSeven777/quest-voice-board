# Keep JNI native methods and classes
-keep class com.k2fsa.sherpa.onnx.** { *; }
-keepclassmembers class * {
    native <methods>;
}
