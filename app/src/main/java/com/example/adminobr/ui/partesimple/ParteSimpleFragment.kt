package com.example.adminobr.ui.partesimple

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.adminobr.R
import com.example.adminobr.data.ParteSimple
import com.example.adminobr.databinding.FragmentParteSimpleBinding
import com.example.adminobr.viewmodel.AppDataViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.gson.reflect.TypeToken
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.inputmethod.InputMethodManager
import androidx.compose.ui.input.key.type
import androidx.core.content.ContextCompat
import androidx.paging.PagingData
import com.example.adminobr.data.Equipo
import com.example.adminobr.ui.adapter.ParteSimpleAdapter
import com.example.adminobr.utils.AutocompleteManager
import com.example.adminobr.viewmodel.ParteSimpleViewModel
import com.google.gson.Gson
import kotlin.math.abs

class ParteSimpleFragment : Fragment() {

    private var _binding: FragmentParteSimpleBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ParteSimpleViewModel by viewModels()

    private lateinit var autocompleteManager: AutocompleteManager
    private val appDataViewModel: AppDataViewModel by activityViewModels()

    // Variable para almacenar el equipo seleccionado
    private var selectedEquipo: Equipo? = null

    private lateinit var adapter: ParteSimpleAdapter

    private lateinit var equipoAutocomplete: AutoCompleteTextView
    private lateinit var horasEditText: TextInputEditText
    private lateinit var equipoTextInputLayout: TextInputLayout
    private lateinit var horasTextInputLayout: TextInputLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentParteSimpleBinding.inflate(inflater, container, false)
        adapter = ParteSimpleAdapter(viewModel) // Pasa el ViewModel al adaptador
        adapter.setViewModel(viewModel) // Llama a setViewModel antes de asignar el adaptador al RecyclerView

        binding.listaPartesSimplesRecyclerView.adapter = adapter
        binding.listaPartesSimplesRecyclerView.layoutManager = LinearLayoutManager(context)

        equipoAutocomplete = binding.equipoAutocomplete
        horasEditText = binding.horasActualesEditText
        equipoTextInputLayout = binding.equipoTextInputLayout // Asegúrate de tener este ID en tu XML
        horasTextInputLayout = binding.horasActualesTextInputLayout // Asegúrate de tener este ID en tu XML

        binding.cargarParteButton.setOnClickListener {
            //val selectedEquipoText = binding.equipoAutocomplete.text.toString()
            val equipoInterno = selectedEquipo?.interno ?: ""

            val horasText = horasEditText.text.toString()
            val horas = if (horasText.isNotBlank()) horasText.toInt() else 0

            val currentDate = getCurrentDate()

            if (validarCampos()) {
                try {
                    val parte = ParteSimple(currentDate, equipoInterno, horas) // Ahora horas es Int
                    viewModel.addParte(parte)

                    // Limpiar campos y poner foco en equipo
                    equipoAutocomplete.text = null
                    horasEditText.text = null
                    equipoAutocomplete.requestFocus()

                } catch (e: NumberFormatException) {
                    // Manejar la excepción, por ejemplo, mostrar un mensaje de error
                    Toast.makeText(requireContext(), "El valor de horas es demasiado grande", Toast.LENGTH_SHORT).show()
                    horasEditText.requestFocus()
                }
            }
        }

        binding.clearHistorialPartesTextView.setOnClickListener {
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle("Borrar Historial")
            builder.setMessage("¿Estás seguro de que quieres borrar el historial de partes simples?")

            val positiveButtonText = SpannableString("Borrar")
            val colorRojo = ContextCompat.getColor(requireContext(), R.color.danger_500) // Reemplaza 'rojo' con el nombre de tu color en colors.xml
            positiveButtonText.setSpan(
                ForegroundColorSpan(colorRojo),
                0,
                positiveButtonText.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            builder.setPositiveButton(positiveButtonText) { dialog, _ ->
//                SharedPreferencesHelper.clearPartesList(requireContext())
                viewModel.clearPartesList() // Limpiar la lista
                // actualizarHistorialPartes()
                dialog.dismiss()
            }
            builder.setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            val dialog = builder.create()
            dialog.show()
        }

        binding.compartirIcon.setOnClickListener {
            compartirPartesSimples()
        }

        // Agregar texto informativo debajo del botón
        binding.infoTextView.text = "Atención: La lista se borrará luego de $TIEMPO_LIMITE_HS horas.\nSe recomienda COMPARTIR la lista de partes."

        return binding.root
    }

    private fun compartirPartesSimples() {
        val viewModel: ParteSimpleViewModel by viewModels()
        val partes = viewModel.partesList.value ?: return
        if (partes.isEmpty()) return

        val partesPorFecha = partes.groupBy { it.fecha }

        val partesText = StringBuilder()
        partesText.append("LISTA DE PARTES SIMPLES\n")

        for ((fecha, partesDeFecha) in partesPorFecha) {
            partesText.append("Fecha: $fecha\n")
            partesText.append("Equipos:\n")
            for (parte in partesDeFecha) {
                partesText.append(" - ${parte.equipo}, Horas: ${parte.horas}\n")
            }
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, partesText.toString())
        }

