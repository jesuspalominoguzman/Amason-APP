package com.example.amasonapp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.amasonapp.R;
import com.example.amasonapp.model.TutorialArticulo;

import java.util.List;

/**
 * ADAPTADOR PARA LAS TARJETAS DE TUTORIAL
 * 
 * Gestiona la visualización de los artículos del tutorial en un RecyclerView.
 * Se encarga de inflar el diseño de cada tarjeta y asignar la imagen y el texto
 * correspondientes de forma dinámica según el idioma seleccionado.
 */
public class TutorialAdapter extends RecyclerView.Adapter<TutorialAdapter.TutorialViewHolder> {

    // Lista de datos que se mostrarán en la interfaz
    private List<TutorialArticulo> articulos;

    // Idioma activo para filtrar las descripciones (es/en)
    private String idiomaActual;

    // Contexto de la aplicación necesario para acceder a recursos
    private Context context;

    /**
     * Constructor del adaptador.
     * 
     * @param articulos    Lista inicial de pasos del tutorial.
     * @param idiomaActual Idioma seleccionado por el usuario.
     * @param context      Contexto de la actividad que contiene el RecyclerView.
     */
    public TutorialAdapter(List<TutorialArticulo> articulos, String idiomaActual, Context context) {
        this.articulos = articulos;
        this.idiomaActual = idiomaActual;
        this.context = context;
    }

    @NonNull
    @Override
    public TutorialViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflamos el diseño de la tarjeta individual (item_tutorial_card.xml)
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tutorial_card, parent, false);
        return new TutorialViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TutorialViewHolder holder, int position) {
        // Obtenemos el artículo correspondiente a la posición actual
        TutorialArticulo articulo = articulos.get(position);

        // 1. Asignamos el texto descriptivo según el idioma
        String textoMostrar = articulo.getTextoSegunIdioma(idiomaActual);
        holder.description.setText(textoMostrar);

        // 2. Cargamos la imagen dinámicamente usando su nombre (string) guardado en
        // Firestore
        int imageResId = context.getResources().getIdentifier(
                articulo.getImagenNombre(),
                "drawable",
                context.getPackageName());

        // Si la imagen existe en drawable la mostramos, si no, ponemos una por defecto
        if (imageResId != 0) {
            holder.image.setImageResource(imageResId);
        } else {
            // Imagen de respaldo en caso de error en el nombre del recurso
            holder.image.setImageResource(R.drawable.ic_ftp);
        }
    }

    @Override
    public int getItemCount() {
        return articulos.size();
    }

    /**
     * Actualiza el idioma de visualización y refresca la lista completa.
     * 
     * @param nuevoIdioma Código del nuevo idioma (es/en).
     */
    public void actualizarIdioma(String nuevoIdioma) {
        this.idiomaActual = nuevoIdioma;
        notifyDataSetChanged();
    }

    /**
     * Actualiza la colección completa de artículos.
     * Útil cuando se cambia de sección de tutorial (ej: de Login a Base de Datos).
     * 
     * @param nuevosArticulos Nueva lista de pasos.
     */
    public void actualizarArticulos(List<TutorialArticulo> nuevosArticulos) {
        this.articulos = nuevosArticulos;
        notifyDataSetChanged();
    }

    /**
     * Clase interna ViewHolder que mantiene las referencias a las vistas de cada
     * tarjeta.
     */
    static class TutorialViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView description;

        public TutorialViewHolder(@NonNull View itemView) {
            super(itemView);
            // Vinculamos los elementos del layout item_tutorial_card.xml
            image = itemView.findViewById(R.id.imageView_tutorial);
            description = itemView.findViewById(R.id.textView_tutorial_description);
        }
    }
}
