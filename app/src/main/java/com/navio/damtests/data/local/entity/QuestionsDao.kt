package com.navio.damtests.data.local.entity

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestionsDao {

    // --- Gestion de preguntas ---

    // Insertar preguntas masivamente (para cargar los JSON)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<Question>)

    // Obtener 20 preguntas aleatorias de un TEMA ESPECIFICO
    @Query("SELECT * FROM questions WHERE subjectId = :subjectId AND topicId = :topicId ORDER BY RANDOM() LIMIT :limit")
    suspend fun getRandomQuestionsForTopic(subjectId: String, topicId: Int, limit: Int): List<Question>

    // Obtener 2- preguntas aleatorias para el TEST GENERAL (Cualquier tema de la asignatura)
    @Query("SELECT * FROM questions WHERE subjectId = :subjectId ORDER BY RANDOM() LIMIT :limit")
    suspend fun getRandomQuestionsForGeneralTest(subjectId: String, limit: Int): List<Question>

    // --- Gestion de Progreso/Notas ---

    // Guardar o actualizar la nota
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(progress: TopicProgress)

    // Obtener el progreso de una asignatura (para pintar la lista en el menu)
    @Query("SELECT * FROM topic_progress WHERE subjectId = :subjectId")
    fun getProgressFlow(subjectId: String): Flow<List<TopicProgress>>

    @Query("SELECT * FROM topic_progress WHERE subjectId = :subjectId AND topicId = :topicId")
    suspend fun getProgress(subjectId: String, topicId: Int): TopicProgress?

    @Query("SELECT * FROM topic_progress")
    fun getAllProgress(): Flow<List<TopicProgress>>
}