package co.netguru.android.chatandroll.feature.call

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.Toast
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import co.netguru.android.chatandroll.R
import co.netguru.android.chatandroll.app.App
import co.netguru.android.chatandroll.common.extension.areAllPermissionsGranted
import co.netguru.android.chatandroll.common.extension.startAppSettings
import co.netguru.android.chatandroll.common.util.CallEvent
import co.netguru.android.chatandroll.common.util.createHangUpCallDialog
import co.netguru.android.chatandroll.feature.base.BaseMvpFragment
import co.netguru.android.chatandroll.feature.call.VideoCallFragment.Companion.newInstance
import co.netguru.android.chatandroll.webrtc.service.WebRtcService
import co.netguru.android.chatandroll.webrtc.service.WebRtcServiceListener
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_hang_up.view.*
import kotlinx.android.synthetic.main.fragment_video.*
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription
import timber.log.Timber


class VideoCallFragment : BaseMvpFragment<VideoFragmentView, VideoCallFragmentPresenter>(), VideoFragmentView, WebRtcServiceListener {

    companion object {
        val TAG: String = VideoCallFragment::class.java.name

        fun newInstance(isOffer: Boolean, userId: String, firstname: String?= "", surname: String? = "", profileImageUrl: String? = null): VideoCallFragment {
            val instance = VideoCallFragment()
            val bundle = Bundle().apply {
                putString("userId", userId)
                putString("firstname", firstname)
                putString("surname", surname)
                putString("profileImageUrl", profileImageUrl)
                putBoolean("isOffer", isOffer)
            }
            instance.arguments = bundle
            return instance
        }

        private const val KEY_IN_CHAT = "key:in_chat"
        private const val CHECK_PERMISSIONS_AND_CONNECT_REQUEST_CODE = 1
        private val NECESSARY_PERMISSIONS = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        private const val CONNECT_BUTTON_ANIMATION_DURATION_MS = 500L
    }

    private var mRemoteUuid: String? = null
    private var mSessionDescription: SessionDescription? = null
    private lateinit var serviceConnection: ServiceConnection

    override fun getLayoutId() = R.layout.fragment_video

    override fun retrievePresenter() = App.getApplicationComponent(requireActivity()).videoCallFragmentComponent().videoCallFragmentPresenter()

    var service: WebRtcService? = null

    var callDialog: Dialog? = null

    var isOffer: Boolean = false
    var userId: String? = null
    var firstname: String?= ""
    var surname: String? = ""
    var profileImageUrl: String? = null

    override val remoteUuid
        get() = service?.getRemoteUuid()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (buttonPanel.layoutParams as CoordinatorLayout.LayoutParams).behavior = MoveUpCallBehavior()
        (localVideoView.layoutParams as CoordinatorLayout.LayoutParams).behavior = MoveUpCallBehavior()
        activity?.volumeControlStream = AudioManager.STREAM_VOICE_CALL

        arguments?.let {
            userId = it.getString("userId")
            firstname = it.getString("firstname")
            surname = it.getString("surname")
            profileImageUrl = it.getString("profileImageUrl")
            isOffer = it.getBoolean("isOffer")
        }

        if (isOffer){
            connectTo(userId!!)
            showCallDialog()
//            showDialog()
            checkPermissionsAndConnect()
        } else {

            //TODO listen for connection
        }

        if (savedInstanceState?.getBoolean(KEY_IN_CHAT) == true) {
            initAlreadyRunningConnection()
        }
        connectButton.setOnClickListener {
            checkPermissionsAndConnect()
        }

        disconnectButton.setOnClickListener {
            getPresenter().disconnectByUser()
        }

        switchCameraButton.setOnClickListener {
            service?.switchCamera()
        }

        cameraEnabledToggle.setOnCheckedChangeListener { _, enabled ->
            service?.enableCamera(enabled)
        }

