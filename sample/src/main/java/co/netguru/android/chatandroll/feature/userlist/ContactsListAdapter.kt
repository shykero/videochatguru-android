package co.netguru.android.chatandroll.feature.userlist

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import co.netguru.android.chatandroll.R
import co.netguru.android.chatandroll.data.firebase.ContactData
import com.bumptech.glide.Glide
import de.hdodenhof.circleimageview.CircleImageView

class ContactsListAdapter(
        val context: Context,
        private val contactsList: ArrayList<Pair<String, ContactData>>,
        private val listener: IListener): RecyclerView.Adapter<ContactsListAdapter.ContactsViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactsViewHolder {
        val inflater = LayoutInflater.from(context)
        val itemView = inflater.inflate(R.layout.contacts_item_row, parent, false)
        return ContactsViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return contactsList.size
    }

    override fun onBindViewHolder(holder: ContactsViewHolder, position: Int) {
        val contactDataPair = contactsList[position]
        holder.firstName.text = contactDataPair.second.firstname
        holder.surname.text = contactDataPair.second.surname
        holder.editButton.setOnClickListener {
            listener.onEditIconClicked(position, contactDataPair.second.userId!!)
        }

        holder.mainLayout.setOnClickListener {
            listener.onContactItemClicked(position, contactDataPair.first, contactDataPair.second)
        }

        val url = contactsList[position].second.profileImageUrl
        if (url == null || url == "default" || url == "null"){
        } else {
            Glide.with(context).load(url).into(holder.profileImage)
        }
//        holder.profileImage.setImageURI(contactsList[position].second.profileImageUrl)
//        holder.profileImage = contactsList[position].second.surname
    }

    fun add (pair: Pair<String, ContactData>){
        contactsList.add(pair)
    }

    fun clear (){
        contactsList.clear()
    }

    class ContactsViewHolder(private val view: View): RecyclerView.ViewHolder(view) {
        val firstName = view.findViewById(R.id.firstname) as TextView
        val surname = view.findViewById(R.id.surname) as TextView
        val profileImage = view.findViewById(R.id.profileImage) as CircleImageView
        val editButton = view.findViewById(R.id.editButton) as ImageView
        val mainLayout = view.findViewById(R.id.mainLayout) as LinearLayout

    }


    interface IListener{
        fun onEditIconClicked(position: Int, userId: String)
        fun onContactItemClicked(position: Int, userId: String, contact: ContactData)
    }
}