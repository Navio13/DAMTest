package com.navio.damtests

import com.navio.damtests.data.local.entity.Question
import com.navio.damtests.data.local.entity.QuestionsDao
import com.navio.damtests.data.local.entity.TopicProgress

class QuizRepository(private val questionsDao: QuestionsDao) {

    // Cambiado para que coincida con tu DAO
    suspend fun getQuestionsByTopic(subjectId: String, topicId: Int, limit: Int): List<Question> {
        return if (topicId == -1) {
            questionsDao.getRandomQuestionsForGeneralTest(subjectId, limit)
        } else {
            questionsDao.getRandomQuestionsForTopic(subjectId, topicId, limit)
        }
    }

    // Cambiado para usar saveProgress que definiste en el DAO
    suspend fun updateProgress(progress: TopicProgress) {
        questionsDao.saveProgress(progress)
    }

    // Útil para el menú principal
    fun getProgressFlow(subjectId: String) = questionsDao.getProgressFlow(subjectId)

    suspend fun getProgress(subjectId: String, topicId: Int) = questionsDao.getProgress(subjectId, topicId)

    fun getAllProgress() = questionsDao.getAllProgress()
}