package com.example.dimsumproject.ui.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.dimsumproject.R
import com.example.dimsumproject.data.api.Contact
import com.example.dimsumproject.databinding.ItemContactGridEditableBinding

class ContactsAdapter(
    private var contacts: List<Contact>,
    private val onEditClick: ((Contact) -> Unit)? = null,
    private val onDeleteClick: ((Contact) -> Unit)? = null
) : RecyclerView.Adapter<ContactsAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemContactGridEditableBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(contact: Contact) {
            binding.apply {
                val (iconResId, contactTypeName) = getContactTypeInfo(contact.contact_type)
                ivContactIcon.setImageResource(iconResId)
                tvContactType.text = contactTypeName

                // Show/hide edit & delete buttons based on callback presence
                btnEdit.visibility = if (onEditClick != null) View.VISIBLE else View.GONE
                btnDelete.visibility = if (onDeleteClick != null) View.VISIBLE else View.GONE

                btnEdit.setOnClickListener { onEditClick?.invoke(contact) }
                btnDelete.setOnClickListener { onDeleteClick?.invoke(contact) }

                // Handle click on the entire item
                root.setOnClickListener {
                    handleContactClick(contact, root.context)
                }
            }
        }

        private fun handleContactClick(contact: Contact, context: Context) {
            try {
                when (contact.contact_type.uppercase()) {
                    "WA" -> {
                        val formattedPhone = contact.contact_value.replace("+", "").replace("-", "")
                        val url = "https://api.whatsapp.com/send?phone=$formattedPhone"
                        launchUrl(context, url)
                    }
                    "IG" -> {
                        val username = contact.contact_value.replace("@", "")
                        val url = when {
                            contact.contact_value.startsWith("http") -> contact.contact_value
                            contact.contact_value.startsWith("instagram.com/") ->
                                "https://${contact.contact_value}"
                            else -> "http://instagram.com/${username}"
                        }
                        launchUrl(context, url)
                    }
                    "LI" -> {
                        val url = when {
                            contact.contact_value.startsWith("http") -> contact.contact_value
                            contact.contact_value.startsWith("linkedin.com/") ->
                                "https://${contact.contact_value}"
                            else -> "https://www.linkedin.com/in/${contact.contact_value}"
                        }
                        launchUrl(context, url)
                    }
                    "FB" -> launchUrl(context, contact.contact_value)
                    "EMAIL" -> {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:${contact.contact_value}")
                        }
                        context.startActivity(intent)
                    }
                    "PHONE" -> {
                        val intent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:${contact.contact_value}")
                        }
                        context.startActivity(intent)
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Tidak dapat membuka aplikasi", Toast.LENGTH_SHORT).show()
            }
        }

        private fun launchUrl(context: Context, url: String) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemContactGridEditableBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(contacts[position])
    }

    override fun getItemCount() = contacts.size

    fun updateContacts(newContacts: List<Contact>) {
        contacts = newContacts
        notifyDataSetChanged()
    }

    companion object {
        fun getContactTypeInfo(contactType: String): Pair<Int, String> {
            return when(contactType.uppercase()) {
                "IG" -> R.drawable.instagram to "Instagram"
                "WA" -> R.drawable.whatsapp to "WhatsApp"
                "FB" -> R.drawable.facebook to "Facebook"
                "X" -> R.drawable.twitter to "Twitter"
                "LI" -> R.drawable.linkedin to "LinkedIn"
                "EMAIL" -> R.drawable.ic_email to "Email"
                "PHONE" -> R.drawable.ic_phone to "Phone"
                else -> R.drawable.ic_contact to contactType
            }
        }
    }
}