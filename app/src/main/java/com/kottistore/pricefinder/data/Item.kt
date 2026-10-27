package com.kottistore.pricefinder.data

data class Item(
    val id: String,
    val name: String,
    val alias: String,
    val category: String,
    val purRate: Double?,
    val selRate: Double?
)
