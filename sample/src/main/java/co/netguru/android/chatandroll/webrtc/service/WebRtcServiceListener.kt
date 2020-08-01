package co.netguru.android.chatandroll.webrtc.service

import org.webrtc.PeerConnection
import org.webrtc.SessionDescription


interface WebRtcServiceListener {

    /**
     * When receiving this exception service is in unrecoverable state and will call stopSelf, bound view(if any) should unbind
     */
    fun criticalWebRTCServiceException(throwable: Throwable)

    fun connectionStateChange(iceConnectionState: PeerConnection.IceConnectionState)

    fun handleRemoteOffer(sessionDescription: SessionDescription, remoteUuid: String?)
    fun showDialog()
}