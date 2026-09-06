# ============================================================================
# FlashRead ProGuard Rules
# ============================================================================
# Only rules that are genuinely needed beyond what R8/AGP handles automatically.
# Libraries like Firebase, AdMob, UMP, Coil ship their own consumer rules.

# ----------------------------------------------------------------------------
# COMPOSE MULTIPLATFORM RESOURCES
# ----------------------------------------------------------------------------
# Resource accessors are generated and looked up by name/reflection.
-keep class com.evgeniich.flashread.resources.** { *; }
-keep class org.jetbrains.compose.resources.** { *; }
-keepclassmembers class org.jetbrains.compose.resources.** { *; }

# ----------------------------------------------------------------------------
# NAVIGATION 3
# ----------------------------------------------------------------------------
# Navigation3 restores the back stack from NavKey implementations.
# Sealed interface and all its data objects must be preserved.
-keep class com.evgeniich.flashread.navigation.AppRoute { *; }
-keep class com.evgeniich.flashread.navigation.AppRoute$* { *; }
-keep interface androidx.navigation3.runtime.NavKey { *; }
-keep class * implements androidx.navigation3.runtime.NavKey { *; }

# ----------------------------------------------------------------------------
# KOTLIN SEALED CLASSES / INTERFACES
# ----------------------------------------------------------------------------
# Sealed hierarchies rely on class names for when() exhaustiveness.
-keep class com.evgeniich.flashread.AppMessage { *; }
-keep class com.evgeniich.flashread.AppMessage$* { *; }
-keep class com.evgeniich.flashread.core.locale.AppLanguage { *; }
-keep class com.evgeniich.flashread.core.locale.AppLanguage$* { *; }
-keep class com.evgeniich.flashread.analytics.AnalyticsEvent { *; }
-keep class com.evgeniich.flashread.analytics.AnalyticsEvent$* { *; }

# ----------------------------------------------------------------------------
# EPUB / FB2 XML PARSING
# ----------------------------------------------------------------------------
# KXmlParser is instantiated by class name.
-keep class org.kxml2.** { *; }
-dontwarn org.xmlpull.v1.**
-dontwarn org.kxml2.**

# ----------------------------------------------------------------------------
# KOTLIN COROUTINES
# ----------------------------------------------------------------------------
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ----------------------------------------------------------------------------
# KOTLIN METADATA
# ----------------------------------------------------------------------------
-keepattributes RuntimeVisibleAnnotations
-keep class kotlin.Metadata { *; }

# ----------------------------------------------------------------------------
# DEBUGGING: Stack traces
# ----------------------------------------------------------------------------
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keepattributes Signature

# ----------------------------------------------------------------------------
# SUPPRESS WARNINGS
# ----------------------------------------------------------------------------
# Optional dependencies that may not be present at runtime.
-dontwarn java.lang.invoke.StringConcatFactory
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
