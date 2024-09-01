package com.example.adminobr.ui.adapter

import android.content.Context
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.Filterable

class CustomArrayAdapter<T>(context: Context, resource: Int, private val items: List<T>) :
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
                    val filteredList = items.filter {
                        it.toString().contains(constraint, ignoreCase = true)
                    }
                    filterResults.values = filteredList
                    filterResults.count = filteredList.size
                } else {
                    filterResults.values = emptyList<T>()
                    filterResults.count = 0
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
