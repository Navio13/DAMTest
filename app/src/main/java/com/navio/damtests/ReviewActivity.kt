package com.navio.damtests

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.navio.damtests.ai.GeminiExplainer // Asegúrate de que el paquete sea este
import com.navio.damtests.ui.viewmodel.QuestionResult
import kotlinx.coroutines.launch

class ReviewActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_review)

        // 1. Recuperamos los datos reales enviados desde QuizActivity
        val score = intent.getIntExtra("SCORE", 0)
        val total = intent.getIntExtra("TOTAL", 0)

        // 2. Actualizamos el texto con valores REALES
        findViewById<TextView>(R.id.tvReviewScore).text = "Resultado final: $score / $total"

        // 3. Configuramos el RecyclerView con la IA
        val rv = findViewById<RecyclerView>(R.id.rvReview)
        rv.layoutManager = LinearLayoutManager(this)

        // Pasamos una función (lambda) al adapter para manejar el click en "Ver Explicación"
        rv.adapter = ReviewAdapter(TestDataHolder.lastResults) { result ->
            showAiExplanation(result)
        }

        // 4. Botones de navegación (Tus botones originales)
        findViewById<MaterialButton>(R.id.btnBackToMenu).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }

        findViewById<MaterialButton>(R.id.btnRepeatTest).setOnClickListener {
            val intent = Intent(this, QuizActivity::class.java).apply {
                putExtra("SUBJECT_ID", TestDataHolder.currentSubjectId)
                putExtra("TOPIC_ID", TestDataHolder.currentTopicId)
            }
            startActivity(intent)
            finish()
        }
    }

    /**
     * Muestra un diálogo con la explicación generada por IA
     */
    private fun showAiExplanation(result: QuestionResult) {
        // Creamos el diálogo de espera
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Explicación de la IA")
            .setMessage("Analizando la pregunta y generando respuesta...")
            .setPositiveButton("Cerrar", null)
            .show()

        // Lanzamos la corrutina para no bloquear la app mientras la IA piensa
        lifecycleScope.launch {
            try {
                val explainer = GeminiExplainer()
                // Llamamos a la IA pasándole la pregunta y el índice que marcó el usuario
                val explanation = explainer.explicarFallo(result.question, result.userSelectedIndex)

                // Una vez recibida, actualizamos el texto del diálogo
                dialog.setMessage(explanation)
            } catch (e: Exception) {
                dialog.setMessage("No se pudo obtener la explicación: ${e.message}")
            }
        }
    }
}