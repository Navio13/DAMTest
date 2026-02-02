package com.navio.damtests.ai

import com.google.ai.client.generativeai.GenerativeModel
import com.navio.damtests.BuildConfig
import com.navio.damtests.data.local.entity.Question // He visto que esta es tu clase base

class GeminiExplainer {
    private val generativeModel = GenerativeModel(
        modelName = "gemini-3.0-pro-preview",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    suspend fun explicarFallo(pregunta: Question, respuestaUsuario: Int): String {
        val prompt = """
            Actúa como un profesor de DAM. Un alumno o alumna ha fallado una pregunta de test.
            Enunciado general: ${pregunta.contextText} ? "no tiene"
            Pregunta: ${pregunta.text}
            Opciones:
            0: ${pregunta.optionA}
            1: ${pregunta.optionB}
            2: ${pregunta.optionC}
            3: ${pregunta.optionD}
            El alumno marcó la opción $respuestaUsuario, pero la correcta es la ${pregunta.correctOptionIndex}.
            Explica de forma breve y clara por qué la respuesta correcta es esa. No te extiendas demasiado pero sé muy claro en tu respuesta,
            como si fueras un profesor de DAM.
        """.trimIndent()

        return try {
            val response = generativeModel.generateContent(prompt)
            response.text ?: "No tengo una explicación disponible ahora mismo."
        } catch (e: Exception) {
            "Error al obtener explicación: ${e.localizedMessage}"
        }
    }

    suspend fun explicarFalloFlash(pregunta: Question, respuestaUsuario: Int): String {
        val prompt = """
        Eres un tutor técnico de DAM. El alumno ha fallado.
        Pregunta: ${pregunta.text}
        Opciones: 0:${pregunta.optionA}, 1:${pregunta.optionB}, 2:${pregunta.optionC}, 3:${pregunta.optionD}
        Correcta: ${pregunta.correctOptionIndex} | Usuario eligió: $respuestaUsuario
        
        Dame dos frases MUY CORTAS (máximo 10 palabras cada una) separadas por el símbolo |.
        La primera frase debe explicar por qué la opción $respuestaUsuario es incorrecta.
        La segunda debe explicar por qué la opción ${pregunta.correctOptionIndex} es la correcta.
        Formato: Explicación fallo | Explicación acierto
    """.trimIndent()

        return try {
            val response = generativeModel.generateContent(prompt)
            response.text ?: "No tengo una explicación disponible ahora mismo."
        } catch (e: Exception) {
            "Error al obtener explicación: ${e.localizedMessage}"
        }
    }
}