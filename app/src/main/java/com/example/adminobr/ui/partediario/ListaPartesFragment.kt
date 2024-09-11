package com.example.adminobr.ui.partediario

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.AutoCompleteTextView
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.adminobr.R
import com.example.adminobr.databinding.FragmentListaPartesBinding
import com.example.adminobr.ui.adapter.ListarPartesAdapter
import com.example.adminobr.utils.AutocompleteManager
import com.example.adminobr.viewmodel.AppDataViewModel
import com.example.adminobr.viewmodel.ParteDiarioViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ListaPartesFragment : Fragment() {

    private var _binding: FragmentListaPartesBinding? = null
    private val binding get() = _binding!!

    private lateinit var autocompleteManager: AutocompleteManager
    private val appDataViewModel: AppDataViewModel by activityViewModels()

    private val viewModel: ParteDiarioViewModel by viewModels()

    private lateinit var equipoAutocomplete: AutoCompleteTextView
    private lateinit var equipoTextInputLayout: TextInputLayout
    private lateinit var fechaInicioTextInputLayout: TextInputLayout
    private lateinit var fechaFinTextInputLayout: TextInputLayout
    private lateinit var fechaInicioEditText: TextInputEditText
    private lateinit var fechaFinEditText: TextInputEditText

    private var isDatePickerOpen = false // Variable para controlar si el DatePicker está abierto

//    private val autocompleteViewModel: AutocompleteViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListaPartesBinding.inflate(inflater, container, false)

        equipoAutocomplete = binding.equipoAutocomplete
        equipoTextInputLayout = binding.equipoTextInputLayout
        fechaInicioTextInputLayout = binding.fechaInicioTextInputLayout
        fechaFinTextInputLayout = binding.fechaFinTextInputLayout
        fechaInicioEditText = binding.fechaInicioEditText
        fechaFinEditText = binding.fechaFinEditText

        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configura el RecyclerView
        val recyclerView = binding.listaPartesRecyclerView
        recyclerView.layoutManager = LinearLayoutManager(context)

        val adapter = ListarPartesAdapter()
        recyclerView.adapter = adapter

        val fab: FloatingActionButton = requireActivity().findViewById(R.id.fab)
        fab.visibility = View.VISIBLE

        // Inicializar AutocompleteManager
        autocompleteManager = AutocompleteManager(requireContext(), appDataViewModel)

        // Configurar los AutoCompleteTextView con AutocompleteManager
        autocompleteManager.loadEquipos(binding.equipoAutocomplete, viewLifecycleOwner)

        // Otras configuraciones del fragmento
        //setupTextWatchers()

        setEditTextToUppercase(equipoAutocomplete)

        // Observa los datos del ViewModel y configura el adaptador del RecyclerView
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.partesDiarios.collectLatest { pagingData ->
                Log.d("ListaPartesFragment", "PagingData recibido: $pagingData")
                adapter.submitData(pagingData)
            }
        }

        adapter.addLoadStateListener { loadState ->
            Log.d("ListaPartesFragment", "LoadState cambiado: ${loadState.source.refresh}")
            if (loadState.source.refresh is LoadState.NotLoading) {
                Log.d("ListaPartesFragment", "Datos recibidos: ${adapter.itemCount}")
                if (adapter.itemCount == 0) {
                    binding.emptyListMessage.visibility = View.VISIBLE
                    binding.listaPartesRecyclerView.visibility = View.GONE
                } else {
                    binding.emptyListMessage.visibility = View.GONE
                    binding.listaPartesRecyclerView.visibility = View.VISIBLE
                }
            } else if (loadState.source.refresh is LoadState.Error) {
                Log.e("ListaPartesFragment", "Error al cargar datos", (loadState.source.refresh as LoadState.Error).error)
            }
        }

        // TextWatcher para equipoAutocomplete
        equipoAutocomplete.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.toString().isEmpty()) {
                    equipoAutocomplete.dismissDropDown()
                } else {
                    equipoAutocomplete.showDropDown()
                }
            }

            override fun afterTextChanged(s: Editable?) {
                if (equipoTextInputLayout.isErrorEnabled) {
                    if (s.isNullOrEmpty()) {
                        equipoTextInputLayout.error = "Campo requerido"
                    } else {
                        equipoTextInputLayout.isErrorEnabled = false
                    }
                }
            }
        })

        // Configura el botón para limpiar todos los filtros
        val clearAllFiltersButton: Button = binding.root.findViewById(R.id.clearAllFiltersButton)
        clearAllFiltersButton.setOnClickListener {
            equipoAutocomplete.setText("")
            fechaInicioEditText.setText("")
            fechaFinEditText.setText("")

//            viewModel.setFilter("", "", "")

            // Borrar la lista
            adapter.submitData(lifecycle, PagingData.empty())

            hideKeyboard()
        }

        // Configura el botón para aplicar los filtros
        val applyFiltersButton: Button = binding.root.findViewById(R.id.applyFiltersButton)
        applyFiltersButton.setOnClickListener {
            val equipo = equipoAutocomplete.text.toString()
            val equipoInterno = equipo.split(" - ").firstOrNull() ?: ""
            // Usa equipoInterno para filtrar
            val fechaInicio = fechaInicioEditText.text.toString()
            val fechaFin = fechaFinEditText.text.toString()
            Log.d("ListaPartesFragment", "Aplicando filtro - Equipo: $equipoInterno, FechaInicio: $fechaInicio, FechaFin: $fechaFin")
            viewModel.setFilter(equipoInterno, fechaInicio, fechaFin)
            hideKeyboard()
        }

        // Configura el FloatingActionButton
        fab.setImageResource(R.drawable.ic_add)
        fab.setOnClickListener {
            findNavController().navigate(R.id.nav_partediario)
        }

        // Configura el OnClickListener para fechaInicioEditText
        fechaInicioEditText.setOnClickListener {
            val locale = Locale.getDefault() // Crea un nuevo objeto Locale con el idioma español
            val calendar = Calendar.getInstance(locale) // Usa el locale para el Calendar

            // Obtener la fecha del EditText si está presente
            val dateString = fechaInicioEditText.text.toString()
            if (dateString.isNotBlank()) {val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val date = formatter.parse(dateString)
                if (date != null) {
                    calendar.time = date
                }
            }

            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(
                requireContext(),
                { _, selectedYear, selectedMonth, selectedDay ->
                    val formattedDate = String.format("%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear)
                    fechaInicioEditText.setText(formattedDate)
                },
                year,
                month,
                day
            )

            // Validar si fechaFinEditText está lleno
            val fechaFinString = fechaFinEditText.text.toString()
            if (fechaFinString.isNotBlank()) {
                val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val fechaFin = formatter.parse(fechaFinString)
                fechaFin?.let { datePickerDialog.datePicker.maxDate = it.time }
            } else {
                // Establecer la fecha máxima en hoy
                datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
            }

            datePickerDialog.show()
        }

        // Configura el OnClickListener para fechaFinEditText
        fechaFinEditText.setOnClickListener {
            val locale = Locale.getDefault() // Crea un nuevo objeto Locale con el idioma español
            val calendar = Calendar.getInstance(locale) // Usa el locale para el Calendar

            // Obtener la fecha del EditText si está presente
            val dateString = fechaFinEditText.text.toString()
            if (dateString.isNotBlank()) {val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val date = formatter.parse(dateString)
                if (date != null) {
                    calendar.time = date
                }
            }

            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(
                requireContext(),
                { _, selectedYear, selectedMonth, selectedDay ->
                    val formattedDate = String.format("%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear)
                    fechaFinEditText.setText(formattedDate)
                },
                year,
                month,
                day
            )

            // Validar si fechaInicioEditText está lleno
            val fechaInicioString = fechaInicioEditText.text.toString()
            if (fechaInicioString.isNotBlank()) {
                val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val fechaInicio = formatter.parse(fechaInicioString)
                fechaInicio?.let { datePickerDialog.datePicker.minDate = it.time }
            }

            // Establecer la fecha máxima en hoy
            datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
            datePickerDialog.show()
        }

        // Configura el OnItemClickListener para equipoAutocomplete
        equipoAutocomplete.setOnItemClickListener { _, _, _, _ ->
            // Si se selecciona un equipo, quita el foco del AutoCompleteTextView
            equipoAutocomplete.clearFocus()
//            applyFiltersButton.requestFocus()
//            binding.applyFiltersButton.requestFocusFromTouch()
        }
    }

    private fun setupTextWatchers() {
        // TextWatcher para calcular horas trabajadas
        val horasTextWatcher = object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                //
            }
        }

        // Otros TextWatchers para los campos requeridos
        //addTextWatcher(binding.equipoTextInputLayout, "Campo requerido")
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


    private fun setEditTextToUppercase(editText: AutoCompleteTextView) {
        editText.filters = arrayOf(InputFilter.AllCaps())
    }

    private fun hideKeyboard() {
        val inputMethodManager = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val view = requireActivity().currentFocus
        view?.let {
            inputMethodManager.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
