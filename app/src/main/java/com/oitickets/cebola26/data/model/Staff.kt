package com.oitickets.cebola26.data.model

data class Staff(
    val id: String,
    val name: String,
    val lastLogin: String
) {
    constructor() : this("", "", "")
}