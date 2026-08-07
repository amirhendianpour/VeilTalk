# Migrate to AGP 9.0 Built-in Kotlin and Fix Obsolete API Warning

This plan aims to resolve the `unitTestVariants` obsolete API warning and the `ClassCastException` encountered during Gradle sync. The root cause is the incompatibility of the legacy `kotlin-android` plugin with the new DSL in Android Gradle Plugin (AGP) 9.0+. AGP 9.0 introduces built-in Kotlin support, making the external Kotlin plugin redundant and problematic for Android modules.

## User Review Required

> [!IMPORTANT]
> **Built-in Kotlin Migration:** This plan removes the `org.jetbrains.kotlin.android` plugin in favor of AGP's built-in Kotlin support. This is the recommended path for AGP 9.0+ projects.
> **Experimental Flags:** I will also remove several experimental and deprecated flags from `gradle.properties` that are no longer needed or have new defaults in AGP 9.0.

## Proposed Changes

### Build Configuration

#### [MODIFY] [libs.versions.toml](file:///C:/Users/amir/AndroidStudioProjects/VeilTalk/gradle/libs.versions.toml)
- Remove `kotlin-android` plugin definition.
- Remove `kotlin-compose` and `kotlin-serialization` plugins if they are redundant with built-in support (will verify during implementation).

#### [MODIFY] [gradle.properties](file:///C:/Users/amir/AndroidStudioProjects/VeilTalk/gradle.properties)
- Remove `android.newDsl=false`.
- Remove `android.builtInKotlin=false` (or set to `true`).
- Remove obsolete/deprecated flags: `android.disallowKotlinSourceSets`, `android.usesSdkInManifest.disallowed`, `android.uniquePackageNames`, `android.dependency.useConstraints`, `android.r8.strictFullModeForKeepRules`, `android.r8.optimizedResourceShrinking`, `android.builtInKotlin`.
- Remove `android.debug.obsoleteApi=true` once verified.

#### [MODIFY] [build.gradle.kts](file:///C:/Users/amir/AndroidStudioProjects/VeilTalk/build.gradle.kts)
- Remove `alias(libs.plugins.kotlin.android) apply false`.
- Remove other Kotlin plugins applied at the top level if they are no longer needed.

#### [MODIFY] [app/build.gradle.kts](file:///C:/Users/amir/AndroidStudioProjects/VeilTalk/app/build.gradle.kts)
- Remove `alias(libs.plugins.kotlin.android)`.
- Remove `kotlinOptions` block (redundant as it defaults to `targetCompatibility`).
- Fix any remaining DSL warnings (e.g., `isMinifyEnabled` suggests setting `isShrinkResources`).

## Verification Plan

### Automated Tests
- Run `./gradlew help` to verify configuration success without warnings.
- Run `./gradlew assembleDebug` to ensure compilation works with built-in Kotlin.

### Manual Verification
- Perform a Gradle Sync in Android Studio to ensure no more `ClassCastException` or `unitTestVariants` warnings appear.
