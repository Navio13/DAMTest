package com.navio.damtests

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.navio.damtests.ai.FastExplainer
import com.navio.damtests.ai.GeminiExplainer
import com.navio.damtests.data.local.db.AppDatabase
import com.navio.damtests.data.local.entity.Question
import com.navio.damtests.ui.viewmodel.QuizViewModel
import com.navio.damtests.ui.viewmodel.QuizViewModelFactory
import kotlinx.coroutines.launch

class QuizActivity : AppCompatActivity() {

    private lateinit var viewModel: QuizViewModel
    private lateinit var tvQuestion: TextView
    private lateinit var btnA: Button
    private lateinit var btnB: Button
    private lateinit var btnC: Button
    private lateinit var btnD: Button
    private lateinit var tvCount: TextView
    private lateinit var progressBar: ProgressBar
    private var currentShuffledQuestion: ShuffledQuestion? = null
    private lateinit var btnContextInfo: Button // Al principio de la clase con los demás
    private val gemini = GeminiExplainer()
    private val groq = FastExplainer()
    private lateinit var btnNext: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz)

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        window.statusBarColor = Color.parseColor("#F8FAFC")

        tvQuestion = findViewById(R.id.tvQuestionText)
        btnA = findViewById(R.id.btnOptionA)
        btnB = findViewById(R.id.btnOptionB)
        btnC = findViewById(R.id.btnOptionC)
        btnD = findViewById(R.id.btnOptionD)
        tvCount = findViewById(R.id.tvQuestionCount)
        progressBar = findViewById(R.id.quizProgressBar)
        btnContextInfo = findViewById(R.id.btnContextInfo)
        btnNext = findViewById(R.id.btnNextQuestion)

        val database = AppDatabase.getDatabase(this)
        val repository = QuizRepository(database.questionsDao())
        val factory = QuizViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[QuizViewModel::class.java]

        val subjectId = intent.getStringExtra("SUBJECT_ID") ?: "programacion"
        val topicId = intent.getStringExtra("TOPIC_ID") ?: "1"

        if (topicId.startsWith("-")) 20 else 10

        TestDataHolder.currentSubjectId = subjectId
        TestDataHolder.currentTopicId = topicId

        setupObservers()
        setupClickListeners()

        viewModel.loadQuestions(subjectId, topicId)
    }

    private fun setupObservers() {
        // 1. Observador de la lista de preguntas
        lifecycleScope.launchWhenStarted {
            viewModel.questions.collect { questions ->
                if (questions.isNotEmpty()) {
                    // Configuramos la barra
                    progressBar.max = questions.size

                    // FORZAMOS el texto inicial aquí mismo
                    val initialPos = viewModel.currentQuestionIndex.value + 1
                    tvCount.text = "$initialPos de ${questions.size}"
                    progressBar.progress = initialPos

                    updateUI(questions[viewModel.currentQuestionIndex.value])
                }
            }
        }

        // 2. Observador del índice (para cuando pases a la siguiente pregunta)
        lifecycleScope.launchWhenStarted {
            viewModel.currentQuestionIndex.collect { index ->
                val questions = viewModel.questions.value
                if (questions.isNotEmpty()) {
                    val currentPos = index + 1
                    tvCount.text = "$currentPos de ${questions.size}"
                    progressBar.progress = currentPos

                    // --- NUEVA LÓGICA AQUÍ ---
                    if (currentPos == questions.size) {
                        btnNext.text = "Finalizar Test"
                        // Opcional: puedes cambiarle el color para que resalte
                        // btnNext.setBackgroundColor(Color.parseColor("#10B981"))
                    } else {
                        btnNext.text = "Siguiente Pregunta"
                    }
                    // -------------------------

                    updateUI(questions[index])
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.isLoading.collect { loading ->
                setButtonsEnabled(!loading)
                if (loading) tvQuestion.text = "Cargando..."
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.isTestFinished.collect { finished ->
                if (finished) showResultsDialog(viewModel.score.value)
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.currentAnswerState.collect { result ->
                if (result != null) highlightButtons(result)
                else resetUI()
            }
        }
    }

    private fun updateUI(question: Question) {
        val shuffled = question.shuffle()
        currentShuffledQuestion = shuffled

        tvQuestion.text = shuffled.originalQuestion.text
        btnA.text = shuffled.shuffledOptions[0]
        btnB.text = shuffled.shuffledOptions[1]
        btnC.text = shuffled.shuffledOptions[2]
        btnD.text = shuffled.shuffledOptions[3]

        // Lógica del enunciado/contexto
        if (!question.contextText.isNullOrEmpty()) {
            btnContextInfo.visibility = View.VISIBLE
            btnContextInfo.setOnClickListener {
                showContextDialog(question.contextText)
            }
        } else {
            btnContextInfo.visibility = View.GONE
        }
    }

    private fun setupClickListeners() {
        btnA.setOnClickListener { processAnswer(0) }
        btnB.setOnClickListener { processAnswer(1) }
        btnC.setOnClickListener { processAnswer(2) }
        btnD.setOnClickListener { processAnswer(3) }
        btnNext.setOnClickListener { viewModel.goToNextQuestion() }
    }

    private fun processAnswer(uiSelectedIndex: Int) {
        val shuffled = currentShuffledQuestion ?: return
        val buttons = listOf(btnA, btnB, btnC, btnD)

        // Obtenemos el texto que hay escrito en el botón que el usuario ha pulsado
        val textSelected = buttons[uiSelectedIndex].text.toString()

        // Le pasamos al ViewModel el texto y la lista de opciones tal cual están en pantalla
        viewModel.checkAnswer(textSelected, shuffled.shuffledOptions)
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        listOf(btnA, btnB, btnC, btnD).forEach { it.isEnabled = enabled }
    }

    private fun showResultsDialog(score: Int) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_results, null)
        val dialog = AlertDialog.Builder(this).setView(dialogView).setCancelable(false).create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<TextView>(R.id.tvDialogMessage).text =
            "Has acertado $score preguntas."

        dialogView.findViewById<Button>(R.id.btnDialogReview).setOnClickListener {
            dialog.dismiss()
            showReviewScreen()
        }

        dialogView.findViewById<Button>(R.id.btnDialogExit).setOnClickListener {
            dialog.dismiss()
            finish()
        }

        dialog.show()
    }

    private fun showReviewScreen() {
        // Obtenemos los resultados reales del ViewModel
        val results = viewModel.getResults()
        results.size
        results.count { it.userSelectedIndex == it.question.correctOptionIndex }

        // Guardamos en el Holder para la lista
        TestDataHolder.lastResults = results

        // Enviamos los datos por Intent para el encabezado
        val intent = Intent(this, ReviewActivity::class.java).apply {
            putExtra("SCORE", viewModel.score.value)
            putExtra("TOTAL", results.size)
        }
        startActivity(intent)
        finish()
    }

    private fun showContextDialog(text: String) {
        AlertDialog.Builder(this).setTitle("Enunciado del Caso").setMessage(text)
            .setPositiveButton("Cerrar", null).show()
    }

    private fun highlightButtons(result: QuizViewModel.AnswerResult) {
        val buttons = listOf(btnA, btnB, btnC, btnD)
        val feedbacks = listOf<TextView>(
            findViewById(R.id.tvFeedbackA),
            findViewById(R.id.tvFeedbackB),
            findViewById(R.id.tvFeedbackC),
            findViewById(R.id.tvFeedbackD)
        )

        setButtonsEnabled(false)

        // 1. Pintamos los bordes de los botones (Lógica de textos infalible)
        val shuffled = currentShuffledQuestion ?: return
        val question = shuffled.originalQuestion
        val correctText = when (question.correctOptionIndex) {
            0 -> question.optionA
            1 -> question.optionB
            2 -> question.optionC
            else -> question.optionD
        }

        buttons.forEachIndexed { index, button ->
            val mBtn = button as com.google.android.material.button.MaterialButton
            val isCorrectBtn = mBtn.text == correctText
            val isSelectedBtn = index == result.selectedIndex

            if (isCorrectBtn) {
                mBtn.setStrokeColorResource(android.R.color.holo_green_dark)
                mBtn.strokeWidth = 8
            } else if (isSelectedBtn && !result.isCorrect) {
                mBtn.setStrokeColorResource(android.R.color.holo_red_dark)
                mBtn.strokeWidth = 8
            }
        }

        // 2. Feedback de carga y llamada a la IA
        if (!result.isCorrect) {
            // Mostramos un estado de carga en los sitios donde irán las explicaciones
            feedbacks[result.selectedIndex].apply {
                visibility = View.VISIBLE
                text = "⏳ Analizando tu respuesta..."
                setTextColor(Color.GRAY)
            }
            feedbacks[result.correctIndex].apply {
                visibility = View.VISIBLE
                text = "⏳ Preparando corrección..."
                setTextColor(Color.GRAY)
            }

            lifecycleScope.launch {
                // 1. Extraemos los textos de las opciones para que la IA sepa qué dicen
                val textoElegido = buttons[result.selectedIndex].text.toString()
                val textoCorrecto = buttons[result.correctIndex].text.toString()

                // 2. Ahora sí pasamos los STRINGS: el texto de la pregunta y los textos de las opciones
                try {
                    val fullResponse = groq.explicarRapido(
                        pregunta = question.text, elegida = textoElegido, correcta = textoCorrecto
                    )

                    val partes = fullResponse.split("|")

                    if (partes.size >= 2) {
                        feedbacks[result.selectedIndex].apply {
                            text = "❌ ${partes[0].trim()}"
                            setTextColor(Color.parseColor("#EF4444"))
                        }
                        feedbacks[result.correctIndex].apply {
                            text = "✅ ${partes[1].trim()}"
                            setTextColor(Color.parseColor("#10B981"))
                        }
                    } else {
                        feedbacks[result.correctIndex].text = "✅ $fullResponse"
                        feedbacks[result.correctIndex].setTextColor(Color.parseColor("#10B981"))
                        feedbacks[result.selectedIndex].visibility = View.GONE
                    }
                } catch (e: Exception) {
                    feedbacks[result.selectedIndex].text = "❌ Error al conectar con la IA"
                }
            }
        }

        btnNext.visibility = View.VISIBLE
    }

    private fun resetUI() {
        val buttons = listOf(btnA, btnB, btnC, btnD)
        val feedbacks = listOf<TextView>(
            findViewById(R.id.tvFeedbackA),
            findViewById(R.id.tvFeedbackB),
            findViewById(R.id.tvFeedbackC),
            findViewById(R.id.tvFeedbackD)
        )

        buttons.forEach {
            (it as com.google.android.material.button.MaterialButton).strokeWidth = 0
        }
        feedbacks.forEach {
            it.visibility = View.GONE
            it.text = ""
        }
        btnNext.visibility = View.GONE
        setButtonsEnabled(true)
    }
}