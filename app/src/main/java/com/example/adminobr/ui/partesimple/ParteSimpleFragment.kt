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
import androidx.core.content.ContextCompat
//import com.example.gestionequipos.ui.adapter.CustomArrayAdapter
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
            val selectedEquipoText = binding.equipoAutocomplete.text.toString()
            val equipoInterno = selectedEquipoText.split(" - ").firstOrNull() ?: ""

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
            val colorRojo = ContextCompat.getColor(requireContext(), R.color.colorAlert) // Reemplaza 'rojo' con el nombre de tu color en colors.xml
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
        binding.infoTextView.text = "Atención: Los partes se eliminaran al salir de la actividad y ya no podran recuperarse. Tambien puede COMPARTIR la lista de partes."

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

        if (equipoAutocomplete.text.isNullOrEmpty()) {
            equipoTextInputLayout.error = "Campo requerido"
            equipoTextInputLayout.isErrorEnabled = true
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

        // Configurar los AutoCompleteTextView con AutocompleteManager
        autocompleteManager.loadEquipos(binding.equipoAutocomplete, viewLifecycleOwner)

        setEditTextToUppercase(equipoAutocomplete)

        // Mensaje alertando sobre la persistencia de los datos
        Toast.makeText(
            requireContext(),
            "La lista de PARTES SIMPLES, se borrará al salir de la actividad.",
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

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val itemView = viewHolder.itemView
                val dX = itemView.translationX

                // Verificar que el swipe fue lo suficientemente largo antes de eliminar
                if (abs(dX) > itemView.width * 0.3) {
                    // Proceder con la eliminación si se deslizó más del 30%
                    viewModel.removeParte(viewHolder.bindingAdapterPosition)
                } else {
                    // Devolver el ítem a su posición original si no se deslizó lo suficiente
                    adapter.notifyItemChanged(viewHolder.bindingAdapterPosition)
                }
            }

//            override fun onChildDraw(
//                c: Canvas,
//                recyclerView: RecyclerView,
//                viewHolder: RecyclerView.ViewHolder,
//                dX: Float,
//                dY: Float,
//                actionState: Int,
//                isCurrentlyActive: Boolean
//            ) {
//                val itemView = viewHolder.itemView
//                val deleteIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_delete) ?: return
//
//                // Cambiar el color del icono
//                val desiredColor = ContextCompat.getColor(requireContext(), R.color.black) // Cambia R.color.white al color que desees
//                deleteIcon.setColorFilter(PorterDuffColorFilter(desiredColor, PorterDuff.Mode.SRC_IN))
//
//                val background = ColorDrawable()
//
//                val iconMargin = (itemView.height - deleteIcon.intrinsicHeight) / 2
//                val iconTop = itemView.top + iconMargin
//                val iconBottom = iconTop + deleteIcon.intrinsicHeight
//
//                val redColor = ContextCompat.getColor(requireContext(), R.color.colorAlert)
//                background.color = redColor
//
//                // Calcular la opacidad del ícono en función de dX
//                val iconOpacity = (1 - abs(dX) / itemView.width) * 255 // 0 (totalmente transparente) a 255 (totalmente opaco)
//
//                if (dX < 0) { // Deslizando a la izquierda
//                    // Calcular la posición del ícono considerando el padding
//                    val recyclerViewPadding = recyclerView.paddingLeft
//                    val recyclerViewPaddingRight = recyclerView.paddingRight
//                    val iconLeft = itemView.right + dX.toInt() - recyclerViewPadding + iconMargin
//                    val iconRight = iconLeft + deleteIcon.intrinsicWidth
//
//
//                    // Dibujar el fondo considerando el padding
//                    background.setBounds(
//                        itemView.right + dX.toInt(),
//                        itemView.top,
//                        itemView.right,
//                        itemView.bottom
//                    )
//                    background.draw(c)
//
//                    // Dibujar el ícono con la opacidad calculada
//                    deleteIcon.alpha = iconOpacity.toInt()
//                    deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
//                    deleteIcon.draw(c)
//                } else {
//                    background.setBounds(0, 0, 0, 0) // No mostrar fondo o ícono si se desliza a la derecha
//                }
//
//                // Dibujar el ítem por encima del fondo y el ícono
//                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
//            }

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
                val redColor = ContextCompat.getColor(requireContext(), R.color.colorAlert)

                when {
                    dX < 0 -> { // Deslizar hacia la izquierda
//                        val iconLeft = itemView.right - iconMargin - icon.intrinsicWidth
//                        val iconRight = itemView.right - iconMargin
//                        icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                        val iconLeft = itemView.right + dX.toInt() + iconMargin
                        val iconRight = itemView.right + dX.toInt() + iconMargin + icon.intrinsicWidth


//                        val iconLeft = itemView.right + dX.toInt() - recyclerViewPadding + iconMargin
//                        val iconRight = iconLeft + deleteIcon.intrinsicWidth

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

        // TextWatcher para equipoAutocomplete
        equipoAutocomplete.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

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

        // Manejar la selección de un equipo en el AutoCompleteTextView
        equipoAutocomplete.setOnItemClickListener { _, _, _, _ ->
            horasEditText.requestFocus()
        }

        // TextWatcher para horasEditText
        horasEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (horasTextInputLayout.isErrorEnabled) {
                    if (s.isNullOrEmpty()) {
                        horasTextInputLayout.error = "Campo requerido"
                    } else {
                        horasTextInputLayout.isErrorEnabled = false
                    }
                }
            }
        })

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

    }

    // Función para obtener la fecha actual en formato dd/MM/yyyy
    private fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return dateFormat.format(Date())
    }

    private fun guardarDatosEnSharedPreferences() {
        val sharedPreferences = requireContext().getSharedPreferences("partes_simples", Context.MODE_PRIVATE)
        val partesSimplesJson = Gson().toJson(viewModel.partesList.value)

        with(sharedPreferences.edit()) {
            putString("partes_simples", partesSimplesJson)
            apply()
        }
    }

    private fun cargarDatosDesdeSharedPreferences() {
        val sharedPreferences = requireContext().getSharedPreferences("partes_simples", Context.MODE_PRIVATE)
        val partesSimplesJson = sharedPreferences.getString("partes_simples", null)
        if (partesSimplesJson != null) {
            val listType = object : TypeToken<List<ParteSimple>>() {}.type
            val partesSimples = Gson().fromJson<List<ParteSimple>>(partesSimplesJson, listType)
            viewModel.setPartesList(partesSimples)
        }
    }

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
        limpiarDatosEnSharedPreferences()
    }

    private fun limpiarDatosEnSharedPreferences() {
        val sharedPreferences = requireContext().getSharedPreferences("partes_simples", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            clear()
            apply()
        }
    }
}
