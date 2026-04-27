package com.navio.damtests.ai

object AiConfig {
    var geminiKey: String = ""
    var groqKey: String = ""

    // Función de utilidad para saber si ya tenemos las llaves
    fun isReady(): Boolean = geminiKey.isNotEmpty() && groqKey.isNotEmpty()
}