import firebase_admin
from firebase_admin import credentials, db
import json

# 1. CONFIGURACIÓN (Verifica que el nombre del .json de tu clave sea correcto)
cred = credentials.Certificate("damtests-5ec43-firebase-adminsdk-fbsvc-71b8378388.json")
if not firebase_admin._apps:
    firebase_admin.initialize_app(cred, {
        'databaseURL': 'https://damtests-5ec43-default-rtdb.firebaseio.com/'
    })

def upload_questions(questions_list):
    temas_a_procesar = {}
    
    # Agrupamos por asignatura y tema (aquí topicId ya es "tema_1", "caso_2", etc.)
    for q in questions_list:
        key = (q['subjectId'], q['topicId'])
        if key not in temas_a_procesar:
            temas_a_procesar[key] = []
        temas_a_procesar[key].append(q)

    # Procesamos cada bloque para subirlo a su nodo correspondiente
    for (subj, topic_key), questions in temas_a_procesar.items():
        preguntas_dict = {}
        
        for i, q in enumerate(questions):
            # Construimos el objeto tal cual lo espera Kotlin
            datos_pregunta = {
                "subjectId": q['subjectId'],
                "topicId": topic_key,
                "text": q['text'],
                "optionA": q['optionA'],
                "optionB": q['optionB'],
                "optionC": q['optionC'],
                "optionD": q['optionD'],
                "correctOptionIndex": q['correctOptionIndex']
            }
            
            if 'contextText' in q:
                datos_pregunta["contextText"] = q['contextText']
            
            preguntas_dict[f"p{i+1}"] = datos_pregunta

        # 1. Subir preguntas al nodo dinámico
        ref_preguntas = db.reference(f'preguntas/{subj}/{topic_key}')
        ref_preguntas.set(preguntas_dict) 
        
        # 2. Actualizar versión para que la App detecte el cambio
        ref_version = db.reference(f'versiones/{subj}/{topic_key}')
        current_version = ref_version.get() or 0
        ref_version.set(current_version + 1)
        
        print(f"✅ Subido: {subj} -> {topic_key} (v{current_version + 1})")

# CAMBIA ESTO por el archivo que quieras subir en cada momento
nombre_archivo = "sostenibilidad.json" 

try:
    with open(nombre_archivo, 'r', encoding='utf-8') as f:
        datos = json.load(f)
    upload_questions(datos)
except FileNotFoundError:
    print(f"❌ Error: No se encuentra el archivo {nombre_archivo}")
except Exception as e:
    print(f"❌ Error inesperado: {e}")