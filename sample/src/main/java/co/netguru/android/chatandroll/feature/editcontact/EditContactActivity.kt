package co.netguru.android.chatandroll.feature.editcontact

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.Selection
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import co.netguru.android.chatandroll.R
import co.netguru.android.chatandroll.data.firebase.ContactData
import co.netguru.android.chatandroll.data.firebase.FirebaseData
import co.netguru.android.chatandroll.data.room.Contact
import co.netguru.android.chatandroll.feature.userlist.ContactsListViewModel
import com.bumptech.glide.Glide
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_edit_contact.*
import java.io.IOException
import java.lang.Exception
import java.util.*
import kotlin.collections.HashMap


private const val PICK_IMAGE_REQUEST = 71

class EditContactActivity : AppCompatActivity() {

    var userId: String? = null

//    var storage: FirebaseStorage? = null
//    var storageReference: StorageReference? = null
    var downloadUri: String? = null

    private lateinit var contactsViewModel: EditContactViewModel

    private val disposables = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_contact)

        contactsViewModel = ViewModelProvider(this).get(EditContactViewModel::class.java)

        userId = intent.getStringExtra("userId")

        if (userId != null){
            contactsViewModel.findContactById(userId!!).observe(this, androidx.lifecycle.Observer {
                fillUserDetails(it)
            })
        }

        deleteButton.setOnClickListener {
            if (userId != null){
                contactsViewModel.deleteContact(userId!!).subscribe {
                    finish()
                }
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

        phoneNumber.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (!s.toString().contains("+49")) {
                    phoneNumber.setText("""+49${s.toString()}""");
                    Selection.setSelection(phoneNumber.text, 3)
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

        })
    }

    private fun fillUserDetails(contact: Contact?) {
        contact?.let {
            userName.setText(contact.firstName)
            surname.setText(contact.lastName)
            phoneNumber.setText(contact.phoneNumber)

            val url = contact.profileImageUrl
            if (url == null || url == "default" || url == "null"){
                profileImage.setImageResource(R.drawable.ic_user_bg_dark)
                changeImageText.visibility = View.VISIBLE
            } else {
                changeImageText.visibility = View.GONE
                try {
                    downloadUri = url
                    Glide.with(this@EditContactActivity).load(url).into(profileImage)
                } catch (e: Exception){
                    e.printStackTrace()
                }
            }
        }

    }

    @SuppressLint("CheckResult")
    private fun saveUserToDatabase() {

        val firstNameValue = userName.text.toString()
        val lastNameValue = surname.text.toString()
        val phoneNumberValue = phoneNumber.text.toString()
        val profileImageUrlValue = downloadUri

        if (userId != null) {
            contactsViewModel.update(userId!!, firstNameValue, lastNameValue, phoneNumberValue, profileImageUrlValue).subscribe {
                finish()
            }
        } else {
            contactsViewModel.insert(Contact(firstName = firstNameValue,
                    lastName = lastNameValue,
                    phoneNumber = phoneNumberValue,
                    profileImageUrl = profileImageUrlValue)).subscribe {
                finish()
            }
        }
    }

    private fun uploadImage(filePath: Uri?) {
        if (filePath != null) {
            downloadUri = filePath.toString()

            try {
                Glide.with(this@EditContactActivity).load(downloadUri).into(profileImage)
            } catch (e: Exception){
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