package com.navio.damtests

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.navio.damtests.ui.viewmodel.QuestionResult

class ReviewAdapter(private val results: List<QuestionResult>) :
    RecyclerView.Adapter<ReviewAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvQuestion: TextView = view.findViewById(R.id.tvReviewQuestion)
        val tvOptions: TextView = view.findViewById(R.id.tvReviewOptions)
        val card: MaterialCardView = view.findViewById(R.id.cardReview)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Inflamos el nuevo item_review que es un MaterialCardView
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_review, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val res = results[position]
        val q = res.question
        val isCorrect = res.userSelectedIndex == q.correctOptionIndex

        holder.tvQuestion.text = "${position + 1}. ${q.text}"

        val optionsText = StringBuilder()
        val labels = listOf("a", "b", "c", "d")

        // USAMOS LAS OPCIONES MEZCLADAS GUARDADAS
        val displayedOptions = res.shuffledOptions

        // Obtenemos el texto de la respuesta que era correcta originalmente
        val correctText = when(q.correctOptionIndex) {
            0 -> q.optionA
            1 -> q.optionB
            2 -> q.optionC
            else -> q.optionD
        }

        // Obtenemos el texto que el usuario eligió realmente
        val userSelectedText = when(res.userSelectedIndex) {
            0 -> q.optionA
            1 -> q.optionB
            2 -> q.optionC
            else -> q.optionD
        }

        for (i in displayedOptions.indices) {
            val currentOptionText = displayedOptions[i]

            val prefix = when {
                // Si este texto es el correcto -> Check
                currentOptionText == correctText -> "✅ "
                // Si el usuario eligió este texto y está mal -> X
                currentOptionText == userSelectedText && !isCorrect -> "❌ "
                else -> "      "
            }
            optionsText.append("$prefix ${labels[i]}) $currentOptionText\n")
        }
        holder.tvOptions.text = optionsText.toString().trim()

        // LÓGICA DE COLORES
        val card = holder.card
        if (isCorrect) {
            card.setCardBackgroundColor(Color.parseColor("#DCFCE7"))
            card.strokeColor = Color.parseColor("#22C55E")
        } else {
            card.setCardBackgroundColor(Color.parseColor("#FEE2E2"))
            card.strokeColor = Color.parseColor("#EF4444")
        }
    }

    override fun getItemCount() = results.size
}