        val chooser = Intent.createChooser(intent, "Compartir partes simples")
        startActivity(chooser)
    }

    private fun validarCampos(): Boolean {
        var camposValidos = true

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

        if (horasEditText.text.isNullOrEmpty()) {
            horasTextInputLayout.error = "Campo requerido"
            horasTextInputLayout.isErrorEnabled = true
            camposValidos = false
        } else {
            horasTextInputLayout.isErrorEnabled = false
        }

        if (!camposValidos) {
            Toast.makeText(requireContext(), "Por favor, complete todos los campos requeridos", Toast.LENGTH_SHORT).show()
        }

        return camposValidos
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fab: FloatingActionButton = requireActivity().findViewById(R.id.fab)
        fab.visibility = View.GONE

        // Inicializar AutocompleteManager
        autocompleteManager = AutocompleteManager(requireContext(), appDataViewModel)

        // Cargar equipos y capturar el objeto Equipo seleccionado
        autocompleteManager.loadEquipos(
            equipoAutocomplete,
            this
        ) { equipo ->
            Log.d("ListarPartesFragment", "Equipo selecionado: $equipo")
            equipoAutocomplete.clearFocus()
            selectedEquipo = equipo // Guardar equipo seleccionado
        }

        // Llamar a la función para convertir el texto a mayúsculas
        setEditTextToUppercase(equipoAutocomplete)

        // Mensaje alertando sobre la persistencia de los datos
        Toast.makeText(
            requireContext(),
//            "La lista de PARTES SIMPLES, se borrará al salir de la actividad.",
            "La lista de PARTES SIMPLES se borrará luego de $TIEMPO_LIMITE_HS horas.",
            Toast.LENGTH_LONG
        ).show()

        cargarDatosDesdeSharedPreferences() // Cargar los datos después de verificar la fecha

        viewModel.partesList.observe(viewLifecycleOwner) { partes ->
            adapter.submitList(partes)
            guardarDatosEnSharedPreferences()
        }

        viewModel.partesList.observe(viewLifecycleOwner) { partes ->
            adapter.submitList(partes)
            guardarDatosEnSharedPreferences()

            // Actualiza la visibilidad de los elementos borrarListaText y compartirIcon
            if (partes.isEmpty()){ // if (partes.size < 1){
                binding.clearHistorialPartesTextView.visibility = View.GONE
                binding.compartirIcon.visibility = View.GONE
                binding.emptyListMessage.visibility = View.VISIBLE
//                binding.infoTextView.visibility = View.GONE
            } else {
                binding.clearHistorialPartesTextView.visibility = View.VISIBLE
                binding.compartirIcon.visibility = View.VISIBLE
                binding.emptyListMessage.visibility = View.GONE
                binding.infoTextView.visibility = View.VISIBLE

            }
        }

        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

//            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
//                val itemView = viewHolder.itemView
//                val dX = itemView.translationX
//
//                // Verificar que el swipe fue lo suficientemente largo antes de eliminar
//                if (abs(dX) > itemView.width * 0.3) {
//                    // Proceder con la eliminación si se deslizó más del 30%
//                    viewModel.removeParte(viewHolder.bindingAdapterPosition)
//                } else {
//                    // Devolver el ítem a su posición original si no se deslizó lo suficiente
//                    adapter.notifyItemChanged(viewHolder.bindingAdapterPosition)
//                }
//            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val itemView = viewHolder.itemView
                val dX = itemView.translationX

                // Verificar que el swipe fue lo suficientemente largo antes de eliminar
                if (abs(dX) > itemView.width * 0.3) {
                    // Proceder con la eliminación si se deslizó más del 30%
                    viewModel.removeParte(viewHolder.bindingAdapterPosition)

                    // Verificar si la lista está vacía después de la eliminación
                    if (viewModel.partesList.value.isNullOrEmpty()) {
                        // Ocultar el TextView con un pequeño retraso
                        binding.infoTextView.postDelayed({
                            binding.infoTextView.visibility = View.GONE
                        }, 300) // Retraso de 300 milisegundos
                    }
                } else {
                    // Devolver el ítem a su posición original si no se deslizó lo suficiente
                    adapter.notifyItemChanged(viewHolder.bindingAdapterPosition)
                }
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)

                val itemView = viewHolder.itemView
                val icon = ContextCompat.getDrawable(recyclerView.context, R.drawable.ic_delete)
                val iconMargin = (itemView.height - icon!!.intrinsicHeight) / 2
                val iconTop = itemView.top + (itemView.height - icon.intrinsicHeight) / 2
                val iconBottom = iconTop + icon.intrinsicHeight
                // Fondo rojo
                val redColor = ContextCompat.getColor(requireContext(), R.color.danger_500)

                //val limit = 110 // itemView.width / 3 // Distancia límite para que el ícono se quede fijo
                val limit = itemView.width / 7 // Distancia límite para que el ícono se quede fijo

                when {
                    dX < 0 -> { // Deslizar hacia la izquierda
                        val iconLeft = if (dX > -limit) {
                            itemView.right + dX.toInt() + iconMargin
                        } else {
                            itemView.right - limit.toInt() + iconMargin
                        }
                        val iconRight = iconLeft + icon.intrinsicWidth

                        // Aplicar tinte al ícono
                        icon.setTint(ContextCompat.getColor(recyclerView.context, R.color.danger_500))

                        icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)

                        val background = ColorDrawable(redColor)
                        background.setBounds(
                            itemView.right + dX.toInt(),
                            itemView.top,
                            itemView.right,
                            itemView.bottom
                        )
                        background.draw(c)

                        // Dibujar el ícono
                        icon.draw(c)
                    }
                    else -> {
                        // Si dX es 0 o positivo (sin deslizamiento o deslizamiento a la derecha)
                        val background = ColorDrawable(redColor)
                        background.setBounds(0, 0, 0, 0)
                        background.draw(c)
                    }
                }
            }

        }

        // Asigna el ItemTouchHelper al RecyclerView
        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(binding.listaPartesSimplesRecyclerView)

        // Agregar el DividerItemDecoration al RecyclerView
        val dividerItemDecoration = DividerItemDecoration(
            binding.listaPartesSimplesRecyclerView.context,
            (binding.listaPartesSimplesRecyclerView.layoutManager as LinearLayoutManager).orientation
        )
        ContextCompat.getDrawable(requireContext(), R.drawable.divider)?.let {
            dividerItemDecoration.setDrawable(it)
        }
        binding.listaPartesSimplesRecyclerView.addItemDecoration(
            dividerItemDecoration
        )

        // Otras configuraciones del fragmento
        setupTextWatchers()

    }

    private fun hideKeyboard() {
        val inputMethodManager = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val view = requireActivity().currentFocus
        view?.let {
            inputMethodManager.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }

    private fun setupTextWatchers() {
        // Otros TextWatchers para los campos requeridos
        addTextWatcher(equipoTextInputLayout, "Campo requerido")
        addTextWatcher(horasTextInputLayout, "Campo requerido")
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


    // Función para obtener la fecha actual en formato dd/MM/yyyy
    private fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return dateFormat.format(Date())
    }

    // ... (código existente) ...

    private val TIEMPO_LIMITE_HS = 24 // horas
    private val TIEMPO_LIMITE_MS = TIEMPO_LIMITE_HS * 60 * 60 * 1000 // 24 horas en milisegundos

    private fun guardarDatosEnSharedPreferences() {
        val sharedPreferences = requireContext().getSharedPreferences("partes_simples", Context.MODE_PRIVATE)
        val partesSimplesJson = Gson().toJson(viewModel.partesList.value)

        with(sharedPreferences.edit()) {
            putString("partes_simples", partesSimplesJson)
            putLong("ultima_actualizacion", System.currentTimeMillis()) // Guardar timestamp
            apply()
        }
    }

    private fun cargarDatosDesdeSharedPreferences() {
        val sharedPreferences = requireContext().getSharedPreferences("partes_simples", Context.MODE_PRIVATE)
        val ultimaActualizacion = sharedPreferences.getLong("ultima_actualizacion", 0)

        if (System.currentTimeMillis() - ultimaActualizacion < TIEMPO_LIMITE_MS) {
            // Los datos no han expirado, cargarlos
            val partesSimplesJson = sharedPreferences.getString("partes_simples", null)
            if (partesSimplesJson != null) {
                val listType = object : TypeToken<List<ParteSimple>>() {}.type
                val partesSimples = Gson().fromJson<List<ParteSimple>>(partesSimplesJson, listType)
                viewModel.setPartesList(partesSimples)
            }
        } else {
            // Los datos han expirado, borrarlos
            limpiarDatosEnSharedPreferences()
        }
    }


//    private fun guardarDatosEnSharedPreferences() {
//        val sharedPreferences = requireContext().getSharedPreferences("partes_simples", Context.MODE_PRIVATE)
//        val partesSimplesJson = Gson().toJson(viewModel.partesList.value)
//
//        with(sharedPreferences.edit()) {
//            putString("partes_simples", partesSimplesJson)
//            apply()
//        }
//    }
//
//    private fun cargarDatosDesdeSharedPreferences() {
//        val sharedPreferences = requireContext().getSharedPreferences("partes_simples", Context.MODE_PRIVATE)
//        val partesSimplesJson = sharedPreferences.getString("partes_simples", null)
//        if (partesSimplesJson != null) {
//            val listType = object : TypeToken<List<ParteSimple>>() {}.type
//            val partesSimples = Gson().fromJson<List<ParteSimple>>(partesSimplesJson, listType)
//            viewModel.setPartesList(partesSimples)
//        }
//    }

    private fun setEditTextToUppercase(editText: AutoCompleteTextView) {
        editText.filters = arrayOf(InputFilter.AllCaps())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        // guardarDatosEnSharedPreferences()
    }

    override fun onDetach() {
        super.onDetach()
        // limpiarDatosEnSharedPreferences()
    }

    private fun limpiarDatosEnSharedPreferences() {
        val sharedPreferences = requireContext().getSharedPreferences("partes_simples", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            clear()
            apply()
        }
    }
}
