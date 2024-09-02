package com.example.adminobr.ui.partediario

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.adminobr.ui.adapter.ParteDiarioAdapter
import com.example.adminobr.utils.AutocompleteManager
import com.example.adminobr.utils.SessionManager
import com.example.adminobr.utils.SharedPreferencesHelper
import com.example.adminobr.viewmodel.ParteDiarioViewModel
import kotlinx.coroutines.launch
import okhttp3.FormBody
import org.json.JSONException
import org.json.JSONObject

import java.io.IOException

class ParteDiarioFragment : Fragment() {

    private val baseUrl = Constants.getBaseUrl() //"http://adminobr.site/"
    private var _binding: FragmentParteDiarioBinding? = null
    private val binding get() = _binding!! // Binding para acceder a los elementos del layout

    private lateinit var autocompleteManager: AutocompleteManager
    private val appDataViewModel: AppDataViewModel by activityViewModels()

    private val viewModel: ParteDiarioViewModel by viewModels()
    private val client = OkHttpClient()

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

        _binding = FragmentParteDiarioBinding.inflate(inflater, container, false)

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

        // Inicializar AutocompleteManager
        autocompleteManager = AutocompleteManager(requireContext(), appDataViewModel)

        // Configurar los AutoCompleteTextView con AutocompleteManager
        autocompleteManager.loadEquipos(binding.equipoAutocomplete, viewLifecycleOwner)
        autocompleteManager.loadObras(binding.obraAutocomplete, viewLifecycleOwner)

        // Otras configuraciones del fragmento
        setupRecyclerView()
        setupFab()
        setupTextWatchers()
        setupListeners()
        actualizarHistorialPartes()
        observeViewModels()

