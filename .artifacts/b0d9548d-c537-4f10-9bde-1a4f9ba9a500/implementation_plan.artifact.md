# Fix KSP and Hilt Classloader Conflict

The build is failing because the Hilt Gradle plugin cannot find KSP task classes. This is a known issue (Dagger #3965) that occurs when these plugins are not in the same classpath/scope or are applied in an order that causes classloader isolation issues.

## Proposed Changes

### Build Configuration

#### [MODIFY] [libs.versions.toml](file:///C:/Users/amir/AndroidStudioProjects/VeilTalk/gradle/libs.versions.toml)
- Update `ksp` version to `2.3.11` (or a version compatible with Kotlin 2.2.10 if confirmed). Given the bleeding edge AGP/Kotlin versions, using the latest stable KSP is a good first step.

#### [MODIFY] [build.gradle.kts](file:///C:/Users/amir/AndroidStudioProjects/VeilTalk/build.gradle.kts) (Root)
- Ensure KSP plugin is declared before Hilt plugin in the `plugins` block.

#### [MODIFY] [app/build.gradle.kts](file:///C:/Users/amir/AndroidStudioProjects/VeilTalk/app/build.gradle.kts)
- Reorder the `plugins` block to apply KSP before Hilt.

## Verification Plan

### Automated Tests
- Run `gradle sync` to verify that the configuration error is resolved.
- Run `app:assembleDebug` to ensure KSP and Hilt can work together during the build.
