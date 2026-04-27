package com.navio.damtests

import android.content.Context
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.firebase.remoteconfig.remoteConfig
import com.navio.damtests.ai.AiConfig
import com.navio.damtests.data.local.entity.Question
import kotlinx.coroutines.tasks.await

class FirebaseSyncManager(private val context: Context, private val repository: QuizRepository) {

    private val prefs = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
    private val database = FirebaseDatabase.getInstance("https://damtests-5ec43-default-rtdb.firebaseio.com/").reference

    // Obtenemos la instancia de Remote Config de forma limpia
    private val remoteConfig: FirebaseRemoteConfig = Firebase.remoteConfig

    companion object {
        private const val TAG = "FirebaseSync"
        private const val KEY_GEMINI = "GEMINI_API_KEY"
        private const val KEY_GROQ = "GROQ_API_KEY"
    }

    suspend fun syncQuestions() {
        try {
            Log.d(TAG, "Accediendo a versiones...")
            val versionesSnapshot = database.child("versiones").get().await()

            for (subjectSnapshot in versionesSnapshot.children) {
                val subjectId = subjectSnapshot.key ?: continue
                for (topicSnapshot in subjectSnapshot.children) {
                    val topicId = topicSnapshot.key ?: continue
                    val remoteVersion = topicSnapshot.getValue(Int::class.java) ?: 0

                    val prefKey = "version_${subjectId}_$topicId"
                    val localVersion = prefs.getInt(prefKey, 0)

                    if (remoteVersion > localVersion) {
                        downloadTopic(subjectId, topicId, remoteVersion)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error crítico en sincronización: ${e.message}")
        }
    }

    private suspend fun downloadTopic(subjectId: String, topicId: String, newVersion: Int) {
        try {
            val questionsSnapshot = database.child("preguntas").child(subjectId).child(topicId).get().await()
            val questionsList = questionsSnapshot.children.mapNotNull { qSnap ->
                qSnap.getValue(Question::class.java)?.copy(subjectId = subjectId, topicId = topicId)
            }

            if (questionsList.isNotEmpty()) {
                repository.updateTopicQuestions(subjectId, topicId, questionsList)
                prefs.edit().putInt("version_${subjectId}_$topicId", newVersion).apply()
                Log.d(TAG, "Tema $topicId actualizado a v$newVersion")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error descargando tema $topicId: ${e.message}")
        }
    }

    /**
     * Obtiene las API Keys desde Firebase Remote Config de forma asíncrona.
     * Devuelve un Pair con (GeminiKey, GroqKey) o null si falla.
     */
    suspend fun fetchApiKeys() {
        val remoteConfig = com.google.firebase.remoteconfig.FirebaseRemoteConfig.getInstance()
        val configSettings = com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(if (BuildConfig.DEBUG) 0 else 3600)
            .build()
        remoteConfig.setConfigSettingsAsync(configSettings)

        try {
            remoteConfig.fetchAndActivate().await()

            // Guardamos en el objeto GLOBAL
            AiConfig.geminiKey = remoteConfig.getString("GEMINI_API_KEY")
            AiConfig.groqKey = remoteConfig.getString("GROQ_API_KEY")

            Log.d("Firebase", "Keys globales configuradas")
        } catch (e: Exception) {
            Log.e("Firebase", "Error al obtener claves: ${e.message}")
        }
    }
}