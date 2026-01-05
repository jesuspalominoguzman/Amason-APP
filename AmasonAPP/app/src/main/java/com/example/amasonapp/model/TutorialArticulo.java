package com.example.amasonapp.model;

/**
 * MODELO DE DATOS PARA ARTÍCULOS DE TUTORIAL
 * 
 * Representa un paso individual dentro de un tutorial. Cada artículo contiene
 * texto bilingüe y una referencia a una imagen.
 */
public class TutorialArticulo {

    // Número que define la posición del paso en la secuencia del tutorial
    private int orden;

    // Descripción del paso en español
    private String textoEs;

    // Descripción del paso en inglés
    private String textoEn;

    // Nombre del recurso de imagen asociado (ubicado en res/drawable)
    private String imagenNombre;

    /**
     * Constructor por defecto necesario para la deserialización de Firestore.
     */
    public TutorialArticulo() {
    }

    // --- MÉTODOS DE ACCESO (Getters y Setters) ---

    public int getOrden() {
        return orden;
    }

    public String getTextoEs() {
        return textoEs;
    }

    public String getTextoEn() {
        return textoEn;
    }

    public String getImagenNombre() {
        return imagenNombre;
    }

    public void setOrden(int orden) {
        this.orden = orden;
    }

    public void setTextoEs(String textoEs) {
        this.textoEs = textoEs;
    }

    public void setTextoEn(String textoEn) {
        this.textoEn = textoEn;
    }

    public void setImagenNombre(String imagenNombre) {
        this.imagenNombre = imagenNombre;
    }

    /**
     * Helper para obtener el texto descriptivo según el idioma de la aplicación.
     * 
     * @param idioma Código del idioma ("es" o "en").
     * @return El texto en el idioma solicitado.
     */
    public String getTextoSegunIdioma(String idioma) {
        if ("en".equals(idioma)) {
            return textoEn;
        }
        return textoEs;
    }
}
