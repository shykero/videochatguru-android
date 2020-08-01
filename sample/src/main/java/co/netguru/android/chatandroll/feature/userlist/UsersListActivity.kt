package co.netguru.android.chatandroll.feature.userlist

import android.app.Activity
import android.app.Dialog
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.widget.AdapterView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import co.netguru.android.chatandroll.R
import co.netguru.android.chatandroll.common.util.CallEvent
import co.netguru.android.chatandroll.common.util.RxUtils
import co.netguru.android.chatandroll.common.util.createHangUpCallDialog
import co.netguru.android.chatandroll.data.firebase.ContactData
import co.netguru.android.chatandroll.data.firebase.FirebaseData
import co.netguru.android.chatandroll.data.firebase.FirebaseData.myID
import co.netguru.android.chatandroll.data.firebase.FirebaseSignalingDisconnect
import co.netguru.android.chatandroll.data.firebase.FirebaseSignalingOnline
import co.netguru.android.chatandroll.data.room.Contact
import co.netguru.android.chatandroll.feature.call.CallActivity
import co.netguru.android.chatandroll.feature.editcontact.EditContactActivity
import co.netguru.android.chatandroll.feature.error.ErrorActivity
import co.netguru.android.chatandroll.feature.hangup.HangUpActivity
import co.netguru.android.chatandroll.webrtc.service.WebRtcService
import co.netguru.android.chatandroll.webrtc.service.WebRtcServiceListener
import com.google.firebase.database.*
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.disposables.Disposables
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_users_list.*
import kotlinx.android.synthetic.main.fragment_video.*
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription
import timber.log.Timber


class UsersListActivity : AppCompatActivity(), WebRtcServiceListener {

    private lateinit var adapter: ContactsListAdapter

    private lateinit var callRef: DatabaseReference

    private lateinit var contactsViewModel: ContactsListViewModel

    private lateinit var serviceConnection: ServiceConnection

    var service: WebRtcService? = null

    lateinit var firebaseDatabase: FirebaseDatabase

    lateinit var firebaseSignalingOnline: FirebaseSignalingOnline
    lateinit var firebaseSignalingDisconnect: FirebaseSignalingDisconnect

    private val disposables = CompositeDisposable()
    private var disconnectOrdersSubscription: Disposable = Disposables.disposed()

    private var mRemoteUuid: String? = null
    private var mSessionDescription: SessionDescription? = null

    private val newWordActivityRequestCode = 1

    var callDialog: Dialog? = null

    var isOffer: Boolean = false
    var userId: String? = null
    var firstname: String?= ""
    var surname: String? = ""
    var profileImageUrl: String? = null

    val remoteUuid = service?.getRemoteUuid()

    val contactsListAdapterListenter = object : ContactsListAdapter.IListener {
        override fun onEditIconClicked(position: Int, userId: String) {
            val intent = Intent(this@UsersListActivity, EditContactActivity::class.java)
            intent.putExtra("userId", userId)
            startActivity(intent)
        }

        override fun onContactItemClicked(position: Int, userId: String, contactData: Contact) {
//                startVideoCall("4SGxUDUntFTCJAMH7XfF3vhsCGE3")
            startVideoCall(userId, contactData.phoneNumber, contactData.firstName, contactData.lastName, contactData.profileImageUrl)
        }

    }

    @Suppress("UNUSED_ANONYMOUS_PARAMETER")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_users_list)

        firebaseDatabase = FirebaseDatabase.getInstance()
        firebaseSignalingOnline = FirebaseSignalingOnline(firebaseDatabase)
        firebaseSignalingDisconnect = FirebaseSignalingDisconnect(firebaseDatabase)

        contactsViewModel = ViewModelProvider(this).get(ContactsListViewModel::class.java)

        contactsViewModel.allContacts.observe(this, Observer {contacts ->
            if(contacts.isNotEmpty()){
                emptyContactsView.visibility = View.GONE
                contactsList.visibility = View.VISIBLE
                adapter = ContactsListAdapter(this,  contacts.toMutableList(), contactsListAdapterListenter)
                contactsList.adapter = adapter
            } else {
                emptyContactsView.visibility = View.VISIBLE
                contactsList.visibility = View.GONE
            }
        })

