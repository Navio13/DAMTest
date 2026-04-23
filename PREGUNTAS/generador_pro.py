import json
import os

def menu_creador():
    directorio_actual = os.path.dirname(os.path.abspath(__file__))
    
    while True: # Bucle principal del programa
        print("\n" + "="*30)
        print("🚀 GENERADOR DE PREGUNTAS PRO")
        print("="*30)
        asignatura = input("📚 Asignatura (o 'salir' para cerrar): ").strip().lower()
        if asignatura == 'salir': break

        archivo_path = os.path.join(directorio_actual, f"{asignatura}.json")
        
        while True: # Bucle de temas dentro de la asignatura
            preguntas_totales = []
            if os.path.exists(archivo_path):
                with open(archivo_path, 'r', encoding='utf-8') as f:
                    preguntas_totales = json.load(f)

            print(f"\n--- Configuración de Bloque para {asignatura.upper()} ---")
            print("1. Tema Normal | 2. Caso Práctico | 3. Repaso | 4. Cambiar Asignatura")
            tipo = input("Selecciona (1-4): ")
            if tipo == "4": break
            
            num = input("Número del bloque (ej: 11): ")
            prefijos = {"1": "tema_", "2": "caso_", "3": "repaso_"}
            topic_id = f"{prefijos.get(tipo, 'tema_')}{num}"
            
            enunciado_comun = input("📝 Enunciado general (enter si no hay): ").strip()

            while True: # Bucle de preguntas dentro del tema
                print(f"\n--- Nueva pregunta para {topic_id} ---")
                texto = input("❓ Texto: ").strip()
                op_a = input("   A) ").strip()
                op_b = input("   B) ").strip()
                op_c = input("   C) ").strip()
                op_d = input("   D) ").strip()
                correcta = int(input("✅ Correcta (0:A, 1:B, 2:C, 3:D): "))

                q = {
                    "subjectId": asignatura,
                    "topicId": topic_id,
                    "text": texto,
                    "optionA": op_a, "optionB": op_b, "optionC": op_c, "optionD": op_d,
                    "correctOptionIndex": correcta
                }
                if enunciado_comun: q["contextText"] = enunciado_comun
                
                preguntas_totales.append(q)

                # Guardar en cada pregunta para no perder datos
                with open(archivo_path, 'w', encoding='utf-8') as f:
                    json.dump(preguntas_totales, f, indent=4, ensure_ascii=False)

                continuar = input("\n¿Otra pregunta en ESTE TEMA? (s/n): ").lower()
                if continuar != 's': break
            
            print(f"\n✅ Tema {topic_id} guardado.")
            if input("¿Quieres configurar OTRO TEMA de esta asignatura? (s/n): ").lower() != 's':
                break

if __name__ == "__main__":
    menu_creador()