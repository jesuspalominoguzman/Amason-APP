package com.example.amasonapp;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Bundle;

import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.example.amasonapp.data.TextosRepository;
import com.example.amasonapp.model.Texto;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.List;

/**
 * PANTALLA DE ACCESO (LOGIN)
 * 
 * Esta actividad gestiona el primer punto de entrada de la aplicación.
 * Ofrece autenticación mediante Google Sign-In y sincronización automática
 * de textos multilingües desde Firebase Firestore. También monitorea el
 * estado de la conexión a Internet en tiempo real.
 */
public class LoginActivity extends AppCompatActivity {

    // --- Componentes de la Interfaz (UI) ---
    private TextView textViewBienvenida;
    private MaterialButton buttonGoogleSignIn;
    private Button buttonIdioma;
    private LinearLayout layoutSinConexion;

    // --- Gestión de Datos y Localización ---
    private TextosRepository textosRepository;
    private String idiomaActual = "es"; // Idioma inicial: Castellano
    private List<Texto> textosActuales;

    // --- Autenticación y Seguridad (Firebase/Google) ---
    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth mAuth;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    // --- Servicios de Sistema (Conectividad) ---
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Pasos de inicialización ordenados lógicamente
        inicializarVistas();
        mAuth = FirebaseAuth.getInstance();
        configurarGoogleSignIn();
        configurarBotonIdioma();
        inicializarMonitoreoRed();
        configurarBotonGoogle();

        // Iniciamos la carga de textos bilingües de la UI
        textosRepository = new TextosRepository();
        iniciarEscuchaDatos();
    }

    /**
     * Vincula las variables con los componentes definidos en el XML activity_login.
     */
    private void inicializarVistas() {
        textViewBienvenida = findViewById(R.id.textView_bienvenida);
        buttonGoogleSignIn = findViewById(R.id.button_google_signin);
        buttonIdioma = findViewById(R.id.button_idioma_login);
        layoutSinConexion = findViewById(R.id.layout_sin_conexion_login);
    }

    /**
     * Define el comportamiento del botón de cambio de idioma.
     * Alterna entre "es" y "en" actualizando la UI de forma inmediata.
     */
    private void configurarBotonIdioma() {
        buttonIdioma.setOnClickListener(v -> {
            if (!hayConexionInternet())
                return;

            // Alternancia lógica de idiomas
            if (idiomaActual.equals("es")) {
                idiomaActual = "en";
                buttonIdioma.setText("EN");
            } else {
                idiomaActual = "es";
                buttonIdioma.setText("ES");
            }

            // Aplicamos los textos traducidos si ya han sido cargados
            if (textosActuales != null) {
                actualizarTextos(textosActuales);
            }
        });
    }

    /**
     * Gestiona el evento de clic para iniciar el flujo de Google Sign-In.
     */
    private void configurarBotonGoogle() {
        buttonGoogleSignIn.setOnClickListener(v -> {
            if (!hayConexionInternet())
                return;
            iniciarSesionConGoogle();
        });
    }

    /**
     * Configura el cliente de Google y el launcher para procesar el resultado del
     * login.
     */
    private void configurarGoogleSignIn() {
        // Solicitamos el ID Token para autenticar posteriormente en Firebase
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Nuevo estándar de Android para recibir resultados de actividades externas
        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                        manejarResultadoSignIn(task);
                    }
                });
    }

    private void iniciarSesionConGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        googleSignInLauncher.launch(signInIntent);
    }

    private void manejarResultadoSignIn(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            autenticarConFirebase(account.getIdToken());
        } catch (ApiException e) {
            Toast.makeText(this, "Error al iniciar sesión: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Valida la cuenta de Google contra Firebase Authentication.
     */
    private void autenticarConFirebase(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        navegarAMainActivity();
                    } else {
                        Toast.makeText(this, "Error de autenticación en Firebase", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Salta a la pantalla principal y limpia el historial de actividades.
     */
    private void navegarAMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    /**
     * Suscribe la pantalla a los cambios en la colección de textos de Firebase.
     */
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
     * Mapea los textos recibidos a sus respectivos componentes de UI.
     */
    private void actualizarTextos(List<Texto> textos) {
        if (!hayConexionInternet())
            return;

        layoutSinConexion.setVisibility(View.GONE);

        for (Texto texto : textos) {
            String textoTraducido = obtenerTextoSegunIdioma(texto);

            // Vinculación por identificador lógico (ClaveTexto)
            if ("login_bienvenida".equals(texto.getClaveTexto())) {
                textViewBienvenida.setText(textoTraducido);
                textViewBienvenida.setVisibility(View.VISIBLE);
            } else if ("login_boton_google".equals(texto.getClaveTexto())) {
                buttonGoogleSignIn.setText(textoTraducido);
                buttonGoogleSignIn.setVisibility(View.VISIBLE);
            }
        }
    }

    private String obtenerTextoSegunIdioma(Texto texto) {
        return (idiomaActual.equals("en")) ? texto.getEn() : texto.getEs();
    }

    /**
     * Oculta el login y muestra un aviso si no hay red detectada.
     */
    private void mostrarMensajeSinConexion() {
        textViewBienvenida.setVisibility(View.GONE);
        buttonGoogleSignIn.setVisibility(View.GONE);
        layoutSinConexion.setVisibility(View.VISIBLE);
    }

    /**
     * Establece un observador sobre el estado de la antena/wifi.
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
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
    }

    /**
     * Comprobación instantánea del estado de red activo.
     */
    private boolean hayConexionInternet() {
        if (connectivityManager != null) {
            Network network = connectivityManager.getActiveNetwork();
            if (network != null) {
                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
                return capabilities != null &&
                        (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
            }
        }
        return false;
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (textosRepository != null) {
            textosRepository.detenerEscucha();
        }
        if (connectivityManager != null && networkCallback != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        }
    }
}