//        adapter = ContactsAdapter(this, ArrayList<Pair<String, ContactData>>(0))

        val contactLayoutManager = LinearLayoutManager(applicationContext)
        contactLayoutManager.orientation = RecyclerView.VERTICAL

        contactsList.layoutManager = contactLayoutManager

        addContact.setOnClickListener {
            val intent = Intent(this@UsersListActivity, EditContactActivity::class.java)
            startActivity(intent)
        }

        FirebaseData.init()

        callRef = FirebaseData.database.getReference("calls/$myID/id")

        setOnline()

        attachService()
    }

    fun setOnline() {
        disposables += firebaseSignalingOnline.connect()
                .andThen(firebaseSignalingDisconnect.cleanDisconnectOrders())
                .doOnComplete { listenForDisconnectOrders() }
                .andThen(firebaseSignalingOnline.setOnline())
                .subscribeBy(
                        onError = {
                            Timber.e(it, "Error while choosing random")
//                            getView()?.showErrorWhileChoosingRandom()
                        },
                        onComplete = {
                            Timber.d("Done")
//                            getView()?.showCamViews()
//                            getView()?.showNoOneAvailable()
                        }
                )

    }


    fun listenForDisconnectOrders() {
        disconnectOrdersSubscription = firebaseSignalingDisconnect.cleanDisconnectOrders()
                .andThen(firebaseSignalingDisconnect.listenForDisconnectOrders())
                .compose(RxUtils.applyFlowableIoSchedulers())
                .subscribeBy(
                        onNext = {
                            Timber.d("Disconnect order")
//                            getView()?.showOtherPartyFinished()
//                            disconnect()
                        }
                )
    }


    override fun onResume() {
        super.onResume()
        callRef.addValueEventListener(callListener)
//        val usersRef = FirebaseData.database.getReference("users/$myID/contacts")
//        usersRef.addValueEventListener(usersListener)
    }

    override fun onPause() {
        super.onPause()
        callRef.removeEventListener(callListener)
        val usersRef = FirebaseData.database.getReference("users/$myID/contacts")
//        usersRef.addValueEventListener(usersListener)
    }

    override fun onStart() {
        super.onStart()
        service?.hideBackgroundWorkWarning()
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) {
            service?.showBackgroundWorkWarning()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!isChangingConfigurations) disconnect()
    }

    private fun startVideoCall(key: String, phoneNumber: String?, firstname: String?, surname: String?, profileImageUrl: String?) {

        val query = FirebaseData.database.getReference("users/").orderByChild("phoneNumber").equalTo(phoneNumber)
        query.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onCancelled(p0: DatabaseError?) {

            }

            override fun onDataChange(p0: DataSnapshot?) {
                var userId: String? = null
                p0?.children?.forEach {
                    userId = it.child("id").value.toString()
                }
                if (userId != null){
//                    FirebaseData.getCallStatusReference(myID).setValue(true)
//                    FirebaseData.getCallIdReference(userId!!).onDisconnect().removeValue()
//                    FirebaseData.getCallIdReference(userId!!).setValue(myID)
                    val intent = Intent(this@UsersListActivity, CallActivity::class.java)
                    intent.putExtra("userId", phoneNumber)
                    intent.putExtra("firstname", firstname)
                    intent.putExtra("surname", surname)
                    intent.putExtra("profileImageUrl", profileImageUrl)
                    startActivity(intent)
//                    VideoCallActivity.startCall(this@UsersListActivity, userId!!, key, firstname, surname, profileImageUrl)
                } else {
                    val intent = Intent(this@UsersListActivity, ErrorActivity::class.java)
                    this@UsersListActivity.startActivity(intent)
                }
            }

        })
    }

    private fun receiveVideoCall(key: String) {
        val intent = Intent(this@UsersListActivity, HangUpActivity::class.java)
        intent.putExtra("key", key)
        intent.putExtra("isOffer", false)
        this@UsersListActivity.startActivity(intent)
//        VideoCallActivity.receiveCall(this, key)
    }


    private val callListener = object : ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            if (dataSnapshot.exists()) {
                receiveVideoCall(dataSnapshot.getValue(String::class.java)!!)
                callRef.removeValue()
            }
        }

        override fun onCancelled(p0: DatabaseError?) {
        }
    }

    private val usersListener = object : ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            adapter.clear()
            if (!dataSnapshot.exists()) {
                emptyContactsView.visibility = View.VISIBLE
                return
            }
            var count = 0
            dataSnapshot.children.forEach {
                if (it.exists() && it.key != myID)
                    adapter.add(it.getValue(Contact::class.java)!!)
                count++
            }

            if (count == 0){
                emptyContactsView.visibility = View.VISIBLE
            } else {
                emptyContactsView.visibility = View.GONE
            }

            adapter.notifyDataSetChanged()
        }

        override fun onCancelled(p0: DatabaseError?) {
        }
    }

    override fun criticalWebRTCServiceException(throwable: Throwable) {
        TODO("Not yet implemented")
    }

    override fun connectionStateChange(iceConnectionState: PeerConnection.IceConnectionState) {
        Timber.d("Ice connection state changed: $iceConnectionState")
        when (iceConnectionState) {
            PeerConnection.IceConnectionState.CONNECTED -> {
//                getView()?.showConnectedMsg()
            }
            PeerConnection.IceConnectionState.DISCONNECTED -> {
//                getView()?.showWillTryToRestartMsg()
            }
            else -> {
                //no-op for now - could show or hide progress bars or messages on given event
            }
        }
    }

    override fun handleRemoteOffer(sessionDescription: SessionDescription, remoteUuid: String?) {
        Timber.i("handleRemoteOffer: $remoteUuid")
        mSessionDescription = sessionDescription
        mRemoteUuid = remoteUuid
        showHangUpDialog()
//        service?.handleRemoteOffer(remoteUuid, sessionDescription)
//        service?.handleRemoteOffer(remoteUuid, sessionDescription)
    }

    override fun showDialog() {
        Timber.d("showDialog called")
    }

    fun attachService() {
        serviceConnection = object : ServiceConnection {
            override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {
                onWebRtcServiceConnected((iBinder as (WebRtcService.LocalBinder)).service)
//                getPresenter().startRoulette(null)
            }

            override fun onServiceDisconnected(componentName: ComponentName) {
                onWebRtcServiceDisconnected()
            }
        }
        startAndBindWebRTCService(serviceConnection)
    }

    private fun onWebRtcServiceConnected(service: WebRtcService) {
        Timber.d("Service connected")
        this.service = service
//        service.attachLocalView(localVideoView)
//        service.attachRemoteView(remoteVideoView)
//        syncButtonsState(service)
        service.attachServiceActionsListener(webRtcServiceListener = this)
    }

    private fun onWebRtcServiceDisconnected() {
        Timber.d("Service disconnected")
    }

    private fun startAndBindWebRTCService(serviceConnection: ServiceConnection) {
        WebRtcService.startService(applicationContext)
        WebRtcService.bindService(applicationContext, serviceConnection)
    }

    fun connectTo(uuid: String) {
        service?.offerDevice(uuid)
    }

    fun disconnect() {
        service?.let {
            it.stopSelf()
            unbindService(serviceConnection)
        }
    }

    private fun unbindService() {
        service?.let {
            it.detachServiceActionsListener()
            unbindService(serviceConnection)
            service = null
        }
    }

    fun showHangUpDialog() {

        //TODO CREATE CUSTOM DIALOG (HANG UP CALL)

        callDialog = createHangUpCallDialog(this@UsersListActivity){newValue ->
            when (newValue) {
                CallEvent.HangUpClickedEvent -> {
                    callDialog?.dismiss()
                    service?.handleRemoteOffer(mRemoteUuid, mSessionDescription!!)
                }
                CallEvent.HangDownClickedEvent -> {
                    callDialog?.dismiss()
                    disconnectByUser()
                }
            }
        }
        callDialog?.show()
    }

    fun disconnectByUser() {
        val remoteUuid = remoteUuid
        if (remoteUuid != null) {
            disposables += firebaseSignalingDisconnect.sendDisconnectOrderToOtherParty(remoteUuid)
                    .compose(RxUtils.applyCompletableIoSchedulers())
                    .subscribeBy(
                            onComplete = {
                                disconnect()
                            }
                    )
        } else {
            disconnect()
        }
    }

}
