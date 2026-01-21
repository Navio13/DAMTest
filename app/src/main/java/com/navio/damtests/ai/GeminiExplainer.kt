package com.navio.damtests.ai // Ajusta según tu paquete real

import com.google.ai.client.generativeai.GenerativeModel
import com.navio.damtests.BuildConfig
import com.navio.damtests.data.local.entity.Question // He visto que esta es tu clase base

class GeminiExplainer {
    // Usamos 1.5-flash que es el más compatible con el SDK 0.7.0
    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    suspend fun explicarFallo(pregunta: Question, respuestaUsuario: Int): String {
        val prompt = """
            Actúa como un profesor de informática. Un alumno ha fallado una pregunta de test.
            Pregunta: ${pregunta.text}
            Opciones:
            0: ${pregunta.optionA}
            1: ${pregunta.optionB}
            2: ${pregunta.optionC}
            3: ${pregunta.optionD}
            El alumno marcó la opción $respuestaUsuario, pero la correcta es la ${pregunta.correctOptionIndex}.
            Explica de forma breve y clara por qué la respuesta correcta es esa.
        """.trimIndent()

        return try {
            val response = generativeModel.generateContent(prompt)
            response.text ?: "No tengo una explicación disponible ahora mismo."
        } catch (e: Exception) {
            "Error al obtener explicación: ${e.localizedMessage}"
        }
    }
}