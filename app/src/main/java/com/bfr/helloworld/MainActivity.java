package com.bfr.helloworld;

import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.bfr.buddy.ui.shared.FacialExpression;
import com.bfr.buddy.ui.shared.IUIFaceAnimationCallback;
import com.bfr.buddy.speech.shared.ITTSCallback;
import com.bfr.buddy.utils.events.EventItem;
import com.bfr.buddysdk.BuddyActivity;
import com.bfr.buddysdk.BuddySDK;
import com.bfr.helloworld.R;

public class MainActivity extends BuddyActivity {

    private static final String TAG = "BuddyTutorBasic";

    // Variables UI
    private Button btnWelcome, btnEncouragement, btnHelp, btnCelebrate;
    private TextView sdkStatus;

    // Variables état
    private boolean isSDKReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialisation des vues
        initializeViews();

        // Configuration des listeners
        setupButtonListeners();

        Log.i(TAG, "Application BuddyTutorBasic démarrée");
    }

    private void initializeViews() {
        btnWelcome = findViewById(R.id.btnWelcome);
        btnEncouragement = findViewById(R.id.btnEncouragement);
        btnHelp = findViewById(R.id.btnHelp);
        btnCelebrate = findViewById(R.id.btnCelebrate);
        sdkStatus = findViewById(R.id.sdkStatus);

        // Désactiver les boutons en attendant l'initialisation SDK
        setButtonsEnabled(false);
    }

    private void setupButtonListeners() {
        btnWelcome.setOnClickListener(v -> onWelcomeClick());
        btnEncouragement.setOnClickListener(v -> onEncouragementClick());
        btnHelp.setOnClickListener(v -> onHelpClick());
        btnCelebrate.setOnClickListener(v -> onCelebrateClick());
    }

    @Override
    public void onSDKReady() {
        Log.i(TAG, "SDK Buddy initialisé avec succès");

        // Interface transparente pour préserver l'aspect social
        BuddySDK.UI.setViewAsFace(findViewById(R.id.view_face));

        // Expression neutre au démarrage
        BuddySDK.UI.setMood(FacialExpression.NEUTRAL, new IUIFaceAnimationCallback.Stub() {
            @Override
            public void onAnimationEnd(String animationName, String animationType) {
                Log.i(TAG, "Animation faciale terminée: " + animationName + " (" + animationType + ")");
            }
        });

        // Mise à jour de l'interface
        runOnUiThread(() -> {
            isSDKReady = true;
            sdkStatus.setText("✅ SDK Prêt - Assistant scolaire actif");
            setButtonsEnabled(true);

            // Message de bienvenue automatique
            speakWithExpression("Bonjour ! Je suis ton assistant scolaire Buddy. Je suis là pour t'aider et t'encourager dans tes études !",
                    FacialExpression.HAPPY);
        });
    }

    // === FONCTIONS D'INTERACTION PÉDAGOGIQUE ===

    private void onWelcomeClick() {
        Log.i(TAG, "Fonction d'accueil activée");

        String[] welcomeMessages = {
                "Bonjour ! Je suis ravi de te voir aujourd'hui !",
                "Salut ! Prêt pour une super séance d'apprentissage ?",
                "Hello ! J'espère que tu vas bien. On va bien travailler ensemble !",
                "Coucou ! J'ai hâte de t'aider dans tes études !"
        };

        String message = welcomeMessages[(int) (Math.random() * welcomeMessages.length)];
        speakWithExpression(message, FacialExpression.HAPPY);
    }

    private void onEncouragementClick() {
        Log.i(TAG, "Fonction d'encouragement activée");

        String[] encouragementMessages = {
                "Tu es vraiment courageux ! Continue comme ça !",
                "Je crois en toi ! Tu vas y arriver !",
                "Bravo pour tes efforts ! Tu fais de gros progrès !",
                "Tu es sur la bonne voie ! Ne lâche rien !",
                "Excellent travail ! Je suis fier de toi !"
        };

        String message = encouragementMessages[(int) (Math.random() * encouragementMessages.length)];
        speakWithExpression(message, FacialExpression.LOVE);
    }

    private void onHelpClick() {
        Log.i(TAG, "Fonction d'aide activée");

        String[] helpMessages = {
                "Bien sûr, je suis là pour t'aider ! Prends ton temps, on va y arriver ensemble.",
                "Pas de problème ! Chaque difficulté est une opportunité d'apprendre.",
                "C'est normal d'avoir des questions ! On va chercher la solution ensemble.",
                "Je vais t'accompagner étape par étape. Tu n'es pas seul !",
                "Excellente question ! Réfléchissons ensemble à la réponse."
        };

        String message = helpMessages[(int) (Math.random() * helpMessages.length)];
        speakWithExpression(message, FacialExpression.THINKING);
    }

    private void onCelebrateClick() {
        Log.i(TAG, "Fonction de célébration activée");

        String[] celebrateMessages = {
                "Fantastique ! Tu as réussi ! Je savais que tu en étais capable !",
                "Bravo ! Quel excellent travail ! Tu peux être fier de toi !",
                "Génial ! Tu as surmonté le défi ! C'est formidable !",
                "Magnifique ! Tu progresses vraiment bien ! Continue ainsi !",
                "Extraordinaire ! Tu viens de franchir une étape importante !"
        };

        String message = celebrateMessages[(int) (Math.random() * celebrateMessages.length)];
        speakWithExpression(message, FacialExpression.SURPRISED);
    }

    // === FONCTIONS UTILITAIRES ===

    private void speakWithExpression(String message, FacialExpression expression) {
        if (!isSDKReady) {
            Toast.makeText(this, "SDK non initialisé", Toast.LENGTH_SHORT).show();
            return;
        }

        // Expression faciale synchronisée avec la parole
        BuddySDK.UI.setMood(expression, new IUIFaceAnimationCallback.Stub() {
            @Override
            public void onAnimationEnd(String animationName, String animationType) {
                Log.i(TAG, "Animation faciale terminée: " + animationName + " (" + animationType + ")");
            }
        });

        // Synthèse vocale avec callback
        BuddySDK.Speech.startSpeaking(message, new ITTSCallback.Stub() {
            @Override
            public void onSuccess(String s) {
                Log.i(TAG, "Message prononcé avec succès : " + s);
                // Retour à l'expression neutre après la parole
                runOnUiThread(() -> {
                    BuddySDK.UI.setMood(FacialExpression.NEUTRAL, new IUIFaceAnimationCallback.Stub() {
                        @Override
                        public void onAnimationEnd(String animationName, String animationType) {
                            Log.i(TAG, "Animation neutre terminée: " + animationName + " (" + animationType + ")");
                        }
                    });
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Erreur TTS : " + error);
                runOnUiThread(() ->
                        Toast.makeText(MainActivity.this, "Erreur de synthèse vocale", Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onPause() {}

            @Override
            public void onResume() {}
        });
    }

    private void setButtonsEnabled(boolean enabled) {
        btnWelcome.setEnabled(enabled);
        btnEncouragement.setEnabled(enabled);
        btnHelp.setEnabled(enabled);
        btnCelebrate.setEnabled(enabled);
    }

    // === GESTION DES ÉVÉNEMENTS ===

    @Override
    public void onEvent(EventItem iEvent) {
        // Gestion des événements spécifiques si nécessaire
        super.onEvent(iEvent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Application BuddyTutorBasic fermée");
    }
}