package fr.upjv.lesombresduson.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Map;

import fr.upjv.lesombresduson.R;
import fr.upjv.lesombresduson.data.model.GameCharacter;
import fr.upjv.lesombresduson.data.model.TutorialStep;
import fr.upjv.lesombresduson.data.remote.FirebaseHelper;
import fr.upjv.lesombresduson.ui.navigation.GameRouter;
import fr.upjv.lesombresduson.manager.core.TutorialManager;

/**
 * Activité de choix de personnage.
 * Gère la sélection et la confirmation du personnage.
 * Démarre une nouvelle partie ou continue une partie en cours.
 */
public class StartChoiseCharacter extends AppCompatActivity {

    private ConstraintLayout selectionGroup;
    private ConstraintLayout confirmedGroup;
    private MaterialButton btnBack;
    private MaterialButton btnContinue;
    private String currentUserId;

    private TutorialManager tutorialManager;

    private final GameCharacter CECILIA = new GameCharacter(1, "Cécilia (cécité totale)", R.drawable.cecilia);
    private final GameCharacter LUM = new GameCharacter(2, "Lum (cécité partielle)", R.drawable.lum);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choice_character);

        tutorialManager = new TutorialManager(this);

        boolean isTestMode = getIntent().getBooleanExtra("IS_TEST_MODE", false);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            currentUserId = currentUser.getUid();
        } else if (isTestMode) {
            // Mode Test : On invente un faux ID pour ne pas crasher
            currentUserId = "test_user_id";
        } else {
            // Production normale : L'utilisateur n'est pas connecté
            Toast.makeText(this, "Erreur: Utilisateur non connecté.", Toast.LENGTH_LONG).show();
            return;
        }

        selectionGroup = findViewById(R.id.character_selection_group);
        confirmedGroup = findViewById(R.id.character_confirmed_group);
        btnBack = findViewById(R.id.button_back);

        LinearLayout layoutCecilia = findViewById(R.id.layout_cecilia);
        LinearLayout layoutLum = findViewById(R.id.layout_lum);

        btnContinue = findViewById(R.id.button_continue);
        MaterialButton btnNewGame = findViewById(R.id.button_new_game);

        btnContinue.setEnabled(false);
        btnContinue.setVisibility(View.GONE);

        layoutCecilia.setOnClickListener(v -> handleCharacterSelection(CECILIA));
        layoutLum.setOnClickListener(v -> handleCharacterSelection(LUM));

        btnBack.setOnClickListener(v -> {
            if (confirmedGroup.getVisibility() == View.VISIBLE) {
                confirmedGroup.setVisibility(View.GONE);
                selectionGroup.setVisibility(View.VISIBLE);
                Toast.makeText(StartChoiseCharacter.this, "Annulation de la sélection.", Toast.LENGTH_SHORT).show();
            } else {
                startActivity(new Intent(StartChoiseCharacter.this, Home.class));
                finish();
            }
        });

        btnContinue.setOnClickListener(v -> launchGameActivity(getLastSelectedCharacter()));

        btnNewGame.setOnClickListener(v -> {
            GameCharacter selectedCharacter = getLastSelectedCharacter();
            if (selectedCharacter == null) return;

            FirebaseHelper.getInstance().checkGameExists(currentUserId, selectedCharacter.getName(), gameExists -> {
                if (gameExists) {
                    showConfirmNewGameDialog(selectedCharacter);
                } else {
                    startNewGame(selectedCharacter);
                }
            });
        });
    }

    /**
     * Récupère le dernier personnage sélectionné.
     *
     * @return Le dernier personnage sélectionné.
     */
    private GameCharacter getLastSelectedCharacter() {
        TextView centerName = findViewById(R.id.text_center_name);
        String name = centerName.getText().toString();

        if (name.equals(CECILIA.getName())) return CECILIA;
        if (name.equals(LUM.getName())) return LUM;
        return null;
    }

    /**
     * Gère la sélection d'un personnage.
     *
     * @param selectedCharacter Le personnage sélectionné.
     */
    private void handleCharacterSelection(GameCharacter selectedCharacter) {
        ImageView centerImage = findViewById(R.id.image_center_character);
        TextView centerName = findViewById(R.id.text_center_name);

        centerImage.setImageResource(selectedCharacter.getImageResId());
        centerName.setText(selectedCharacter.getName());

        selectionGroup.setVisibility(View.GONE);
        confirmedGroup.setVisibility(View.VISIBLE);

        checkIfGameExists(selectedCharacter.getName());
    }

    /**
     * Vérifie si une partie existe pour le personnage sélectionné.
     *
     * @param characterName Le nom du personnage.
     */
    private void checkIfGameExists(String characterName) {
        if (currentUserId == null) return;

        btnContinue.setVisibility(View.GONE);
        btnContinue.setEnabled(false);

        FirebaseHelper.getInstance().getGameData(currentUserId, characterName, new FirebaseHelper.GameDataCallback() {
            @Override
            public void onDataLoaded(Map<String, Object> gameData) {
                if (gameData != null && "started".equals(gameData.get("state"))) {
                    btnContinue.setText("Continuer");
                    btnContinue.setEnabled(true);
                    btnContinue.setVisibility(View.VISIBLE);
                } else {
                    btnContinue.setEnabled(false);
                    btnContinue.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(StartChoiseCharacter.this, "Erreur de connexion Firebase", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Affiche une boite de dialogue de confirmation pour recommencer la partie.
     *
     * @param character Le personnage sélectionné.
     */
    private void showConfirmNewGameDialog(GameCharacter character) {
        new AlertDialog.Builder(this)
                .setTitle("Partie Existante")
                .setMessage("Vous avez déjà une partie en cours avec " + character.getName() + ". Voulez-vous la recommencer et perdre la progression non sauvegardée ?")
                .setPositiveButton("Nouvelle Partie", (dialog, which) -> startNewGame(character))
                .setNegativeButton("Annuler", (dialog, which) -> dialog.dismiss())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    /**
     * Démarre une nouvelle partie avec le personnage sélectionné.
     *
     * @param character Le personnage sélectionné.
     */
    private void startNewGame(GameCharacter character) {
        if (currentUserId == null || character == null) return;

        FirebaseHelper.getInstance().finishGame(currentUserId, character.getName());

        btnContinue.setVisibility(View.GONE);
        btnContinue.setEnabled(false);

        FirebaseHelper.getInstance().saveNewGame(currentUserId, character.getName());

        // 1. Déterminer le nom du personnage tel qu'il est écrit dans le JSON
        String charKey = (character.getId() == CECILIA.getId()) ? "cecilia" : "lum";

        // 2. Récupérer la liste des tutoriels pour ce personnage
        java.util.List<fr.upjv.lesombresduson.data.model.TutorialStep> steps = tutorialManager.getTutorialsForCharacter(charKey);

        // 3. Si on a des tutoriels, on les affiche, sinon on lance le jeu direct
        if (steps != null && !steps.isEmpty()) {
            showTutorialStep(steps, 0, character);
        } else {
            launchGameActivity(character);
        }
    }

    /**
     * Affiche les tutoriels l'un après l'autre.
     */
    private void showTutorialStep(java.util.List<TutorialStep> steps, int index, GameCharacter character) {
        // Si on a tout lu, on lance le jeu !
        if (index >= steps.size()) {
            launchGameActivity(character);
            return;
        }

        TutorialStep currentStep = steps.get(index);

        int iconResId = getResources().getIdentifier(currentStep.getImageRes(), "drawable", getPackageName());

        String buttonText = (index == steps.size() - 1) ? "Jouer !" : "Suivant";

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(currentStep.getTitle())
                .setMessage(currentStep.getDescription())
                .setCancelable(false) // Empêche de cliquer à côté pour fermer
                .setPositiveButton(buttonText, (dialog, which) -> {
                    // On passe à l'étape suivante
                    showTutorialStep(steps, index + 1, character);
                });

        // Si on a bien trouvé l'image dans le dossier drawable, on l'ajoute
        if (iconResId != 0) {
            builder.setIcon(iconResId);
        }

        builder.show();
    }

    /**
     * Lance l'activité du jeu en fonction du personnage sélectionné.
     *
     * @param character Le personnage sélectionné.
     */
    private void launchGameActivity(GameCharacter character) {
        if (character == null || currentUserId == null) return;

        FirebaseHelper.getInstance().getGameData(currentUserId, character.getName(), new FirebaseHelper.GameDataCallback() {
            @Override
            public void onDataLoaded(Map<String, Object> gameData) {
                boolean introFinished = false;
                int currentLevel = 1;
                boolean level5IntroFinished = false;
                int savedVisionIndex = -1;

                if (gameData != null) {
                    if (gameData.get("introFinished") instanceof Boolean) {
                        introFinished = (Boolean) gameData.get("introFinished");
                    }
                    if (gameData.get("currentLevel") instanceof Number) {
                        currentLevel = ((Number) gameData.get("currentLevel")).intValue();
                    }
                    if (gameData.get("level5IntroFinished") instanceof Boolean) {
                        level5IntroFinished = (Boolean) gameData.get("level5IntroFinished");
                    }
                    if (gameData.get("visionIndex") instanceof Number) {
                        savedVisionIndex = ((Number) gameData.get("visionIndex")).intValue();
                    }
                }

                Class<?> targetClass = null;
                String toastMessage = "";

                if (character.getId() == CECILIA.getId()) {
                    targetClass = GameRouter.getCeciliaActivityClass(currentLevel, introFinished);
                    toastMessage = currentLevel == 1 ? (introFinished ? "Reprise du Niveau 1..." : "Nouvelle partie : Introduction...") : "Niveau " + currentLevel + " (Cecilia)";
                } else {
                    targetClass = GameRouter.getLumActivityClass();
                    toastMessage = savedVisionIndex != -1 ? "Reprise de la partie de Lum..." : "Nouvelle partie de Lum...";
                }

                if (targetClass != null) {
                    Toast.makeText(StartChoiseCharacter.this, toastMessage, Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(StartChoiseCharacter.this, targetClass);
                    intent.putExtra("CHARACTER_NAME", character.getName());
                    intent.putExtra("level5IntroFinished", level5IntroFinished);

                    // On passe les données nécessaires à LumGameActivity
                    intent.putExtra("VISION_INDEX", savedVisionIndex);
                    intent.putExtra("USER_ID", currentUserId);

                    startActivity(intent);
                    finish();
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(StartChoiseCharacter.this, "Erreur de chargement de la progression.", Toast.LENGTH_LONG).show();
            }
        });
    }
}