package fr.upjv.lesombresduson.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.google.firebase.auth.FirebaseAuth;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import fr.upjv.lesombresduson.R;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class MainActivityTest {

    // Cette règle lance la MainActivity avant chaque test
    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Before
    public void setUp() {
        // On force la déconnexion pour éviter le finish() immédiat dans onCreate
        FirebaseAuth.getInstance().signOut();
    }

    /**
     * Test de l'affichage du bouton de connexion Google
     */
    @Test
    public void testSignInButtonIsDisplayed() {
        // Vérifie que le bouton de connexion Google est bien visible à l'écran
        onView(withId(R.id.btnGoogleSignIn)).check(matches(isDisplayed()));
    }

    /**
     * Test de la présence de la permission Internet
     */
    @Test
    public void testHasInternetPermission() {
        // On récupère l'instance de l'activité pour tester la méthode publique
        activityRule.getScenario().onActivity(activity -> {
            boolean hasPermission = activity.hasInternetPermission();
            assertTrue("La permission Internet devrait être accordée", hasPermission);
        });
    }
}