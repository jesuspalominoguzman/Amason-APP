package com.example.amasonapp.data;

import com.example.amasonapp.model.TutorialArticulo;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * REPOSITORIO DE CONTENIDO DE TUTORIALES
 * 
 * Esta clase gestiona la recuperación dinámica de los pasos y artículos de los
 * tutoriales.
 * Permite cargar diferentes secciones (Login, DB, FTP, etc.) basándose en el
 * nombre
 * de la colección solicitado.
 */
public class TutorialRepository {

    // Instancia de Cloud Firestore
    private final FirebaseFirestore db;

    // Registro para el control del ciclo de vida de la conexión
    private ListenerRegistration listenerRegistration;

    /**
     * Constructor del repositorio.
     * Vincula la instancia activa de Firestore.
     */
    public TutorialRepository() {
        db = FirebaseFirestore.getInstance();
    }

    // -----------------------------------------------------------------------------------------
    // CALLBACK: Comunicación de resultados de carga de tutoriales
    // -----------------------------------------------------------------------------------------

    public interface TutorialCallback {
        /**
         * Notifica que los artículos han sido recuperados y ordenados.
         * 
         * @param articulos Lista de pasos del tutorial cargado.
         */
        void onTutorialesCargados(List<TutorialArticulo> articulos);

        /**
         * Notifica fallos de conexión o de formato de datos.
         * 
         * @param e Excepción de Firebase o conversión.
         */
        void onError(Exception e);
    }

    // -----------------------------------------------------------------------------------------
    // LOGICA DE CARGA DINÁMICA
    // -----------------------------------------------------------------------------------------

    /**
     * Inicia una suscripción en tiempo real a una colección específica de
     * tutoriales.
     * Los resultados se ordenan automáticamente por el campo 'orden'.
     * 
     * @param nombreColeccion Nombre de la tabla en Firestore (ej:
     *                        "tutoriales_login").
     * @param callback        Referencia para devolver los datos.
     */
    public void empezarEscucha(String nombreColeccion, final TutorialCallback callback) {
        listenerRegistration = db.collection(nombreColeccion)
                // Es vital ordenar por el campo 'orden' para que el tutorial tenga sentido
                // lógico
                .orderBy("orden", Query.Direction.ASCENDING)
                .addSnapshotListener((value, e) -> {

                    // 1. Control de incidencias en la nube
                    if (e != null) {
                        callback.onError(e);
                        return;
                    }

                    // 2. Mapeo de documentos a objetos de negocio
                    List<TutorialArticulo> articulosList = new ArrayList<>();

                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            try {
                                // Convertimos el documento al modelo TutorialArticulo
                                TutorialArticulo articulo = doc.toObject(TutorialArticulo.class);
                                articulosList.add(articulo);

                            } catch (Exception conversionError) {
                                // Si el documento en Firestore no tiene los campos esperados
                                callback.onError(conversionError);
                                return;
                            }
                        }
                    }

                    // 3. Respuesta a la capa de UI (Fragment)
                    callback.onTutorialesCargados(articulosList);
                });
    }

    /**
     * Cierra la conexión de streaming con Firestore.
     */
    public void detenerEscucha() {
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }
}
