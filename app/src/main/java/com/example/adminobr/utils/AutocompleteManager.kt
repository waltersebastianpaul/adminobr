package com.example.adminobr.utils

//noinspection SuspiciousImport
import android.R
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.example.adminobr.data.Empresa
import com.example.adminobr.data.Equipo
import com.example.adminobr.data.Estado
import com.example.adminobr.data.Obra
import com.example.adminobr.data.Rol
import com.example.adminobr.ui.adapter.CustomArrayAdapter
import com.example.adminobr.ui.adapter.EmpresaArrayAdapter
import com.example.adminobr.viewmodel.AppDataViewModel

class AutocompleteManager(private val context: Context, private val viewModel: AppDataViewModel) {

    // HashMap para almacenar la relación nombre-Empresa
    private val empresaMap = HashMap<String, Empresa>()
    private val obraMap = HashMap<String, Obra>()
    private val equipoMap = HashMap<String, Equipo>()
    private val rolMap = HashMap<String, Rol>()
    private val estadoMap = HashMap<String, Estado>()

    // Callback para manejar la selección de la empresa
    fun loadEmpresas(
        autoCompleteTextView: AutoCompleteTextView? = null, lifecycleOwner: LifecycleOwner? = null,
        onEmpresaSelected: ((Empresa) -> Unit)? = null,
    ) {
        viewModel.cargarEmpresas()
        if (autoCompleteTextView != null && lifecycleOwner != null) {
            viewModel.empresas.observe(lifecycleOwner, Observer { empresas ->
                val adapter = EmpresaArrayAdapter(
                    context, R.layout.simple_dropdown_item_1line,
                    empresas.map { it.nombre }
                )
                autoCompleteTextView.setAdapter(adapter)

                empresaMap.clear()
                empresaMap.putAll(empresas.associateBy { it.nombre })

                autoCompleteTextView.setOnItemClickListener { parent, _, position, _ ->
                    val selectedEmpresaNombre = parent.getItemAtPosition(position) as String
                    val selectedEmpresa = empresaMap[selectedEmpresaNombre]
                    selectedEmpresa?.let {
                        onEmpresaSelected?.invoke(it)

                        // Cerrar el teclado después de seleccionar la empresa
                        AppUtils.closeKeyboard(context, autoCompleteTextView)
                    }
                }

                autoCompleteTextView.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                        // No es necesario hacer nada aquí
                    }

                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                        if (s?.isNotEmpty() == true) {
                            autoCompleteTextView.showDropDown()
                        }
                    }

                    override fun afterTextChanged(s: Editable?) {
                        // No es necesario hacer nada aquí
                    }
                })

