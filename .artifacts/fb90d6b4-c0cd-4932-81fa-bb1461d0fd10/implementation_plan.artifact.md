# WebRTC Layer Improvements Plan

This plan addresses several issues in the WebRTC implementation of VeilTalk, focusing on permissions, reliability, background stability, and audio management.

## User Review Required

> [!IMPORTANT]
> The TURN server configuration requires a real domain and credentials. For this plan, placeholders like `yourdomain.com` will be used. You MUST update these with your actual server details in `WebRtcClient.kt`.

## Proposed Changes

### 1. Permissions Management
Fix the order of permission requests to ensure they are granted BEFORE WebRTC attempts to access local media.

#### [MODIFY] [ChatScreen.kt](file:///C:/Users/amir/AndroidStudioProjects/VeilTalk/app/src/main/java/com/example/veiltalk/feature/chat/ui/ChatScreen.kt)
- Add a `rememberLauncherForActivityResult` for permissions.
- Wrap `startCall` calls with a check for required permissions.

#### [MODIFY] [CallOverlay.kt](file:///C:/Users/amir/AndroidStudioProjects/VeilTalk/app/src/main/java/com/example/veiltalk/feature/call/ui/CallOverlay.kt)
- Add a permission launcher for incoming calls.
- Check permissions before calling `acceptCall()`.
- Remove the delayed permission request in `LaunchedEffect`.

---

### 2. Reliable TURN Server
Switch from the unreliable public TURN server to a self-hosted Coturn instance.

#### [MODIFY] [WebRtcClient.kt](file:///C:/Users/amir/AndroidStudioProjects/VeilTalk/app/src/main/java/com/example/veiltalk/feature/call/data/webrtc/WebRtcClient.kt)
- Update `iceServers` list with the new TURN server details.

---

### 3. Foreground Service for Calls (Android 14+)
Ensure the app remains stable when a call is in progress and the app moves to the background.

#### [NEW] [CallForegroundService.kt](file:///C:/Users/amir/AndroidStudioProjects/VeilTalk/app/src/main/java/com/example/veiltalk/feature/call/service/CallForegroundService.kt)
- Implement a foreground service that handles `microphone` and `camera` types.

#### [MODIFY] [AndroidManifest.xml](file:///C:/Users/amir/AndroidStudioProjects/VeilTalk/app/src/main/AndroidManifest.xml)
- Register `CallForegroundService` with `microphone|camera` foreground service types.

#### [MODIFY] [CallRepository.kt](file:///C:/Users/amir/AndroidStudioProjects/VeilTalk/app/src/main/java/com/example/veiltalk/feature/call/data/CallRepository.kt)
- Start `CallForegroundService` when a call is initiated or accepted.
- Stop it in `cleanup()`.

---

### 4. Audio Management
Properly configure `AudioManager` for VOIP calls.

#### [MODIFY] [CallRepository.kt](file:///C:/Users/amir/AndroidStudioProjects/VeilTalk/app/src/main/java/com/example/veiltalk/feature/call/data/CallRepository.kt)
- Set `AudioManager.MODE_IN_COMMUNICATION` and request audio focus in `setupPeerConnection`.
- Reset mode and release focus in `cleanup`.

---

### 5. ICE Restart Logic
Handle network switches gracefully.

#### [MODIFY] [WebRtcClient.kt](file:///C:/Users/amir/AndroidStudioProjects/VeilTalk/app/src/main/java/com/example/veiltalk/feature/call/data/webrtc/WebRtcClient.kt)
- Add a method to trigger ICE restart.

#### [MODIFY] [CallRepository.kt](file:///C:/Users/amir/AndroidStudioProjects/VeilTalk/app/src/main/java/com/example/veiltalk/feature/call/data/CallRepository.kt)
- Implement logic to trigger ICE restart when `PeerConnectionState` is `DISCONNECTED`.

## Verification Plan

### Automated Tests
- N/A (Manual verification is more suitable for WebRTC and permissions).

### Manual Verification
1.  **Permissions**: Deny permissions, then try to start a call. Verify it asks for permissions first.
2.  **Audio**: Verify audio works and switches to the correct mode (should be controllable by volume keys for calls).
3.  **Background**: Start a call, move to home screen. Verify notification appears and call continues (on Android 14+).
4.  **Network Switch**: (If possible) Switch from Wi-Fi to Data during a call and verify it recovers.
