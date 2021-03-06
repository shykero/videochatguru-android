package co.netguru.android.chatandroll.feature.entry

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import co.netguru.android.chatandroll.R
import co.netguru.android.chatandroll.app.App
import co.netguru.android.chatandroll.feature.login.LoginActivity
import co.netguru.android.chatandroll.feature.userlist.UsersListActivity
import com.google.firebase.auth.FirebaseAuth

class EntryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_entry)

        val auth = FirebaseAuth.getInstance()

        if (auth.currentUser != null) {
//             already signed in
            Log.e("currentUser", auth.currentUser!!.phoneNumber.toString())
            App.CURRENT_DEVICE_UUID = auth.currentUser!!.phoneNumber.toString()
            startActivity(Intent(this, UsersListActivity::class.java))
            finish()
            overridePendingTransition(0, 0)
        } else {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
            overridePendingTransition(0, 0)
        }
    }
}