package com.example.adminobr.ui.partediario

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.Spannable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.text

import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels

import com.example.adminobr.R
import com.example.adminobr.data.ParteDiario
import com.example.adminobr.databinding.FragmentParteDiarioBinding
import com.example.adminobr.viewmodel.AppDataViewModel
import com.example.adminobr.utils.Constants
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import androidx.lifecycle.lifecycleScope

import com.example.adminobr.data.Equipo
import com.example.adminobr.data.Obra
import com.example.adminobr.ui.adapter.ParteDiarioAdapter
import com.example.adminobr.utils.AppUtils
import com.example.adminobr.utils.AutocompleteManager
import com.example.adminobr.utils.NetworkErrorCallback
import com.example.adminobr.utils.NetworkStatusHelper
import com.example.adminobr.utils.SessionManager
import com.example.adminobr.utils.SharedPreferencesHelper
import com.example.adminobr.viewmodel.ParteDiarioViewModel
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray

class ParteDiarioFragment : Fragment(), NetworkErrorCallback {

    private val BASE_URL = Constants.getBaseUrl() //"http://adminobr.site/"

    // Helper para verificar el estado de la conexión de red
    private lateinit var networkHelper: NetworkStatusHelper

    private var _binding: FragmentParteDiarioBinding? = null
    private val binding get() = _binding!! // Binding para acceder a los elementos del layout

    // Layout para mostrar errores de conexión de red
    private lateinit var networkErrorLayout: View

    private lateinit var autocompleteManager: AutocompleteManager
    private val appDataViewModel: AppDataViewModel by activityViewModels()

    private val viewModel: ParteDiarioViewModel by viewModels()
    private val client = OkHttpClient()

    private var selectedEquipo: Equipo? = null
    private var selectedObra: Obra? = null

    // Variables de UI
    private lateinit var fechaEditText: TextInputEditText
    private lateinit var parteDiarioIdTextView: EditText
    private lateinit var equipoAutocomplete: AutoCompleteTextView
    private lateinit var horasInicioEditText: EditText
    private lateinit var horasFinEditText: EditText
    private lateinit var horasTrabajadasEditText: EditText
    private lateinit var observacionesEditText: EditText
    private lateinit var mantenimientoTitleTextView: TextView
    private lateinit var mantenimientoContentLayout: LinearLayout
    private lateinit var combustibleTipoAutocomplete: AutoCompleteTextView
    private lateinit var combustibleCantEditText: EditText
    private lateinit var lubricanteMotorCantEditText: EditText
    private lateinit var lubricanteHidraulicoCantEditText: EditText
    private lateinit var lubricanteOtroCantEditText: EditText
    private lateinit var engraseCheckBox: CheckBox
    private lateinit var filtroAireCheckBox: CheckBox
    private lateinit var filtroAceiteCheckBox: CheckBox
    private lateinit var filtroCombustibleCheckBox: CheckBox

    private lateinit var filtroOtroCheckBox: CheckBox

    private lateinit var obraAutocomplete: AutoCompleteTextView

    private lateinit var guardarButton: Button
    private lateinit var sessionManager: SessionManager

    // Adaptador para el RecyclerView
    private lateinit var adapter: ParteDiarioAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inicializa el SessionManager con el contexto del fragmento
        sessionManager = SessionManager(requireContext())
        networkHelper = NetworkStatusHelper(requireContext())
        networkHelper.networkErrorCallback = this // Asignamos el callback

        _binding = FragmentParteDiarioBinding.inflate(inflater, container, false) // Mover esta línea aquí

        // Obtén empresaDbName desde el SessionManager
        val empresaDbName = sessionManager.getEmpresaData()?.db_name ?: ""
        adapter = ParteDiarioAdapter(requireContext(), sessionManager, viewModel, empresaDbName)
        binding.listaPartesDiariosRecyclerView.adapter = adapter

