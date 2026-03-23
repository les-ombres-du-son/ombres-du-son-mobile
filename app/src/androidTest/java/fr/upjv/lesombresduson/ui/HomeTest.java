package fr.upjv.lesombresduson.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import fr.upjv.lesombresduson.R;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class HomeTest {

    private static final String PREFS_NAME = "AppPrefs";
    private static final String KEY_PRIVACY_ACCEPTED = "privacy_accepted";

    @Before
    public void setUp() {
        // Reset des préférences pour forcer le comportement d'une première installation
        Context context = ApplicationProvider.getApplicationContext();
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().commit();
    }

    @Test
    public void testPrivacyPopupDisplaysOnFirstLaunch() {
        try (ActivityScenario<Home> scenario = ActivityScenario.launch(Home.class)) {
            // L'alerte RGPD doit s'afficher si aucun choix n'est sauvegardé
            onView(withText("Politique de confidentialité")).check(matches(isDisplayed()));
            onView(withText("J'accepte")).check(matches(isDisplayed()));
        }
    }

    @Test
    public void testAcceptPrivacyPolicy_UpdatesSharedPreferences() {
        try (ActivityScenario<Home> scenario = ActivityScenario.launch(Home.class)) {
            onView(withText("J'accepte")).perform(click());

            // Vérif que l'acceptation est bien stockée en local
            Context context = ApplicationProvider.getApplicationContext();
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

            assertTrue("Le flag privacy_accepted n'est pas à true", prefs.getBoolean(KEY_PRIVACY_ACCEPTED, false));
        }
    }

    @Test
    public void testButtonsAreVisible_WhenPrivacyAlreadyAccepted() {
        // Mock de l'état : on simule un utilisateur ayant déjà accepté
        Context context = ApplicationProvider.getApplicationContext();
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_PRIVACY_ACCEPTED, true).commit();

        try (ActivityScenario<Home> scenario = ActivityScenario.launch(Home.class)) {
            // On s'attend à voir le menu principal sans être bloqué par la popup
            onView(withId(R.id.button_start)).check(matches(isDisplayed()));
            onView(withId(R.id.button_setting)).check(matches(isDisplayed()));
            onView(withId(R.id.button_logout)).check(matches(isDisplayed()));
        }
    }
}