package com.example.amasonapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import com.example.amasonapp.data.TextosRepository;
import com.example.amasonapp.model.Texto;
import com.example.amasonapp.fragments.TutorialFragment;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import java.util.List;

/**
 * ACTIVIDAD PRINCIPAL (CENTRO DE CONTROL)
 * 
 * Gestiona el menú de navegación lateral (Drawer), la carga dinámica de
 * tutoriales
 * y la sincronización global de idiomas. Es la encargada de orquestar el
 * contenido
 * que se muestra al usuario una vez autenticado.
 */
public class MainActivity extends AppCompatActivity {

    // --- Gestión de Datos y Estado ---
    private TextosRepository textosRepository;
    private String idiomaActual = "es";
    private List<Texto> textosActuales;

    // --- Componentes de la Interfaz (UI) ---
    private Button buttonTraduccion;
    private Button buttonLogout;
    private TextView textViewUserGreeting;
    private LinearLayout layoutSinConexion;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private FloatingActionButton buttonMenu;

    // --- Servicios y Autenticación ---
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Inicializamos los servicios de autenticación de Google
        mAuth = FirebaseAuth.getInstance();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)).requestEmail().build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // 2. Vinculamos las vistas del XML activity_main
        vincularComponentes();

        // 3. Configuramos la lógica de navegación y botones
        mostrarNombreUsuario();
        configurarBotonTraduccion();
        configurarBotonLogout();
        configurarMenuButton();
        configurarDrawerNavigation();

        // 4. Cargamos el primer tutorial por defecto (Login) si es la primera vez
        if (savedInstanceState == null) {
            loadFragment(TutorialFragment.newInstance("tutoriales_login"));
        }

        // 5. Activamos el monitoreo de red y la escucha de textos de Firebase
        inicializarMonitoreoRed();
        textosRepository = new TextosRepository();
        iniciarEscuchaDatos();
    }

    private void vincularComponentes() {
        buttonTraduccion = findViewById(R.id.button_traduccion);
        buttonLogout = findViewById(R.id.button_logout);
        textViewUserGreeting = findViewById(R.id.textView_user_greeting);
        layoutSinConexion = findViewById(R.id.layout_sin_conexion);
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        buttonMenu = findViewById(R.id.button_menu);
    }

    /**
     * Configura el botón flotante (FAB) para abrir el catálogo de tutoriales.
     */
    private void configurarMenuButton() {
        buttonMenu.setOnClickListener(v -> drawerLayout.openDrawer(navigationView));
    }

    /**
     * Define la lógica de clic en los ítems del menú lateral.
     * Cada ítem carga una colección diferente de Firestore en el fragmento
     * genérico.
     */
    private void configurarDrawerNavigation() {
        navigationView.setNavigationItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int id = item.getItemId();

            // Mapeo de ID de menú a colección de base de datos
            if (id == R.id.nav_login) {
                selectedFragment = TutorialFragment.newInstance("tutoriales_login");
            } else if (id == R.id.nav_database) {
                selectedFragment = TutorialFragment.newInstance("tutoriales_database");
            } else if (id == R.id.nav_ftp) {
                selectedFragment = TutorialFragment.newInstance("tutoriales_ftp");
            } else if (id == R.id.nav_email) {
                selectedFragment = TutorialFragment.newInstance("tutoriales_email");
            } else if (id == R.id.nav_mailbox) {
                selectedFragment = TutorialFragment.newInstance("tutoriales_buzon");
            } else if (id == R.id.nav_admin) {
                selectedFragment = TutorialFragment.newInstance("tutoriales_admin");
            } else if (id == R.id.nav_logs) {
                selectedFragment = TutorialFragment.newInstance("tutoriales_logs");
            }

            if (selectedFragment != null) {
                drawerLayout.closeDrawers();
                loadFragment(selectedFragment);
            }
            return true;
        });
    }

    /**
     * Reemplaza el contenido del contenedor principal por el nuevo fragmento
     * solicitado.
     */
    private void loadFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }

    private void configurarBotonLogout() {
        buttonLogout.setOnClickListener(v -> cerrarSesion());
    }

    /**
     * Desconecta al usuario de Firebase y Google, devolviéndolo al Login.
     */
    private void cerrarSesion() {
        mAuth.signOut();
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    /**
     * Alterna globalmente el idioma de la aplicación.
     * Afecta al menú, la cabecera y el tutorial que esté cargado actualmente.
     */
    private void configurarBotonTraduccion() {
        buttonTraduccion.setOnClickListener(v -> {
            if (!hayConexionInternet())
                return;

            if (idiomaActual.equals("es")) {
                idiomaActual = "en";
                buttonTraduccion.setText("EN");
            } else {
                idiomaActual = "es";
                buttonTraduccion.setText("ES");
            }

            // Actualizamos textos de la actividad y del fragmento visible
            if (textosActuales != null) {
                actualizarTextos(textosActuales);
            }
            actualizarIdiomaFragment();
            enviarBroadcastCambioIdioma();
        });
    }

    /**
     * Notifica al fragmento actual que el idioma ha cambiado para que refresque su
     * contenido.
     */
    private void actualizarIdiomaFragment() {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (currentFragment instanceof TutorialFragment) {
            ((TutorialFragment) currentFragment).actualizarIdioma(idiomaActual);
        }
    }

    private void iniciarEscuchaDatos() {
        textosRepository.empezarEscucha(new TextosRepository.TextosCallback() {
            @Override
            public void onTextosCargados(List<Texto> textos) {
                textosActuales = textos;
                actualizarTextos(textos);
            }

            @Override
            public void onError(Exception e) {
                mostrarMensajeSinConexion();
            }
        });
    }

    /**
     * Actualiza dinámicamente los títulos de los ítems del menú de navegación.
     */
    private void actualizarTextos(List<Texto> textos) {
        if (!hayConexionInternet())
            return;

        layoutSinConexion.setVisibility(android.view.View.GONE);

        for (Texto texto : textos) {
            String textoTraducido = obtenerTextoSegunIdioma(texto);
            String clave = texto.getClaveTexto();

            // Mantenemos los botones sincronizados
            if ("logout".equals(clave)) {
                buttonLogout.setText(textoTraducido);
                buttonLogout.setVisibility(android.view.View.VISIBLE);
            }
            // Actualización de los títulos del Drawer Navigation
            else if (clave.startsWith("nav_")) {
                int menuId = getResources().getIdentifier(clave, "id", getPackageName());
                MenuItem item = navigationView.getMenu().findItem(menuId);
                if (item != null)
                    item.setTitle(textoTraducido);
            }
        }
    }

    private String obtenerTextoSegunIdioma(Texto texto) {
        return (idiomaActual.equals("en")) ? texto.getEn() : texto.getEs();
    }

    /**
     * Extrae el alias del email del usuario y lo muestra truncated en la cabecera.
     */
    private void mostrarNombreUsuario() {
        if (mAuth.getCurrentUser() != null) {
            String email = mAuth.getCurrentUser().getEmail();
            if (email != null) {
                String nombre = email.split("@")[0];
                if (nombre.length() > 20) {
                    nombre = nombre.substring(0, 20) + "...";
                }
                textViewUserGreeting.setText(nombre);
            }
        } else {
            textViewUserGreeting.setText("Usuario");
        }
    }

    private void mostrarMensajeSinConexion() {
        layoutSinConexion.setVisibility(android.view.View.VISIBLE);
    }

    /**
     * Sistema de detección de red para garantizar la sincronización con Firebase.
     */
    private void inicializarMonitoreoRed() {
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (!hayConexionInternet()) {
            mostrarMensajeSinConexion();
        }
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                runOnUiThread(() -> {
                    if (textosActuales != null && !textosActuales.isEmpty()) {
                        actualizarTextos(textosActuales);
                    } else {
                        iniciarEscuchaDatos();
                    }
                });
            }

            @Override
            public void onLost(Network network) {
                runOnUiThread(() -> mostrarMensajeSinConexion());
            }
        };
        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build();
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
    }

    private boolean hayConexionInternet() {
        Network network = connectivityManager.getActiveNetwork();
        if (network == null)
            return false;
        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
        return capabilities != null && (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
    }

    /**
     * Enmienda un aviso global a nivel de sistema para posibles integraciones
     * futuras.
     */
    private void enviarBroadcastCambioIdioma() {
        Intent intent = new Intent("com.example.amasonapp.CAMBIO_IDIOMA");
        intent.putExtra("idioma", idiomaActual);
        sendBroadcast(intent);
    }

    @Override
    protected void onStop() {
        super.onStop();
        cerrarSesion();
    }

    public String getIdiomaActual() {
        return idiomaActual;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (textosRepository != null)
            textosRepository.detenerEscucha();
        if (connectivityManager != null && networkCallback != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        }
    }
}