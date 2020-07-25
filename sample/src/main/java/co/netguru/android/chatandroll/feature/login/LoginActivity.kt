package co.netguru.android.chatandroll.feature.login

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Button
import android.widget.Toast
import co.netguru.android.chatandroll.R
import co.netguru.android.chatandroll.feature.verification.VerificationActivity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.android.synthetic.main.activity_login.*
import java.util.concurrent.TimeUnit

class LoginActivity : AppCompatActivity() {

    lateinit var auth: FirebaseAuth
    val TIME_OUT = 60
    var mCallback: PhoneAuthProvider.OnVerificationStateChangedCallbacks? = null

    val VERIFICATION_REQUEST_CODE = 101


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        mCallback = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(phoneAuthCredential: PhoneAuthCredential) {
                Toast.makeText(this@LoginActivity, "verification completed", Toast.LENGTH_SHORT)
                        .show()
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Toast.makeText(this@LoginActivity, "verification failed", Toast.LENGTH_SHORT).show()
                Log.d("FirebaseException", e.toString())
            }

            override fun onCodeSent(s: String, forceResendingToken: PhoneAuthProvider.ForceResendingToken) {
                super.onCodeSent(s, forceResendingToken)
                Log.d("verificationCode", s)

                val intent = Intent(this@LoginActivity, VerificationActivity::class.java)
                intent.putExtra("verification_code", s)
                startActivityForResult(intent, VERIFICATION_REQUEST_CODE)
//                Toast.makeText(this@LoginActivity, "Code Sent", Toast.LENGTH_SHORT).show()
            }
        }

        sendButton.setOnClickListener {
            //TODO send OTP to the selected phone number
            val phoneNumber = phoneNumberValue.text.toString()
            if (phoneNumber.length >= 8){
                val phoneNumberWithCode = ""+countryCode.text+phoneNumberValue.text.toString()
//                val intent = Intent(this, VerificationActivity::class.java)
//                intent.putExtra("phone_number", phoneNumberWithCode)
//                startActivityForResult(intent, VERIFICATION_REQUEST_CODE)

                PhoneAuthProvider.getInstance().verifyPhoneNumber(
                            phoneNumberWithCode,// Phone number to verify
                            TIME_OUT.toLong(), // Timeout duration
                            TimeUnit.SECONDS,  // Unit of timeout
                            this@LoginActivity, // Activity (for callback binding)
                            mCallback!!
                )
            } else {
//                hideKeyboard()
                Toast.makeText(this@LoginActivity, "Please type phone number or pick from list", Toast.LENGTH_SHORT).show()
            }

        }

        val itemsCount = phoneNumbersGrid.childCount
        for (i in 0 until itemsCount){
            val itemView = phoneNumbersGrid.getChildAt(i)
            when (itemView){
                is Button -> {
                    itemView.setOnClickListener {
                        if (phoneNumberValue.text!!.length <= 15)
                            phoneNumberValue.append(itemView.text)
                    }
                }
                else -> {
                    itemView.setOnClickListener {
                        val currentText = phoneNumberValue.text.toString()
                        if (currentText.isNotEmpty()){
                            val result = currentText.substring(0, currentText.length-1)
                            phoneNumberValue.setText(result)
                        }
                    }
                }
            }
        }
    }
}