package com.bfr.helloworld.ui;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;
import com.bfr.helloworld.R;
import com.bfr.helloworld.utils.Logger;

/**
 * Contrôleur UI pour interface 100% vocale
 * Pas de boutons, interaction uniquement par la voix
 */
public class VocalUIController implements UICallback {
    private static final String TAG = "VocalUIController";
    private static final boolean DEBUG_MODE = false; // Mettre à true pour voir les infos de debug

    private final Activity activity;
    private final TextView txtDebugStatus;
    private final TextView txtDebugScore;
    private final View debugOverlay;

    public VocalUIController(Activity activity) {
        this.activity = activity;

        // Initialisation des vues de debug (optionnelles)
        txtDebugStatus = activity.findViewById(R.id.txtDebugStatus);
        txtDebugScore = activity.findViewById(R.id.txtDebugScore);
        debugOverlay = activity.findViewById(R.id.debug_overlay);

        // Masquer le debug par défaut
        setDebugVisible(DEBUG_MODE);

        Logger.i(TAG, "VocalUIController initialisé - Mode 100% vocal");
    }

    /**
     * Active/désactive l'affichage du debug
     */
    public void setDebugVisible(boolean visible) {
        if (debugOverlay != null) {
            debugOverlay.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void updateStatus(String message) {
        Logger.i(TAG, "Status: " + message);

        if (txtDebugStatus != null && DEBUG_MODE) {
            activity.runOnUiThread(() -> txtDebugStatus.setText("Status: " + message));
        }
    }

    @Override
    public void updateQuestion(String question, int questionNumber, int totalQuestions) {
        Logger.i(TAG, "Question " + questionNumber + "/" + totalQuestions + ": " + question);

        // Pas d'affichage visuel, tout est vocal
        updateStatus("Question " + questionNumber + "/" + totalQuestions);
    }

    @Override
    public void updateScore(int correctAnswers, int currentQuestion) {
        Logger.i(TAG, "Score: " + correctAnswers + "/" + currentQuestion);

        if (txtDebugScore != null && DEBUG_MODE) {
            activity.runOnUiThread(() -> {
                String scoreText = "Score: " + correctAnswers + "/" + currentQuestion;
                txtDebugScore.setText(scoreText);
            });
        }
    }

    @Override
    public void setStartQuizEnabled(boolean enabled) {
        // Pas de boutons dans l'interface vocale
        Logger.d(TAG, "Quiz " + (enabled ? "peut être démarré" : "ne peut pas être démarré"));
    }

    @Override
    public void setListenAnswerEnabled(boolean enabled) {
        // Pas de boutons dans l'interface vocale
        Logger.d(TAG, "Écoute " + (enabled ? "activée" : "désactivée"));
    }

    @Override
    public void showError(String error) {
        Logger.e(TAG, "Erreur: " + error);
        updateStatus("Erreur: " + error);
    }

    @Override
    public void showSuccess(String message) {
        Logger.i(TAG, "Succès: " + message);
        updateStatus("Succès: " + message);
    }

    /**
     * Initialise l'état par défaut (interface vocale)
     */
    public void initializeDefaultState() {
        activity.runOnUiThread(() -> {
            updateStatus("Interface vocale prête");
            updateScore(0, 0);
        });
        Logger.i(TAG, "État par défaut initialisé - Interface 100% vocale");
    }
}