                autoCompleteTextView.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
                    // No es necesario hacer nada aquí
                }
            })
        }
    }


    fun loadEquipos(
        autoCompleteTextView: AutoCompleteTextView? = null, lifecycleOwner: LifecycleOwner? = null,
        onEquipoSelected: ((Equipo) -> Unit)? = null,
    ) {
        viewModel.cargarEquipos()
        if (autoCompleteTextView != null && lifecycleOwner != null) {
            viewModel.equipos.observe(lifecycleOwner, Observer { equipos ->
                val adapter = CustomArrayAdapter(
                    context, R.layout.simple_dropdown_item_1line,
                    equipos.map { "${it.interno} - ${it.descripcion}" }
                )
                autoCompleteTextView.setAdapter(adapter)

                equipoMap.clear()
                equipoMap.putAll(equipos.associateBy { "${it.interno} - ${it.descripcion}" })

                autoCompleteTextView.setOnItemClickListener { parent, _, position, _ ->
                    val selectedEquipoNombre = parent.getItemAtPosition(position) as String
                    val selectedEquipo = equipoMap[selectedEquipoNombre]
                    Log.d("AutocompleteManager", "Equipo seleccionado: $selectedEquipo") // Agregar log
                    selectedEquipo?.let {
                        onEquipoSelected?.invoke(it)

                        // Cerrar el teclado después de seleccionar un equipo
                        AppUtils.closeKeyboard(context, autoCompleteTextView)
                    }
                }

                autoCompleteTextView.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                        // No es necesario hacer nada aquí
                    }

                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                        if (s?.isNotEmpty() == true) {
                            autoCompleteTextView.showDropDown()
                        }
                    }

                    override fun afterTextChanged(s: Editable?) {
                        // No es necesario hacer nada aquí
                    }
                })

                autoCompleteTextView.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
                    // No es necesario hacer nada aquí
                }
            })
        }
    }

    fun loadObras(
        autoCompleteTextView: AutoCompleteTextView? = null, lifecycleOwner: LifecycleOwner? = null,
        onObraSelected: ((Obra) -> Unit)? = null,
    ) {
        viewModel.cargarObras()
        if (autoCompleteTextView != null && lifecycleOwner != null) {
            viewModel.obras.observe(lifecycleOwner, Observer { obras ->
                val adapter = CustomArrayAdapter(
                    context, R.layout.simple_dropdown_item_1line,

                    obras.map { "${it.centroCosto} - ${it.nombre}" }

                )
                autoCompleteTextView.setAdapter(adapter)

                obraMap.clear()
                obraMap.putAll(obras.associateBy { "${it.centroCosto} - ${it.nombre}" })

                autoCompleteTextView.setOnItemClickListener { parent, _, position, _ ->
                    val selectedObraNombre = parent.getItemAtPosition(position) as String
                    val selectedObra = obraMap[selectedObraNombre]
                    Log.d("AutocompleteManager", "Obra seleccionada: $selectedObra") // Agregar log
                    selectedObra?.let {
                        onObraSelected?.invoke(it)

                        // Cerrar el teclado después de seleccionar una obra
                        AppUtils.closeKeyboard(context, autoCompleteTextView)
                    }
                }

                autoCompleteTextView.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                        // No es necesario hacer nada aquí
                    }

                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                        if (s?.isNotEmpty() == true) {
                            autoCompleteTextView.showDropDown()
                        }
                    }

                    override fun afterTextChanged(s: Editable?) {
                        // No es necesario hacer nada aquí
                    }
                })

                autoCompleteTextView.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
                    // No es necesario hacer nada aquí
                }
            })
        }
    }


    fun loadRoles(
        autoCompleteTextView: AutoCompleteTextView? = null, lifecycleOwner: LifecycleOwner? = null,
        onRolSelected: ((Rol) -> Unit)? = null,
    ) {
        viewModel.cargarRoles()
        if (autoCompleteTextView != null && lifecycleOwner != null) {
            viewModel.roles.observe(lifecycleOwner, Observer { roles ->
                val adapter = CustomArrayAdapter(
                    context, R.layout.simple_dropdown_item_1line,
                    roles.map {it.nombre}
                )
                autoCompleteTextView.setAdapter(adapter)

                rolMap.clear()
                rolMap.putAll(roles.associateBy {it.nombre})

                autoCompleteTextView.setOnItemClickListener { parent, _, position, _ ->
                    val selectedRolNombre = parent.getItemAtPosition(position) as String
                    val selectedRol = rolMap[selectedRolNombre]
                    Log.d("AutocompleteManager", "Rol seleccionado: $selectedRol") // Agregar log
                    selectedRol?.let {
                        onRolSelected?.invoke(it)

                        // Cerrar el teclado después de seleccionar un rol
                        //AppUtils.closeKeyboard(context, autoCompleteTextView)
                    }
                }

                autoCompleteTextView.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(
                        s: CharSequence?,
                        start: Int,
                        count: Int,
                        after: Int,
                    ) {
                        // No es necesario hacer nada aquí
                    }

                    override fun onTextChanged(
                        s: CharSequence?,
                        start: Int,
                        before: Int,
                        count: Int,
                    ) {
                        if (s?.isNotEmpty() == true) {
                            autoCompleteTextView.showDropDown()
                        }
                    }

                    override fun afterTextChanged(s: Editable?) {
                        // No es necesario hacer nada aquí
                    }
                })

                autoCompleteTextView.onFocusChangeListener =
                    View.OnFocusChangeListener { _, hasFocus ->
                        // No es necesario hacer nada aquí
                    }

            })
        }
    }

    fun loadEstados(
        autoCompleteTextView: AutoCompleteTextView? = null, lifecycleOwner: LifecycleOwner? = null,
        onEstadoSelected: ((Estado) -> Unit)? = null,
    ) {
        viewModel.cargarEstados()
        if (autoCompleteTextView != null && lifecycleOwner != null) {
            viewModel.estados.observe(lifecycleOwner, Observer { estados ->
                val adapter = CustomArrayAdapter(
                    context, R.layout.simple_dropdown_item_1line,
                    estados.map { it.nombre }
                )
                autoCompleteTextView.setAdapter(adapter)

                estadoMap.clear()
                estadoMap.putAll(estados.associateBy { it.nombre })

                autoCompleteTextView.setOnItemClickListener { parent, _, position, _ ->
                    val selectedEstadoNombre = parent.getItemAtPosition(position) as String
                    val selectedEstado = estadoMap[selectedEstadoNombre]
                    Log.d("AutocompleteManager", "Estado seleccionado: $selectedEstado") // Agregar log

                    selectedEstado?.let {
                        onEstadoSelected?.invoke(it)

                        // Cerrar el teclado después de seleccionar un estado
                        //AppUtils.closeKeyboard(context, autoCompleteTextView)
                    }
                }

                autoCompleteTextView.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(
                        s: CharSequence?,
                        start: Int,
                        count: Int,
                        after: Int,
                    ) {
                        // No es necesario hacer nada aquí
                    }

                    override fun onTextChanged(
                        s: CharSequence?,
                        start: Int,
                        before: Int,
                        count: Int,
                    ) {
                        if (s?.isNotEmpty() == true) {
                            autoCompleteTextView.showDropDown()
                        }
                    }

                    override fun afterTextChanged(s: Editable?) {
                        // No es necesario hacer nada aquí
                    }
                })

                autoCompleteTextView.onFocusChangeListener =
                    View.OnFocusChangeListener { _, hasFocus ->
                        // No es necesario hacer nada aquí
                    }

            })
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

    fun getObraByName(obraName: String): Obra? {
        return obraMap.entries.find { it.key.equals(obraName, ignoreCase = true) }?.value
    }

    fun getObraById(obraId: Int): Obra? {
        return obraMap.values.find { it.id == obraId }
    }

    fun getEquipoByName(equipoName: String): Equipo? {
        return equipoMap.entries.find { it.key.equals(equipoName, ignoreCase = true) }?.value
    }

    fun getEquipoById(equipoId: Int): Equipo? {
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
