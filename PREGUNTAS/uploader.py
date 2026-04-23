import firebase_admin
from firebase_admin import credentials, db
import json
import os

# 1. RUTA FIJA PARA LA LLAVE
RUTA_ALFONSO = r"C:\Alfonso"
nombre_llave = "damtests-5ec43-firebase-adminsdk-fbsvc-1cdbdab176.json"
ruta_llave = os.path.join(RUTA_ALFONSO, nombre_llave)

# Directorio donde están los scripts y los JSON de preguntas
BASE_DIR = os.path.dirname(os.path.abspath(__file__))

try:
    cred = credentials.Certificate(ruta_llave)
    if not firebase_admin._apps:
        firebase_admin.initialize_app(cred, {
            'databaseURL': 'https://damtests-5ec43-default-rtdb.firebaseio.com/'
        })
except Exception as e:
    print(f"❌ Error al cargar la llave en {ruta_llave}: {e}")
    exit()

def upload_questions(questions_list):
    temas_a_procesar = {}
    for q in questions_list:
        tid = q.get('topicId', '1')
        topic_key = f"tema_{tid}" if isinstance(tid, int) or str(tid).isdigit() else tid
        key = (q['subjectId'], topic_key)
        if key not in temas_a_procesar: temas_a_procesar[key] = []
        temas_a_procesar[key].append(q)

    for (subj, topic_key), questions in temas_a_procesar.items():
        preguntas_dict = {f"p{i+1}": {
            "subjectId": subj,
            "topicId": topic_key,
            "text": q['text'],
            "optionA": q.get('optionA', ""),
            "optionB": q.get('optionB', ""),
            "optionC": q.get('optionC', ""),
            "optionD": q.get('optionD', ""),
            "correctOptionIndex": q['correctOptionIndex'],
            **({"contextText": q['contextText']} if 'contextText' in q else {})
        } for i, q in enumerate(questions)}

        db.reference(f'preguntas/{subj}/{topic_key}').set(preguntas_dict) 
        ref_version = db.reference(f'versiones/{subj}/{topic_key}')
        current_version = ref_version.get() or 0
        ref_version.set(current_version + 1)
        print(f"✅ Subido: {subj} -> {topic_key} (v{current_version + 1})")

# SUBIDA AUTOMÁTICA: Sube todos los JSON que encuentre en la carpeta
archivos_json = [f for f in os.listdir(BASE_DIR) if f.endswith('.json') and f != nombre_llave]

if not archivos_json:
    print("No se encontraron archivos JSON para subir.")
else:
    for archivo in archivos_json:
        print(f"--- Procesando {archivo} ---")
        with open(os.path.join(BASE_DIR, archivo), 'r', encoding='utf-8') as f:
            upload_questions(json.load(f))