        setEditTextToUppercase(equipoAutocomplete)
        setEditTextToUppercase(obraAutocomplete)

    }

    private fun setupListeners() {
        fechaEditText.setOnClickListener {
            showDatePickerDialog()
        }

        equipoAutocomplete.setOnItemClickListener { _, _, _, _ ->
            val selectedEquipoText = equipoAutocomplete.text.toString()
            val selectedEquipo = selectedEquipoText.split(" - ").firstOrNull() ?: ""
            fetchUltimoParteDiario(selectedEquipo)
            obraAutocomplete.requestFocus()
        }

        obraAutocomplete.setOnItemClickListener { _, _, _, _ ->
            if (horasInicioEditText.text.isNullOrEmpty()) {
                horasInicioEditText.requestFocus()
            } else {
                horasFinEditText.requestFocus()
            }
        }

        equipoAutocomplete.setOnItemClickListener { _, _, _, _ ->
            val selectedEquipoText = equipoAutocomplete.text.toString()
            val selectedEquipo = selectedEquipoText.split(" - ").firstOrNull() ?: ""
            fetchUltimoParteDiario(selectedEquipo)
            obraAutocomplete.requestFocus()
        }

        // Manejar la selección de un equipo en el AutoCompleteTextView
        obraAutocomplete.setOnItemClickListener { _, _, _, _ ->
            if (horasInicioEditText.text.isNullOrEmpty()) {
                horasInicioEditText.requestFocus()
            }else{
                horasFinEditText.requestFocus()
            }
        }

        guardarButton.setOnClickListener {
            guardarParteDiario()
        }

        binding.clearHistorialPartesTextView.setOnClickListener {
            showClearHistorialDialog()
        }
    }

    private fun showClearHistorialDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Borrar Historial")
        builder.setMessage("¿Estás seguro de que quieres borrar el historial de partes?")

        val positiveButtonText = SpannableString("Borrar")
        val colorRojo = ContextCompat.getColor(requireContext(), R.color.colorAlert)
        positiveButtonText.setSpan(
            ForegroundColorSpan(colorRojo),
            0,
            positiveButtonText.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        builder.setPositiveButton(positiveButtonText) { dialog, _ ->
            SharedPreferencesHelper.clearPartesList(requireContext())
            actualizarHistorialPartes()
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
        builder.create().show()
    }

    private fun guardarParteDiario() {
        // Extensión para convertir String a Editable
        fun String.toEditable(): Editable = Editable.Factory.getInstance().newEditable(this)

        val selectedEquipoText = equipoAutocomplete.text.toString()
        val equipoInterno = selectedEquipoText.split(" - ").firstOrNull() ?: ""
        val selectedEquipo = appDataViewModel.equipos.value?.find { it.interno.trim().equals(equipoInterno.trim(), ignoreCase = true) }

        val selectedObraText = obraAutocomplete.text.toString()
        val obraCentroCosto = selectedObraText.split(" - ").firstOrNull() ?: ""
        val selectedObra = appDataViewModel.obras.value?.find { it.centro_costo.trim().equals(obraCentroCosto.trim(), ignoreCase = true) }

        //val userId = requireActivity().intent.getIntExtra("id", -1)
        val userId = sessionManager.getUserId()

        if (validarCampos()) {
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

            viewModel.guardarParteDiario(parteDiario) { success, nuevoId ->
                if (success) {
                    deshabilitarFormulario()
                    val fab = requireActivity().findViewById<FloatingActionButton>(R.id.fab)
                    fab.visibility = View.VISIBLE
                    binding.ultimoParteLayout.visibility = View.GONE

                    nuevoId?.let {
                        binding.parteDiarioIdTextView.text = it.toString().toEditable()
                    }

                    // Agregar el nuevo ParteDiario al adaptador
                    adapter.addParteDiario(parteDiario)
                    actualizarHistorialPartes()

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
        if (dateString.isNotBlank()) {
            val formatter = SimpleDateFormat("dd/MM/yyyy", locale)
            val date = formatter.parse(dateString)
            date?.let { calendar.time = it }
        }

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                val formattedDate = String.format("%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear)
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
        adapter = ParteDiarioAdapter(requireContext()) // Pasar la lista de equipos al adaptador
        binding.listaPartesDiariosRecyclerView.adapter = adapter
        binding.listaPartesDiariosRecyclerView.layoutManager = LinearLayoutManager(requireContext())

    }

    private fun fetchUltimoParteDiario(equipo: String) {
        Log.d("ParteDiarioFragment", "Equipo seleccionado: $equipo") // Log del equipo

        // Usa viewLifecycleOwner.lifecycleScope para lanzar la corutina
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Obtén empresaDbName desde el SessionManager
                val empresaDbName = sessionManager.getEmpresaData()?.db_name ?: return@launch

                // Crea el cuerpo de la solicitud POST
                val requestBody = FormBody.Builder()
                    .add("equipo", equipo)
                    .add("empresaDbName", empresaDbName)
                    .build()

                val request = Request.Builder()
                    .url("$baseUrl${Constants.PartesDiarios.GET_ULTIMO_PARTE}")
                    .post(requestBody)  // Usando POST en lugar de GET
                    .build()

//                val url = "$baseUrl${Constants.PartesDiarios.GET_ULTIMO_PARTE}?equipo=$equipo"
//                Log.d("ParteDiarioFragment", "Request URL: $url") // Log de la URL
//
//                val request = Request.Builder()
//                    .url(url)
//                    .build()

                val resultado = withContext(Dispatchers.IO) {
                    val response = client.newCall(request).execute()
                    response.body?.string() // Aquí se obtiene la respuesta
                }

                // Verificar si el resultado es nulo o si contiene la palabra "null"
                if (resultado.isNullOrEmpty() || resultado == "null") {
                    Log.d("ParteDiarioFragment", "No se encontraron partes para el equipo seleccionado")
                    binding.ultimoParteLayout.visibility = View.GONE
                } else {
                    // Procesa el resultado y actualiza la UI
                    resultado.let { responseText ->
                        Log.d("ParteDiarioFragment", "Respuesta del servidor: $responseText") // Log de la respuesta

                        // Convertir la respuesta en un objeto JSON
                        val jsonObject = JSONObject(responseText)

                        // Extraer los valores
                        val fecha = jsonObject.getString("fecha")
                        val equipo = jsonObject.getString("interno")
                        val horasInicio = jsonObject.getString("horas_inicio")
                        val horasFin = jsonObject.getString("horas_fin")

                        // Mostrar los datos en el UI
                        mostrarUltimoParte(fecha, equipo, horasInicio, horasFin)
                    }
                }

            } catch (e: IOException) {
                Log.e("ParteDiarioFragment", "Error dered: ${e.message}")
                // Mostrar mensaje de error de red al usuario
                Toast.makeText(requireContext(), "Error de red: ${e.message}", Toast.LENGTH_SHORT).show()
                // O puedes usar un SnackBar o un AlertDialog para mostrar el mensaje
            } catch (e: JSONException) {
                Log.e("ParteDiarioFragment", "Error al procesar JSON: ${e.message}")
                // Mostrar mensaje de error de parseo al usuario
                Toast.makeText(requireContext(), "Error al procesar datos: ${e.message}", Toast.LENGTH_SHORT).show()
                // O puedes usar un SnackBar o un AlertDialog para mostrar el mensaje
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

        if (equipoAutocomplete.text.isNullOrEmpty()) {
            binding.equipoTextInputLayout.error = "Campo requerido"
            binding.equipoTextInputLayout.isErrorEnabled = true
            camposValidos = false
        } else {
            // Verificar si el texto coincide con el interno de algún equipo
            val esEquipoValido = appDataViewModel.equipos.value?.any { equipo ->
                equipoAutocomplete.text.startsWith(equipo.interno) // Verifica si el texto coincide con el interno de algún equipo
            } ?: false

            if (!esEquipoValido) {
                binding.equipoTextInputLayout.error = "Equipo inválido"
                binding.equipoTextInputLayout.isErrorEnabled = true
                camposValidos = false
            } else {
                binding.equipoTextInputLayout.isErrorEnabled = false
            }
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

        if (obraAutocomplete.text.isNullOrEmpty()) {
            binding.obraTextInputLayout.error = "Campo requerido"
            binding.obraTextInputLayout.isErrorEnabled = true
            camposValidos = false
        } else {
            binding.obraTextInputLayout.isErrorEnabled = false
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

    // Actualizar historial de partes
    private fun actualizarHistorialPartes() {
        val newList = SharedPreferencesHelper.getPartesList(requireContext())
        adapter.submitList(newList)

        // Retrasar la actualización de la visibilidad del mensaje
        Handler(Looper.getMainLooper()).postDelayed({
            if (adapter.itemCount == 0) {
                binding.clearHistorialPartesTextView.visibility = View.GONE
                binding.emptyListMessage.visibility = View.VISIBLE
            } else {
                binding.clearHistorialPartesTextView.visibility = View.VISIBLE
                binding.emptyListMessage.visibility = View.GONE
            }
        }, 100) // Retrasar 100 milisegundos
    }

    private fun cerrarTeclado() {
        // Cerrar el teclado
        val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(requireView().windowToken, 0)
    }

}

