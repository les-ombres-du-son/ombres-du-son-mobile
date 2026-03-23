package fr.upjv.lesombresduson.ui.navigation;

import fr.upjv.lesombresduson.ui.game.cecilia.CeciliaFinalScoreActivity;
import fr.upjv.lesombresduson.ui.game.cecilia.CeciliaIntroActivity;
import fr.upjv.lesombresduson.ui.game.cecilia.CeciliaLevel1Activity;
import fr.upjv.lesombresduson.ui.game.cecilia.CeciliaLevel2Activity;
import fr.upjv.lesombresduson.ui.game.cecilia.CeciliaLevel3Activity;
import fr.upjv.lesombresduson.ui.game.cecilia.CeciliaLevel4Activity;
import fr.upjv.lesombresduson.ui.game.cecilia.CeciliaLevel5Activity;
import fr.upjv.lesombresduson.ui.game.lum.LumGameActivity;

/**
 * Classe utilitaire pour la navigation entre les activités du jeu.
 */
public class GameRouter {

    /**
     * Détermine quelle activité lancer pour le personnage Cécilia.
     */
    public static Class<?> getCeciliaActivityClass(int level, boolean introFinished) {
        switch (level) {
            case 2:
                return CeciliaLevel2Activity.class;
            case 3:
                return CeciliaLevel3Activity.class;
            case 4:
                return CeciliaLevel4Activity.class;
            case 5:
                return CeciliaLevel5Activity.class;
            case 6:
                return CeciliaFinalScoreActivity.class;
            default:
                // Par défaut (Niveau 1 ou inconnu), on gère la logique de l'intro
                return introFinished ? CeciliaLevel1Activity.class : CeciliaIntroActivity.class;
        }
    }

    /**
     * Détermine quelle activité lancer pour le personnage Lum.
     */
    public static Class<?> getLumActivityClass() {
        return LumGameActivity.class;
    }
}