package com.navio.damtests.data.local.entity

data class Subject(
    val id: String,
    val name: String,
    val iconRes: Int, // Aquí guardaremos el ID del icono (ej: R.drawable.ic_code)
    val colorRes: Int  // Un color para cada una
)