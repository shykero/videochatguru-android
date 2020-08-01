package co.netguru.android.chatandroll.data

import androidx.lifecycle.LiveData
import co.netguru.android.chatandroll.data.room.Contact
import co.netguru.android.chatandroll.data.room.ContactDao
import io.reactivex.Completable

// Declares the DAO as a private property in the constructor. Pass in the DAO
// instead of the whole database, because you only need access to the DAO
class ContactsRepository(private val contactsDao: ContactDao) {

    // Room executes all queries on a separate thread.
    // Observed LiveData will notify the observer when the data has changed.
    val allContacts: LiveData<List<Contact>> = contactsDao.getAllContactsLiveData()


    fun insert(contact: Contact) : Completable = Completable.fromAction {
        contactsDao.insert(contact)
    }

    fun update(uuid: String, firstName: String, lastName: String, phoneNumber: String, imageUrl: String?) : Completable = Completable.fromAction {
        contactsDao.update(uuid, firstName, lastName, phoneNumber, imageUrl)
    }

    fun getContactById(uid: String): LiveData<Contact>{
        return contactsDao.findContactById(uid)
    }

    fun deleteContact(uid: String): Completable = Completable.fromAction {
        contactsDao.deleteByContactId(uid)
    }
}