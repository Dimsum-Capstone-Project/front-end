package com.example.dimsumproject.ui.history_visit

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Window
import com.example.dimsumproject.data.api.HistoryItem
import com.example.dimsumproject.databinding.DialogHistoryDetailBinding
import com.example.dimsumproject.ui.home.ContactsAdapter

class HistoryDetailDialog(
    context: Context,
    private val historyItem: HistoryItem
) : Dialog(context) {

    private lateinit var binding: DialogHistoryDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = DialogHistoryDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
    }

    private fun setupUI() {
        binding.apply {
            tvName.text = historyItem.profile.name
            tvBio.text = historyItem.profile.bio
            tvJobTitle.text = historyItem.profile.job_title
            tvCompany.text = historyItem.profile.company

            // Setup contacts
            val contactsAdapter = ContactsAdapter(historyItem.contacts)
            rvContacts.adapter = contactsAdapter

            btnClose.setOnClickListener { dismiss() }
        }
    }
}