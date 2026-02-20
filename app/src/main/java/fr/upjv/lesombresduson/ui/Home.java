package fr.upjv.lesombresduson.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog; // Attention à bien importer la version AndroidX
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;

import fr.upjv.lesombresduson.R;
import fr.upjv.lesombresduson.ui.settings.SettingsActivity;

/**
 * Activité principale de l'accueil (Menu Principal).
 * Gère la navigation vers le jeu, les paramètres et la déconnexion.
 * Vérifie également les permissions critiques avant de lancer le jeu.
 */
public class Home extends AppCompatActivity {

    private Button btnLogout;
    private Button btnSetting;
    private Button btnStart;

    // Définition des permissions requises
    private static final String RECORD_AUDIO_PERMISSION = android.Manifest.permission.RECORD_AUDIO;
    private static final String CAMERA_PERMISSION = android.Manifest.permission.CAMERA;
    private static final String[] REQUIRED_PERMISSIONS = {RECORD_AUDIO_PERMISSION, CAMERA_PERMISSION};

    // Lanceur d'activité pour gérer les résultats des demandes de permissions
    private ActivityResultLauncher<String[]> permissionLauncher;

    // Constantes pour les préférences de l'application
    private static final String PREFS_NAME = "AppPrefs";
    private static final String KEY_PRIVACY_ACCEPTED = "privacy_accepted";

    /**
     * Initialise l'interface, enregistre le callback de permissions et configure les listeners des boutons.
     * @param savedInstanceState État sauvegardé de l'instance.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        btnLogout = findViewById(R.id.button_logout);
        btnSetting = findViewById(R.id.button_setting);
        btnStart = findViewById(R.id.button_start);

        // Vérifier si l'utilisateur a déjà accepté la politique de confidentialité
        checkPrivacyConsent();

        // Initialisation du lanceur de permissions
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                permissions -> {
                    // Vérifie si toutes les permissions requises sont accordées
                    boolean allGranted = true;
                    for (String permission : REQUIRED_PERMISSIONS) {
                        // Utilisation de Boolean.TRUE.equals pour éviter les NullPointerException potentiels
                        if (!Boolean.TRUE.equals(permissions.get(permission))) {
                            allGranted = false;
                            break;
                        }
                    }

                    if (allGranted) {
                        // Toutes les permissions sont accordées, lancer l'activité de choix
                        launchStartChoiseCharacterActivity();
                    } else {
                        // Au moins une permission a été refusée, rediriger vers les paramètres de l'application
                        Toast.makeText(this, "Permissions requises refusées. Veuillez les accorder dans les paramètres.", Toast.LENGTH_LONG).show();
                        launchSettingsActivity();
                    }
                }
        );

        // Logique du bouton "Déconnexion"
        btnLogout.setOnClickListener(v -> logoutUser());

        // Logique du bouton "Paramètres"
        btnSetting.setOnClickListener(v -> launchSettingsActivity());

        // Logique du bouton "Démarrer"
        btnStart.setOnClickListener(v -> checkAndRequestPermissions());
    }

    // =========================================================================
    //                 GESTION DU CONSENTEMENT (RGPD)
    // =========================================================================

    /**
     * Vérifie dans les préférences locales si l'utilisateur a déjà donné son accord.
     * Si ce n'est pas le cas, affiche la popup de consentement.
     */
    private void checkPrivacyConsent() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean hasAccepted = prefs.getBoolean(KEY_PRIVACY_ACCEPTED, false);

        if (!hasAccepted) {
            showPrivacyPolicyPopup();
        }
    }

    /**
     * Affiche une popup bloquante demandant l'accord pour le traitement des données.
     */
    private void showPrivacyPolicyPopup() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Politique de confidentialité");


        String message = "Pour continuer à utiliser l'application et profiter de l'expérience de jeu, vous devez accepter le traitement de vos données d'utilisation (temps de réaction, capteurs). <br><br>Consultez notre <a href=\"https://ton-site.com/politique-confidentialite\">Politique de confidentialité</a>.";

        Spanned htmlMessage;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            htmlMessage = Html.fromHtml(message, Html.FROM_HTML_MODE_COMPACT);
        } else {
            htmlMessage = Html.fromHtml(message);
        }

        builder.setMessage(htmlMessage);

        // Empêche de fermer la popup en cliquant à l'extérieur
        builder.setCancelable(false);

        // Si l'utilisateur ACCEPTE
        builder.setPositiveButton("J'accepte", (dialog, which) -> {
            // On sauvegarde l'accord dans les SharedPreferences pour ne plus lui redemander
            SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
            editor.putBoolean(KEY_PRIVACY_ACCEPTED, true);
            editor.apply();
        });

        // Si l'utilisateur REFUSE
        builder.setNegativeButton("Je refuse", (dialog, which) -> {
            Toast.makeText(Home.this, "Consentement refusé. Déconnexion.", Toast.LENGTH_SHORT).show();
            logoutUser(); // Déconnexion immédiate
        });

        AlertDialog dialog = builder.create();
        dialog.show();

        // Étape CRUCIALE : Rendre le lien cliquable dans le message du dialog
        TextView messageView = dialog.findViewById(android.R.id.message);
        if (messageView != null) {
            messageView.setMovementMethod(LinkMovementMethod.getInstance());
        }
    }

    /**
     * Déconnecte l'utilisateur de Firebase et le renvoie sur la page de connexion.
     */
    private void logoutUser() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(Home.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Vérifie si les permissions (Micro et Caméra) sont déjà accordées par le système.
     * Si oui, lance le jeu. Si non, déclenche la demande de permission système.
     */
    private void checkAndRequestPermissions() {
        boolean audioGranted = ContextCompat.checkSelfPermission(this, RECORD_AUDIO_PERMISSION) == PackageManager.PERMISSION_GRANTED;
        boolean cameraGranted = ContextCompat.checkSelfPermission(this, CAMERA_PERMISSION) == PackageManager.PERMISSION_GRANTED;

        if (audioGranted && cameraGranted) {
            // Toutes les permissions sont déjà accordées
            launchStartChoiseCharacterActivity();
        } else {
            // Demander les permissions manquantes
            permissionLauncher.launch(REQUIRED_PERMISSIONS);
        }
    }

    /**
     * Lance l'activité de sélection de personnage (Étape suivante).
     */
    private void launchStartChoiseCharacterActivity() {
        // Petite note : Attention à l'orthographe, en anglais c'est "Choice" et non "Choise"
        Intent intent = new Intent(Home.this, StartChoiseCharacter.class);
        startActivity(intent);
    }

    /**
     * Lance l'activité des paramètres de l'application.
     */
    private void launchSettingsActivity() {
        Intent intent = new Intent(Home.this, SettingsActivity.class);
        startActivity(intent);
    }
}