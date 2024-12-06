package com.example.dimsumproject.ui.register

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.dimsumproject.R
import com.example.dimsumproject.databinding.ItemRegisterContactBinding

data class RegisterContact(
    val contact_type: String,
    val contact_value: String,
    val notes: String
)

class RegisterContactAdapter(
    private val contacts: MutableList<RegisterContact>,
    private val onDeleteClick: (Int) -> Unit
) : RecyclerView.Adapter<RegisterContactAdapter.ContactViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val binding = ItemRegisterContactBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ContactViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        holder.bind(contacts[position], onDeleteClick)
    }

    override fun getItemCount() = contacts.size

    class ContactViewHolder(private val binding: ItemRegisterContactBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(contact: RegisterContact, onDeleteClick: (Int) -> Unit) {
            val iconResId = when(contact.contact_type) {
                "IG" -> R.drawable.instagram
                "WA" -> R.drawable.whatsapp
                "FB" -> R.drawable.facebook
                "X" -> R.drawable.twitter
                "LI" -> R.drawable.linkedin
                else -> R.drawable.ic_contact
            }

            val contactTypeName = when(contact.contact_type) {
                "IG" -> "Instagram"
                "WA" -> "WhatsApp"
                "FB" -> "Facebook"
                "X" -> "Twitter"
                "LI" -> "LinkedIn"
                else -> contact.contact_type
            }

            binding.apply {
                ivContactIcon.setImageResource(iconResId)
                tvContactType.text = contactTypeName
                tvContactValue.text = contact.contact_value
                tvNotes.text = contact.notes
                btnDelete.setOnClickListener { onDeleteClick(adapterPosition) }
            }
        }
    }

    fun addContact(contact: RegisterContact) {
        contacts.add(contact)
        notifyItemInserted(contacts.size - 1)
    }

    fun removeContact(position: Int) {
        contacts.removeAt(position)
        notifyItemRemoved(position)
    }

    fun getContacts(): List<RegisterContact> = contacts
}