# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in ${sdk.dir}/tools/proguard/proguard-android.txt

# Keep data classes
-keep class com.moviepilot.app.data.model.** { *; }

# Keep Retrofit models
-keepattributes Signature
-keepattributes *Annotation*
