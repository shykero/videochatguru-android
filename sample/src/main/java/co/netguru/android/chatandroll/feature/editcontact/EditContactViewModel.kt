package co.netguru.android.chatandroll.feature.editcontact

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import co.netguru.android.chatandroll.data.ContactsRepository
import co.netguru.android.chatandroll.data.room.AppDatabase
import co.netguru.android.chatandroll.data.room.Contact
import io.reactivex.Completable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy

class EditContactViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ContactsRepository
    // Using LiveData and caching what getAlphabetizedWords returns has several benefits:
    // - We can put an observer on the data (instead of polling for changes) and only update the
    //   the UI when the data actually changes.
    // - Repository is completely separated from the UI through the ViewModel.
    lateinit var contacts: LiveData<List<Contact>>

    private val disposables = CompositeDisposable()

    init {
        val wordsDao = AppDatabase.getDatabase(application).wordDao()
        repository = ContactsRepository(wordsDao)
        contacts = repository.allContacts
    }

    /**
     * Launching a new coroutine to insert the data in a non-blocking way
     */
    fun insert(contact: Contact): Completable {
        return repository.insert(contact)
    }

    /**
     * Launching a new coroutine to insert the data in a non-blocking way
     */
    fun update(uuid: String, firstName: String, lastName: String, phoneNumber: String, imageUrl: String?): Completable {
        return repository.update(uuid, firstName, lastName, phoneNumber, imageUrl)
    }

    fun deleteContact(uuid: String): Completable{
        return repository.deleteContact(uuid)
    }


    fun findContactById(uid: String): LiveData<Contact> {
        return repository.getContactById(uid)
    }

    fun onDestroy() {
        disposables.dispose()
    }
}