package com.example.adminobr.ui.partediario

import android.annotation.SuppressLint
import android.app.Activity
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.adminobr.R
import com.example.adminobr.application.MyApplication
import com.example.adminobr.data.Estado
import com.example.adminobr.data.Obra
import com.example.adminobr.data.Equipo
import com.example.adminobr.data.ParteDiario
import com.example.adminobr.databinding.FragmentParteDiarioFormBinding
import com.example.adminobr.utils.AppUtils
import com.example.adminobr.utils.AutocompleteManager
import com.example.adminobr.utils.Constants
import com.example.adminobr.utils.SessionManager
import com.example.adminobr.viewmodel.AppDataViewModel
import com.example.adminobr.viewmodel.ParteDiarioViewModel
import com.example.adminobr.viewmodel.ParteDiarioViewModelFactory
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputLayout
import com.example.adminobr.ui.adapter.ParteDiarioFormAdapter
import com.example.adminobr.utils.FeedbackVisualUtils
import com.example.adminobr.utils.NetworkStatusHelper
import kotlinx.coroutines.launch
import kotlin.text.toIntOrNull

class ParteDiarioFormFragment : Fragment() {
    private val viewModel: ParteDiarioViewModel by viewModels {
        ParteDiarioViewModelFactory(requireActivity().application)
    }

    // Adaptador para el RecyclerView
    private lateinit var adapter: ParteDiarioFormAdapter

    private lateinit var autocompleteManager: AutocompleteManager
    private val appDataViewModel: AppDataViewModel by activityViewModels()

    private var _binding: FragmentParteDiarioFormBinding? = null
    private val binding get() = _binding!! // Binding para acceder a los elementos del layout

    private val _parte = MutableLiveData<ParteDiario?>()
    private val parte: LiveData<ParteDiario?> = _parte

    // Otras variables
    private var idParteDiario: Int? = null
    private var selectedEquipo: Equipo? = null
    private var selectedObra: Obra? = null
    private var selectedEstado: Estado? = null
    private var selectedCombustible: String? = null
    private var editParteMode = false
    private var editType: EditType = EditType.EDIT_ALL

    private lateinit var equipoAutocomplete: AutoCompleteTextView
    private lateinit var obraAutocomplete: AutoCompleteTextView
    private lateinit var estadoAutocomplete: AutoCompleteTextView
    private lateinit var combustibleTipoAutocomplete: AutoCompleteTextView

    private lateinit var sessionManager: SessionManager
    private var userId: Int = -1 // Inicializa con un valor predeterminado
    private var previousConnectionState: Boolean? = null
    private var isNetworkCheckEnabled = Constants.getNetworkStatusHelper()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentParteDiarioFormBinding.inflate(inflater, container, false)
        sessionManager = SessionManager(requireContext())

        // Inicializar userId después de que sessionManager esté inicializado
        userId = sessionManager.getUserId()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Observa el estado de la red y ejecuta una acción específica en reconexión
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                NetworkStatusHelper.networkAvailable
                    .collect { isConnected ->
                        if (isNetworkCheckEnabled) {
                            if (previousConnectionState == false && isConnected) {
//                                Log.d("ParteDiarioFormFragment", "Conexión restaurada, recargando datos...")
                                // Realiza una acción específica al recuperar conexión

                                reloadData()
                            }
                            previousConnectionState = isConnected
                        }
                    }
            }
        }

        equipoAutocomplete = binding.equipoAutocomplete
        obraAutocomplete = binding.obraAutocomplete
        estadoAutocomplete = binding.estadoAutocomplete
        combustibleTipoAutocomplete = binding.combustibleTipoAutocomplete

        // Inicializar AutocompleteManager
        autocompleteManager = AutocompleteManager(requireContext(), appDataViewModel)

        // Cargar datos de Autocomplete
        loadAllAutocompleteData()

        // Obtener el modo de edición del argumento
        editParteMode = arguments?.getBoolean("editParteMode", false) ?: false

        // Obtener el modo de edición del argumento (EDIT_ALL u otro)
        editType = arguments?.getString("editType")?.let { EditType.valueOf(it) } ?: EditType.EDIT_ALL

        if (!editParteMode && binding.run {
                !fechaEditText.text.isNullOrEmpty() ||
                !equipoAutocomplete.text.isNullOrEmpty() ||
                !parteDiarioIdTextView.text.isNullOrEmpty()
            }) {
            limpiarFormulario()
        }

        // Configura el título y visibilidad de los campos basados en `editType`
        setFormTitleAndVisibility()
        setupFab()
        observeViewModels()
        setupTextWatchers()
        setupListeners()

        // Llamar a la función para convertir el texto a mayúsculas
        setAutocompleteToUppercase(equipoAutocomplete)
        setAutocompleteToUppercase(obraAutocomplete)
