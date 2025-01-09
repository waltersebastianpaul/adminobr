package com.example.adminobr.ui.partediario

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.adminobr.R
import com.example.adminobr.data.Equipo
import com.example.adminobr.data.Obra
import com.example.adminobr.databinding.FragmentListarPartesBinding
import com.example.adminobr.ui.adapter.ListarPartesAdapter
import com.example.adminobr.utils.AppUtils
import com.example.adminobr.utils.AutocompleteManager
import com.example.adminobr.utils.Constants
import com.example.adminobr.utils.NetworkStatusHelper
import com.example.adminobr.utils.SessionManager
import com.example.adminobr.viewmodel.AppDataViewModel
import com.example.adminobr.viewmodel.ParteDiarioViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ListarPartesFragment : Fragment(R.layout.fragment_listar_partes) {

    private var _binding: FragmentListarPartesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ParteDiarioViewModel by viewModels()
    private lateinit var adapter: ListarPartesAdapter

    // Manager para la gestión de sesiones, carga solo cuando se accede a él
    private val sessionManager by lazy { SessionManager(requireContext()) }

    private lateinit var autocompleteManager: AutocompleteManager
    private val appDataViewModel: AppDataViewModel by activityViewModels()

    private var selectedEquipo: Equipo? = null
    private var selectedObra: Obra? = null

    private var isNetworkCheckEnabled = Constants.getNetworkStatusHelper()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListarPartesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupDatePickers()
        setupFilterButtons()
        setupAutocomplete()

        // Observamos los cambios de partes solo cuando el usuario presione el botón de "Filtrar"
        observePartesDiarios(false)  // Inicialmente no observar
    }

    private fun setupAutocomplete() {
        autocompleteManager = AutocompleteManager(requireContext(), appDataViewModel, sessionManager)

        autocompleteManager.loadEquipos(
            binding.equipoAutocomplete,
            this
        ) { equipo ->
            selectedEquipo = equipo
            Log.d("ListarPartesFragment", "Equipo seleccionado: ${equipo.id}")
        }

        // Cargar las obras en el AutoCompleteTextView (sin forzar actualización en SessionManager)
        autocompleteManager.loadObras(
            autoCompleteTextView = binding.obraAutocomplete,
            lifecycleOwner = viewLifecycleOwner,
            empresaDbName = sessionManager.getEmpresaDbName(),
            forceRefresh = false, // Cargar desde caché si está disponible
            filterEstado = true // Filtrar solo las obras activas
        ) { obra ->
            // Asignar la obra seleccionada a la variable local, pero no actualizar SessionManager
            selectedObra = obra
            Log.d("ListarPartesFragment", "Obra seleccionada: ${obra.id} - ${obra.nombre}")
        }

        // Usar addTextWatcher para limpiar `selectedEquipo` si el texto de equipo se edita manualmente
        addTextWatcher(binding.equipoAutocomplete) {
            selectedEquipo = null
        }

        // Agregar un TextWatcher para limpiar la variable `selectedObra` si se borra manualmente el texto
        addTextWatcher(binding.obraAutocomplete) {
            selectedObra = null
            Log.d("ListarPartesFragment", "Obra seleccionada limpiada manualmente")
        }
    }

    private fun addTextWatcher(editText: AutoCompleteTextView, onClearSelection: () -> Unit) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (s.isNullOrEmpty()) {
                    onClearSelection() // Limpiar selección cuando el texto se borra
                    Log.d("ListarPartesFragment", "${editText.hint} limpiado manualmente")
                }
            }
        })
    }


    private fun setupRecyclerView() {
        adapter = ListarPartesAdapter(viewModel, requireContext(), childFragmentManager)
        binding.listaPartesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.listaPartesRecyclerView.adapter = adapter
    }

    @SuppressLint("DefaultLocale")
    private fun setupDatePickers() {
        binding.fechaInicioEditText.setOnClickListener {
            showDatePicker(binding.fechaInicioEditText)
        }

        binding.fechaFinEditText.setOnClickListener {
            showDatePicker(binding.fechaFinEditText)
        }
    }

    private fun showDatePicker(targetView: View) {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
//                val formattedDate = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year)
                val formattedDate = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, month + 1, year)
                (targetView as? android.widget.EditText)?.setText(formattedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // Lógica de validación y límites
        when (targetView) {
            binding.fechaInicioEditText -> {
                // Validar si fechaFinEditText está lleno
                val fechaFinString = binding.fechaFinEditText.text.toString()
                if (fechaFinString.isNotBlank()) {
                    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val fechaFin = formatter.parse(fechaFinString)
                    fechaFin?.let { datePickerDialog.datePicker.maxDate = it.time }
                } else {
                    // Establecer la fecha máxima en hoy
                    datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
                }
            }
            binding.fechaFinEditText -> {
                // Validar si fechaInicioEditText está lleno
                val fechaInicioString = binding.fechaInicioEditText.text.toString()
                if (fechaInicioString.isNotBlank()) {
                    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val fechaInicio = formatter.parse(fechaInicioString)
                    fechaInicio?.let { datePickerDialog.datePicker.minDate = it.time }
                }
                // Establecer la fecha máxima en hoy
                datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
            }
        }

        datePickerDialog.show()
    }

    private fun setupFilterButtons() {
        binding.applyFiltersButton.setOnClickListener {
            // Ocultar el teclado usando AppUtils
            AppUtils.closeKeyboard(requireActivity(), view)

            if (isNetworkCheckEnabled && NetworkStatusHelper.isConnected()) {
                val equipoId = selectedEquipo?.id ?: 0
                val obraId = selectedObra?.id ?: 0
                val fechaInicio = binding.fechaInicioEditText.text.toString()
                val fechaFin = binding.fechaFinEditText.text.toString()
                viewModel.updateFilters(equipoId, obraId, fechaInicio, fechaFin)

                // Empezamos a observar los datos de partes con los filtros actualizados
                observePartesDiarios(true)  // Ahora sí, activamos la observación
            } else {
//                Toast.makeText(requireContext(), "No hay conexión a internet, intenta mas tardes.", Toast.LENGTH_SHORT).show()
                Snackbar.make(binding.root, "No hay conexión a internet, intenta mas tardes.", Snackbar.LENGTH_LONG).show()
            }
        }

        binding.clearAllFiltersButton.setOnClickListener {
            // Ocultar el teclado usando AppUtils
            AppUtils.closeKeyboard(requireActivity(), view)

            if (isNetworkCheckEnabled && NetworkStatusHelper.isConnected()) {
                binding.equipoAutocomplete.setText("")
                binding.obraAutocomplete.setText("")
                binding.fechaInicioEditText.setText("")
                binding.fechaFinEditText.setText("")
                selectedEquipo = null
                selectedObra = null

                Log.d("ListarPartesFragment", "Filtros limpiados")
                viewModel.updateFilters(0, 0, "", "")
            } else {
//                Toast.makeText(requireContext(), "No hay conexión a internet, intenta mas tardes.", Toast.LENGTH_SHORT).show()
                Snackbar.make(binding.root, "No hay conexión a internet, intenta mas tardes.", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun observePartesDiarios(shouldObserve: Boolean) {
        if (shouldObserve) {
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.partesDiarios.collectLatest { pagingData ->
                    Log.d("ListarPartesFragment", "Actualizando RecyclerView con nuevos datos")
                    adapter.submitData(pagingData)
                }
            }
        } else {
            // Si no es necesario observar, no hacemos nada
            Log.d("ListarPartesFragment", "No se está observando partes")
        }

        viewModel.recargarListaPartes.observe(viewLifecycleOwner) { recargar ->
            if (recargar) {
                adapter.refresh() // o adapter.notifyDataSetChanged() si no usas Paging
                viewModel.resetRecargarListaPartes() // Para que no vuelva a activarse
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