        microphoneEnabledToggle.setOnCheckedChangeListener { _, enabled ->
            service?.enableMicrophone(enabled)
        }
    }

    override fun onStart() {
        super.onStart()
        service?.hideBackgroundWorkWarning()
    }

    override fun onStop() {
        super.onStop()
        if (!requireActivity().isChangingConfigurations) {
            service?.showBackgroundWorkWarning()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        service?.let {
            it.detachViews()
            unbindService()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (remoteVideoView.visibility == View.VISIBLE) {
            outState.putBoolean(KEY_IN_CHAT, true)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!requireActivity().isChangingConfigurations) disconnect()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) = when (requestCode) {
        CHECK_PERMISSIONS_AND_CONNECT_REQUEST_CODE -> {
            val grantResult = grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (grantResult) {
                checkPermissionsAndConnect()
            } else {
                showNoPermissionsSnackbar()
            }
        }
        else -> {
            error("Unknown permission request code $requestCode")
        }
    }

    override fun attachService() {
        serviceConnection = object : ServiceConnection {
            override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {
                onWebRtcServiceConnected((iBinder as (WebRtcService.LocalBinder)).service)
                getPresenter().startRoulette(userId)
            }

            override fun onServiceDisconnected(componentName: ComponentName) {
                onWebRtcServiceDisconnected()
            }
        }
        startAndBindWebRTCService(serviceConnection)
    }

    @SuppressLint("Range")
    override fun criticalWebRTCServiceException(throwable: Throwable) {
        unbindService()
        showSnackbarMessage(R.string.error_web_rtc_error, Snackbar.LENGTH_LONG)
        Timber.e(throwable, "Critical WebRTC service error")
    }

    override fun connectionStateChange(iceConnectionState: PeerConnection.IceConnectionState) {
        getPresenter().connectionStateChange(iceConnectionState)
    }

    override fun handleRemoteOffer(sessionDescription: SessionDescription, remoteUuid: String?) {
        Timber.i("handleRemoteOffer: $remoteUuid")
        mSessionDescription = sessionDescription
        mRemoteUuid = remoteUuid
        showDialog()
//        service?.handleRemoteOffer(remoteUuid, sessionDescription)
    }

    override fun connectTo(uuid: String) {
        service?.offerDevice(uuid)
    }

    override fun disconnect() {
        service?.let {
            it.stopSelf()
            unbindService()
        }
    }

    private fun unbindService() {
        service?.let {
            it.detachServiceActionsListener()
            requireContext().unbindService(serviceConnection)
            service = null
        }
    }

    override fun showCamViews() {
        buttonPanel.visibility = View.VISIBLE
        remoteVideoView.visibility = View.VISIBLE
        localVideoView.visibility = View.VISIBLE
        connectButton.visibility = View.GONE
    }

    override fun showStartRouletteView() {
        buttonPanel.visibility = View.GONE
        remoteVideoView.visibility = View.GONE
        localVideoView.visibility = View.GONE
        connectButton.visibility = View.VISIBLE
    }

    @SuppressLint("Range")
    override fun showErrorWhileChoosingRandom() {
        showSnackbarMessage(R.string.error_choosing_random_partner, Snackbar.LENGTH_LONG)
    }

    @SuppressLint("Range")
    override fun showNoOneAvailable() {
        showSnackbarMessage(R.string.msg_no_one_available, Snackbar.LENGTH_LONG)
    }

    @SuppressLint("Range")
    override fun showLookingForPartnerMessage() {
        showSnackbarMessage(R.string.msg_looking_for_partner, Snackbar.LENGTH_SHORT)
    }

    override fun hideConnectButtonWithAnimation() {
        connectButton.animate().scaleX(0f).scaleY(0f)
                .setInterpolator(OvershootInterpolator())
                .setDuration(CONNECT_BUTTON_ANIMATION_DURATION_MS)
                .withStartAction { connectButton.isClickable = false }
                .withEndAction {
                    connectButton.isClickable = true
                    connectButton.visibility = View.GONE
                    connectButton.scaleX = 1f
                    connectButton.scaleY = 1f
                }
                .start()
    }

    @SuppressLint("Range")
    override fun showOtherPartyFinished() {
        showSnackbarMessage(R.string.msg_other_party_finished, Snackbar.LENGTH_SHORT)
    }

    @SuppressLint("Range")
    override fun showConnectedMsg() {
        showSnackbarMessage(R.string.msg_connected_to_other_party, Snackbar.LENGTH_LONG)
    }

    @SuppressLint("Range")
    override fun showWillTryToRestartMsg() {
        showSnackbarMessage(R.string.msg_will_try_to_restart_msg, Snackbar.LENGTH_LONG)
    }

    private fun initAlreadyRunningConnection() {
        showCamViews()
        serviceConnection = object : ServiceConnection {
            override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {
                onWebRtcServiceConnected((iBinder as (WebRtcService.LocalBinder)).service)
                getPresenter().listenForDisconnectOrders()
            }

            override fun onServiceDisconnected(componentName: ComponentName) {
                onWebRtcServiceDisconnected()
            }
        }
        startAndBindWebRTCService(serviceConnection)
    }

    private fun startAndBindWebRTCService(serviceConnection: ServiceConnection) {
        WebRtcService.startService(requireContext())
        WebRtcService.bindService(requireContext(), serviceConnection)
    }

    private fun checkPermissionsAndConnect() {
        if (requireContext().areAllPermissionsGranted(*NECESSARY_PERMISSIONS)) {
            getPresenter().connect()
        } else {
            requestPermissions(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO), CHECK_PERMISSIONS_AND_CONNECT_REQUEST_CODE)
        }
    }

    @SuppressLint("Range")
    private fun showNoPermissionsSnackbar() {
        view?.let {
            Snackbar.make(it, R.string.msg_permissions, Snackbar.LENGTH_LONG)
                    .setAction(R.string.action_settings) {
                        try {
                            requireContext().startAppSettings()
                        } catch (e: ActivityNotFoundException) {
                            showSnackbarMessage(R.string.error_permissions_couldnt_start_settings, Snackbar.LENGTH_LONG)
                        }
                    }
                    .show()
        }
    }

    private fun onWebRtcServiceConnected(service: WebRtcService) {
        Timber.d("Service connected")
        this.service = service
        service.attachLocalView(localVideoView)
        service.attachRemoteView(remoteVideoView)
        syncButtonsState(service)
        service.attachServiceActionsListener(webRtcServiceListener = this)
    }

    private fun syncButtonsState(service: WebRtcService) {
        cameraEnabledToggle.isChecked = service.isCameraEnabled()
        microphoneEnabledToggle.isChecked = service.isMicrophoneEnabled()
    }

    private fun onWebRtcServiceDisconnected() {
        Timber.d("Service disconnected")
    }

    override fun showDialog() {
        Timber.d("Service disconnected")
    }

    fun showHangUpDialog() {

        //TODO CREATE CUSTOM DIALOG (HANG UP CALL)

        callDialog = createHangUpCallDialog(requireActivity()){newValue ->
            when (newValue) {
                CallEvent.HangUpClickedEvent -> {
                    callDialog?.dismiss()
                    service?.handleRemoteOffer(mRemoteUuid, mSessionDescription!!)
                }
                CallEvent.HangDownClickedEvent -> {
                    callDialog?.dismiss()
                    getPresenter().disconnectByUser()
                }
            }
        }
        callDialog?.show()
    }

    fun showCallDialog() {

        //TODO CREATE CUSTOM DIALOG (HANG UP CALL)

        callDialog = createHangUpCallDialog(requireActivity()){newValue ->
            when (newValue) {
                CallEvent.HangUpClickedEvent -> {
                    callDialog?.dismiss()
                    service?.handleRemoteOffer(mRemoteUuid, mSessionDescription!!)
                }
                CallEvent.HangDownClickedEvent -> {
                    callDialog?.dismiss()
                    getPresenter().disconnectByUser()
                }
            }
        }
        callDialog?.show()
    }
}