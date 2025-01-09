package com.example.adminobr.ui.adapter

import android.content.Context
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.Filterable

// Adaptador personalizado para un AutoCompleteTextView que admite filtrado y personalización de elementos mostrados y filtrados
class CustomArrayAdapter<T>(
    context: Context, // Contexto de la aplicación
    resource: Int, // Diseño del elemento de lista (generalmente simple_dropdown_item_1line)
    private val items: List<T>, // Lista completa de elementos a mostrar
    private val itemToDisplay: (T) -> String, // Función que determina qué texto se muestra en el desplegable
    private val itemToFilter: (T) -> String  // Función que determina el texto por el que se filtran los elementos
) : ArrayAdapter<T>(context, resource, items), Filterable {

    // Lista filtrada de elementos que se mostrará en el desplegable
    private var filteredItems: List<T> = items

    // Devuelve la cantidad de elementos filtrados actualmente
    override fun getCount(): Int {
        return filteredItems.size
    }

    // Devuelve el elemento en una posición específica dentro de la lista filtrada
    override fun getItem(position: Int): T? {
        return filteredItems[position]
    }

    // Devuelve el filtro personalizado para el adaptador
    override fun getFilter(): Filter {
        return object : Filter() {

            // Filtrado de elementos según el texto ingresado por el usuario
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val filterResults = FilterResults()
                if (!constraint.isNullOrEmpty()) {
                    // Filtrar la lista original utilizando la función `itemToFilter`
                    val filteredList = items.filter {
                        itemToFilter(it).contains(constraint, ignoreCase = true) // Coincidencias sin importar mayúsculas/minúsculas
                    }
                    // Asignar los resultados filtrados al objeto de resultados
                    filterResults.values = filteredList
                    filterResults.count = filteredList.size
                } else {
                    // Si no hay filtro, mostrar todos los elementos
                    filterResults.values = items
                    filterResults.count = items.size
                }
                return filterResults
            }

            // Publicar los resultados filtrados para que se actualice la lista mostrada en el desplegable
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredItems = if (results != null && results.count > 0) {
                    results.values as List<T> // Actualizar la lista filtrada
                } else {
                    emptyList() // Si no hay resultados, mostrar una lista vacía
                }
                notifyDataSetChanged() // Notificar al adaptador que los datos han cambiado
            }
        }
    }

    // Personaliza cómo se muestra cada elemento en el desplegable
    override fun getView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
        // Utiliza el diseño base proporcionado
        val view = super.getView(position, convertView, parent)
        // Obtiene la referencia al TextView donde se mostrará el texto
        val textView = view.findViewById<android.widget.TextView>(android.R.id.text1)
        // Establece el texto utilizando la función `itemToDisplay`
        textView.text = itemToDisplay(filteredItems[position])
        return view
    }
}
