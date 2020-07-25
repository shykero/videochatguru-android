package co.netguru.android.chatandroll.feature.hangup

import android.content.Intent
import android.media.AsyncPlayer
import android.media.AudioManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import co.netguru.android.chatandroll.BuildConfig
import co.netguru.android.chatandroll.R
import co.netguru.android.chatandroll.data.firebase.FirebaseData
import com.bumptech.glide.Glide
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_hang_up.*
import kotlinx.android.synthetic.main.activity_hang_up.callerName
import kotlinx.android.synthetic.main.activity_hang_up.callerSurname
import kotlinx.android.synthetic.main.activity_hang_up.cancel_call_button
import kotlinx.android.synthetic.main.activity_hang_up.hangup_call_button
import java.lang.Exception

class HangUpActivity : AppCompatActivity() {

    val asyncPlayer = AsyncPlayer(null)

    val dbRef = FirebaseData.database.getReference("${FirebaseData.CALLS}/${FirebaseData.myID}/isBusy")!!

    lateinit var callStatusRef: DatabaseReference

    val valueEventListener = object : ValueEventListener{
        override fun onCancelled(p0: DatabaseError?) {}

        override fun onDataChange(p0: DataSnapshot?) {
            Log.e("status", "status=$p0")
            if (p0?.value != null){
                val status = p0?.value as Boolean
                if (!status){
                    try {
                        callStatusRef.removeValue()
                        asyncPlayer.stop()
                        this@HangUpActivity.finish()
                    } catch (e: Exception){
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hang_up)

        val key = intent.getStringExtra("key")

        callStatusRef = FirebaseData.getCallStatusReference(key)

        val uri = Uri.parse("android.resource://" + BuildConfig.APPLICATION_ID + "/" +
                R.raw.incoming_call_ring)
        asyncPlayer.play(this@HangUpActivity, uri, true,  AudioManager.STREAM_RING)


        val query = FirebaseData.database.getReference("users/").orderByChild("id").equalTo(key)
        query.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onCancelled(p0: DatabaseError?) {
            }
            override fun onDataChange(p0: DataSnapshot?) {
                var phoneNumber: String? = null
                p0?.children?.forEach {
                    phoneNumber = it.child("phoneNumber").value.toString()
                }
                if (phoneNumber != null){
                    getContactData(phoneNumber)
                }
            }
        })
        val callerRef = FirebaseData.database.getReference("${FirebaseData.CALLS}/${FirebaseData.myID}/isBusy")!!

        hangup_call_button.setOnClickListener {
            asyncPlayer.stop()
            callStatusRef.removeEventListener(valueEventListener)
//                VideoCallActivity.receiveCall(this, key)
                finish()
        }
        cancel_call_button.setOnClickListener {
            FirebaseData.getCallBusyStatusReference(key).setValue(true)
            dbRef.setValue(true)
            callStatusRef.removeEventListener(valueEventListener)
            callStatusRef.setValue(false)
            asyncPlayer.stop()
            finish()
        }

        callStatusRef = FirebaseData.getCallStatusReference(key)
        callStatusRef.addValueEventListener(valueEventListener)

        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(p0: DataSnapshot?) {
                Log.e("VideoCall", "onDataChange:$p0")
            }
            override fun onCancelled(p0: DatabaseError?) {
                Log.e("VideoCall", "onCancelled")
            }
        })

//        VideoCallActivity.receiveCall(this, key)



    }

    private fun getContactData(phoneNumber: String?) {
        val query = FirebaseData.database.getReference("users/${FirebaseData.myID}/contacts").orderByChild("phoneNumber").equalTo(phoneNumber)
        query.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onCancelled(p0: DatabaseError?) {

            }

            override fun onDataChange(p0: DataSnapshot?) {
                var username: String? = null
                var surname: String? = null
                var profileUrl: String? = null
                p0?.children?.forEach {
                    username = it.child("firstname").value.toString()
                    surname = it.child("surname").value.toString()
                    profileUrl = it.child("profileImageUrl").value.toString()
                }
                if (phoneNumber != null){
                    fillContactData(username, surname, profileUrl)
                }
            }
        })
    }

    private fun fillContactData(username: String?, surname: String?, profileUrl: String?) {
        username?.let { callerName.text = it }
        surname?.let { callerSurname.text = it }

        if (profileUrl != null && profileUrl != "default" && profileUrl != "null") {
            try {
                Glide.with(this@HangUpActivity).load(profileUrl).into(profileImage)
            } catch (e: Exception){
                e.printStackTrace()
            }
        }
    }
}