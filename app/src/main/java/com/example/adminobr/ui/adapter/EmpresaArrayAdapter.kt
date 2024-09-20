package com.example.adminobr.ui.adapter

import android.content.Context
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.Filterable
import java.util.Locale

class EmpresaArrayAdapter<T>(context: Context, resource: Int, private val items: List<T>) :
    ArrayAdapter<T>(context, resource, items), Filterable {

    private var filteredItems: List<T> = items

    override fun getCount(): Int {
        return filteredItems.size
    }

    override fun getItem(position: Int): T? {
        return filteredItems[position]
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val filterResults = FilterResults()
                if (constraint != null && constraint.isNotEmpty()) {
                    val filterPattern = constraint.toString().lowercase(Locale.getDefault()).trim()
                    val regex = Regex("\\b$filterPattern")
                    val filteredList = items.filter {
                        regex.containsMatchIn(it.toString().lowercase(Locale.getDefault()))
                    }
                    filterResults.values = filteredList
                    filterResults.count = filteredList.size
                } else {
                    filterResults.values = items
                    filterResults.count = items.size
                }
                return filterResults
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredItems = if (results != null && results.count > 0) {
                    results.values as List<T>
                } else {
                    emptyList()
                }
                notifyDataSetChanged()
            }
        }
    }
}
