package com.example.adminobr.utils

import android.content.Context
import android.view.View
import android.widget.AutoCompleteTextView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.example.adminobr.data.Empresa
import com.example.adminobr.ui.adapter.CustomArrayAdapter
import com.example.adminobr.viewmodel.AppDataViewModel

class AutocompleteManager(private val context: Context, private val viewModel: AppDataViewModel) {

    // HashMap para almacenar la relación nombre-Empresa
    private val empresaMap = HashMap<String, Empresa>()

    // Callback para manejar la selección de la empresa
    fun loadEmpresas(
        autoCompleteTextView: AutoCompleteTextView? = null,
        lifecycleOwner: LifecycleOwner? = null,
        onEmpresaSelected: ((Empresa) -> Unit)? = null // Callback para devolver la empresa seleccionada
    ) {
        viewModel.cargarEmpresas()
        if (autoCompleteTextView != null && lifecycleOwner != null) {
            viewModel.empresas.observe(lifecycleOwner, Observer { empresas ->
                val adapter = CustomArrayAdapter(
                    context, android.R.layout.simple_dropdown_item_1line,
                    empresas.map { it.nombre }
                )
                autoCompleteTextView.setAdapter(adapter)

                // Guardar la relación nombre-Empresa en el HashMap
                //val empresaMap = empresas.associateBy { it.nombre }
                // Llenar el empresaMap de la clase
                empresaMap.clear()
                empresaMap.putAll(empresas.associateBy { it.nombre })

                // Agregar OnItemClickListener para obtener el objeto Empresa al seleccionar una empresa
                autoCompleteTextView.setOnItemClickListener { parent, _, position, _ ->
                    val selectedEmpresaNombre = parent.getItemAtPosition(position) as String
                    val selectedEmpresa = empresaMap[selectedEmpresaNombre]

                    // Llamar al callback con la Empresa seleccionada
                    selectedEmpresa?.let {
                        onEmpresaSelected?.invoke(it)
                    }
                }

                autoCompleteTextView.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
                    if (hasFocus && autoCompleteTextView.text.isNotEmpty()) {
                        autoCompleteTextView.showDropDown()
                    }
                }
            })
        }
    }

    fun loadEquipos(autoCompleteTextView: AutoCompleteTextView? = null, lifecycleOwner: LifecycleOwner? = null) {
        viewModel.cargarEquipos()
        if (autoCompleteTextView != null && lifecycleOwner != null) {
            viewModel.equipos.observe(lifecycleOwner) { equipos ->
//                val adapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, equipos.map { "${it.interno} - ${it.descripcion}" })
                val adapter = CustomArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, equipos.map { "${it.interno} - ${it.descripcion}" })
                autoCompleteTextView.setAdapter(adapter)
            }
        }
    }

    fun loadObras(autoCompleteTextView: AutoCompleteTextView? = null, lifecycleOwner: LifecycleOwner? = null) {
        viewModel.cargarObras()
        if (autoCompleteTextView != null && lifecycleOwner != null) {
            viewModel.obras.observe(lifecycleOwner) { obras ->
//                val adapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, obras.map { "${it.centro_costo} - ${it.nombre}" })
                val adapter = CustomArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, obras.map { "${it.centro_costo} - ${it.nombre}" })
                autoCompleteTextView.setAdapter(adapter)
            }
        }
    }

    fun getEmpresaByName(empresaName: String): Empresa? {
        return empresaMap[empresaName]
    }
}
