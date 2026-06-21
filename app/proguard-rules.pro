# R8 / ProGuard rules for the Vector release build.
#
# Most of our deps ship their own consumer rules (AndroidX, Compose, Room,
# Firebase) so this file mostly handles the gaps R8 can't infer.

# ----------------------------------------------------------------------------
# Kotlin
# ----------------------------------------------------------------------------
# Keep coroutines internals — coroutines uses some volatile field ASM tricks
# that R8 can occasionally over-shrink.
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }
-dontwarn kotlinx.coroutines.**

# Keep enum names — Room uses our enum names as TEXT in the schema.
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ----------------------------------------------------------------------------
# Room — we register custom @TypeConverters that reference enum classes by name.
# Keep our entities, DAOs, and converters so R8 doesn't rename them out from
# under Room's generated code.
# ----------------------------------------------------------------------------
-keep class com.example.fitness_tracker.data.** { *; }

# ----------------------------------------------------------------------------
# Firebase AI — uses reflection for backend resolution and serialization of
# request/response types. Keep the Firebase namespace fully intact.
# ----------------------------------------------------------------------------
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# ----------------------------------------------------------------------------
# Compose / lifecycle — both ship consumer rules already, but be explicit
# about ViewModels (we instantiate them via reflection through `viewModel()`).
# ----------------------------------------------------------------------------
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keep class * extends androidx.lifecycle.AndroidViewModel {
    <init>(...);
}

# ----------------------------------------------------------------------------
# Don't strip line numbers from crash stacks — saves a few KB but makes
# debugging shipped APKs miserable. Keep them.
# ----------------------------------------------------------------------------
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
