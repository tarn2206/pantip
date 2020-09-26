# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in D:/App/android-sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

#-optimizationpasses 3
#-dontusemixedcaseclassnames
#-dontskipnonpubliclibraryclasses
-dontpreverify
-verbose

-keep class android.** { *; }
-keep interface android.** { *; }
-keep class com.** { *; }
-keep interface com.** { *; }
-keep class io.** { *; }
-keep interface io.** { *; }
-keep class org.** { *; }
-keep interface org.** { *; }
-keep class tarn.pantip.** { *; }
-keep interface tarn.pantip.** { *; }
-keep class **.R$* { public static <fields>; }
-keepattributes InnerClasses,Signature,*Annotation*,SourceFile,LineNumberTable
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}

-dontwarn androidx.media.**
-dontwarn com.google.android.gms.**
-dontnote com.google.android.gms.**
-dontnote org.apache.commons.lang3.ObjectUtils

# GooglePlayService
# http://developer.android.com/google/play-services/setup.html#Proguard
-keep class * extends java.util.ListResourceBundle {
    protected Object[][] getContents();
}

-keep public class com.google.android.gms.common.internal.safeparcel.SafeParcelable {
    public static final *** NULL;
}

-keepnames @com.google.android.gms.common.annotation.KeepName class *
-keepclassmembernames class * {
    @com.google.android.gms.common.annotation.KeepName *;
}

-keepnames class * implements android.os.Parcelable {
    public static final ** CREATOR;
}
# GooglePlayService