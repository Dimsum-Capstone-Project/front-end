package com.example.dimsumproject.ui.settings

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import com.example.dimsumproject.R
import com.example.dimsumproject.data.api.ProfileResponse
import com.example.dimsumproject.databinding.BottomSheetEditProfileBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class EditProfileBottomSheet(
    context: Context,
    private val currentProfile: ProfileResponse,
    private val onSave: (String, String, String, String) -> Unit
) : BottomSheetDialog(context) {

    private lateinit var binding: BottomSheetEditProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = BottomSheetEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupActions()
    }

    private fun setupUI() {
        // Auto-fill existing data
        binding.apply {
            usernameInput.setText(currentProfile.username)
            bioInput.setText(currentProfile.bio)
            companyInput.setText(currentProfile.company)
            jobTitleInput.setText(currentProfile.job_title)
        }
    }

    private fun setupActions() {
        binding.apply {
            closeButton.setOnClickListener { dismiss() }

            saveButton.setOnClickListener {
                val username = usernameInput.text.toString()
                val bio = bioInput.text.toString()
                val company = companyInput.text.toString()
                val jobTitle = jobTitleInput.text.toString()

                if (validateInputs(username, bio, company, jobTitle)) {
                    showConfirmationDialog(username, bio, company, jobTitle)
                }
            }
        }
    }

    private fun validateInputs(
        username: String,
        bio: String,
        company: String,
        jobTitle: String
    ): Boolean {
        if (username.isEmpty() || bio.isEmpty() || company.isEmpty() || jobTitle.isEmpty()) {
            Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun showConfirmationDialog(
        username: String,
        bio: String,
        company: String,
        jobTitle: String
    ) {
        val dialog = MaterialAlertDialogBuilder(context)
            .setView(R.layout.dialog_confirmation)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()

        dialog.findViewById<MaterialButton>(R.id.confirmButton)?.setOnClickListener {
            onSave(username, bio, company, jobTitle)
            dialog.dismiss()
            dismiss()
        }

        dialog.findViewById<MaterialButton>(R.id.cancelButton)?.setOnClickListener {
            dialog.dismiss()
        }
    }
}