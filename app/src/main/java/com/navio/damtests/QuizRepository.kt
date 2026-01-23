package com.navio.damtests

import com.navio.damtests.data.local.entity.Question
import com.navio.damtests.data.local.entity.QuestionsDao
import com.navio.damtests.data.local.entity.TopicProgress

class QuizRepository(private val questionsDao: QuestionsDao) {

    // 1. Actualiza solo un tema específico (Lo que usará el sincronizador)
    suspend fun updateTopicQuestions(subjectId: String, topicId: String, questions: List<Question>) {
        questionsDao.deleteQuestionsByTopic(subjectId, topicId)
        questionsDao.insertQuestions(questions)
    }

    // 2. Mantenemos el refresh global por si acaso, pero cambiando el tipo de dato si fuera necesario
    suspend fun refreshQuestions(questions: List<Question>) {
        questionsDao.refreshAllQuestions(questions)
    }

    // 3. Cambiamos topicId de Int a String
    suspend fun getQuestionsByTopic(subjectId: String, topicId: String, limit: Int): List<Question> {
        return if (topicId == "-1") {
            questionsDao.getRandomQuestionsForGeneralTest(subjectId, limit)
        } else {
            questionsDao.getRandomQuestionsForTopic(subjectId, topicId, limit)
        }
    }

    suspend fun updateProgress(progress: TopicProgress) {
        questionsDao.saveProgress(progress)
    }

    fun getProgressFlow(subjectId: String) = questionsDao.getProgressFlow(subjectId)

    // 4. Cambiamos topicId de Int a String aquí también
    suspend fun getProgress(subjectId: String, topicId: String) = questionsDao.getProgress(subjectId, topicId)

    fun getAllProgress() = questionsDao.getAllProgress()
}