package com.navio.damtests

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.navio.damtests.data.local.db.AppDatabase
import com.navio.damtests.data.local.entity.Topic
import com.navio.damtests.ui.TopicAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

class TopicSelectionActivity : AppCompatActivity() {

    private lateinit var repository: QuizRepository
    private lateinit var adapter: TopicAdapter
    private val client = OkHttpClient() // Cliente para descargas

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_topic_selection)

        val subjectId = intent.getStringExtra("SUBJECT_ID") ?: "programacion"

        val database = AppDatabase.getDatabase(this)
        repository = QuizRepository(database.questionsDao())

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar_topics)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = subjectId.replace("_", " ").uppercase()
            elevation = 0f
        }

        window.statusBarColor = getColor(R.color.colorPrimary)

        val rvTopics = findViewById<RecyclerView>(R.id.rvTopics)
        rvTopics.layoutManager = LinearLayoutManager(this)

        val topicsList = getMockTopics(subjectId)

        lifecycleScope.launchWhenStarted {
            repository.getProgressFlow(subjectId).collect { progressList ->
                adapter = TopicAdapter(topicsList, progressList,
                    onTopicClick = { topic ->
                        startQuiz(subjectId, topic.id)
                    },
                    onPdfClick = { topic ->
                        openPdf(subjectId, topic.id)
                    }
                )
                rvTopics.adapter = adapter
            }
        }
    }

    private fun openPdf(subjectId: String, topicId: Int) {
        if (topicId == -1) return

        val fileName = "${subjectId}_$topicId.pdf"
        val localFile = File(cacheDir, fileName)

        if (localFile.exists()) {
            // Caso A: Ya lo tenemos cacheado, lo abrimos
            showPdf(localFile)
        } else {
            // Caso B: No está, hay que bajarlo de GitHub
            downloadAndOpenPdf(fileName, localFile)
        }
    }

    private fun downloadAndOpenPdf(fileName: String, destinationFile: File) {
        // URL a la carpeta externa que has creado en tu repo
        val url = "https://raw.githubusercontent.com/Navio13/DAMTest/master/RECURSOS_APP/$fileName"

        Toast.makeText(this, "Descargando tema...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    response.body?.let { body ->
                        FileOutputStream(destinationFile).use { output ->
                            output.write(body.bytes())
                        }
                        // Volvemos al hilo principal para abrir el PDF
                        withContext(Dispatchers.Main) {
                            showPdf(destinationFile)
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@TopicSelectionActivity, "El PDF no está en el servidor", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@TopicSelectionActivity, "Error de red o sin conexión", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showPdf(file: File) {
        try {
            // Usamos tu FileProvider (asegúrate de que en el Manifest el provider termine en .provider o .fileprovider)
            val uri: Uri = FileProvider.getUriForFile(
                this,
                "${packageName}.provider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Error al abrir el PDF", Toast.LENGTH_SHORT).show()
        }
    }

    // --- El resto de tus métodos se mantienen exactamente igual ---

    private fun getMockTopics(subjectId: String): List<Topic> {
        val list = mutableListOf<Topic>()
        val names = when (subjectId) {
            "base_de_datos" -> listOf("Sistemas de representación de la información. Ficheros", "Fundamentos de las bases de datos", "Sistemas gestores de bases de datos (SGBD)", "El modelo de datos. Fases y modelo E/R", "Modelo entidad-relación extendido", "Elaboración de diagramas E/R. Conceptos del modelo relacional", "Transformación de diagramas E/R y normalización", "El lenguaje SQL", "La sentencia SELECT", "Operadores y funciones")
            "digitalizacion" -> listOf("La Cuarta Revolución Industrial", "Digitalización en las empresas", "Tecnologías Habilitadoras Digitales", "Otras Tecnologías Habilitadoras Digitales", "Aplicación de las Tecnologías Habilitadoras Digitales en las empresas", "Introducción al Cloud Computing", "Cloud Computing y arquitecturas de computación", "Introducción a la Inteligencia Artificial", "IA y datos", "Inteligencia artificial aplicada")
            "entornos" -> listOf("El software", "Ingeniería del software", "Explotación de código", "Introducción a los entornos de desarrollo", "Instalación y explotación de entornos de desarrollo", "El lenguaje UML", "Elaboración de diagramas de clases", "Diagramas de comportamiento", "Diagramas de casos de uso", "Diagramas de interacción")
            "ipe" -> listOf("La prevención de riesgos en el entorno laboral", "Evaluación de riesgos en la empresa", "Tipos de daños profesionales. Accidentes y enfermedades en el trabajo", "Emergencias y primeros auxilios en el trabajo", "Oportunidades de empleo e inserción laboral", "Requerimientos exigidos para el empleo", "Actitudes y aptitudes requeridas para la actividad profesional. El currículum", "La jornada laboral", "La relación laboral. Obligaciones y derechos", "El contrato de trabajo. Modalidades de contratación")
            "marcas" -> listOf("Lenguajes de marcas", "Introducción al documento HTML", "Estructura de un documento HTML", "Identificación de etiquetas y atributos", "Tablas y formularios", "La web semántica. HTML 5", "Hojas de estilo. CSS", "Propiedades CSS", "Disposición de los elementos. CSS3", "Definición de esquemas y vocabulario en XML")
            "programacion" -> listOf("Introducción a la programación", "Elementos de un programa informático", "Programación estructurada", "Programación modular", "Recursividad", "Estructuras de almacenamiento y cadenas de caracteres", "Introducción a la orientación a objetos", "Gestión de clases y objetos", "Manipulación de clases y objetos", "Herencia")
            "sistemas" -> listOf("Caracterización de los sistemas informáticos", "Explotación de aplicaciones", "Los sistemas operativos", "Máquinas virtuales", "Sistemas operativos propietarios. Instalación, administración y configuración", "Sistemas operativos libres. Instalación, administración y configuración", "Administración de sistemas operativos libres", "Gestión de varios sistemas operativos en un ordenador", "Redes informáticas", "Direcciones de red")
            "sostenibilidad" -> listOf("Introducción a la sostenibilidad y su marco internacional", "Objetivos de desarrollo sostenible", "Los Desafíos Globales", "Gobernanza de las Naciones Unidas ante grandes retos: Cambio climático", "Gobernanza de las Naciones Unicas ante grandes retos: Biodiversidad", "Superpoblación y consumo de recursos naturales", "Cambio climático", "Descarbonización de la economía", "Medidas para alcanzar el Net Zero", "Economía circular")
            else -> listOf("Tema 1", "Tema 2", "Tema 3")
        }
        names.forEachIndexed { index, name ->
            list.add(Topic(index + 1, "Tema ${index + 1}: $name", subjectId))
        }
        list.add(Topic(-1, "TEST GENERAL", subjectId))
        return list
    }

    private fun startQuiz(subjectId: String, topicId: Int) {
        val intent = Intent(this, QuizActivity::class.java).apply {
            putExtra("SUBJECT_ID", subjectId)
            putExtra("TOPIC_ID", topicId)
        }
        startActivity(intent)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}