package com.example.adminobr.ui.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.example.adminobr.R

class ProgressDialogFragment : DialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.ProgressDialogStyle)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_progress_dialog, container, false)
    }

    companion object {
        const val TAG = "ProgressDialogFragment"

        fun show(fragmentManager: FragmentManager): ProgressDialogFragment {
            val progressDialogFragment = ProgressDialogFragment()
            progressDialogFragment.show(fragmentManager, TAG)
            return progressDialogFragment
        }fun dismiss(fragmentManager: FragmentManager) {
            val progressDialogFragment = fragmentManager.findFragmentByTag(TAG) as ProgressDialogFragment?
            progressDialogFragment?.dismiss()
        }
    }
}