        fechaEditText = binding.fechaEditText
        parteDiarioIdTextView = binding.parteDiarioIdTextView
        equipoAutocomplete = binding.equipoAutocomplete
        horasInicioEditText = binding.horasInicioEditText
        horasFinEditText = binding.horasFinEditText
        horasTrabajadasEditText = binding.horasTrabajadasEditText

        // Obtén referencias al TextView del título y al LinearLayout del contenido
        mantenimientoTitleTextView = binding.mantenimientoTitleTextView
        mantenimientoContentLayout = binding.mantenimientoContentLayout
        lubricanteOtroCantEditText = binding.lubricanteOtroCantEditText

        combustibleTipoAutocomplete = binding.combustibleTipoAutocomplete
        combustibleCantEditText = binding.combustibleCantEditText
        lubricanteMotorCantEditText = binding.lubricanteMotorCantEditText
        lubricanteHidraulicoCantEditText = binding.lubricanteHidraulicoCantEditText
        engraseCheckBox = binding.engraseCheckBox
        filtroAireCheckBox = binding.filtroAireCheckBox
        filtroAceiteCheckBox = binding.filtroAceiteCheckBox
        filtroCombustibleCheckBox = binding.filtroCombustibleCheckBox
        filtroOtroCheckBox = binding.filtroOtroCheckBox

        observacionesEditText = binding.observacionesEditText

        obraAutocomplete = binding.obraAutocomplete
        guardarButton = binding.guardarButton

        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userId = sessionManager.getUserId()
        Log.d("ParteDiarioFragment", "User ID: $userId")
        binding.listaPartesDiariosRecyclerView.adapter = adapter

        // Obtener los últimos partes diarios al ingresar al Fragment
        viewModel.getUltimosPartesDiarios(userId).observe(viewLifecycleOwner) { partesDiarios ->
            adapter.submitList(partesDiarios)
        }

        // Inicializar AutocompleteManager
        autocompleteManager = AutocompleteManager(requireContext(), appDataViewModel)

        // Cargar equipos y capturar el objeto Equipo seleccionado
        autocompleteManager.loadEquipos(
            equipoAutocomplete,
            this
        ) { equipo ->
            fetchUltimoParteDiario(equipo.interno)

            obraAutocomplete.requestFocus()
            selectedEquipo = equipo // Guardar equipo seleccionado
        }

        // Cargar equipos y capturar el objeto Equipo seleccionado
        autocompleteManager.loadObras(
            obraAutocomplete,
            this
        ) { obra ->
            Log.d("ParteDiarioFragment", "Obra selecionada: $obra")

            if (horasInicioEditText.text.isNullOrEmpty()) {
                horasInicioEditText.requestFocus()
            } else {
                horasFinEditText.requestFocus()
            }

            selectedObra = obra // Guardar equipo seleccionado
        }

        // Configurar AutoCompleteTextView para combustible
        val combustibleItems = arrayOf("Diesel", "Nafta")
        val combustibleAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, combustibleItems)
        combustibleTipoAutocomplete.setAdapter(combustibleAdapter)
        val combustibleSeleccionado = binding.combustibleTipoAutocomplete.text.toString()

        val combustibleCant = combustibleCantEditText.text.toString().toIntOrNull() ?: 0
        val lubricanteMotorCant = lubricanteMotorCantEditText.text.toString().toIntOrNull() ?: 0
        val lubricanteHidraulicoCant = lubricanteHidraulicoCantEditText.text.toString().toIntOrNull() ?: 0
        val lubricanteOtroCant = lubricanteOtroCantEditText.text.toString().toIntOrNull() ?: 0
        val engraseGeneral = engraseCheckBox.isChecked
        val filtroAire = binding.filtroAireCheckBox.isChecked
        val filtroAceite = binding.filtroAceiteCheckBox.isChecked
        val filtroCombustible = binding.filtroCombustibleCheckBox.isChecked
        val filtroOtro = binding.filtroOtroCheckBox.isChecked


        // Verificar el tipo de conexión
