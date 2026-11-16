# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Preserve Firestore data models used with toObject() / @DocumentId / @ServerTimestamp
-keepclassmembers class com.example.data.remote.** { *; }
-keepclassmembers class com.example.model.** { *; }

# Preserve App Check reCAPTCHA site key string resource looked up dynamically
-keepclassmembers class com.example.R$string {
    public static int recaptcha_site_key;
}
