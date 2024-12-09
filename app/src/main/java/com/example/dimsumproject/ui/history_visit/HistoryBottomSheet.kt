package com.example.dimsumproject.ui.history_visit

import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.dimsumproject.R
import com.example.dimsumproject.data.api.HistoryItem
import com.example.dimsumproject.databinding.BottomSheetHistoryDetailBinding
import com.example.dimsumproject.ui.home.ContactsAdapter
import com.google.android.material.bottomsheet.BottomSheetDialog

class HistoryBottomSheet(
    context: Context,
    private val historyItem: HistoryItem
) : BottomSheetDialog(context) {

    private lateinit var binding: BottomSheetHistoryDetailBinding
    private lateinit var contactsAdapter: ContactsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = BottomSheetHistoryDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set rounded corners
        window?.let {
            // Set background transparent to see rounded corners
            it.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            // Set background untuk bottom sheet
            binding.root.background = ContextCompat.getDrawable(
                context,
                R.drawable.bottom_sheet_background
            )
        }

        setupUI()
        loadData()
    }

    private fun setupUI() {
        binding.apply {
            // Setup RecyclerView dengan GridLayoutManager
            contactsAdapter = ContactsAdapter(emptyList())
            rvContacts.apply {
                layoutManager = GridLayoutManager(context, 2).apply {
                    spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                        override fun getSpanSize(position: Int): Int {
                            // Jika hanya 1 item, gunakan full width
                            return if (contactsAdapter.itemCount <= 1) 2 else 1
                        }
                    }
                }
                adapter = contactsAdapter
                // Tambahkan spacing
                addItemDecoration(object : RecyclerView.ItemDecoration() {
                    override fun getItemOffsets(
                        outRect: Rect,
                        view: View,
                        parent: RecyclerView,
                        state: RecyclerView.State
                    ) {
                        val position = parent.getChildAdapterPosition(view)
                        val spanCount = 2
                        val spacing = 16 // 16dp spacing

                        val column = position % spanCount

                        outRect.left = column * spacing / spanCount
                        outRect.right = spacing - (column + 1) * spacing / spanCount

                        if (position >= spanCount) {
                            outRect.top = spacing
                        }
                    }
                })
            }

            // Setup close buttons
            btnClose.setOnClickListener { dismiss() }
            btnCloseSheet.setOnClickListener { dismiss() }
        }
    }

    private fun loadData() {
        binding.apply {
            skeletonLoading.root.visibility = View.VISIBLE

            Handler(Looper.getMainLooper()).postDelayed({
                skeletonLoading.root.visibility = View.GONE

                tvName.text = historyItem.profile.name

                Glide.with(context)
                    .load(historyItem.profile.profile_picture)
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .error(R.drawable.ic_profile_placeholder)
                    .circleCrop()
                    .into(profileImageView)

                contactsAdapter.updateContacts(historyItem.contacts)
            }, 1000)
        }
    }
}

