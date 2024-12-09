package com.example.dimsumproject.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.dimsumproject.R
import com.example.dimsumproject.data.api.Contact
import com.example.dimsumproject.databinding.ItemContactBinding

class ContactsAdapter(private val contacts: List<Contact>) :
    RecyclerView.Adapter<ContactsAdapter.ContactViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val binding = ItemContactBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ContactViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        holder.bind(contacts[position])
    }

    override fun getItemCount() = contacts.size

    class ContactViewHolder(private val binding: ItemContactBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(contact: Contact) {
            val iconResId = when(contact.contact_type) {
                "IG" -> R.drawable.instagram
                "WA" -> R.drawable.whatsapp
                "FB" -> R.drawable.facebook
                "X" -> R.drawable.twitter
                "LI" -> R.drawable.linkedin
                else -> R.drawable.ic_contact
            }

            // Set nama lengkap social media
            val contactTypeName = when(contact.contact_type) {
                "IG" -> "Instagram"
                "WA" -> "WhatsApp"
                "FB" -> "Facebook"
                "X" -> "Twitter"
                "LI" -> "LinkedIn"
                else -> contact.contact_type
            }

            binding.ivContactIcon.setImageResource(iconResId)
            binding.tvContactType.text = contactTypeName
            binding.tvContactValue.text = contact.contact_value
        }
    }
}