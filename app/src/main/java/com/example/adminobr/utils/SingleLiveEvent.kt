package com.example.adminobr.utils

import androidx.annotation.MainThread
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Clase para emitir eventos únicos desde un LiveData.
 * Garantiza que los observadores reciban solo un evento,
 * incluso si el LiveData tiene múltiples observadores o es configurado varias veces.
 */
class SingleLiveEvent<T> : MutableLiveData<T>() {

    private val pending = AtomicBoolean(false)

    @MainThread
    override fun observe(owner: LifecycleOwner, observer: Observer<in T>) {
        super.observe(owner) { t ->
            if (pending.compareAndSet(true, false)) {
                observer.onChanged(t)
            }
        }
    }

    @MainThread
    override fun setValue(t: T?) {
        pending.set(true)
        super.setValue(t)
    }

    /**
     * Llama al evento sin enviar ningún dato.
     */
    @MainThread
    fun call() {
        value = null
    }
}
