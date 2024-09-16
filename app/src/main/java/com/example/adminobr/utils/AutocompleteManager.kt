package com.example.adminobr.utils


import android.content.Context
import android.util.Log
import android.view.View
import android.widget.AutoCompleteTextView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.example.adminobr.data.Empresa
import com.example.adminobr.data.Equipo
import com.example.adminobr.data.Obra
import com.example.adminobr.ui.adapter.CustomArrayAdapter
import com.example.adminobr.viewmodel.AppDataViewModel


class AutocompleteManager(private val context: Context, private val viewModel: AppDataViewModel) {


    // HashMap para almacenar la relación nombre-Empresa
    private val empresaMap = HashMap<String, Empresa>()
    private val obraMap = HashMap<String, Obra>()
    private val equipoMap = HashMap<String, Equipo>()


    // Callback para manejar la selección de la empresa
    fun loadEmpresas(autoCompleteTextView: AutoCompleteTextView? = null, lifecycleOwner: LifecycleOwner? = null,
                     onEmpresaSelected: ((Empresa) -> Unit)? = null
    ) {
        viewModel.cargarEmpresas()
        if (autoCompleteTextView != null && lifecycleOwner != null) {
            viewModel.empresas.observe(lifecycleOwner, Observer { empresas ->
                val adapter = CustomArrayAdapter(
                    context, android.R.layout.simple_dropdown_item_1line,
                    empresas.map { it.nombre }
                )
                autoCompleteTextView.setAdapter(adapter)


                empresaMap.clear()
                empresaMap.putAll(empresas.associateBy { it.nombre })


                autoCompleteTextView.setOnItemClickListener { parent, _, position, _ ->
                    val selectedEmpresaNombre = parent.getItemAtPosition(position) as String
                    val selectedEmpresa = empresaMap[selectedEmpresaNombre]
                    selectedEmpresa?.let { onEmpresaSelected?.invoke(it) }
                }


                autoCompleteTextView.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
                    if (hasFocus && autoCompleteTextView.text.isNotEmpty()) {
                        autoCompleteTextView.showDropDown()
                    }
                }
            })
        }
    }


    fun loadEquipos(autoCompleteTextView: AutoCompleteTextView? = null, lifecycleOwner: LifecycleOwner? = null,
                    onEquipoSelected: ((Equipo) -> Unit)? = null
    ) {
        viewModel.cargarEquipos()
        if (autoCompleteTextView != null && lifecycleOwner != null) {
            viewModel.equipos.observe(lifecycleOwner, Observer { equipos ->
                val adapter = CustomArrayAdapter(
                    context, android.R.layout.simple_dropdown_item_1line,
                    equipos.map { "${it.interno} - ${it.descripcion}" }
                )
                autoCompleteTextView.setAdapter(adapter)


                equipoMap.clear()
                equipoMap.putAll(equipos.associateBy { "${it.interno} - ${it.descripcion}" })

                autoCompleteTextView.setOnItemClickListener { parent, _, position, _ ->
                    val selectedEquipoNombre = parent.getItemAtPosition(position) as String
                    val selectedEquipo = equipoMap[selectedEquipoNombre]
                    Log.d("AutocompleteManager", "Equipo seleccionado: $selectedEquipo") // Agregar log
                    selectedEquipo?.let { onEquipoSelected?.invoke(it) }
                }


                autoCompleteTextView.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
                    if (hasFocus && autoCompleteTextView.text.isNotEmpty()) {
                        autoCompleteTextView.showDropDown()
                    }
                }
            })
        }
    }


    fun loadObras(autoCompleteTextView: AutoCompleteTextView? = null, lifecycleOwner: LifecycleOwner? = null,
                  onObraSelected: ((Obra) -> Unit)? = null
    ) {
        viewModel.cargarObras()
        if (autoCompleteTextView != null && lifecycleOwner != null) {
            viewModel.obras.observe(lifecycleOwner, Observer { obras ->
                val adapter = CustomArrayAdapter(
                    context, android.R.layout.simple_dropdown_item_1line,
                    obras.map { "${it.centro_costo} - ${it.nombre}" }
                )
                autoCompleteTextView.setAdapter(adapter)


                obraMap.clear()
                obraMap.putAll(obras.associateBy { "${it.centro_costo} - ${it.nombre}" })


                autoCompleteTextView.setOnItemClickListener { parent, _, position, _ ->
                    val selectedObraNombre = parent.getItemAtPosition(position) as String
                    val selectedObra = obraMap[selectedObraNombre]
                    Log.d("AutocompleteManager", "Obra seleccionada: $selectedObra") // Agregar log
                    selectedObra?.let { onObraSelected?.invoke(it) }
                }


                autoCompleteTextView.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
                    if (hasFocus && autoCompleteTextView.text.isNotEmpty()) {
                        autoCompleteTextView.showDropDown()
                    }
                }
            })
        }
    }


    fun getEmpresaByName(empresaName: String): Empresa? {
        return empresaMap[empresaName]
    }


    fun getObraByName(obraName: String): Obra? {
        return obraMap[obraName]
    }


    fun getEquipoByName(equipoName: String): Equipo? {
        return equipoMap[equipoName]
    }
}
