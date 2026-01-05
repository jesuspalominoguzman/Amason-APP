package com.example.amasonapp.model;

/**
 * MODELO DE DATOS PARA TRADUCCIONES GENERALES
 * 
 * Esta clase representa una etiqueta de texto con sus versiones en español e
 * inglés.
 * Se utiliza principalmente para los elementos de la UI como botones y títulos
 * de menú.
 */
public class Texto {

    // Clave identificadora del texto (ej: "nav_login", "logout")
    private String claveTexto;

    // Contenido del texto en español
    private String es;

    // Contenido del texto en inglés
    private String en;

    /**
     * Constructor vacío requerido por Firebase Firestore para la conversión
     * automática.
     */
    public Texto() {
    }

    // Getters y Setters documentados implícitamente por su función estándar

    public String getClaveTexto() {
        return claveTexto;
    }

    public String getEs() {
        return es;
    }

    public String getEn() {
        return en;
    }

    public void setClaveTexto(String claveTexto) {
        this.claveTexto = claveTexto;
    }
}