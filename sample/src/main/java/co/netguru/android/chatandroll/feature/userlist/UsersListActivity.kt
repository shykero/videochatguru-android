package co.netguru.android.chatandroll.feature.userlist

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.widget.AdapterView
import co.netguru.android.chatandroll.R
import co.netguru.android.chatandroll.data.firebase.ContactData
import co.netguru.android.chatandroll.data.firebase.FirebaseData
import co.netguru.android.chatandroll.data.firebase.FirebaseData.myID
import co.netguru.android.chatandroll.feature.editcontact.EditContactActivity
import co.netguru.android.chatandroll.feature.hangup.HangUpActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_users_list.*


class UsersListActivity : AppCompatActivity() {

    private lateinit var adapter: ContactsListAdapter

    private lateinit var callRef: DatabaseReference

    @Suppress("UNUSED_ANONYMOUS_PARAMETER")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_users_list)

//        adapter = ContactsAdapter(this, ArrayList<Pair<String, ContactData>>(0))
        adapter = ContactsListAdapter(this,  ArrayList(0), object: ContactsListAdapter.IListener {
            override fun onEditIconClicked(position: Int, userId: String) {
                val intent = Intent(this@UsersListActivity, EditContactActivity::class.java)
                intent.putExtra("userId", userId)
                startActivity(intent)
            }

            override fun onContactItemClicked(position: Int, userId: String, contactData: ContactData) {
//                startVideoCall("4SGxUDUntFTCJAMH7XfF3vhsCGE3")
                startVideoCall(userId, contactData.phoneNumber, contactData.firstname, contactData.surname, contactData.profileImageUrl)
            }

        })

        contactsList.adapter = adapter

        val contactLayoutManager = LinearLayoutManager(applicationContext)
        contactLayoutManager.orientation = RecyclerView.VERTICAL

        contactsList.layoutManager = contactLayoutManager

        addContact.setOnClickListener {
            val intent = Intent(this@UsersListActivity, EditContactActivity::class.java)
            startActivity(intent)
        }

        FirebaseData.init()

        callRef = FirebaseData.database.getReference("calls/$myID/id")
    }


    override fun onResume() {
        super.onResume()
        callRef.addValueEventListener(callListener)
        val usersRef = FirebaseData.database.getReference("users/$myID/contacts")
        usersRef.addValueEventListener(usersListener)
    }

    override fun onPause() {
        super.onPause()
        callRef.removeEventListener(callListener)
        val usersRef = FirebaseData.database.getReference("users/$myID/contacts")
        usersRef.addValueEventListener(usersListener)
    }

    private fun startVideoCall(key: String, phoneNumber: String?, firstname: String?, surname: String?, profileImageUrl: String?) {

//        val query = FirebaseData.database.getReference("users/").orderByChild("phoneNumber").equalTo(phoneNumber)
//        query.addListenerForSingleValueEvent(object : ValueEventListener{
//            override fun onCancelled(p0: DatabaseError?) {
//
//            }
//
//            override fun onDataChange(p0: DataSnapshot?) {
//                var userId: String? = null
//                p0?.children?.forEach {
//                    userId = it.child("id").value.toString()
//                }
//                if (userId != null){
//                    FirebaseData.getCallStatusReference(myID).setValue(true)
//                    FirebaseData.getCallIdReference(userId!!).onDisconnect().removeValue()
//                    FirebaseData.getCallIdReference(userId!!).setValue(myID)
//                    VideoCallActivity.startCall(this@UsersListActivity, userId!!, key, firstname, surname, profileImageUrl)
//                } else {
//                    val intent = Intent(this@UsersListActivity, ErrorActivity::class.java)
//                    this@UsersListActivity.startActivity(intent)
//                }
//            }
//
//        })
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
                    adapter.add(Pair(it.key, it.getValue(ContactData::class.java)!!))
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

}
