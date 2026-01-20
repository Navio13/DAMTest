package com.navio.damtests.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.navio.damtests.JsonUtils
import com.navio.damtests.data.local.entity.Question
import com.navio.damtests.data.local.entity.QuestionsDao
import com.navio.damtests.data.local.entity.TopicProgress
import kotlinx.coroutines.launch

@Database(entities = [Question::class, TopicProgress::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun questionsDao(): QuestionsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "quiz_database"
                )
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                val dao = getDatabase(context).questionsDao()

                                // Lista con todos tus archivos JSON en la carpeta assets
                                val archivosJson = listOf(
                                    "base_de_datos.json",
                                    "digitalizacion.json",
                                    "entornos.json",
                                    "programacion.json",

                                    "ipe.json",
                                    "marcas.json",
                                    "sistemas.json",
                                    "sostenibilidad.json"
                                )

                                try {
                                    archivosJson.forEach { nombreArchivo ->
                                        val preguntas = JsonUtils.loadQuestionsFromAsset(context, nombreArchivo)
                                        dao.insertQuestions(preguntas)
                                        android.util.Log.d("DB_DEBUG", "Cargadas preguntas de: $nombreArchivo")
                                    }
                                    android.util.Log.d("DB_DEBUG", "Carga inicial completa de todas las asignaturas")
                                } catch (e: Exception) {
                                    android.util.Log.e("DB_DEBUG", "Error cargando preguntas: ${e.message}")
                                }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}