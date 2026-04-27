package com.navio.damtests.ai

import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIHost
import com.navio.damtests.BuildConfig

class FastExplainer() {
    private val openai = OpenAI(
        host = OpenAIHost("https://api.groq.com/openai/v1/"),
        token = AiConfig.groqKey
    )

    suspend fun explicarRapido(pregunta: String, elegida: String, correcta: String): String {
        val systemPrompt = """
    Eres un profesor de informática de grado superior. Tu alumno es inteligente pero ha fallado esta pregunta.
    Reglas de oro:
    1. PROHIBIDO decir "tu opción es incorrecta" o frases genéricas. 
    2. Debes señalar el error técnico específico de la opción elegida (ej. "Confundes un protocolo de red con uno de transporte").
    3. En la explicación de la correcta, aporta un dato técnico que no esté en el enunciado.
    4. Formato: [Crítica técnica] | [Justificación experta].
    5. Usa el separador '|' sin excepciones.
""".trimIndent()

        val userContent = """
        Pregunta: $pregunta
        Elegida por usuario: $elegida
        Respuesta correcta: $correcta
    """.trimIndent()

        val chatCompletion = openai.chatCompletion(
            ChatCompletionRequest(
                model = ModelId("llama-3.3-70b-versatile"),
                messages = listOf(
                    ChatMessage(role = ChatRole.System, content = systemPrompt),
                    ChatMessage(role = ChatRole.User, content = userContent)
                ),
                temperature = 0.5 // Bajamos la temperatura para que sea más determinista con el formato
            )
        )
        return chatCompletion.choices.first().message.content
            ?: "Error en análisis | Inténtalo de nuevo"
    }
}