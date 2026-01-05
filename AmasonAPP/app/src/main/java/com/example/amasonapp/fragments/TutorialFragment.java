package com.example.amasonapp.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.amasonapp.MainActivity;
import com.example.amasonapp.R;
import com.example.amasonapp.adapters.TutorialAdapter;
import com.example.amasonapp.data.TutorialRepository;
import com.example.amasonapp.model.TutorialArticulo;

import java.util.ArrayList;
import java.util.List;

/**
 * FRAGMENTO GENÉRICO DE TUTORIALES
 * 
 * Este fragmento es capaz de mostrar cualquier sección de tutoriales de la
 * aplicación.
 * Recibe por argumentos el nombre de la colección de Firestore que debe cargar
 * y se encarga de gestionar el RecyclerView y la comunicación con el
 * repositorio.
 */
public class TutorialFragment extends Fragment {

    // Clave para el paso de argumentos al fragmento
    private static final String ARG_COLECCION = "coleccion_nome";

    private String coleccionNombre;
    private RecyclerView recyclerView;
    private TutorialAdapter adapter;
    private TutorialRepository repository;

    /**
     * Método estático para crear nuevas instancias del fragmento de forma segura.
     * 
     * @param coleccionNombre Nombre de la colección en Firebase (ej:
     *                        "tutoriales_ftp").
     * @return Una nueva instancia configurada del fragmento.
     */
    public static TutorialFragment newInstance(String coleccionNombre) {
        TutorialFragment fragment = new TutorialFragment();
        Bundle args = new Bundle();
        args.putString(ARG_COLECCION, coleccionNombre);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Recuperamos el nombre de la colección a mostrar
        if (getArguments() != null) {
            coleccionNombre = getArguments().getString(ARG_COLECCION);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        // Inflamos el contenedor genérico de tutoriales
        View view = inflater.inflate(R.layout.fragment_tutorial_container, container, false);

        // Configuramos el RecyclerView con un LayoutManager vertical básico
        recyclerView = view.findViewById(R.id.recyclerView_tutorial);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Inicializamos el adaptador vacío y lo vinculamos
        String idiomaActual = getIdiomaFromActivity();
        adapter = new TutorialAdapter(new ArrayList<>(), idiomaActual, getContext());
        recyclerView.setAdapter(adapter);

        // Iniciamos la carga de datos desde el repositorio
        repository = new TutorialRepository();
        cargarTutoriales();

        return view;
    }

    /**
     * Obtiene el idioma actual consultando directamente a la actividad principal.
     */
    private String getIdiomaFromActivity() {
        if (getActivity() instanceof MainActivity) {
            return ((MainActivity) getActivity()).getIdiomaActual();
        }
        return "es";
    }

    /**
     * Conecta con el repositorio para empezar a escuchar cambios en la colección
     * asignada.
     */
    private void cargarTutoriales() {
        if (coleccionNombre == null)
            return;

        repository.empezarEscucha(coleccionNombre, new TutorialRepository.TutorialCallback() {
            @Override
            public void onTutorialesCargados(List<TutorialArticulo> articulos) {
                // Actualizamos la UI cuando los datos llegan de Firestore
                if (adapter != null) {
                    adapter.actualizarArticulos(articulos);
                }
            }

            @Override
            public void onError(Exception e) {
                // Manejo silencioso de errores de carga
            }
        });
    }

    /**
     * Método público llamado por MainActivity para propagar el cambio de idioma
     * a todos los elementos visibles del tutorial actual.
     */
    public void actualizarIdioma(String nuevoIdioma) {
        if (adapter != null) {
            adapter.actualizarIdioma(nuevoIdioma);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Cerramos la conexión con Firebase al destruir la vista para ahorrar datos y
        // batería
        if (repository != null) {
            repository.detenerEscucha();
        }
    }
}