//        if (networkHelper.isWifiConnected()) {
//            Snackbar.make(binding.root, "Conectado por Wi-Fi", Snackbar.LENGTH_SHORT).show()
//        } else {
//            Snackbar.make(binding.root, "Conectado por Datos Moviles", Snackbar.LENGTH_SHORT).show()
//        }

        // Inicializar networkErrorLayout
        networkErrorLayout = view.findViewById(R.id.networkErrorLayout) // Usar view.findViewById

        // Capturar el layout incluido
        val networkErrorLayout = view.findViewById<ConstraintLayout>(R.id.networkErrorLayout)

        // Capturar el botón retry_button dentro de networkErrorLayout
        val retryButton = networkErrorLayout.findViewById<Button>(R.id.retry_button)

        // Asegúrate de que binding se haya inicializado antes de acceder a networkErrorView
        networkErrorLayout.visibility = View.GONE // O View.VISIBLE si quieres que se muestre inicialmente

        // Configurar el listener para el botón de retry_button
        retryButton.setOnClickListener {
            manageNetworkErrorLayout()
        }

        // Llamar a la función para convertir el texto a mayúsculas
        setEditTextToUppercase(equipoAutocomplete)
        setEditTextToUppercase(obraAutocomplete)

        // Otras configuraciones del fragmento
        setupRecyclerView()
        setupFab()
        setupTextWatchers()
        setupListeners()
        //actualizarHistorialPartes()
        observeViewModels()

        networkHelper.registerNetworkCallback()  // Registrar cuando la actividad esté visible
        // Verificar el estado de la red, para mostrar el layout de errores
        manageNetworkErrorLayout()
    }

    @SuppressLint("DefaultLocale")
    private fun setupListeners() {
        val userId = sessionManager.getUserId()

        // Configurar el listener para el ícono del calendario
        fechaEditText.setOnClickListener {
            //showDatePickerDialog()

            AppUtils.showDatePickerDialog(requireContext(), fechaEditText) { _, selectedYear, selectedMonth, selectedDay ->
                val formattedDate = String.format("%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear)
                fechaEditText.setText(formattedDate)
                equipoAutocomplete.requestFocus() // Mantener el enfoque en equipoAutocomplete
            }
        }

        // Configura un OnClickListener para el TextView del título
        mantenimientoTitleTextView.setOnClickListener {
            // Alterna la visibilidad del contenido
            if (mantenimientoContentLayout.visibility == View.VISIBLE) {
                mantenimientoContentLayout.visibility = View.GONE
                // Cambiar el icono a flecha abajo
            } else {
                mantenimientoContentLayout.visibility = View.VISIBLE
                // Cambiar el icono a flecha arriba
            }
        }

        guardarButton.setOnClickListener {

            guardarParteDiario()

            // Actualizar la lista después de guardar
            viewModel.getUltimosPartesDiarios(userId).observe(viewLifecycleOwner) { partesDiarios ->
                adapter.submitList(partesDiarios) // Actualizar la lista del adaptador
            }
        }

        binding.ultimoParteLayout.setOnClickListener {
            horasInicioEditText.setText(ultimoParteHorasFin)
            horasInicioEditText.requestFocus() // Opcional: enfocar el campo
            // Puedes agregar más acciones aquí si es necesario
        }
    }

    private fun showClearHistorialDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Borrar Historial")
        builder.setMessage("¿Estás seguro de que quieres borrar el historial de partes?")

        val positiveButtonText = SpannableString("Borrar")
        val colorRojo = ContextCompat.getColor(requireContext(), R.color.danger_500)
        positiveButtonText.setSpan(
            ForegroundColorSpan(colorRojo),
            0,
            positiveButtonText.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        builder.setPositiveButton(positiveButtonText) { dialog, _ ->
            SharedPreferencesHelper.clearPartesList(requireContext())
            //actualizarHistorialPartes()
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
        builder.create().show()
    }

    private fun guardarParteDiario() {
        // Forzar pérdida de foco de los EditText
        horasTrabajadasEditText.clearFocus()
        horasInicioEditText.clearFocus()
        horasFinEditText.clearFocus()

        // Ocultar el teclado usando AppUtils
        AppUtils.closeKeyboard(requireActivity(), view)

        // Extensión para convertir String a Editable
        fun String.toEditable(): Editable = Editable.Factory.getInstance().newEditable(this)

        val userId = sessionManager.getUserId()

        if (validarCampos()) {

            //val selectedEquipoText = equipoAutocomplete.text.toString()
            // Obtener el equipo seleccionado
            val equipoInterno = selectedEquipo!!.interno //selectedEquipoText.split(" - ").firstOrNull() ?: ""
            val selectedEquipo = appDataViewModel.equipos.value?.find { it.interno.trim().equals(equipoInterno.trim(), ignoreCase = true) }

            //val selectedObraText = obraAutocomplete.text.toString()
            val obraCentroCosto = selectedObra!!.centro_costo //selectedObraText.split(" - ").firstOrNull() ?: ""
            val selectedObra = appDataViewModel.obras.value?.find { it.centro_costo.trim().equals(obraCentroCosto.trim(), ignoreCase = true) }

            val parteDiario = ParteDiario(
                fecha = fechaEditText.text.toString(),
                equipoId = selectedEquipo?.id ?: 0,
                equipoInterno = equipoInterno,
                horasInicio = horasInicioEditText.text.toString().toInt(),
                horasFin = horasFinEditText.text.toString().toInt(),
                horasTrabajadas = horasTrabajadasEditText.text.toString().toInt(),
                observaciones = observacionesEditText.text.toString(),
                obraId = selectedObra?.id ?: 0,
                userCreated = userId,
                estadoId = 1,

                combustible_tipo = combustibleTipoAutocomplete.text.toString(),
                combustible_cant = combustibleCantEditText.text.toString().toIntOrNull() ?: 0,
                aceite_motor_cant = lubricanteMotorCantEditText.text.toString().toIntOrNull() ?: 0,
                aceite_hidra_cant = lubricanteHidraulicoCantEditText.text.toString().toIntOrNull() ?: 0,
                aceite_otro_cant = lubricanteOtroCantEditText.text.toString().toIntOrNull() ?: 0,
                engrase_general = engraseCheckBox.isChecked,
                filtro_aire = binding.filtroAireCheckBox.isChecked,
                filtro_aceite = binding.filtroAceiteCheckBox.isChecked,
                filtro_comb = binding.filtroCombustibleCheckBox.isChecked,
                filtro_otro = binding.filtroOtroCheckBox.isChecked,

            )
            Log.d("ParteDiarioFragment", "ParteDiario: ${parteDiario.fecha}")

            viewModel.guardarParteDiario(parteDiario) { success, nuevoId ->
                if (success) {
                    deshabilitarFormulario()
                    val fab = requireActivity().findViewById<FloatingActionButton>(R.id.fab)
                    fab.visibility = View.VISIBLE
                    binding.ultimoParteLayout.visibility = View.GONE

                    nuevoId?.let {
                        binding.parteDiarioIdTextView.text = it.toString().toEditable()
                    }

                    // Obtener los últimos partes diarios y actualizar la lista
                    viewModel.getUltimosPartesDiarios(userId).observe(viewLifecycleOwner) { partesDiarios ->
                        adapter.submitList(partesDiarios)
                    }
                } else {
                    Toast.makeText(requireContext(), "Error al guardar el parte diario", Toast.LENGTH_SHORT).show()
                }
            }

            viewModel.mensaje.observe(viewLifecycleOwner) { event ->
                event.getContentIfNotHandled()?.let { mensaje ->
                    Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupTextWatchers() {
        // TextWatcher para calcular horas trabajadas
        val horasTextWatcher = object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                // Calcular horas trabajadas
                // calcularHorasTrabajadas()
            }
        }

        // Configurar TextWatchers para los campos de horas
        horasInicioEditText.addTextChangedListener(horasTextWatcher)
        horasFinEditText.addTextChangedListener(horasTextWatcher)
        horasTrabajadasEditText.addTextChangedListener(horasTextWatcher)

        // Otros TextWatchers para los campos requeridos
        addTextWatcher(binding.fechaTextInputLayout, "Campo requerido")
        addTextWatcher(binding.equipoTextInputLayout, "Campo requerido")
        addTextWatcher(binding.obraTextInputLayout, "Campo requerido")
        addTextWatcher(binding.horasInicioTextInputLayout, "Campo requerido")
        addTextWatcher(binding.horasFinTextInputLayout, "Campo requerido")
        //addTextWatcher(binding.observacionesTextInputLayout, "Campo requerido")

        horasInicioEditText.onFocusChangeListener = View.OnFocusChangeListener { view, hasFocus ->
            if (!hasFocus) {
                calcularHorasTrabajadas(view.id) // Pasar el ID del campo como argumento
            }
        }

        horasFinEditText.onFocusChangeListener = View.OnFocusChangeListener { view, hasFocus ->
            if (!hasFocus) {
                calcularHorasTrabajadas(view.id) // Pasar el ID del campo como argumento
            }
        }

        horasTrabajadasEditText.onFocusChangeListener = View.OnFocusChangeListener { view, hasFocus ->
            if (!hasFocus) {
                calcularHorasTrabajadas(view.id) // Pasar el ID del campo como argumento
            }
        }
    }

    private fun observeViewModels() {
        viewModel.mensaje.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { mensaje ->
                Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupFab() {
        val fab: FloatingActionButton = requireActivity().findViewById(R.id.fab)
        fab.visibility = View.GONE
        fab.setImageResource(R.drawable.ic_add)
        fab.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.colorPrimary))
        fab.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.white)))

        fab.setOnClickListener {
            limpiarFormulario()
            fab.visibility = View.GONE
        }
    }

    private fun setupRecyclerView() {
        // Inicializar el RecyclerView y el adapter
        val equipos = appDataViewModel.equipos.value ?: emptyList() // Obtener la lista de equipos
        binding.listaPartesDiariosRecyclerView.adapter = adapter
//        binding.listaPartesDiariosRecyclerView.layoutManager = LinearLayoutManager(requireContext())

    }

    private fun fetchUltimoParteDiario(equipo: String) {
        Log.d("ParteDiarioFragment", "Equipo seleccionado: $equipo")

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Obtén empresaDbName desde el SessionManager
                val empresaDbName = sessionManager.getEmpresaData()?.db_name ?: ""
                //adapter = ParteDiarioAdapter(requireContext(), sessionManager, viewModel, empresaDbName) // Pasa sessionManager, viewModel y empresaDbName aquí

                // Crea el objeto JSON para la solicitud
                val jsonBody = JSONObject().apply {
                    put("equipo", equipo)
                    put("empresaDbName", empresaDbName)
                }

                // Define el MediaType para JSON
                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val requestBody = jsonBody.toString().toRequestBody(mediaType)

                // Crea la solicitud POST con JSON
                val request = Request.Builder()
                    .url("$BASE_URL${Constants.PartesDiarios.GET_ULTIMO_PARTE}")
                    .post(requestBody)  // POST con JSON
                    .build()

                Log.d("ParteDiarioFragment", "Base URL: $BASE_URL${Constants.PartesDiarios.GET_ULTIMO_PARTE}")

                // Ejecutar la solicitud
                val resultado = withContext(Dispatchers.IO) {
                    val response = client.newCall(request).execute()
                    response.body?.string()  // Obtener respuesta como cadena
                }

                // Manejar la respuesta
                if (resultado.isNullOrEmpty() || resultado == "null") {
                    Log.d("ParteDiarioFragment", "No se encontraron partes para el equipo seleccionado")
                    binding.ultimoParteLayout.visibility = View.GONE
                } else {
                    Log.d("ParteDiarioFragment", "Respuesta del servidor: $resultado")

                    // Procesar el JSON de la respuesta como JSONArray
                    val jsonArray = JSONArray(resultado)

                    // Obtener el primer elemento del array (si existe)
                    if (jsonArray.length() > 0) {
                        val jsonObject = jsonArray.getJSONObject(0)

                        // Extraer valores
                        val fecha = jsonObject.getString("fecha")
                        val equipo = jsonObject.getString("interno")
                        val horasInicio = jsonObject.getString("horas_inicio")
                        val horasFin = jsonObject.getString("horas_fin")

                        // Mostrar los datos en la interfaz
                        mostrarUltimoParte(fecha, equipo, horasInicio, horasFin)
                    } else {
                        // No se encontraron partes
                        binding.ultimoParteLayout.visibility = View.GONE
                    }
                }

            } catch (e: IOException) {
                Log.e("ParteDiarioFragment", "Error de red: ${e.message}")
                Toast.makeText(requireContext(), "Error de red: ${e.message}", Toast.LENGTH_SHORT).show()
            } catch (e: JSONException) {
                Log.e("ParteDiarioFragment", "Error al procesar JSON: ${e.message}")
                Toast.makeText(requireContext(), "Error al procesar datos: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private var ultimoParteHorasFin: String? = null // Variable para guardar horasFin

    private fun mostrarUltimoParte(fecha: String, equipo: String, horasInicio: String, horasFin: String) {
        binding.ultimoParteLayout.visibility = View.VISIBLE
        binding.ultimoParteFechaTextView.text = "Fecha: $fecha"
        binding.ultimoParteEquipoTextView.text = "Equipo: $equipo"
        binding.ultimoParteHorasInicioTextView.text = "Horas Inicio: $horasInicio"
        binding.ultimoParteHorasFinTextView.text = "Horas Fin: $horasFin"

        // Guardar horasFin en la variable
        ultimoParteHorasFin = horasFin

        // Convertir horasInicio a Editable
        //binding.horasInicioEditText.text = Editable.Factory.getInstance().newEditable(horasFin)
    }

    private fun setEditTextToUppercase(editText: AutoCompleteTextView) {
        editText.filters = arrayOf(InputFilter.AllCaps())
    }

    private fun limpiarFormulario() {
        fechaEditText.text?.clear()
        parteDiarioIdTextView.text?.clear()
        equipoAutocomplete.text?.clear()
        obraAutocomplete.text?.clear()
        horasInicioEditText.text?.clear()
        horasFinEditText.text?.clear()
        horasTrabajadasEditText.text?.clear()
        observacionesEditText.text?.clear()
        combustibleTipoAutocomplete.text?.clear()
        combustibleCantEditText.text?.clear()
        lubricanteMotorCantEditText.text?.clear()
        lubricanteHidraulicoCantEditText.text?.clear()
        lubricanteOtroCantEditText.text?.clear()
        engraseCheckBox.isChecked = false
        filtroAireCheckBox.isChecked = false
        filtroAceiteCheckBox.isChecked = false
        filtroCombustibleCheckBox.isChecked = false
        filtroOtroCheckBox.isChecked = false

        habilitarFormulario()

    }

    private fun habilitarFormulario() {
        fechaEditText.isEnabled = true
        equipoAutocomplete.isEnabled = true
        horasInicioEditText.isEnabled = true
        horasFinEditText.isEnabled = true
        horasTrabajadasEditText.isEnabled = true
        observacionesEditText.isEnabled = true

        mantenimientoContentLayout.isEnabled = true
        mantenimientoContentLayout.visibility = View.GONE

        obraAutocomplete.isEnabled = true
        guardarButton.isEnabled = true
        // Habilita el ícono del calendario
        binding.fechaTextInputLayout.isEndIconVisible = true
    }

    private fun deshabilitarFormulario() {
        fechaEditText.isEnabled = false
        equipoAutocomplete.isEnabled = false
        horasInicioEditText.isEnabled = false
        horasFinEditText.isEnabled = false
        horasTrabajadasEditText.isEnabled = false
        observacionesEditText.isEnabled = false
        mantenimientoContentLayout.isEnabled = false

        obraAutocomplete.isEnabled = false
        guardarButton.isEnabled = false
        // Deshabilita el ícono del calendario
        binding.fechaTextInputLayout.isEndIconVisible = false
    }

    /**
     * Función que gestiona el layout de errores de red y recarga componentes si la red está disponible.
     */
    override fun manageNetworkErrorLayout() {
        if (networkHelper.isNetworkAvailable()) {
            networkErrorLayout.visibility = View.GONE
            reloadComponents() // Recargar componentes que dependen de la red
            binding.guardarButton.isEnabled = true
        } else {
            // Verificar si el layout de error está visible
            if (networkErrorLayout.visibility == View.VISIBLE) {
                // Cambiar el texto del TextView en el layout de error
                val textViewError = networkErrorLayout.findViewById<TextView>(R.id.textViewError)
                textViewError?.text = "Aún no hay conexión a internet" // Cambiar el texto
            }
            // Muestra el layout de error de red
            networkErrorLayout.visibility = View.VISIBLE
            binding.guardarButton.isEnabled = false
        }
    }

    /**
     * Método que se ejecuta al destruir la actividad. Desregistra los callbacks para evitar fugas de memoria.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        // Desregistrar cuando la actividad deje de ser visible
        networkHelper.unregisterNetworkCallback()

        // Limpiar el binding
        _binding = null
    }

    private fun reloadComponents() {
        // Recargar componentes que dependen de la red
    }

    private fun validarCampos(): Boolean {
        var camposValidos = true

        if (fechaEditText.text.isNullOrEmpty()) {
            binding.fechaTextInputLayout.error = "Campo requerido"
            binding.fechaTextInputLayout.isErrorEnabled = true
            camposValidos = false
        } else {
            binding.fechaTextInputLayout.isErrorEnabled = false
        }

        if (equipoAutocomplete.text.isNotEmpty()) {
            if (selectedEquipo == null) {
                val equipoName = binding.equipoAutocomplete.text.toString()
                selectedEquipo = autocompleteManager.getEquipoByName(equipoName)
                if (selectedEquipo == null) {
                    binding.equipoTextInputLayout.error = "Seleccione un equipo válido"
                    binding.equipoTextInputLayout.isErrorEnabled = true
                    camposValidos = false
                } else {
                    binding.equipoTextInputLayout.isErrorEnabled = false
                }
            }
        } else {
            binding.equipoTextInputLayout.error = "Campo requerido"
            binding.equipoTextInputLayout.isErrorEnabled = true
            camposValidos = false
        }

        if (horasInicioEditText.text.isNullOrEmpty()) {
            binding.horasInicioTextInputLayout.error = "Campo requerido"
            binding.horasInicioTextInputLayout.isErrorEnabled = true
            camposValidos = false
        } else {
            binding.horasInicioTextInputLayout.isErrorEnabled =false
        }

        if (horasFinEditText.text.isNullOrEmpty()) {
            binding.horasFinTextInputLayout.error = "Campo requerido"
            binding.horasFinTextInputLayout.isErrorEnabled = true
            camposValidos = false
        } else {
            binding.horasFinTextInputLayout.isErrorEnabled = false
        }

        if (obraAutocomplete.text.isNotEmpty()) {
            if (selectedObra == null) {
                val obraName = obraAutocomplete.text.toString()
                selectedObra = autocompleteManager.getObraByName(obraName)
                if (selectedEquipo == null) {
                    binding.obraTextInputLayout.error = "Seleccione una obra válida"
                    binding.obraTextInputLayout.isErrorEnabled = true
                    camposValidos = false
                } else {
                    binding.obraTextInputLayout.isErrorEnabled = false
                }
            }
        } else {
            binding.obraTextInputLayout.error = "Campo requerido"
            binding.obraTextInputLayout.isErrorEnabled = true
            camposValidos = false
        }


        // Validación de observaciones para "Otro"
        val isOtroLubricanteNotEmpty = lubricanteOtroCantEditText.text.toString().isNotEmpty() && observacionesEditText.text.toString().isNotEmpty()
        val isOtroFiltroChecked = filtroOtroCheckBox.isChecked

        if (isOtroLubricanteNotEmpty || isOtroFiltroChecked) {
            if (observacionesEditText.text.isNullOrEmpty()) {
                binding.observacionesTextInputLayout.error = "Por favor, describe el tipo de lubricante (Otro) o filtro (Otro) usado en el mantenimiento."
                binding.observacionesTextInputLayout.isErrorEnabled = true
                camposValidos = false
            } else {
                binding.observacionesTextInputLayout.isErrorEnabled = false
            }
        } else {
            binding.observacionesTextInputLayout.isErrorEnabled = false // Limpia el error si no se selecciona "Otro"
        }


        if (!camposValidos) {
            Toast.makeText(requireContext(), "Por favor, complete todos los campos requeridos", Toast.LENGTH_SHORT).show()
            // O puedes usar un AlertDialog para mostrar la advertencia
        }

        return camposValidos
    }

    @SuppressLint("SetTextI18n")

    private fun calcularHorasTrabajadas(fieldId: Int) {
        when (fieldId) {
            R.id.horasInicioEditText -> {
                if (horasInicioEditText.text.isNotEmpty() && horasFinEditText.text.isNotEmpty()) {
                    val horasInicio = horasInicioEditText.text.toString().toDouble()
                    val horasFin = horasFinEditText.text.toString().toDouble()
                    val horasTrabajadas = (horasFin - horasInicio).toInt() // Convertir a entero
                    horasTrabajadasEditText.setText(horasTrabajadas.toString()) // Mostrar sin coma decimal
                } else {
                    horasTrabajadasEditText.setText("")
                }
            }
            R.id.horasFinEditText -> {
                if (horasInicioEditText.text.isNotEmpty() && horasFinEditText.text.isNotEmpty()) {
                    val horasInicio = horasInicioEditText.text.toString().toDouble()
                    val horasFin = horasFinEditText.text.toString().toDouble()
                    val horasTrabajadas = (horasFin - horasInicio).toInt() // Convertir a entero
                    horasTrabajadasEditText.setText(horasTrabajadas.toString()) // Mostrar sin coma decimal
                } else {
                    horasTrabajadasEditText.setText("")
                }
            }
            R.id.horasTrabajadasEditText -> {
                if (horasInicioEditText.text.isNotEmpty() && horasTrabajadasEditText.text.isNotEmpty()) {
                    val horasInicio = horasInicioEditText.text.toString().toDouble()
                    val horastrabajadas = horasTrabajadasEditText.text.toString().toDouble()
                    val horasFin = (horasInicio + horastrabajadas).toInt() // Convertir a entero
                    horasFinEditText.setText(horasFin.toString()) // Mostrar sin coma decimal
                } else {
                    horasFinEditText.setText("")
                }
            }
        }
    }

    private fun addTextWatcher(textInputLayout: TextInputLayout, errorMessage: String) {
        val editText = textInputLayout.editText
        editText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Implementación vacía o tu código aquí
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Implementación vacía o tu código aquí
            }

            override fun afterTextChanged(s: Editable?) {
                if (editText == equipoAutocomplete) {
                    selectedEquipo = null // Limpiar selectedEquipo solo si es el AutoCompleteTextView de equipos
                } else if (editText == obraAutocomplete) {
                    selectedObra = null // Limpiar selectedObra solo si es el AutoCompleteTextView de obras
                }

                if (textInputLayout.isErrorEnabled) {
                    if (s.isNullOrEmpty()) {
                        textInputLayout.error = errorMessage
                    } else {
                        textInputLayout.isErrorEnabled = false
                    }
                }
            }
        })
    }
}

