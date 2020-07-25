package co.netguru.android.chatandroll.data.firebase

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase


/**
 * FirebaseData
 */
object FirebaseData {

    var myID: String = ""
    const val CALLS = "calls"

    val database = FirebaseDatabase.getInstance()


    fun getCallDataPath(id: String) = "${CALLS}/$id/data"
    fun getCallStatusPath(id: String) = "${CALLS}/$id/status"

    fun getCallDataReference(id: String) = database.getReference("${CALLS}/$id/data")!!
    fun getCallStatusReference(id: String) = database.getReference("${CALLS}/$id/status")!!
    fun getCallBusyStatusReference(id: String) = database.getReference("${CALLS}/$id/isBusy")!!
    fun getCallIdReference(id: String) = database.getReference("${CALLS}/$id/id")!!
    fun getCallReference(id: String): DatabaseReference = database.getReference("${CALLS}/$id")

    fun init() {
        val auth = FirebaseAuth.getInstance()!!
        auth.currentUser?.let {
            myID = it.uid
            database.getReference("users/${myID}/online").onDisconnect().setValue(false)
//            database.getReference("users/${myID}").setValue(
//                    ContactData(it.uid, it.displayName, it.displayName, it.phoneNumber, it.photoUrl.toString(), true))
        }

    }

}

data class ContactData(
        val userId: String? = "",
        val firstname: String? = "",
        val surname: String? = "",
        val phoneNumber: String? = "",
        val profileImageUrl: String? = null,
        val online: Boolean = false)