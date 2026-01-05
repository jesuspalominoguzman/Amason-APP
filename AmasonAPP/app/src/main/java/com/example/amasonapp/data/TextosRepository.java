package com.example.amasonapp.data;

import com.example.amasonapp.model.Texto;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * REPOSITORIO DE TEXTOS Y TRADUCCIONES
 * 
 * Esta clase se encarga de la comunicación directa con Cloud Firestore para
 * gestionar
 * todos los textos de la interfaz de usuario que soportan multilingüismo.
 */
public class TextosRepository {

    // Instancia de la base de datos Firestore
    private final FirebaseFirestore db;

    // Nombre de la colección en Firestore donde se almacenan las etiquetas de texto
    private final String COLECCION_TEXTOS = "traducciones";

    // Registro del listener para poder detener la escucha activa y evitar fugas de
    // memoria
    private ListenerRegistration listenerRegistration;

    /**
     * Constructor del repositorio.
     * Inicializa la referencia a Firebase Firestore.
     */
    public TextosRepository() {
        db = FirebaseFirestore.getInstance();
    }

    // -----------------------------------------------------------------------------------------
    // INTERFAZ DE CALLBACK: Define cómo se devuelven los datos a la Activity o
    // Fragment
    // -----------------------------------------------------------------------------------------

    public interface TextosCallback {
        /**
         * Se ejecuta cuando los textos han sido cargados con éxito desde la nube.
         * 
         * @param textos Lista de objetos Texto con sus traducciones.
         */
        void onTextosCargados(List<Texto> textos);

        /**
         * Se ejecuta si ocurre algún error durante la sincronización.
         * 
         * @param e Excepción capturada.
         */
        void onError(Exception e);
    }

    // -----------------------------------------------------------------------------------------
    // LECTURA EN TIEMPO REAL (SNAPSHOT LISTENER)
    // -----------------------------------------------------------------------------------------

    /**
     * Inicia la escucha activa de la colección de traducciones.
     * Cualquier cambio realizado en el panel de Firebase se reflejará
     * instantáneamente en la app.
     * 
     * @param callback Interfaz para notificar a la UI sobre los cambios.
     */
    public void empezarEscucha(final TextosCallback callback) {
        // Establecemos el listener en la colección de traducciones
        listenerRegistration = db.collection(COLECCION_TEXTOS)
                .addSnapshotListener((value, e) -> {

                    // 1. Verificación de errores de red o permisos
                    if (e != null) {
                        callback.onError(e);
                        return;
                    }

                    // 2. Procesamiento de los documentos recibidos
                    List<Texto> textosList = new ArrayList<>();

                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            try {
                                // Mapeo automático del documento de Firestore a la clase POJO Texto
                                Texto texto = doc.toObject(Texto.class);

                                // Usamos el ID del documento como clave única para identificar el elemento (ej:
                                // "nav_login")
                                texto.setClaveTexto(doc.getId());
                                textosList.add(texto);

                            } catch (Exception conversionError) {
                                // Manejo de errores en caso de que el formato en Firestore sea incorrecto
                                callback.onError(conversionError);
                                return;
                            }
                        }
                    }

                    // 3. Notificación a la UI con la lista actualizada
                    callback.onTextosCargados(textosList);
                });
    }

    /**
     * Detiene la escucha de actualizaciones.
     * Es fundamental llamar a este método en el ciclo de vida onDestroy para
     * liberar recursos.
     */
    public void detenerEscucha() {
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }
}