//package com.example.adminobr.viewmodel
//
//import android.net.Uri
//import androidx.lifecycle.ViewModel
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.StateFlow
//
//data class UpdateState(
//    val pendingInstallationUri: Uri? = null,
//    val isSnackbarVisible: Boolean = false
//)
//
//class UpdateStateViewModel : ViewModel() {
//
//    private val _updateState = MutableStateFlow(UpdateState())
//    val updateState: StateFlow<UpdateState> get() = _updateState
//
//    /**
//     * Establece el URI pendiente de instalación.
//     */
//    fun setPendingInstallation(uri: Uri?) {
//        _updateState.value = _updateState.value.copy(
//            pendingInstallationUri = uri,
//            isSnackbarVisible = uri != null
//        )
//    }
//
//    /**
//     * Marca que el Snackbar ya no está visible.
//     */
//    fun dismissSnackbar() {
//        _updateState.value = _updateState.value.copy(isSnackbarVisible = false)
//    }
//
//    /**
//     * Limpia el estado pendiente por completo.
//     */
//    fun clearPendingState() {
//        _updateState.value = UpdateState()
//    }
//}
