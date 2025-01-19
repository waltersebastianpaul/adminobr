package com.example.adminobr.utils

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.lifecycle.LifecycleOwner
import com.example.adminobr.data.Empresa
import com.example.adminobr.data.Equipo
import com.example.adminobr.data.Estado
import com.example.adminobr.data.Obra
import com.example.adminobr.data.Rol
import com.example.adminobr.ui.adapter.CustomArrayAdapter
import com.example.adminobr.ui.adapter.EmpresaArrayAdapter
import com.example.adminobr.viewmodel.AppDataViewModel

class AutocompleteManager(
    private val context: Context,
    private val viewModel: AppDataViewModel,
    private val sessionManager: SessionManager
) {

    // Maps para almacenar las instancias de las entidades
    private val empresaMap = HashMap<String, Empresa>()
    private val obraMap = HashMap<String, Obra>()
    private val equipoMap = HashMap<String, Equipo>()
    private val rolMap = HashMap<String, Rol>()
    private val estadoMap = HashMap<String, Estado>()

    fun loadEquipos(
        autoCompleteTextView: AutoCompleteTextView? = null,
        lifecycleOwner: LifecycleOwner? = null,
        onEquipoSelected: ((Equipo) -> Unit)? = null,
    ) {
        viewModel.cargarEquipos()
        if (autoCompleteTextView != null && lifecycleOwner != null) {
            viewModel.equipos.observe(lifecycleOwner) { equipos ->
                val adapter = CustomArrayAdapter(
                    context,
                    android.R.layout.simple_dropdown_item_1line,
                    equipos,
                    itemToDisplay = { "${it.interno} - ${it.descripcion}" }, // Cómo se muestra en el campo
                    itemToFilter = { "${it.interno} - ${it.descripcion}" }   // Cómo se filtra
                )
                autoCompleteTextView.setAdapter(adapter)

                equipoMap.clear()
                equipoMap.putAll(equipos.associateBy { "${it.interno} - ${it.descripcion}" })

                autoCompleteTextView.setOnItemClickListener { parent, _, position, _ ->
                    val selectedEquipo = adapter.getItem(position)
                    Log.d("AutocompleteManager", "Equipo seleccionado: $selectedEquipo")
                    selectedEquipo?.let {
                        onEquipoSelected?.invoke(it)
                        AppUtils.closeKeyboard(context, autoCompleteTextView)
                    }
                }
            }
        }
    }

    // Método para cargar obras en un AutoCompleteTextView con opciones de filtrado y actualización
    fun loadObras( // pasar Si o Si el arg "empresaDbName" [sessionManager.getEmpresaDbName()]
        autoCompleteTextView: AutoCompleteTextView? = null, // Campo AutoCompleteTextView donde se mostrarán las obras
        lifecycleOwner: LifecycleOwner? = null, // Observador del ciclo de vida para observar LiveData
        empresaDbName: String? = null, // Pasar Si o Si el nombre de la base de datos de la empresa [sessionManager.getEmpresaDbName()]
        forceRefresh: Boolean = false, // Indica si debe forzar la recarga de datos desde el servidor
        filterEstado: Boolean = false, // Indica si se debe filtrar por estado activo/inactivo
        onObraSelected: ((Obra) -> Unit)? = null // Callback ejecutado cuando se selecciona una obra
    ) {
        // Si se debe forzar la recarga, cargar las obras directamente desde el servidor
        if (forceRefresh) {
            viewModel.cargarObras(empresaDbName, filterEstado)
        } else {
            // Obtener las obras guardadas en SessionManager (caché local)
            val obrasGuardadas = sessionManager.getObraList()
            if (!obrasGuardadas.isNullOrEmpty()) {
                // Si hay obras en caché, configurar el adaptador con esas obras y salir
                setupAdapter(autoCompleteTextView, obrasGuardadas, onObraSelected)
                return
            }
            // Si no hay obras en caché, cargar desde el servidor
            viewModel.cargarObras(empresaDbName, filterEstado)
        }

        // Observar el LiveData de obras en el ViewModel para configurar el adaptador cuando lleguen los datos
        viewModel.obras.observe(lifecycleOwner!!) { obras ->
            setupAdapter(autoCompleteTextView, obras, onObraSelected)
        }
    }

    // Configurar el adaptador para mostrar y filtrar las obras en el AutoCompleteTextView
    private fun setupAdapter(
        autoCompleteTextView: AutoCompleteTextView?, // Campo AutoCompleteTextView a configurar
        obras: List<Obra>, // Lista de obras a mostrar en el adaptador
        onObraSelected: ((Obra) -> Unit)? // Callback para manejar la selección de una obra
    ) {
        // Crear el adaptador personalizado con las funciones de visualización y filtrado
        val adapter = CustomArrayAdapter(
            context,
            android.R.layout.simple_dropdown_item_1line, // Diseño para cada elemento del desplegable
            obras,
            itemToDisplay = { "${it.centroCosto} - ${it.nombre}" }, // Texto mostrado en el campo y el desplegable
            itemToFilter = { "${it.centroCosto} - ${it.nombre} (${it.localidad})" } // Texto usado para filtrar los elementos
        )

        // Asignar el adaptador al AutoCompleteTextView
        autoCompleteTextView?.setAdapter(adapter)

        // Limpiar y actualizar el mapa de obras para búsquedas rápidas
        obraMap.clear()
        obraMap.putAll(obras.associateBy { "${it.centroCosto} - ${it.nombre}" })

        // Manejar la selección de elementos en el desplegable
        autoCompleteTextView?.setOnItemClickListener { parent, _, position, _ ->
            // Obtener la obra seleccionada del adaptador
            val selectedObra = adapter.getItem(position)
            // Ejecutar el callback con la obra seleccionada, si está definida
            selectedObra?.let { onObraSelected?.invoke(it) }
        }
    }

    fun loadRoles(
        autoCompleteTextView: AutoCompleteTextView? = null,
        lifecycleOwner: LifecycleOwner? = null,
        onRolSelected: ((Rol) -> Unit)? = null,
    ) {
        viewModel.cargarRoles()
        if (autoCompleteTextView != null && lifecycleOwner != null) {
            viewModel.roles.observe(lifecycleOwner) { roles ->
                val adapter = CustomArrayAdapter(
                    context,
                    android.R.layout.simple_dropdown_item_1line,
                    roles,
                    itemToDisplay = { it.nombre }, // Cómo se muestra en el campo
                    itemToFilter = { it.nombre }   // Cómo se filtra
                )
                autoCompleteTextView.setAdapter(adapter)

                rolMap.clear()
                rolMap.putAll(roles.associateBy { it.nombre })

                autoCompleteTextView.setOnItemClickListener { parent, _, position, _ ->
                    val selectedRol = adapter.getItem(position)
                    Log.d("AutocompleteManager", "Rol seleccionado: $selectedRol")
                    selectedRol?.let {
                        onRolSelected?.invoke(it)
                    }
                }
            }
        }
    }

    fun loadEstados(
        autoCompleteTextView: AutoCompleteTextView? = null,
        lifecycleOwner: LifecycleOwner? = null,
        onEstadoSelected: ((Estado) -> Unit)? = null,
    ) {
        viewModel.cargarEstados()
        if (autoCompleteTextView != null && lifecycleOwner != null) {
            viewModel.estados.observe(lifecycleOwner) { estados ->
                val adapter = CustomArrayAdapter(
                    context,
                    android.R.layout.simple_dropdown_item_1line,
                    estados,
                    itemToDisplay = { it.nombre }, // Cómo se muestra en el campo
                    itemToFilter = { it.nombre }   // Cómo se filtra
                )
                autoCompleteTextView.setAdapter(adapter)

                estadoMap.clear()
                estadoMap.putAll(estados.associateBy { it.nombre })

                autoCompleteTextView.setOnItemClickListener { parent, _, position, _ ->
                    val selectedEstado = adapter.getItem(position)
                    Log.d("AutocompleteManager", "Estado seleccionado: $selectedEstado")
                    selectedEstado?.let {
                        onEstadoSelected?.invoke(it)
                    }
                }
            }
        }
    }

    fun loadTipoCombustible(
        autoCompleteTextView: AutoCompleteTextView? = null,
        lifecycleOwner: LifecycleOwner? = null,
        onTipoCombustibleSelected: ((String) -> Unit)? = null
    ) {
        val tipoCombustibleItems = arrayOf("Diesel", "Nafta")
        val adapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, tipoCombustibleItems)
        autoCompleteTextView?.setAdapter(adapter)

        // Variable para almacenar el valor seleccionado
        var tipoCombustibleSelected = ""

        autoCompleteTextView?.setOnItemClickListener { parent, _, position, _ ->
            tipoCombustibleSelected = parent.getItemAtPosition(position) as String
            onTipoCombustibleSelected?.invoke(tipoCombustibleSelected)

            // Cerrar el teclado después de seleccionar un tipo de combustible
            AppUtils.closeKeyboard(context, autoCompleteTextView)
        }

        autoCompleteTextView?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // No es necesario hacer nada aquí
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s?.isNotEmpty() == true) {
                    autoCompleteTextView.showDropDown()
                }
            }

            override fun afterTextChanged(s: Editable?) {
                if (s.isNullOrEmpty()) {
                    // Si el texto es borrado, limpiar la variable seleccionada
                    tipoCombustibleSelected = ""
                }
            }
        })

        autoCompleteTextView?.let { autoComplete ->
            autoComplete.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    val inputText = autoComplete.text.toString()
                    if (inputText.isNotEmpty() && !tipoCombustibleItems.contains(inputText)) {
                        // Si el texto no coincide con ningún item, borra el campo y la variable seleccionada
                        autoComplete.text.clear()
                        tipoCombustibleSelected = ""
                    }
                }
            }
        }
    }

    fun getEmpresaByName(empresaName: String): Empresa? {
        return empresaMap.entries.find { it.key.equals(empresaName, ignoreCase = true) }?.value
    }

    fun getObraByName(obraText: String): Obra? {
        Log.d("AutocompleteManager", "Buscando obra por texto: $obraText")

        val foundObra = obraMap.entries.find { (key, _) ->
            val keyCentroCosto = key.substringBefore(" - ").trim()
            val keyNombre = key.substringAfter(" - ").substringBefore(" (").trim()
            val inputCentroCosto = obraText.substringBefore(" - ").trim()
            val inputNombre = obraText.substringAfter(" - ").substringBefore(" (").trim()

            val match = keyCentroCosto.equals(inputCentroCosto, ignoreCase = true) &&
                    keyNombre.equals(inputNombre, ignoreCase = true)

            Log.d("AutocompleteManager", "Comparando: '$key' con '$obraText', resultado: $match")
            match
        }?.value

        Log.d("AutocompleteManager", "Resultado de la búsqueda: ${foundObra?.nombre ?: "Ninguna obra encontrada"}")
        return foundObra
    }



    fun getObraById(obraId: Int): Obra? {
        return obraMap.values.find { it.id == obraId }
    }

    fun getEquipoByName(equipoName: String): Equipo? {
        return equipoMap.entries.find { it.key.equals(equipoName, ignoreCase = true) }?.value
    }

    fun getEquipoById(equipoId: Int): Equipo? {
        Log.d("AutocompleteManager", "Buscando equipo por ID: $equipoId")
        return equipoMap.values.find { it.id == equipoId }
    }

    fun getCombustibleByName(combustibleName: String): String? {
        return when (combustibleName) {
            "Diesel" -> "Diesel"
            "Nafta" -> "Nafta"
            else -> null
        }
    }

    fun getRolByName(rolName: String): Rol? {
        return rolMap.entries.find { it.key.equals(rolName, ignoreCase = true) }?.value
    }

    fun getRolById(rolId: Int): Rol? {
        return rolMap.values.find { it.id == rolId }
    }

    fun getEstadoByName(estadoName: String): Estado? {
        return estadoMap.entries.find { it.key.equals(estadoName, ignoreCase = true) }?.value
    }

    fun getEstadoById(estadoId: Int): Estado? {
        return estadoMap.values.find { it.id == estadoId }
    }
}
