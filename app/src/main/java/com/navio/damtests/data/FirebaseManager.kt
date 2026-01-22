package com.navio.damtests.data

import android.content.Context
import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import com.navio.damtests.QuizRepository
import com.navio.damtests.data.local.entity.Question
import kotlinx.coroutines.tasks.await

class FirebaseManager(private val context: Context, private val repository: QuizRepository) {

    private val db = FirebaseDatabase.getInstance().reference
    private val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    suspend fun checkAndUpdateQuestions() {
        try {
            // 1. Mirar versión en Firebase
            val remoteVersion = db.child("metadata").child("version_preguntas").get().await().getValue(Int::class.java) ?: 0
            val localVersion = prefs.getInt("local_question_version", 0)

            if (remoteVersion > localVersion) {
                Log.d("FIREBASE", "Nueva versión detectada ($remoteVersion). Descargando...")

                // 2. Descargar preguntas
                // Asumimos que guardas todas las preguntas en un nodo "preguntas"
                val snapshot = db.child("preguntas").get().await()
                val nuevasPreguntas = mutableListOf<Question>()

                snapshot.children.forEach { child ->
                    // Aquí el mapeo depende de cómo subas el JSON a Firebase
                    val q = child.getValue(Question::class.java)
                    q?.let { nuevasPreguntas.add(it) }
                }

                if (nuevasPreguntas.isNotEmpty()) {
                    // 3. Actualizar Room
                    repository.refreshQuestions(nuevasPreguntas)

                    // 4. Guardar nueva versión local
                    prefs.edit().putInt("local_question_version", remoteVersion).apply()
                    Log.d("FIREBASE", "Base de datos actualizada correctamente")
                }
            } else {
                Log.d("FIREBASE", "La base de datos está al día (Versión $localVersion)")
            }
        } catch (e: Exception) {
            Log.e("FIREBASE", "Error en sincronización (Sin internet o error): ${e.message}")
            // Si falla (ej. no hay internet), no hacemos nada.
            // La app usará lo que ya tiene en Room por defecto.
        }
    }
}