//        setEditTextToUppercase(combustibleTipoAutocomplete)
//        setEditTextToUppercase(estadoAutocomplete)

        // Configura el RecyclerView para los partes diarios
        setupRecyclerView()
        cargarUltimosPartesPorUsuario()

        // Cargar el parte si es un modo de edición, independientemente del tipo de edición
        val parteDiarioId = arguments?.getInt("parteDiarioId", -1) ?: -1
        if (parteDiarioId != -1) {
            cargarDatosParte(parteDiarioId)
        }
    }

    private fun reloadData() {
//        Toast.makeText(requireContext(), "Conexión restaurada, recargando datos...", Toast.LENGTH_SHORT).show()

        // Acción que se ejecuta al reconectarse
        loadAllAutocompleteData()
    }

    private fun loadAllAutocompleteData() {
//        Toast.makeText(requireContext(), "Cargando datos...", Toast.LENGTH_SHORT).show()
        loadDataIfEmpty(equipoAutocomplete) {
            autocompleteManager.loadEquipos(
                equipoAutocomplete,
                this
            ) { equipo ->
                selectedEquipo = equipo
                selectedEquipo?.let {
                    Log.d("ParteDiarioFormFragment", "Equipo seleccionado: ${it.interno}")
                    viewModel.obtenerUltimoPartePorEquipo(it.id)
                }
            }
        }

        loadDataIfEmpty(obraAutocomplete) {
            autocompleteManager.loadObras(
                obraAutocomplete,
                this
            ) { obra ->
                selectedObra = obra
                Log.d("ParteDiarioFormFragment", "Obra seleccionada: $obra")
            }
        }

        loadDataIfEmpty(estadoAutocomplete) {
            autocompleteManager.loadEstados(
                estadoAutocomplete,
                this
            ) { estado ->
                selectedEstado = estado
                Log.d("ParteDiarioFormFragment", "Estado seleccionado: $estado")
            }
        }

        loadDataIfEmpty(combustibleTipoAutocomplete) {
            autocompleteManager.loadTipoCombustible(
                combustibleTipoAutocomplete,
                this
            ) { tipoCombustible ->
                selectedCombustible = tipoCombustible
                Log.d("ParteDiarioFormFragment", "Tipo de combustible seleccionado: $tipoCombustible")
            }
        }
    }

    /**
     * Verifica si el adaptador del AutoCompleteTextView está vacío antes de cargar los datos.
     *
     * @param autoCompleteTextView El AutoCompleteTextView a verificar.
     * @param loadFunction La función de carga que se ejecutará si el adaptador está vacío.
     */
    private fun loadDataIfEmpty(autoCompleteTextView: AutoCompleteTextView, loadFunction: () -> Unit) {
        val adapterCount = autoCompleteTextView.adapter?.count ?: 0
        if (adapterCount == 0) {
            loadFunction()
        }
    }


    private fun setupRecyclerView() {
        userId = sessionManager.getUserId()

        // Aquí creas el adaptador pasando el callback de edición
        adapter = ParteDiarioFormAdapter(viewModel, requireContext(), userId, childFragmentManager) { parteDiario ->
            // Callback que maneja la edición
            val bundle = bundleOf("parteDiarioId" to parteDiario.idParteDiario, "editType" to EditType.EDIT_ALL.name)
            view?.findNavController()?.navigate(R.id.action_nav_parteDiarioFormFragment_edit, bundle)
        }

        binding.listaPartesDiariosRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@ParteDiarioFormFragment.adapter
        }

        viewModel.ultimosPartes.observe(viewLifecycleOwner) { partes ->
            if (partes != null && partes.isNotEmpty()) {
                Log.d("ParteDiarioFormFragment", "Cargando ${partes.size} elementos en RecyclerView.")
                adapter.submitList(partes)
            } else {
                Log.d("ParteDiarioFormFragment", "No hay datos para mostrar en RecyclerView.")
                binding.emptyListMessage.visibility = View.VISIBLE // Mostrar mensaje si la lista está vacía
            }
        }
    }

    private fun cargarUltimosPartesPorUsuario() {
        if (!editParteMode) {
            viewModel.cargarUltimosPartesPorUsuario(userId)
        }
    }

    private fun setAutocompleteToUppercase(autocomplete: AutoCompleteTextView) {
        autocomplete.filters = arrayOf(InputFilter.AllCaps())
    }

    private fun setTextViewToLowercase(textView: TextView) {
        textView.filters = arrayOf(InputFilter { source, start, end, dest, dstart, dend ->
            source.toString().lowercase()
        })
    }

    private fun setTextViewToUppercase(textView: TextView) {
        textView.filters = arrayOf(InputFilter.AllCaps())
    }

    private fun setFormTitleAndVisibility() {
        if (editParteMode) {
            (activity as? AppCompatActivity)?.supportActionBar?.title = "Editar Parte Diario"
            binding.horizontalConstraintLayout.visibility = View.GONE
            binding.listaPartesDiariosRecyclerView.visibility = View.GONE
        } else {
            (activity as? AppCompatActivity)?.supportActionBar?.title = "Nuevo Parte Diario"
            binding.horizontalConstraintLayout.visibility = View.VISIBLE
            binding.listaPartesDiariosRecyclerView.visibility = View.VISIBLE
        }

        when (editType) {
//            EditType.EDIT_PROFILE -> {
//                (activity as? AppCompatActivity)?.supportActionBar?.title = "Editar Perfil"
//                configureFieldsForProfileEdit()
//            }
//            EditType.CHANGE_PASSWORD -> {
//                (activity as? AppCompatActivity)?.supportActionBar?.title = "Cambiar Contraseña"
//                configureFieldsForPasswordChange()
//            }
            EditType.EDIT_ALL -> {
                configureFieldsForFullEdit()
            }
        }
    }

    private fun configureFieldsForFullEdit() {
        binding.apply {
            // Mostrar todos los campos para crear o editar parte
        }
    }

    private fun actualizarUiConDatosDeParte(parte: ParteDiario) {
        Log.d("ParteDiarioFormFragment", "Actualizando UI con obraId: ${parte.obraId}")
        Log.d("ParteDiarioFormFragment", "Actualizando UI con equipoId: ${parte.equipoId}")
        binding.apply {

            // Configura los valores en la UI y usa valores predeterminados para evitar nulls
            parteDiarioIdTextView.setText(parte.idParteDiario.toString())
//            parteDiarioIdTextView.text = (parte.idParteDiario ?: 0).toString()

            fechaEditText.setText(parte.fecha ?: "")
            horasInicioEditText.setText((parte.horasInicio ?: 0).toString())
            horasFinEditText.setText((parte.horasFin ?: 0).toString())
            horasTrabajadasEditText.setText((parte.horasTrabajadas ?: 0).toString())
            observacionesEditText.setText(parte.observaciones ?: "")

            combustibleCantEditText.setText(if (parte.combustibleCant != null && parte.combustibleCant != 0) parte.combustibleCant.toString() else "")
            lubricanteMotorCantEditText.setText(if (parte.aceiteMotorCant != null && parte.aceiteMotorCant != 0) parte.aceiteMotorCant.toString() else "")
            lubricanteHidraulicoCantEditText.setText(if (parte.aceiteHidraCant != null && parte.aceiteHidraCant != 0) parte.aceiteHidraCant.toString() else "")
            lubricanteOtroCantEditText.setText(if (parte.aceiteOtroCant != null && parte.aceiteOtroCant != 0) parte.aceiteOtroCant.toString() else "")

            // Ajustar Checkboxes
            engraseCheckBox.isChecked = parte.engraseGeneral == 1  // Si es 1, isChecked será true
            filtroAireCheckBox.isChecked = parte.filtroAire == 1 // Si es 1, isChecked será true
            filtroAceiteCheckBox.isChecked = parte.filtroAceite == 1 // Si es 1, isChecked será true
            filtroCombustibleCheckBox.isChecked = parte.filtroComb == 1 // Si es 1, isChecked será true
            filtroOtroCheckBox.isChecked = parte.filtroOtro == 1 // Si es 1, isChecked será true

            // Configura el texto en `equipoAutocomplete` sin abrir el menú desplegable.
            selectedEquipo = autocompleteManager.getEquipoById(parte.equipoId ?: 0)
            equipoAutocomplete.setText(selectedEquipo?.toString() ?: "", false)
            equipoAutocomplete.dismissDropDown()

            // Configura el texto en `combustibleAutocomplete` sin abrir el menú desplegable.
            selectedCombustible = parte.combustibleTipo
            combustibleTipoAutocomplete.setText(selectedCombustible ?: "", false)
            combustibleTipoAutocomplete.dismissDropDown()

            // Configura el texto en `obraAutocomplete` sin abrir el menú desplegable.
            selectedObra = autocompleteManager.getObraById(parte.obraId ?: 0)
            obraAutocomplete.setText(selectedObra?.toString() ?: "", false)
            obraAutocomplete.dismissDropDown()

            // Configura el texto en `estadoAutocomplete` sin abrir el menú desplegable.
            selectedEstado = autocompleteManager.getEstadoById(parte.estadoId ?: 0)
            estadoAutocomplete.setText(selectedEstado?.toString() ?: "", false)
            estadoAutocomplete.dismissDropDown()
        }
    }

    private fun cargarDatosParte(parteDiarioId: Int) {
        viewModel.obtenerParteDiarioPorId(parteDiarioId)
        viewModel.partes.observe(viewLifecycleOwner) { partes ->
            if (!partes.isNullOrEmpty()) {
                actualizarUiConDatosDeParte(partes[0])
                partes[0].idParteDiario?.let { idParteDiario = it } // Asignación segura si el ID no es nulo
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

        combustibleTipoAutocomplete.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (s?.isEmpty() == true) {
                    combustibleTipoAutocomplete.dismissDropDown() // Cierra el dropdown
                    // Opcional: Limpiar la selección programáticamente si es necesario
                     combustibleTipoAutocomplete.setSelection(0)
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Configurar TextWatchers para los campos de horas
        binding.horasInicioEditText.addTextChangedListener(horasTextWatcher)
        binding.horasFinEditText.addTextChangedListener(horasTextWatcher)
        binding.horasTrabajadasEditText.addTextChangedListener(horasTextWatcher)

        // Otros TextWatchers para los campos requeridos
        addTextWatcher(binding.fechaTextInputLayout, "Campo requerido")
        addTextWatcher(binding.equipoTextInputLayout, "Campo requerido")
        addTextWatcher(binding.obraTextInputLayout, "Campo requerido")
        addTextWatcher(binding.horasInicioTextInputLayout, "Campo requerido")
        addTextWatcher(binding.horasFinTextInputLayout, "Campo requerido")
        //addTextWatcher(binding.observacionesTextInputLayout, "Campo requerido")

        binding.horasInicioEditText.onFocusChangeListener = View.OnFocusChangeListener { view, hasFocus ->
            if (!hasFocus) {
                calcularHorasTrabajadas(view.id) // Pasar el ID del campo como argumento
            }
        }

        binding.horasFinEditText.onFocusChangeListener = View.OnFocusChangeListener { view, hasFocus ->
            if (!hasFocus) {
                calcularHorasTrabajadas(view.id) // Pasar el ID del campo como argumento
            }
        }

        binding.horasTrabajadasEditText.onFocusChangeListener = View.OnFocusChangeListener { view, hasFocus ->
            if (!hasFocus) {
                calcularHorasTrabajadas(view.id) // Pasar el ID del campo como argumento
            }
        }
    }

    private fun calcularHorasTrabajadas(fieldId: Int) {
        val horasInicioText = binding.horasInicioEditText.text.toString()
        val horasFinText = binding.horasFinEditText.text.toString()
        val horasTrabajadasText = binding.horasTrabajadasEditText.text.toString()

        // Convertimos los valores existentes a Double, si son válidos.
        val horasInicio = horasInicioText.toDoubleOrNull()
        val horasFin = horasFinText.toDoubleOrNull()
        val horasTrabajadas = horasTrabajadasText.toDoubleOrNull()

        when (fieldId) {
            R.id.horasInicioEditText -> {
                if (horasInicio != null) {
                    when {
                        horasFin != null -> {
                            // Si se tienen horasInicio y horasFin, calcula horasTrabajadas
                            val calculo = horasFin - horasInicio
                            binding.horasTrabajadasEditText.setText(calculo.toInt().toString())
                        }
                        horasTrabajadas != null -> {
                            // Si se tienen horasInicio y horasTrabajadas, calcula horasFin
                            val calculo = horasInicio + horasTrabajadas
                            binding.horasFinEditText.setText(calculo.toInt().toString())
                        }
                    }
                }
            }

            R.id.horasFinEditText -> {
                if (horasFin != null) {
                    when {
                        horasInicio != null -> {
                            // Si se tienen horasInicio y horasFin, calcula horasTrabajadas
                            val calculo = horasFin - horasInicio
                            binding.horasTrabajadasEditText.setText(calculo.toInt().toString())
                        }
                        horasTrabajadas != null -> {
                            // Si se tienen horasFin y horasTrabajadas, calcula horasInicio
                            val calculo = horasFin - horasTrabajadas
                            binding.horasInicioEditText.setText(calculo.toInt().toString())
                        }
                    }
                }
            }

            R.id.horasTrabajadasEditText -> {
                if (horasTrabajadas != null) {
                    when {
                        horasInicio != null -> {
                            // Si se tienen horasInicio y horasTrabajadas, calcula horasFin
                            val calculo = horasInicio + horasTrabajadas
                            binding.horasFinEditText.setText(calculo.toInt().toString())
                        }
                        horasFin != null -> {
                            // Si se tienen horasFin y horasTrabajadas, calcula horasInicio
                            val calculo = horasFin - horasTrabajadas
                            binding.horasInicioEditText.setText(calculo.toInt().toString())
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("DefaultLocale")
    private fun setupListeners() {

        // Configurar el listener para el checkbox de otros filtros
        binding.filtroOtroCheckBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                Toast.makeText(
                    requireContext(),
                    "Describir el tipo de filtro (Otro)",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // Configurar el listener para el ícono del calendario
        binding.fechaEditText.setOnClickListener {
            AppUtils.showDatePickerDialog(requireContext(), binding.fechaEditText) { _, selectedYear, selectedMonth, selectedDay ->
                val formattedDate = String.format("%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear)
                binding.fechaEditText.setText(formattedDate)
                equipoAutocomplete.requestFocus() // Mantener el enfoque en equipoAutocomplete
            }
        }

        // Configura un OnClickListener para el TextView del título
        binding.mantenimientoTitleTextView.setOnClickListener {
            // Alterna la visibilidad del contenido
            if (binding.mantenimientoContentLayout.visibility == View.VISIBLE) {
                binding.mantenimientoContentLayout.visibility = View.GONE
                binding.mantenimientoTitleTextView.gravity = Gravity.CENTER // Cambiar la gravedad del texto
                // Cambiar el icono a flecha abajo
            } else {
                binding.mantenimientoContentLayout.visibility = View.VISIBLE
                binding.mantenimientoTitleTextView.gravity = Gravity.START // Cambiar la gravedad del texto
                // Cambiar el icono a flecha arriba
            }
        }

        // TextWatcher para combustibleTipoAutocomplete
        combustibleTipoAutocomplete.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                selectedCombustible = if (s.isNullOrEmpty()) "" else s.toString()
                Log.d("ParteDiarioFormFragment", "Combustible actualizado a: $selectedCombustible")
            }
        })

        binding.guardarButton.setOnClickListener {
            guardarParteDiario()
            binding.ultimoParteLayout.visibility = View.GONE
        }

        binding.ultimoParteLayout.setOnClickListener {
            binding.horasInicioEditText.setText(ultimoParteHorasFin)
            binding.horasInicioEditText.requestFocus() // Opcional: enfocar el campo
            // Puedes agregar más acciones aquí si es necesario
        }
    }

    private fun guardarParteDiario() {
        // Forzar pérdida de foco de los EditText
        binding.apply {
            horasInicioEditText.clearFocus()
            horasFinEditText.clearFocus()
            horasTrabajadasEditText.clearFocus()
        }

        // Ocultar el teclado usando AppUtils
        AppUtils.closeKeyboard(requireActivity(), view)

        // Extensión para convertir String a Editable
        fun String.toEditable(): Editable = Editable.Factory.getInstance().newEditable(this)

        val userId = sessionManager.getUserId()

        when (editType) {
//            EditType.OTRO -> {
//                val nuevaContrasena = binding.passwordEditText.text.toString()
//                if (nuevaContrasena != "") {
//                    val usuario = Usuario(
//                        id = userId
//                    )
//                    viewModel.actualizarContrasenaUsuario(usuario, binding.passwordEditText.text.toString())
//                    Toast.makeText(requireContext(), "Actualizando contraseña...", Toast.LENGTH_SHORT).show()
//                } else {
//                    Toast.makeText(requireContext(), "Contraseña vacía", Toast.LENGTH_SHORT).show()
//                }
//            }

            EditType.EDIT_ALL -> {
                if (validarCamposCompletos()) {

                    // Verificar si hay conexión a internet
                    if (!NetworkStatusHelper.isNetworkAvailable()) {
                        // Opcionalmente, puedes quitar el foco de la vista actual
                        AppUtils.clearFocus(requireContext())

                        Toast.makeText(requireContext(), "No hay conexión a internet", Toast.LENGTH_SHORT).show()
                        return
                    }

                    // Asignar "" a selectedCombustible si el campo está vacío antes de guardar
                    if (combustibleTipoAutocomplete.text.isNullOrEmpty()) {
                        selectedCombustible = ""
                    }
                    // Crear el objeto ParteDiario
                    val parte = ParteDiario(
                        idParteDiario = idParteDiario,
                        fecha = binding.fechaEditText.text.toString(),
                        equipoId = selectedEquipo?.id ?: 0,
                        obraId = selectedObra?.id ?: 0,
                        userCreated = userId,
                        userUpdated = userId,
                        horasInicio = binding.horasInicioEditText.text.toString().toInt(),
                        horasFin = binding.horasFinEditText.text.toString().toInt(),
                        horasTrabajadas = binding.horasTrabajadasEditText.text.toString().toInt(),
                        observaciones = binding.observacionesEditText.text.toString(),
                        combustibleTipo = selectedCombustible ?: "", // Asegura que el valor guardado sea "" si el campo está vacío
                        combustibleCant = binding.combustibleCantEditText.text.toString().toIntOrNull() ?: 0,
                        engraseGeneral = binding.engraseCheckBox.isChecked.toInt(),
                        aceiteMotorCant = binding.lubricanteMotorCantEditText.text.toString().toIntOrNull() ?: 0,
                        aceiteHidraCant = binding.lubricanteHidraulicoCantEditText.text.toString().toIntOrNull() ?: 0,
                        aceiteOtroCant = binding.lubricanteOtroCantEditText.text.toString().toIntOrNull() ?: 0,
                        filtroAire = binding.filtroAireCheckBox.isChecked.toInt(),
                        filtroAceite = binding.filtroAceiteCheckBox.isChecked.toInt(),
                        filtroComb = binding.filtroCombustibleCheckBox.isChecked.toInt(),
                        filtroOtro = binding.filtroOtroCheckBox.isChecked.toInt(),
                        estadoId = selectedEstado?.id ?: 1,
                        equipoInterno = (selectedEquipo?.interno ?: false).toString()
                    )

                    if (editParteMode) {
                        viewModel.actualizarParteDiario(parte) { success ->
                            if (success) {
                                FeedbackVisualUtils.mostrarFeedbackVisualSuccess(requireActivity(), binding.guardarButton)
                                deshabilitarFormulario()

                                // Mostrar el botón flotante
                                val fab = requireActivity().findViewById<FloatingActionButton>(R.id.fab)
                                fab.visibility = View.VISIBLE
                            } else {
                                FeedbackVisualUtils.mostrarFeedbackVisualError(requireActivity(), binding.guardarButton)
                                Toast.makeText(requireContext(), "Error al actualizar el parte", Toast.LENGTH_SHORT).show()
                            }
                        }
//                        Toast.makeText(requireContext(), "Actualizando Parte...", Toast.LENGTH_SHORT).show()
                    } else {
                        // Llamada a la función de creación con callback
                        viewModel.crearParteDiario(parte) { success, nuevoId ->
                            if (success) {
                                nuevoId?.let {
                                    // Actualizar el ID en la vista cuando se crea exitosamente
                                    binding.parteDiarioIdTextView.setText(it.toString())
                                }
                                cargarUltimosPartesPorUsuario() // Recargar lista tras guardar exitosamente
                                // Usar la función de feedback visual
                                FeedbackVisualUtils.mostrarFeedbackVisualSuccess(requireActivity(), binding.guardarButton) // Cambiar el color al guardar con éxito
                                deshabilitarFormulario()

                                // Mostrar el botón flotante
                                val fab = requireActivity().findViewById<FloatingActionButton>(R.id.fab)
                                fab.visibility = View.VISIBLE
                            } else {
                                Toast.makeText(
                                    requireContext(),
                                    "Error al crear parte",
                                    Toast.LENGTH_SHORT
                                ).show()
                                // Usar la función de feedback visual
                                FeedbackVisualUtils.mostrarFeedbackVisualError(requireActivity(), binding.guardarButton) // Cambiar el color al error
                            }
                        }
//                        Toast.makeText(requireContext(), "Creando parte...", Toast.LENGTH_SHORT).show()
                    }
                    (context as? Activity)?.currentFocus?.clearFocus()
                }
            }
        }
    }

    // Método que se ejecuta al destruir la actividad.
    override fun onDestroyView() {
        super.onDestroyView()

        // Restaurar el color original
        FeedbackVisualUtils.restaurarColorOriginal(requireActivity(), binding.guardarButton)

        // Limpiar el binding
        _binding = null
    }

    private fun validarCamposCompletos(): Boolean {
        var camposValidos = true
        binding.apply {

            if (fechaEditText.text.isNullOrEmpty()) {
                fechaTextInputLayout.error = "Campo requerido"
                fechaTextInputLayout.isErrorEnabled = true
                camposValidos = false
            } else {
                fechaTextInputLayout.isErrorEnabled = false
            }

            if (equipoAutocomplete.text.isNotEmpty()) {
                if (selectedEquipo == null) {
                    val equipoName = equipoAutocomplete.text.toString()
                    selectedEquipo = autocompleteManager.getEquipoByName(equipoName)
                    if (selectedEquipo == null) {
                        equipoTextInputLayout.error = "Seleccione un equipo válido"
                        equipoTextInputLayout.isErrorEnabled = true
                        camposValidos = false
                    } else {
                        equipoTextInputLayout.isErrorEnabled = false
                    }
                }
            } else {
                equipoTextInputLayout.error = "Campo requerido"
                equipoTextInputLayout.isErrorEnabled = true
                camposValidos = false
            }

            if (horasInicioEditText.text.isNullOrEmpty()) {
                horasInicioTextInputLayout.error = "Campo requerido"
                horasInicioTextInputLayout.isErrorEnabled = true
                camposValidos = false
            } else {
                horasInicioTextInputLayout.isErrorEnabled = false
            }

            if (horasFinEditText.text.isNullOrEmpty()) {
                horasFinTextInputLayout.error = "Campo requerido"
                horasFinTextInputLayout.isErrorEnabled = true
                camposValidos = false
            } else {
                horasFinTextInputLayout.isErrorEnabled = false
            }

            if (obraAutocomplete.text.isNotEmpty()) {
                if (selectedObra == null) {
                    val obraName = obraAutocomplete.text.toString()
                    selectedObra = autocompleteManager.getObraByName(obraName)
                    if (selectedEquipo == null) {
                        obraTextInputLayout.error = "Seleccione una obra válida"
                        obraTextInputLayout.isErrorEnabled = true
                        camposValidos = false
                    } else {
                        obraTextInputLayout.isErrorEnabled = false
                    }
                }
            } else {
                obraTextInputLayout.error = "Campo requerido"
                obraTextInputLayout.isErrorEnabled = true
                camposValidos = false
            }

            val isOtroLubricanteNotEmpty = lubricanteOtroCantEditText.text.toString().isNotEmpty()
            val isOtroFiltroChecked = filtroOtroCheckBox.isChecked

            if (isOtroLubricanteNotEmpty || isOtroFiltroChecked) {
                // Validación de "Observaciones" si alguno de los campos "Otro" está activo
                if (observacionesEditText.text.isNullOrEmpty()) {
                    binding.observacionesTextInputLayout.error =
                        "Por favor, describe el tipo de lubricante (Otro) o filtro (Otro) usado en el mantenimiento."
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
            }

            return camposValidos
        }
    }

    private fun setupFab() {
        val fab: FloatingActionButton? = activity?.findViewById(R.id.fab)
        fab?.apply {
            visibility = View.GONE
            setImageResource(R.drawable.ic_add)
            backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.colorPrimary))
            setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.colorWhite)))

            setOnClickListener {
                limpiarFormulario()
                visibility = View.GONE
            }
        }
    }

    private var ultimoParteHorasFin: String? = null // Variable para guardar horasFin

    @SuppressLint("SetTextI18n")
    private fun mostrarUltimoParte(ultimoParte : ParteDiario) {
        binding.apply {
            ultimoParteLayout.visibility = View.VISIBLE
            ultimoParteFechaTextView.text = "Fecha: ${ultimoParte.fecha}"
            ultimoParteEquipoTextView.text = "Equipo: ${ultimoParte.equipoInterno}"
            ultimoParteHorasInicioTextView.text = "Horas Inicio: ${ultimoParte.horasInicio}"
            ultimoParteHorasFinTextView.text = "Horas Fin: ${ultimoParte.horasFin}"
        }

        // Guardar horasFin en la variable
        ultimoParteHorasFin = ultimoParte.horasFin.toString()

        // Convertir horasInicio a Editable
        //binding.horasInicioEditText.text = Editable.Factory.getInstance().newEditable(horasFin)
    }

    private fun limpiarFormulario() {
        // Limpiar todos los campos de texto y autocompletado, y ocultar el último parte
        binding.apply {
            listOf(
                fechaEditText,
                parteDiarioIdTextView,
                equipoAutocomplete,
                obraAutocomplete,
                horasInicioEditText,
                horasFinEditText,
                horasTrabajadasEditText,
                observacionesEditText,
                combustibleTipoAutocomplete,
                combustibleCantEditText,
                lubricanteMotorCantEditText,
                lubricanteHidraulicoCantEditText,
                lubricanteOtroCantEditText,
                estadoAutocomplete
            ).forEach {
                it.text?.clear()
                it.error = null // Limpia cualquier mensaje de error en el campo
            }

            // Limpiar todos los CheckBox
            listOf(
                engraseCheckBox,
                filtroAireCheckBox,
                filtroAceiteCheckBox,
                filtroCombustibleCheckBox,
                filtroOtroCheckBox
            ).forEach { it.isChecked = false }

            // Ocultar el layout "último parte"
            ultimoParteLayout.visibility = View.GONE
        }
        habilitarFormulario()
    }

    private fun habilitarFormulario() {
        binding.apply {
            fechaTextInputLayout.isEnabled = true
            equipoTextInputLayout.isEnabled = true
            obraTextInputLayout.isEnabled = true
            horasInicioTextInputLayout.isEnabled = true
            horasFinTextInputLayout.isEnabled = true
            horasTrabajadasTextInputLayout.isEnabled = true
            observacionesTextInputLayout.isEnabled = true

            mantenimientoContentLayout.visibility = View.GONE
            mantenimientoCardView.isEnabled = false
            mantenimientoCardView.alpha = 1.0f // Restablece la opacidad al 100%
            mantenimientoTitleTextView.isClickable = true
            mantenimientoContentLayout.isEnabled = true
            combustibleTipoTextInputLayout.isEnabled = true
            combustibleCantTextInputLayout.isEnabled = true
            lubricanteMotorCantTextInputLayout.isEnabled = true
            lubricanteHidraulicoCantTextInputLayout.isEnabled = true
            lubricanteOtroCantTextInputLayout.isEnabled = true
            engraseCheckBox.isEnabled = true
            filtroAireCheckBox.isEnabled = true
            filtroAceiteCheckBox.isEnabled = true
            filtroCombustibleCheckBox.isEnabled = true
            filtroOtroCheckBox.isEnabled = true

            estadoTextInputLayout.isEnabled = true
            guardarButton.isEnabled = true
        }
    }

    private fun deshabilitarFormulario() {
        binding.apply {
            fechaTextInputLayout.isEnabled = false
            equipoTextInputLayout.isEnabled = false
            obraTextInputLayout.isEnabled = false
            horasInicioTextInputLayout.isEnabled = false
            horasFinTextInputLayout.isEnabled = false
            horasTrabajadasTextInputLayout.isEnabled = false
            observacionesTextInputLayout.isEnabled = false

            mantenimientoCardView.isEnabled = false
            mantenimientoCardView.alpha = 0.5f // Reduce la opacidad al 50%
            mantenimientoTitleTextView.isClickable = false
            mantenimientoContentLayout.isEnabled = false
            combustibleTipoTextInputLayout.isEnabled = false
            combustibleCantTextInputLayout.isEnabled = false
            lubricanteMotorCantTextInputLayout.isEnabled = false
            lubricanteHidraulicoCantTextInputLayout.isEnabled = false
            lubricanteOtroCantTextInputLayout.isEnabled = false
            engraseCheckBox.isEnabled = false
            filtroAireCheckBox.isEnabled = false
            filtroAceiteCheckBox.isEnabled = false
            filtroCombustibleCheckBox.isEnabled = false
            filtroOtroCheckBox.isEnabled = false

            estadoTextInputLayout.isEnabled = false
            guardarButton.isEnabled = false
        }
    }

    private fun observeViewModels() {

        // Aquí consumimos el mensaje usando getContentIfNotHandled()
        viewModel.errorMessage.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { mensaje ->
                Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.mensaje.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { mensaje ->
                Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.ultimoPartePorEquipo.observe(viewLifecycleOwner) { ultimoParte ->
            Log.d("ParteDiarioFormFragment", "Observando ultimoPartePorEquipo: $ultimoParte")
            if (ultimoParte != null) {
                mostrarUltimoParte(ultimoParte)
            } else {
                // Oculta el layout si no se encontró parte
                binding.ultimoParteLayout.visibility = View.GONE
            }
        }

        viewModel.ultimosPartes.observe(viewLifecycleOwner) { partes ->
            adapter.submitList(partes)
        }

        viewModel.recargarListaPartesPorUsuario.observe(viewLifecycleOwner) { recargar ->
            if (recargar) {
                cargarUltimosPartesPorUsuario()
                viewModel.resetRecargarListaPartesPorUsuario() // Para que no vuelva a activarse
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
                    // Oculta el layout si no se encontró parte
                    binding.ultimoParteLayout.visibility = View.GONE
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

private fun Boolean.toInt(): Int {
    return if (this) 1 else 0
}
