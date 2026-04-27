package com.navio.damtests.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.navio.damtests.QuizRepository
import com.navio.damtests.data.local.entity.Question
import com.navio.damtests.data.local.entity.TopicProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class QuizViewModel(private val repository: QuizRepository) : ViewModel() {

    private val _questions = MutableStateFlow<List<Question>>(emptyList())
    val questions: StateFlow<List<Question>> = _questions

    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex

    private val _score = MutableStateFlow(0)
    val score: StateFlow<Int> = _score

    private val _isTestFinished = MutableStateFlow(false)
    val isTestFinished: StateFlow<Boolean> = _isTestFinished

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _resultsList = mutableListOf<QuestionResult>()

    private val _currentAnswerState = MutableStateFlow<AnswerResult?>(null)
    val currentAnswerState: StateFlow<AnswerResult?> = _currentAnswerState

    data class AnswerResult(val selectedIndex: Int, val correctIndex: Int, val isCorrect: Boolean)

    // Cargar preguntas al iniciar el test - Ahora recibe String
    fun loadQuestions(subjectId: String, topicId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _resultsList.clear()
            _score.value = 0
            _currentQuestionIndex.value = 0

            val limit = if (topicId.startsWith("-")) 20 else 10

            val loadedQuestions = when (topicId) {
                "-1" -> repository.getRandomQuestionsForGeneralTest(subjectId, limit)
                "-2" -> repository.getQuestionsForRange(subjectId, 1, 10, limit)  // Bloque 1
                "-3" -> repository.getQuestionsForRange(subjectId, 11, 20, limit) // Bloque 2
                else -> repository.getQuestionsByTopic(subjectId, topicId, limit)
            }

            _questions.value = loadedQuestions
            _isLoading.value = false
        }
    }

    // Comprobar respuesta
    fun checkAnswer(selectedText: String, shuffledOptions: List<String>) {
        val currentQuestion = _questions.value.getOrNull(_currentQuestionIndex.value) ?: return

        val correctText = when(currentQuestion.correctOptionIndex) {
            0 -> currentQuestion.optionA
            1 -> currentQuestion.optionB
            2 -> currentQuestion.optionC
            else -> currentQuestion.optionD
        }

        val isCorrect = selectedText == correctText

        // GUARDAMOS EL RESULTADO CON EL BOOLEANO YA CALCULADO
        val uiIndex = shuffledOptions.indexOf(selectedText)
        _resultsList.add(QuestionResult(currentQuestion, uiIndex, shuffledOptions, isCorrect)) // <--- CAMBIO AQUÍ

        if (isCorrect) _score.value += 1

        val correctUiIndex = shuffledOptions.indexOf(correctText)
        _currentAnswerState.value = AnswerResult(uiIndex, correctUiIndex, isCorrect)
    }

    fun goToNextQuestion() {
        _currentAnswerState.value = null // Reseteamos colores
        if (_currentQuestionIndex.value < _questions.value.size - 1) {
            _currentQuestionIndex.value += 1
        } else {
            _isTestFinished.value = true
            val q = _questions.value.first()
            saveFinalProgress(q.subjectId, q.topicId)
        }
    }

    // Guardar progreso - Cambiado a String
    private fun saveFinalProgress(subjectId: String, topicId: String) {
        viewModelScope.launch {
            // 1. Buscamos si ya existe progreso previo para este tema
            val currentProgress = repository.getProgress(subjectId, topicId)

            // 2. Calculamos el nuevo número de intentos
            val newAttemptsCount = (currentProgress?.attemptsCount ?: 0) + 1

            // 3. Creamos el objeto con la info actualizada
            val progress = TopicProgress(
                subjectId = subjectId,
                topicId = topicId, // Ahora es String
                lastScore = _score.value,
                totalQuestions = _questions.value.size,
                attemptsCount = newAttemptsCount,
                lastAttemptTimestamp = System.currentTimeMillis()
            )

            // 4. Guardamos en la DB
            repository.updateProgress(progress)
        }
    }

    fun getResults(): List<QuestionResult> = _resultsList
}