package co.netguru.android.chatandroll.feature.verification

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import co.netguru.android.chatandroll.R
import co.netguru.android.chatandroll.app.App
import co.netguru.android.chatandroll.common.extension.areAllPermissionsGranted
import co.netguru.android.chatandroll.data.firebase.FirebaseData
import co.netguru.android.chatandroll.feature.call.VideoCallFragment
import co.netguru.android.chatandroll.feature.userlist.UsersListActivity
import co.netguru.android.chatandroll.webrtc.service.WebRtcService
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.android.synthetic.main.activity_verification.*
import java.lang.StringBuilder

class VerificationActivity : AppCompatActivity() {

    lateinit var auth: FirebaseAuth
    val TIME_OUT = 60
    var mCallback: PhoneAuthProvider.OnVerificationStateChangedCallbacks? = null

    var verificationCode: String? = null


    private val KEY_IN_CHAT = "key:in_chat"
    private val CHECK_PERMISSIONS_AND_CONNECT_REQUEST_CODE = 1
    private val NECESSARY_PERMISSIONS = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
    private val CONNECT_BUTTON_ANIMATION_DURATION_MS = 500L


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verification)

        auth = FirebaseAuth.getInstance()
        mCallback = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(phoneAuthCredential: PhoneAuthCredential) {
//                Toast.makeText(this@VerificationActivity, "verification completed", Toast.LENGTH_SHORT)
//                        .show()
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Toast.makeText(this@VerificationActivity, "verification failed", Toast.LENGTH_SHORT).show()
                Log.d("FirebaseException", e.toString())
            }

            override fun onCodeSent(s: String, forceResendingToken: PhoneAuthProvider.ForceResendingToken) {
                super.onCodeSent(s, forceResendingToken)
                Log.d("verificationCode", s)
            }
        }

        verificationCode = intent.getStringExtra("verification_code")

        val itemsCount = phoneNumbersGrid.childCount
        for (i in 0 until itemsCount){
            val itemView = phoneNumbersGrid.getChildAt(i)
            when (itemView){
                is Button -> {
                    itemView.setOnClickListener {
                        when {
                            firstNumber.text.isNullOrEmpty() -> {
                                firstNumber.text = itemView.text
                            }
                            secondNumber.text.isNullOrEmpty() -> {
                                secondNumber.text = itemView.text
                            }
                            thirdNumber.text.isNullOrEmpty() -> {
                                thirdNumber.text = itemView.text
                            }
                            fourthNumber.text.isNullOrEmpty() -> {
                                fourthNumber.text = itemView.text
                            }
                            fifthNumber.text.isNullOrEmpty() -> {
                                fifthNumber.text = itemView.text
                            }
                            sixthNumber.text.isNullOrEmpty() -> {
                                sixthNumber.text = itemView.text
                                progressBar.visibility = View.VISIBLE
                                signInWithPhone(provideVerificationCode())
                            }
                        }
                    }
                }
                else -> {
                    itemView.setOnClickListener {
                        when {
                            sixthNumber.text.isNullOrEmpty() -> {
                                sixthNumber.text = ""
                            }
                            fifthNumber.text.isNullOrEmpty() -> {
                                fifthNumber.text = ""
                            }
                            fourthNumber.text.isNullOrEmpty() -> {
                                fourthNumber.text = ""
                            }
                            thirdNumber.text.isNotEmpty() -> {
                                thirdNumber.text = ""
                            }
                            secondNumber.text.isNotEmpty() -> {
                                secondNumber.text = ""
                            }
                            firstNumber.text.isNotEmpty() -> {
                                firstNumber.text = ""
                            }
                        }
                    }
                }
            }
        }
    }

    private fun provideVerificationCode(): String {
        val stringBuilder = StringBuilder()
        stringBuilder.append(firstNumber.text)
        stringBuilder.append(secondNumber.text)
        stringBuilder.append(thirdNumber.text)
        stringBuilder.append(fourthNumber.text)
        stringBuilder.append(fifthNumber.text)
        stringBuilder.append(sixthNumber.text)

        return stringBuilder.toString()
    }

    private fun signInWithPhone(codeForVerification: String) {
        val credential = PhoneAuthProvider.getCredential(verificationCode!!, codeForVerification)
        auth.signInWithCredential(credential)
                .addOnCompleteListener { task ->
                    progressBar.visibility = View.GONE

                    if (task.isSuccessful) {
//                        Toast.makeText(this@VerificationActivity, "Correct OTP", Toast.LENGTH_SHORT).show()
                        setResult(RESULT_OK)
                        saveUserToDatabase(task.result.user)
//                        task.result.user.displayName
                    } else {
                        Toast.makeText(this@VerificationActivity, "Incorrect OTP", Toast.LENGTH_SHORT).show()
                    }
                }
    }

    private fun saveUserToDatabase(user: FirebaseUser?) {
        val userId = user!!.uid
        val usersRef = FirebaseData.database.getReference("users").child(userId)

        val hashMap = HashMap<String, String>().apply {
            put("id", userId)
            put("username", "")
            put("surname", "")
            put("imageUrl", "default")
            user.phoneNumber?.let { put("phoneNumber", it) }
        }

        usersRef.setValue(hashMap).addOnCompleteListener {
            if (it.isSuccessful){
                App.CURRENT_DEVICE_UUID = user.phoneNumber.toString()
                openUsersListActivity()
                finish()
            } else {
                Toast.makeText(this@VerificationActivity, "Error while registering user", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openUsersListActivity() {
        val intent = Intent(this@VerificationActivity, UsersListActivity::class.java)
        startActivity(intent)
    }
}