package co.netguru.android.chatandroll.feature.editcontact

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import co.netguru.android.chatandroll.R
import co.netguru.android.chatandroll.data.firebase.ContactData
import co.netguru.android.chatandroll.data.firebase.FirebaseData
import com.bumptech.glide.Glide
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.android.synthetic.main.activity_edit_contact.*
import java.io.IOException
import java.lang.Exception
import java.util.*
import kotlin.collections.HashMap


private const val PICK_IMAGE_REQUEST = 71

class EditContactActivity : AppCompatActivity() {

    var userId: String? = null

    var storage: FirebaseStorage? = null
    var storageReference: StorageReference? = null
    var downloadUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_contact)

        userId = intent.getStringExtra("userId")

//        val usersRef = FirebaseData.database.getReference("users").child(userId)

        var usersRef = if (userId!=null){
            FirebaseData.database.getReference("users/${FirebaseData.myID}/contacts/$userId")
        } else {
            FirebaseData.database.getReference("users/${FirebaseData.myID}/contacts")
        }

        storage = FirebaseStorage.getInstance();
        storageReference = storage!!.reference;
//        val usersRef = FirebaseData.database.getReference("users").child(userId)

        usersRef.addValueEventListener(object: ValueEventListener {
            override fun onCancelled(p0: DatabaseError?) {

            }

            override fun onDataChange(p0: DataSnapshot?) {
                val user = p0!!.getValue(ContactData::class.java)
                user?.let {
                    userName.setText(user.firstname)
                    surname.setText(user.surname)
                    phoneNumber.setText(user.phoneNumber)
                }
                if (user != null){
                    val url = user.profileImageUrl
                    if (url == null || url == "default" || url == "null"){
                        profileImage.setImageResource(R.drawable.ic_user_bg_dark)
                        changeImageText.visibility = View.VISIBLE
                    } else {
                        changeImageText.visibility = View.GONE
                        try {
                            Glide.with(this@EditContactActivity).load(url).into(profileImage)
                        } catch (e: Exception){
                            e.printStackTrace()
                        }
                    }
                }
            }
        })

        deleteButton.setOnClickListener {
            if (userId != null){
                val contactsRef = FirebaseData.database.getReference("users/${FirebaseData.myID}/contacts/$userId")
                contactsRef.removeValue()
                finish()
            } else {
                finish()
            }
        }

        saveButton.setOnClickListener {
            saveUserToDatabase()
        }

        profileImage.setOnClickListener {
            chooseImage()
        }
    }

    private fun saveUserToDatabase() {
        val usersRef: DatabaseReference

        if (userId != null) {
            usersRef = FirebaseData.database.getReference("users/${FirebaseData.myID}/contacts").child(userId)
        } else {
            usersRef = FirebaseData.database.getReference("users/${FirebaseData.myID}/contacts").push()
        }

            val hashMap = HashMap<String, Any>().apply {
                put("firstname", userName.text.toString())
                put("surname", surname.text.toString())
                put("phoneNumber", phoneNumber.text.toString())
                put("userId", usersRef.key)
        }
        if (downloadUri != null){
            hashMap["profileImageUrl"] = downloadUri.toString()
        }
            usersRef.updateChildren(hashMap).addOnCompleteListener {
                if (it.isSuccessful){
                    finish()
                } else {
                    Toast.makeText(this@EditContactActivity, "Error while registering user", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun uploadImage(filePath: Uri?) {
        if (filePath != null) {
            val ref = storageReference!!.child("images/" + UUID.randomUUID().toString())
            ref.putFile(filePath)
                    .addOnSuccessListener {
                        downloadUri = it.downloadUrl
                        if (userId != null){
                            val userRef = FirebaseData.database.getReference("users/${FirebaseData.myID}/contacts/$userId")
                            userRef.child("profileImageUrl").setValue(downloadUri.toString())
                        }
                    }
                    .addOnFailureListener { e ->
                        e.printStackTrace()
                    }
        }
    }

    private fun chooseImage() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            val filePath = data.data
            try {
                if (userId == null){
                    val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, filePath)
                    profileImage.setImageBitmap(bitmap)
                    changeImageText.visibility = View.GONE
                }
                uploadImage(filePath)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}