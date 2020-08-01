package co.netguru.android.chatandroll.data.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity (tableName = "contacts")
data class Contact(
        @PrimaryKey (autoGenerate = true) val uid: Int = 0,
        @ColumnInfo(name = "first_name") val firstName: String?,
        @ColumnInfo(name = "last_name") val lastName: String?,
        @ColumnInfo(name = "phone_number") val phoneNumber: String?,
        @ColumnInfo(name = "profile_image_url") val profileImageUrl: String?,
        @ColumnInfo(name = "online") val online: Boolean = false
)