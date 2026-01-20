package com.navio.damtests.ui.viewmodel

import com.navio.damtests.data.local.entity.Question

// En tu archivo donde esté definido QuestionResult
data class QuestionResult(
    val question: Question,
    val userSelectedIndex: Int, // El índice original (0-3) para lógica de negocio
    val shuffledOptions: List<String> // El orden real que vio el usuario
)