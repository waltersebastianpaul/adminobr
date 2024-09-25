package com.example.adminobr.ui.partediario

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
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
import android.view.inputmethod.InputMethodManager
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.compose.ui.semantics.text
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

import androidx.lifecycle.lifecycleScope

import com.example.adminobr.data.Equipo
import com.example.adminobr.data.Obra
import com.example.adminobr.ui.adapter.ParteDiarioAdapter
import com.example.adminobr.utils.AutocompleteManager
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

class ParteDiarioFragment : Fragment() {

    private val baseUrl = Constants.getBaseUrl() //"http://adminobr.site/"

    private var _binding: FragmentParteDiarioBinding? = null
    private val binding get() = _binding!! // Binding para acceder a los elementos del layout

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
        observacionesEditText = binding.observacionesEditText
        obraAutocomplete = binding.obraAutocomplete
        guardarButton = binding.guardarButton

        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userId = sessionManager.getUserId()
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

            Log.d("ListarPartesFragment", "Equipo selecionado: $equipo")

            cerrarTeclado()

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

    }

    private fun setupListeners() {
        val userId = sessionManager.getUserId()

        fechaEditText.setOnClickListener {
            showDatePickerDialog()
        }

        guardarButton.setOnClickListener {
            guardarParteDiario()

            // Actualizar la lista después de guardar
            viewModel.getUltimosPartesDiarios(userId).observe(viewLifecycleOwner) { partesDiarios ->
                adapter.submitList(partesDiarios) // Actualizar la lista del adaptador
            }
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
                estadoId = 1
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

    private fun showDatePickerDialog() {
        val locale = Locale.getDefault()
        val calendar = Calendar.getInstance(locale)
        val dateString = fechaEditText.text.toString()

        Log.d("DatePickerDialog", "Fecha actual en EditText: $dateString")

        if (dateString.isNotBlank()) {
            val formatter = SimpleDateFormat("dd/MM/yyyy", locale)
            val date = formatter.parse(dateString)

            Log.d("DatePickerDialog", "Fecha parseada: $date")

            date?.let {
                calendar.time = it
                Log.d("DatePickerDialog", "Calendario actualizado con fecha: ${calendar.time}")
            }
        }

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        Log.d("DatePickerDialog", "Año: $year, Mes: $month, Día: $day")

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                val formattedDate = String.format("%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear)

                Log.d("DatePickerDialog", "Fecha formateada: $formattedDate")

                fechaEditText.setText(formattedDate)
                equipoAutocomplete.requestFocus()
            },
            year,
            month,
            day
        )
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

    private fun setupTextWatchers() {
        // TextWatcher para calcular horas trabajadas
        val horasTextWatcher = object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                // Calcular horas trabajadas
                calcularHorasTrabajadas()
            }
        }

        // Configurar TextWatchers para los campos de horas
        horasInicioEditText.addTextChangedListener(horasTextWatcher)
        horasFinEditText.addTextChangedListener(horasTextWatcher)

        // Otros TextWatchers para los campos requeridos
        addTextWatcher(binding.fechaTextInputLayout, "Campo requerido")
        addTextWatcher(binding.equipoTextInputLayout, "Campo requerido")
        addTextWatcher(binding.horasInicioTextInputLayout, "Campo requerido")
        addTextWatcher(binding.horasFinTextInputLayout, "Campo requerido")
        addTextWatcher(binding.obraTextInputLayout, "Campo requerido")
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
                    .url("$baseUrl${Constants.PartesDiarios.GET_ULTIMO_PARTE}")
                    .post(requestBody)  // POST con JSON
                    .build()

                Log.d("ParteDiarioFragment", "Base URL: $$baseUrl${Constants.PartesDiarios.GET_ULTIMO_PARTE}")

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
        observacionesEditText.text?.clear()

        habilitarFormulario()

    }

    private fun habilitarFormulario() {
        fechaEditText.isEnabled = true
        equipoAutocomplete.isEnabled = true
        horasInicioEditText.isEnabled = true
        horasFinEditText.isEnabled = true
        observacionesEditText.isEnabled = true
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
        observacionesEditText.isEnabled = false
        obraAutocomplete.isEnabled = false
        guardarButton.isEnabled = false
        // Deshabilita el ícono del calendario
        binding.fechaTextInputLayout.isEndIconVisible = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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

        if (!camposValidos) {
            Toast.makeText(requireContext(), "Por favor, complete todos los campos requeridos", Toast.LENGTH_SHORT).show()
            // O puedes usar un AlertDialog para mostrar la advertencia
        }

        return camposValidos
    }

    @SuppressLint("SetTextI18n")
    private fun calcularHorasTrabajadas() {
        val horasInicioText = horasInicioEditText.text.toString()
        val horasFinText = horasFinEditText.text.toString()

        if (horasInicioText.isNotEmpty() && horasFinText.isNotEmpty()) {
            try {
                val horasInicio = horasInicioText.toDouble()
                val horasFin = horasFinText.toDouble()
                val horasTrabajadas = (horasFin - horasInicio).toInt() // Convertir a entero
                // Establecer el valor en horasTrabajadasEditText
                horasTrabajadasEditText.setText(horasTrabajadas.toString())

                // Verificar si es negativo y aplicar los cambios de color
                if (horasTrabajadas < 0) {
                    horasTrabajadasEditText.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorRed))
                } else {
                    // Restablecer a los colores predeterminados si no es negativo
                    horasTrabajadasEditText.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorDisabledText))
                }
            } catch (e: NumberFormatException) {
                // Manejar el caso en que los valores no sean números válidos
                horasTrabajadasEditText.setText("")}
        } else {
            // Si alguno de los campos está vacío, limpia horasTrabajadasEditText
            horasTrabajadasEditText.setText("")
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

    private fun hideKeyboard() {
        val inputMethodManager = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val view = requireActivity().currentFocus
        view?.let {
            inputMethodManager.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }

    private fun cerrarTeclado() {
        // Cerrar el teclado
        val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(requireView().windowToken, 0)
    }

}

