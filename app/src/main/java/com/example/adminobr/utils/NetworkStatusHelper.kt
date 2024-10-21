package com.example.adminobr.utils

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar


/**
 * NetworkStatusHelper: Monitorea el estado de la conexión de red y notifica a los callbacks.
 *
 * Esta clase proporciona una forma sencilla de monitorear el estado de la conexión de red
 * y realizar acciones en función de los cambios de conectividad. También permite registrar
 * un callback para recibir notificaciones sobre cambios en el estado de la red.
 *
 * **Uso en Activities:**
 *
 * 1. Crea una instancia de NetworkStatusHelper en tu Activity:
 *    val networkHelper = NetworkStatusHelper(this)
 * 2. Registra el callback de red en `onStart()` y desregístralo en `onStop()`:
 *    override fun onStart() {
 *        super.onStart()
 *        networkHelper.registerNetworkCallback()
 *    }
 *    override fun onStop() {
 *        super.onStop()
 *        networkHelper.unregisterNetworkCallback()
 *    }
 * 3. Implementa la interfaz NetworkErrorCallback y asigna la instancia de tu Activity a la propiedad
 *    networkErrorCallback de NetworkStatusHelper:
 *    class MyActivity : AppCompatActivity(), NetworkErrorCallback {
 *        // ...
 *        override fun manageNetworkErrorLayout() {
 *            // Implementación de la función para manejar el layout de error de red
 *        }
 *        // ...
 *    }
 *    // En onCreate() o en otro lugar apropiado:
 *    networkHelper.networkErrorCallback = this
 * 4. Verifica la disponibilidad de la red usando `isNetworkAvailable()`:
 *    // ... (código para verificar la disponibilidad de la red)
 *
 * **Uso en Fragments:**
 *
 * 1. Crea una instancia de NetworkStatusHelper en tu Fragment:
 *    val networkHelper = NetworkStatusHelper(requireContext())
 * 2. Registra el callback de red en `onViewCreated()` y desregístralo en `onDestroyView()`:
 *    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
 *        super.onViewCreated(view, savedInstanceState)
 *        networkHelper.registerNetworkCallback()
 *    }
 *    override fun onDestroyView() {
 *        super.onDestroyView()
 *        networkHelper.unregisterNetworkCallback()
 *    }
 * 3. Implementa la interfaz NetworkErrorCallback y asigna la instancia de tu Fragment a la propiedad
 *    networkErrorCallback de NetworkStatusHelper:
 *    class MyFragment : Fragment(), NetworkErrorCallback {
 *        // ...
 *        override fun manageNetworkErrorLayout() {
 *            // Implementación de la función para manejar el layout de error de red
 *        }
 *        // ...
 *    }
 *    // En onViewCreated() o en otro lugar apropiado:
 *    networkHelper.networkErrorCallback = this
 * 4. Verifica la disponibilidad de la red usando `isNetworkAvailable()`:
 *    // ... (código para verificar la disponibilidad de la red)
 *
 * **Funciones adicionales:**
 *
 * - `isWifiConnected()`: Verifica si la conexión actual es a través de Wi-Fi.
 * - `isMobileDataConnected()`: Verifica si la conexión actual es a través de datos móviles.
 *
 * **Notas:**
 *
 * - El callback de red activará `onAvailable()` cuando la red esté conectada y `onLost()` cuando se pierda la conexión.
 * - La función `manageNetworkErrorLayout()` en tu Activity o Fragment se llamará cuando la red se conecte o se pierda.
 * - Puedes personalizar el mensaje de error y la acción de reintentar en la función `showNetworkLostWarning()`.
 */

class NetworkStatusHelper(private val context: Context) {

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private var snackbar: Snackbar? = null
    var networkErrorCallback: NetworkErrorCallback? = null

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            (context as? Activity)?.runOnUiThread {

                networkErrorCallback?.manageNetworkErrorLayout()

                // Solo muestra el Toast cuando la red se restablezca
//                if (isWifiConnected()) {
//                    Toast.makeText(context, "Conectado por Wi-Fi", Toast.LENGTH_SHORT).show()
//                } else if (isMobileDataConnected()) {
//                    Toast.makeText(context, "Conectado por Datos Móviles", Toast.LENGTH_SHORT).show()
//                }
            }
        }

        override fun onLost(network: Network) {
            (context as? Activity)?.runOnUiThread {
                //Toast.makeText(context, "Se ha perdido la conexión a Internet", Toast.LENGTH_SHORT).show()
                networkErrorCallback?.manageNetworkErrorLayout()

                // Oculta el teclado
                AppUtils.closeKeyboard(context)

                // Opcionalmente, puedes quitar el foco de la vista actual
                (context as? Activity)?.currentFocus?.clearFocus()
            }
        }

//        override fun onUnavailable() { // Corregido: onUnavailable() sin argumentos
//            (context as? Activity)?.runOnUiThread {
//                Toast.makeText(context, "Se ha perdido la conexión a Internet", Toast.LENGTH_SHORT).show()
//            }
//        }
    }

    /**
     * Registra el callback de red para recibir notificaciones sobre cambios en la conectividad.
     */
    fun registerNetworkCallback() {
        connectivityManager.registerNetworkCallback(
            NetworkRequest.Builder().build(),
            networkCallback
        )
    }

    /**
     * Desregistra el callback de red para evitar fugas de memoria.
     */
    fun unregisterNetworkCallback() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }

    /**
     * Verifica si hay una conexión a internet disponible.
     * @return true si hay conexión a internet, false en caso contrario.
     */
    fun isNetworkAvailable(): Boolean {
        val network = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
        return networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ?: false
    }

    /**
     * Verifica si la conexión actual es a través de Wi-Fi.
     * @return true si está conectado a Wi-Fi, false en caso contrario.
     */
    fun isWifiConnected(): Boolean {
        val network = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
        return networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ?: false
    }

    /**
     * Verifica si la conexión actual es a través de datos móviles.
     * @return true si está conectado a datos móviles, false en caso contrario.
     */
    fun isMobileDataConnected(): Boolean {
        val network = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
        return networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ?: false
    }

    private fun showNetworkLostWarning() {
        val activity = context as? Activity ?: return
        val view = activity.findViewById<View>(android.R.id.content)

        // Muestra un Snackbar con una acción para recargar
        snackbar = Snackbar.make(view, "No hay conexión a internet", Snackbar.LENGTH_INDEFINITE)
            .setAction("Reintentar") {
                if (isNetworkAvailable()) {
                    reload()
                } else {
                    showNetworkLostWarning()
                }
            }
        snackbar?.show()
    }

    // Recarga la actividad o fragmento si hay conexión
    private fun reload() {
        when (context) {
            is Activity -> context.recreate()
            is Fragment -> {
                context.activity?.supportFragmentManager?.beginTransaction()
                    ?.detach(context)?.attach(context)?.commitAllowingStateLoss()
            }
        }
    }

}
