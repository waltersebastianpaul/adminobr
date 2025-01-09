package com.example.adminobr.utils

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.util.Log
import android.view.Window
import android.widget.TextView
import androidx.lifecycle.LifecycleCoroutineScope
import com.example.adminobr.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object LoadingDialogUtil {
    private var dialog: Dialog? = null

    fun showLoading(context: Context, message: String = "Cargando...") {
        if (dialog?.isShowing == true) {
            return
        }
        dialog = Dialog(context, android.R.style.Theme_Translucent_NoTitleBar)
        dialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog?.setCancelable(false)
        dialog?.setContentView(R.layout.dialog_loading_fullscreen)
        val messageTextView = dialog?.findViewById<TextView>(R.id.loadingMessage)
        messageTextView?.text = message
        dialog?.show()
    }

    fun hideLoading(lifecycleScope: LifecycleCoroutineScope, delayMillis: Long = 0) {
        lifecycleScope.launch {
            delay(delayMillis)
            try {
                if (dialog?.isShowing == true) {
                    if (dialog?.context is Activity) {
                        val activity = dialog?.context as Activity
                        if (!activity.isFinishing) {
                            dialog?.dismiss()
                            dialog = null
                        }
                    } else {
                        dialog?.dismiss()
                        dialog = null
                    }
                }
            } catch (e: IllegalArgumentException) {
                Log.e("LoadingDialogUtil", "Error al cerrar el diálogo de carga: ${e.message}")
                dialog = null
            }
        }
    }
}
