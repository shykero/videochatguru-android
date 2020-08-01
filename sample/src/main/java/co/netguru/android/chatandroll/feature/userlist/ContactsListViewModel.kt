package co.netguru.android.chatandroll.feature.userlist

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import co.netguru.android.chatandroll.data.ContactsRepository
import co.netguru.android.chatandroll.data.room.AppDatabase
import co.netguru.android.chatandroll.data.room.Contact
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy

class ContactsListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ContactsRepository
    // Using LiveData and caching what getAlphabetizedWords returns has several benefits:
    // - We can put an observer on the data (instead of polling for changes) and only update the
    //   the UI when the data actually changes.
    // - Repository is completely separated from the UI through the ViewModel.
    val allContacts: LiveData<List<Contact>>

    private val disposables = CompositeDisposable()

    init {
        val wordsDao = AppDatabase.getDatabase(application).wordDao()
        repository = ContactsRepository(wordsDao)
        allContacts = repository.allContacts
    }

    /**
     * Launching a new coroutine to insert the data in a non-blocking way
     */
    fun insert(contact: Contact) {
        disposables += (repository.insert(contact)).subscribeBy {
            Log.e("TAG", "contact added successfully")
        }
//        return viewModelScope.launch(Dispatchers.IO) {
//        }
    }

    fun onDestroy() {
        disposables.dispose()
    }
}