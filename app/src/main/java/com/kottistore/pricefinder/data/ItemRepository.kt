package com.kottistore.pricefinder.data

import android.content.Context
import org.json.JSONObject

class ItemRepository(private val context: Context) {

    fun load(): List<Item> {
        val text = context.assets.open("items.json").bufferedReader().use { it.readText() }
        val root = JSONObject(text)
        val array = root.getJSONArray("items")
        val items = ArrayList<Item>(array.length())
        for (index in 0 until array.length()) {
            val obj = array.getJSONObject(index)
            items.add(
                Item(
                    id = obj.optString("id"),
                    name = obj.optString("name"),
                    alias = obj.optString("alias"),
                    category = obj.optString("category"),
                    purRate = if (obj.isNull("purRate")) null else obj.optDouble("purRate"),
                    selRate = if (obj.isNull("selRate")) null else obj.optDouble("selRate")
                )
            )
        }
        return items
    }
}
