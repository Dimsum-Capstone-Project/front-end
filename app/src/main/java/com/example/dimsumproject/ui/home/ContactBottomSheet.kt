package com.example.dimsumproject.ui.home

import android.content.Context
import android.os.Bundle
import android.widget.ArrayAdapter
import com.example.dimsumproject.data.api.Contact
import com.example.dimsumproject.databinding.BottomSheetContactBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ContactBottomSheet(
    context: Context,
    private val mode: Mode,
    private val contact: Contact? = null,
    private val onSubmit: (String, String, String) -> Unit
) : BottomSheetDialog(context) {

    private lateinit var binding: BottomSheetContactBinding
    private lateinit var spinnerAdapter: ArrayAdapter<String>

    enum class Mode {
        ADD, EDIT
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = BottomSheetContactBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inisialisasi adapter sebelum setup view
        setupSpinnerAdapter()
        setupView()
        setupAction()
    }

    private fun setupSpinnerAdapter() {
        // Setup contact type spinner
        val contactTypes = arrayOf("IG", "WA", "FB", "X", "LI", "EMAIL", "PHONE")
        spinnerAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, contactTypes).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.spinnerContactType.adapter = spinnerAdapter
    }

    private fun setupView() {
        binding.apply {
            tvTitle.text = if (mode == Mode.ADD) "Add Contact" else "Edit Contact"
            btnSubmit.text = if (mode == Mode.ADD) "Add" else "Save Changes"

            if (mode == Mode.EDIT) {
                contact?.let {
                    spinnerContactType.isEnabled = false
                    // Set selected contact type
                    val position = spinnerAdapter.getPosition(it.contact_type)
                    spinnerContactType.setSelection(position)

                    etContactValue.setText(it.contact_value)
                    etNotes.setText(it.notes)
                }
            }
        }
    }

    private fun setupAction() {
        binding.apply {
            btnClose.setOnClickListener { dismiss() }

            btnSubmit.setOnClickListener {
                val type = spinnerContactType.selectedItem.toString()
                val value = etContactValue.text.toString()
                val notes = etNotes.text.toString()

                if (value.isEmpty()) {
                    etContactValue.error = "Please enter contact value"
                    return@setOnClickListener
                }

                showConfirmationDialog {
                    onSubmit(type, value, notes)
                    dismiss()
                }
            }
        }
    }

    private fun showConfirmationDialog(onConfirm: () -> Unit) {
        MaterialAlertDialogBuilder(context)
            .setTitle("Confirmation")
            .setMessage("Are you sure you want to ${if (mode == Mode.ADD) "add" else "update"} this contact?")
            .setPositiveButton("Yes") { _, _ -> onConfirm() }
            .setNegativeButton("No", null)
            .show()
    }
}