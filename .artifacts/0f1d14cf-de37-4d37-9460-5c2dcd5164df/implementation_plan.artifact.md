# Fix Deprecated `AudioManager.isSpeakerphoneOn` Warnings

The `isSpeakerphoneOn` property in `AudioManager` is deprecated starting from Android 12 (API 31). This plan introduces a version-aware helper to manage speakerphone switching using the modern `CommunicationDevice` APIs on newer Android versions while maintaining compatibility with older versions.

## Proposed Changes

### [Call Component]

#### [MODIFY] [CallRepository.kt](file:///C:/Users/amir/AndroidStudioProjects/VeilTalk/app/src/main/java/com/example/veiltalk/feature/call/data/CallRepository.kt)

- Add import for `android.media.AudioDeviceInfo`.
- Implement a private helper method `setSpeakerphone(on: Boolean)` that:
    - On Android 12+ (API 31+): Uses `AudioManager.setCommunicationDevice()` and `AudioManager.clearCommunicationDevice()`.
    - On older versions: Falls back to the now-deprecated `isSpeakerphoneOn`.
- Replace direct calls to `audioManager.isSpeakerphoneOn` with the new helper method.

## Verification Plan

### Automated Tests
- Run `:app:compileDebugKotlin` to ensure deprecation warnings are either gone or correctly suppressed in the compatibility layer.
- Unit tests for `CallRepository` (if applicable) to ensure state changes are still triggered.

### Manual Verification
- Test audio and video calls on an Android 12+ device/emulator:
    - Start a video call: verify speaker is ON by default.
    - Start an audio call: verify speaker is OFF by default.
    - Toggle speaker manually during a call: verify it works.
- Repeat on an older device (API < 31) to ensure backward compatibility.
