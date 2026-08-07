# Migrate to AGP 9.0 Built-in Kotlin Support

The project is currently experiencing a sync error because it's using Android Gradle Plugin (AGP) 9.3.1 but still applying the legacy `kotlin-android` plugin. In AGP 9.0 and later, Kotlin support is built-in and the legacy `kotlin-android` plugin is incompatible with the new DSL.

This plan outlines the steps to migrate the project to the built-in Kotlin support and the new AGP DSL.

## Proposed Changes

### Build Configuration

#### [MODIFY] [gradle.properties](file:///C:/Users/amir/AndroidStudioProjects/VeilTalk/gradle.properties)
- Enable `android.builtInKotlin`.
- Enable `android.newDsl`.
- Disable `android.debug.obsoleteApi` to reduce noise from other possible deprecations.

#### [MODIFY] [libs.versions.toml](file:///C:/Users/amir/AndroidStudioProjects/VeilTalk/gradle/libs.versions.toml)
- Remove the `kotlin-android` plugin definition as it's no longer required.

#### [MODIFY] [root build.gradle.kts](file:///C:/Users/amir/AndroidStudioProjects/VeilTalk/build.gradle.kts)
- Remove the `kotlin-android` plugin declaration.

#### [MODIFY] [app build.gradle.kts](file:///C:/Users/amir/AndroidStudioProjects/VeilTalk/app/build.gradle.kts)
- Remove the `kotlin-android` plugin application.
- Migrate `kotlinOptions` to the new `kotlin { compilerOptions { ... } }` DSL.
- Ensure compatibility with `kotlin-compose` and `kotlin-serialization` plugins.

## Verification Plan

### Automated Tests
- Run Gradle Sync to ensure the `unitTestVariants` error is resolved.
- Run a build to verify Kotlin compilation still works.
  - `gradlew :app:assembleDebug`

### Manual Verification
- Verify that the IDE no longer reports sync errors related to the variant API.
