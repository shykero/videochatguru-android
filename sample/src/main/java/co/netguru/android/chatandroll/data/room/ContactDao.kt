package co.netguru.android.chatandroll.data.room

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts")
    fun getAllContacts(): List<Contact>

    @Query("SELECT * FROM contacts WHERE uid IN (:userIds)")
    fun loadAllByIds(userIds: IntArray): List<Contact>

    @Query("SELECT * from contacts ORDER BY uid ASC")
    fun getAllContactsLiveData(): LiveData<List<Contact>>

    @Query("SELECT * FROM contacts WHERE first_name LIKE :first AND " +
            "last_name LIKE :last LIMIT 1")
    fun findByName(first: String, last: String): Contact

    @Query("SELECT * FROM contacts WHERE uid LIKE :uuid LIMIT 1")
    fun findContactById(uuid: String): LiveData<Contact>

    @Insert
    fun insertAll(vararg users: Contact)

    @Insert
    fun insert(user: Contact)

    @Query("UPDATE contacts SET first_name = :firstName, last_name = :lastName, phone_number = :phoneNumber, profile_image_url = :imageUrl WHERE uid = :uuid")
    fun update(uuid: String, firstName: String, lastName: String, phoneNumber: String, imageUrl: String?)

    @Delete
    fun delete(user: Contact)

    @Query("DELETE FROM contacts WHERE uid = :contactId")
    fun deleteByContactId(contactId: String)
}