package com.example.veiltalk.feature.call.data.webrtc

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.webrtc.*

class WebRtcClient(private val context: Context) {

    private val eglBase: EglBase = EglBase.create()
    val eglBaseContext: EglBase.Context get() = eglBase.eglBaseContext

    // تمام callback های PeerConnection.Observer روی signaling thread داخلی WebRTC اجرا می‌شوند.
    // هرگز نباید مستقیماً از داخل آن‌ها close()/dispose() صدا زده شود (باعث فراخوانی بازگشتی native و کرش SIGABRT می‌شود).
    // پس همه‌ی callback ها را به main thread پاس می‌دهیم.
    private val mainHandler = Handler(Looper.getMainLooper())

    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var videoCapturer: CameraVideoCapturer? = null
    private var localVideoSource: VideoSource? = null
    private var localAudioSource: AudioSource? = null

    var localVideoTrack: VideoTrack? = null
        private set
    var localAudioTrack: AudioTrack? = null
        private set

    var onRemoteTrack: ((MediaStreamTrack) -> Unit)? = null
    var onIceCandidate: ((IceCandidate) -> Unit)? = null
    var onConnectionStateChanged: ((PeerConnection.PeerConnectionState) -> Unit)? = null

    // ⚠️ برای اتصال بین دو شبکه موبایل مختلف (نه یک وای‌فای مشترک)، فقط STUN اغلب کافی نیست
    // چون هر دو طرف پشت NAT شبکه موبایل هستند و مسیر مستقیم P2P برقرار نمی‌شود.
    // یک سرور TURN واقعی (مثلاً coturn خودت روی همان سرور اوبونتو، یا سرویس‌هایی مثل Twilio/Metered) اضافه کن.
    // پایین یک TURN تستی رایگان گذاشته‌ام صرفاً برای عیب‌یابی — برای Production حتماً TURN خودت را جایگزین کن.
    private val iceServers = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("turn:49.13.120.183:3478?transport=udp")
            .setUsername("amir")
            .setPassword("Amir1234")
            .createIceServer(),
        PeerConnection.IceServer.builder("turn:49.13.120.183:3478?transport=tcp")
            .setUsername("amir")
            .setPassword("Amir1234")
            .createIceServer(),
        PeerConnection.IceServer.builder("turns:49.13.120.183.nip.io:5349?transport=tcp")
            .setUsername("amir")
            .setPassword("Amir1234")
            .createIceServer()
    )

    fun initFactory() {
        if (peerConnectionFactory != null) return

        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(false)
                .createInitializationOptions()
        )

        // با io.getstream:stream-webrtc-android دوباره از Default استفاده می‌کنیم؛
        // این فورک کدک نرم‌افزاری را کامل لینک کرده و برای باگ‌های سخت‌افزاری شناخته‌شده (مثل Exynos H.264) پچ دارد.
        val encoderFactory = DefaultVideoEncoderFactory(eglBaseContext, true, true)
        val decoderFactory = DefaultVideoDecoderFactory(eglBaseContext)

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .createPeerConnectionFactory()
    }

    fun createPeerConnection() {
        val factory = peerConnectionFactory ?: return
        val config = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            // اجبار به استفاده از relay در صورت نیاز کمک می‌کند سریع‌تر به مسیر معتبر برسیم؛
            // حالت پیش‌فرض ALL باشد کافی است، فقط این کامنت برای یادآوری تنظیمات پیشرفته‌تر است.
        }

        peerConnection = factory.createPeerConnection(config, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate) {
                mainHandler.post { onIceCandidate?.invoke(candidate) }
            }
            override fun onAddTrack(receiver: RtpReceiver, streams: Array<out MediaStream>) {
                mainHandler.post { receiver.track()?.let { onRemoteTrack?.invoke(it) } }
            }
            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState) {
                // نکته حیاتی: این خط دیگر مستقیم close() صدا نمی‌زند؛ فقط با تأخیر به main thread پاس داده می‌شود
                mainHandler.post { onConnectionStateChanged?.invoke(newState) }
            }
            override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState) {}
            override fun onIceConnectionReceivingChange(p0: Boolean) {}
            override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState) {}
            override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>) {}
            override fun onAddStream(p0: MediaStream?) {}
            override fun onRemoveStream(p0: MediaStream?) {}
            override fun onDataChannel(p0: DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onSignalingChange(p0: PeerConnection.SignalingState?) {}
            override fun onTrack(transceiver: RtpTransceiver?) {
                mainHandler.post {
                    transceiver?.receiver?.track()?.let { onRemoteTrack?.invoke(it) }
                }
            }
        })
    }

    fun attachLocalMedia(withVideo: Boolean) {
        val factory = peerConnectionFactory ?: return

        val audioConstraints = MediaConstraints()
        localAudioSource = factory.createAudioSource(audioConstraints)
        localAudioTrack = factory.createAudioTrack("audio_track", localAudioSource)
        peerConnection?.addTrack(localAudioTrack)

        if (withVideo) {
            videoCapturer = createCameraCapturer()
            val surfaceHelper = SurfaceTextureHelper.create("CaptureThread", eglBaseContext)
            localVideoSource = factory.createVideoSource(false)
            videoCapturer?.initialize(surfaceHelper, context, localVideoSource!!.capturerObserver)
            videoCapturer?.startCapture(640, 480, 30)
            localVideoTrack = factory.createVideoTrack("video_track", localVideoSource)
            peerConnection?.addTrack(localVideoTrack)
        }
    }

    private fun createCameraCapturer(): CameraVideoCapturer? {
        val enumerator = Camera2Enumerator(context)
        val deviceNames = enumerator.deviceNames

        for (name in deviceNames) {
            if (enumerator.isFrontFacing(name)) {
                enumerator.createCapturer(name, null)?.let { return it }
            }
        }
        for (name in deviceNames) {
            enumerator.createCapturer(name, null)?.let { return it }
        }
        return null
    }

    fun createOffer(onSuccess: (SessionDescription) -> Unit) {
        val constraints = MediaConstraints()
        peerConnection?.createOffer(object : SdpObserverAdapter() {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                peerConnection?.setLocalDescription(SdpObserverAdapter(), sdp)
                mainHandler.post { sdp?.let { onSuccess(it) } }
            }
        }, constraints)
    }

    fun restartIce(onSuccess: (SessionDescription) -> Unit) {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("IceRestart", "true"))
        }
        peerConnection?.createOffer(object : SdpObserverAdapter() {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                peerConnection?.setLocalDescription(SdpObserverAdapter(), sdp)
                mainHandler.post { sdp?.let { onSuccess(it) } }
            }
        }, constraints)
    }

    fun createAnswer(onSuccess: (SessionDescription) -> Unit) {
        val constraints = MediaConstraints()
        peerConnection?.createAnswer(object : SdpObserverAdapter() {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                peerConnection?.setLocalDescription(SdpObserverAdapter(), sdp)
                mainHandler.post { sdp?.let { onSuccess(it) } }
            }
        }, constraints)
    }

    fun setRemoteDescription(sdp: SessionDescription, onComplete: () -> Unit = {}) {
        peerConnection?.setRemoteDescription(object : SdpObserverAdapter() {
            override fun onSetSuccess() {
                mainHandler.post { onComplete() }
            }
            override fun onSetFailure(error: String?) {
                mainHandler.post { 
                    android.util.Log.e("WebRtcClient", "Failed to set remote description: $error")
                }
            }
        }, sdp)
    }

    fun addIceCandidate(candidate: IceCandidate) {
        peerConnection?.addIceCandidate(candidate)
    }

    fun hasRemoteDescription(): Boolean = peerConnection?.remoteDescription != null

    fun setAudioEnabled(enabled: Boolean) {
        localAudioTrack?.setEnabled(enabled)
    }

    fun setVideoEnabled(enabled: Boolean) {
        localVideoTrack?.setEnabled(enabled)
    }

    fun flipCamera() {
        videoCapturer?.switchCamera(null)
    }

    // این متد اکنون همیشه باید از main thread صدا زده شود (CallRepository.cleanup این تضمین را رعایت می‌کند)
    fun close() {
        videoCapturer?.stopCapture()
        videoCapturer?.dispose()
        videoCapturer = null

        localVideoTrack?.dispose()
        localAudioTrack?.dispose()
        localVideoSource?.dispose()
        localAudioSource?.dispose()
        localVideoTrack = null
        localAudioTrack = null
        localVideoSource = null
        localAudioSource = null

        peerConnection?.close()
        peerConnection?.dispose()
        peerConnection = null
    }

    fun release() {
        close()
        peerConnectionFactory?.dispose()
        peerConnectionFactory = null
        eglBase.release()
    }
}

private open class SdpObserverAdapter : SdpObserver {
    override fun onCreateSuccess(sdp: SessionDescription?) {}
    override fun onSetSuccess() {}
    override fun onCreateFailure(error: String?) {}
    override fun onSetFailure(error: String?) {}
}