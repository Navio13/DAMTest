package com.navio.damtests

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class ReviewActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_review)

        // Recuperamos los datos reales enviados desde QuizActivity
        val score = intent.getIntExtra("SCORE", 0)
        val total = intent.getIntExtra("TOTAL", 0)

        // Actualizamos el texto con valores REALES
        findViewById<TextView>(R.id.tvReviewScore).text = "Resultado final: $score / $total"

        val rv = findViewById<RecyclerView>(R.id.rvReview)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = ReviewAdapter(TestDataHolder.lastResults)

        // Usamos MaterialButton para evitar errores de casteo con el nuevo XML
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
}