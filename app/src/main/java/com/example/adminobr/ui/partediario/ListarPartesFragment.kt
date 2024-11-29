package com.example.adminobr.ui.partediario

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.PagingData
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.adminobr.R
import com.example.adminobr.data.Equipo
import com.example.adminobr.databinding.FragmentListarPartesBinding
import com.example.adminobr.ui.adapter.ListarPartesAdapter
import com.example.adminobr.utils.AppUtils
import com.example.adminobr.utils.AutocompleteManager
import com.example.adminobr.viewmodel.AppDataViewModel
import com.example.adminobr.viewmodel.ParteDiarioViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ListarPartesFragment : Fragment(R.layout.fragment_listar_partes) {

    private var _binding: FragmentListarPartesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ParteDiarioViewModel by viewModels()
    private lateinit var adapter: ListarPartesAdapter

    private lateinit var autocompleteManager: AutocompleteManager
    private val appDataViewModel: AppDataViewModel by activityViewModels()

    private var selectedEquipo: Equipo? = null

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
        observePartesDiarios()
    }

    private fun setupAutocomplete() {
        // Inicializar AutocompleteManager
        autocompleteManager = AutocompleteManager(requireContext(), appDataViewModel)
        autocompleteManager.loadEquipos(
            binding.equipoAutocomplete,
            this
        ) { equipo ->
            // Guardar equipo seleccionado
            selectedEquipo = equipo
            Log.d("ListarPartesFragment", "Equipo seleccionado: $equipo")
        }
    }

    private fun setupRecyclerView() {
        adapter = ListarPartesAdapter(viewModel, requireContext(), childFragmentManager)
        binding.listaPartesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.listaPartesRecyclerView.adapter = adapter
    }

    @SuppressLint("DefaultLocale")
    private fun setupDatePickers() {

        // Configura el OnClickListener para fechaInicioEditText
        binding.fechaInicioEditText.setOnClickListener {
            val locale = Locale.getDefault() // Crea un nuevo objeto Locale con el idioma español
            val calendar = Calendar.getInstance(locale) // Usa el locale para el Calendar

            // Obtener la fecha del EditText si está presente
            val dateString = binding.fechaInicioEditText.text.toString()
            if (dateString.isNotBlank()) {
                val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                Log.d("ListarPartesFragment", "Fecha de inicio seleccionada: $formatter")
                val date = formatter.parse(dateString)
                if (date != null) {
                    calendar.time = date
                }
                Log.d("ListarPartesFragment", "Fecha de inicio seleccionada: $date")
            }

            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(
                requireContext(),
                { _, selectedYear, selectedMonth, selectedDay ->
                    val formattedDate = String.format(
                        "%02d/%02d/%04d",
                        selectedDay,
                        selectedMonth + 1,
                        selectedYear
                    )
                    binding.fechaInicioEditText.setText(formattedDate)
                },
                year,
                month,
                day
            )
            Log.d("ListarPartesFragment", "Fecha de inicio seleccionada: $datePickerDialog")

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

            datePickerDialog.show()
        }

        // Configura el OnClickListener para fechaFinEditText
        binding.fechaFinEditText.setOnClickListener {
            val locale = Locale.getDefault() // Crea un nuevo objeto Locale con el idioma español
            val calendar = Calendar.getInstance(locale) // Usa el locale para el Calendar

            // Obtener la fecha del EditText si está presente
            val dateString = binding.fechaFinEditText.text.toString()
            if (dateString.isNotBlank()) {
                val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                Log.d("ListarPartesFragment", "Fecha de fin seleccionada: $formatter")
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
                    val formattedDate = String.format(
                        "%02d/%02d/%04d",
                        selectedDay,
                        selectedMonth + 1,
                        selectedYear
                    )
                    binding.fechaFinEditText.setText(formattedDate)
                },
                year,
                month,
                day
            )

            // Validar si fechaInicioEditText está lleno
            val fechaInicioString = binding.fechaInicioEditText.text.toString()
            if (fechaInicioString.isNotBlank()) {
                val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val fechaInicio = formatter.parse(fechaInicioString)
                fechaInicio?.let { datePickerDialog.datePicker.minDate = it.time }
            }

            // Establecer la fecha máxima en hoy
            datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
            datePickerDialog.show()
        }
    }

    private fun setupFilterButtons() {
        binding.applyFiltersButton.setOnClickListener {
            // Verificar el ID del equipo seleccionado
            val equipoId = selectedEquipo?.id ?: 0
            val fechaInicio = binding.fechaInicioEditText.text.toString()
            val fechaFin = binding.fechaFinEditText.text.toString()

            Log.d("ListarPartesFragment", "Aplicando filtros - EquipoID: $equipoId, FechaInicio: $fechaInicio, FechaFin: $fechaFin")
            // Enviar filtros al ViewModel
            viewModel.updateFilters(equipoId, fechaInicio, fechaFin)
        }

        binding.clearAllFiltersButton.setOnClickListener {
            binding.equipoAutocomplete.setText("")
            binding.fechaInicioEditText.setText("")
            binding.fechaFinEditText.setText("")
            viewModel.updateFilters(0, "", "")
            adapter.submitData(lifecycle, PagingData.empty())
            AppUtils.closeKeyboard(requireActivity(), view)
        }
    }

    private fun observePartesDiarios() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.partesDiarios.collectLatest { pagingData ->
                Log.d("ListarPartesFragment", "Actualizando RecyclerView con nuevos datos")
                adapter.submitData(pagingData)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
