package com.example.adminobr.utils

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.app.Activity
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.adminobr.R

object FeedbackVisualUtils {

    // Cambiar el color principal y la barra de estado con animación para éxito
    fun mostrarFeedbackVisualSuccess(activity: Activity, vararg buttons: View?) {
        val nuevoColor = ContextCompat.getColor(activity, R.color.colorSuccess)
        cambiarColorConAnimacion(activity, nuevoColor, alpha = 0.5f, *buttons)
    }

    // Cambiar el color principal y la barra de estado con animación para error
    fun mostrarFeedbackVisualError(activity: Activity, vararg buttons: View?) {
        val nuevoColor = ContextCompat.getColor(activity, R.color.colorDanger)
        cambiarColorConAnimacion(activity, nuevoColor, alpha = 0.5f, *buttons)
    }

    // Restaurar el color original con animación
    fun restaurarColorOriginal(activity: Activity, vararg buttons: View?) {
        val originalColor = ContextCompat.getColor(activity, R.color.colorPrimary)
        cambiarColorConAnimacion(activity, originalColor, alpha = 1.0f, *buttons)
    }

    // Mostrar feedback visual temporal (éxito)
    fun mostrarFeedbackVisualSuccessTemp(
        activity: Activity,
        delay: Long = 3000L,
        vararg buttons: View?
    ) {
        mostrarFeedbackVisualSuccess(activity, *buttons)

        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            restaurarColorOriginal(activity, *buttons)
        }, delay)
    }

    // Mostrar feedback visual temporal (error)
    fun mostrarFeedbackVisualErrorTemp(
        activity: Activity,
        delay: Long = 3000L,
        vararg buttons: View?
    ) {
        mostrarFeedbackVisualError(activity, *buttons)

        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            restaurarColorOriginal(activity, *buttons)
        }, delay)
    }

    // Función interna para animar el cambio de color y ajustar la opacidad
    private fun cambiarColorConAnimacion(activity: Activity, color: Int, alpha: Float, vararg buttons: View?) {
        val originalColor = ContextCompat.getColor(activity, R.color.colorPrimary)

        // Animar el color de la barra de estado
        animarCambioDeColor(
            currentColor = activity.window.statusBarColor,
            targetColor = color,
            onUpdate = { animColor -> activity.window.statusBarColor = animColor }
        )

        // Animar el color del ActionBar
        (activity as? AppCompatActivity)?.supportActionBar?.let { actionBar ->
            val actionBarColor = (actionBar.customView?.background as? ColorDrawable)?.color
                ?: ContextCompat.getColor(activity, R.color.colorPrimary)

            animarCambioDeColor(
                currentColor = actionBarColor,
                targetColor = color,
                onUpdate = { animColor -> actionBar.setBackgroundDrawable(ColorDrawable(animColor)) }
            )
        }

        // Animar el color y opacidad de los botones proporcionados
        buttons.forEach { button ->
            if (button is Button) {
                animarCambioDeColor(
                    currentColor = (button.background as? ColorDrawable)?.color ?: originalColor,
                    targetColor = color,
                    onUpdate = { animColor -> button.setBackgroundColor(animColor) }
                )
                button.alpha = alpha // Ajustar la opacidad
                button.setTextColor(ContextCompat.getColor(activity, R.color.colorWhite)) // Contraste
            }
        }
    }

    // Función reutilizable para animar cambios de color
    private fun animarCambioDeColor(currentColor: Int, targetColor: Int, onUpdate: (Int) -> Unit) {
        ObjectAnimator.ofObject(
            ArgbEvaluator(),
            currentColor,
            targetColor
        ).apply {
            duration = 300L // Duración de la animación (0.3 segundos)
            addUpdateListener { animator -> onUpdate(animator.animatedValue as Int) }
            start()
        }
    }
}
