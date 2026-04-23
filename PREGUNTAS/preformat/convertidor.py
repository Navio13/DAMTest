import json
import os

def convertir_archivos():
    # Detectar la carpeta actual
    directorio_actual = os.path.dirname(os.path.abspath(__file__))
    archivos = [f for f in os.listdir(directorio_actual) if f.endswith('.json') and f != 'tu_llave_firebase.json']

    for nombre_archivo in archivos:
        ruta = os.path.join(directorio_actual, nombre_archivo)
        
        with open(ruta, 'r', encoding='utf-8') as f:
            try:
                datos = json.load(f)
            except:
                continue # Saltar si no es un JSON válido

        modificado = False
        nuevos_datos = []

        for q in datos:
            # Si tiene el campo 'options' (formato viejo), lo convertimos
            if 'options' in q and isinstance(q['options'], list):
                q['optionA'] = q['options'][0]
                q['optionB'] = q['options'][1]
                q['optionC'] = q['options'][2]
                q['optionD'] = q['options'][3]
                
                # Eliminamos la lista vieja para que no estorbe
                del q['options']
                
                # Convertimos topicId a string si era un número (ej: 10 -> "tema_10")
                if isinstance(q['topicId'], int):
                    q['topicId'] = f"tema_{q['topicId']}"
                elif str(q['topicId']).isdigit():
                    q['topicId'] = f"tema_{q['topicId']}"
                
                modificado = True
            
            nuevos_datos.append(q)

        if modificado:
            with open(ruta, 'w', encoding='utf-8') as f:
                json.dump(nuevos_datos, f, indent=4, ensure_ascii=False)
            print(f"✅ Convertido con éxito: {nombre_archivo}")
        else:
            print(f"ℹ️ El archivo {nombre_archivo} ya tenía el formato nuevo.")

if __name__ == "__main__":
    convertir_archivos()
    print("\n🚀 ¡Proceso finalizado! Todos tus archivos están ahora en el formato correcto.")