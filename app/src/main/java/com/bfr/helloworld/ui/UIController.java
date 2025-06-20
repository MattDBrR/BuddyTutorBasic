package com.bfr.helloworld.ui;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;
import com.bfr.helloworld.R;
import com.bfr.helloworld.utils.Logger;

/**
 * Contrôleur de l'interface utilisateur pour interface 100% vocale
 * Compatible avec UICallback interface
 */
public class UIController implements UICallback {
    private static final String TAG = "UIController";

    private final Activity activity;
    private final TextView txtDebugStatus;
    private final TextView txtDebugScore;
    private final View debugOverlay;

    public UIController(Activity activity) {
        this.activity = activity;

        // Vues optionnelles pour le debug (peuvent être null)
        txtDebugStatus = activity.findViewById(R.id.txtDebugStatus);
        txtDebugScore = activity.findViewById(R.id.txtDebugScore);
        debugOverlay = activity.findViewById(R.id.debug_overlay);

        Logger.i(TAG, "UIController initialisé pour interface vocale");
    }

    @Override
    public void updateStatus(String message) {
        Logger.i(TAG, "Status: " + message);

        if (txtDebugStatus != null) {
            activity.runOnUiThread(() -> txtDebugStatus.setText(message));
        }
    }

    @Override
    public void updateQuestion(String question, int questionNumber, int totalQuestions) {
        Logger.i(TAG, "Question " + questionNumber + "/" + totalQuestions + ": " + question);
        updateStatus("Question " + questionNumber + "/" + totalQuestions);
    }

    @Override
    public void updateScore(int correctAnswers, int currentQuestion) {
        Logger.i(TAG, "Score: " + correctAnswers + "/" + currentQuestion);

        if (txtDebugScore != null) {
            activity.runOnUiThread(() -> {
                String scoreText = "Score: " + correctAnswers + "/" + currentQuestion;
                txtDebugScore.setText(scoreText);
            });
        }
    }

    @Override
    public void setStartQuizEnabled(boolean enabled) {
        Logger.d(TAG, "Quiz " + (enabled ? "peut être démarré" : "ne peut pas être démarré"));
        // Pas de boutons dans l'interface vocale
    }

    @Override
    public void setListenAnswerEnabled(boolean enabled) {
        Logger.d(TAG, "Écoute " + (enabled ? "activée" : "désactivée"));
        // Pas de boutons dans l'interface vocale
    }

    @Override
    public void showError(String error) {
        Logger.e(TAG, "Erreur: " + error);
        updateStatus("❌ " + error);
    }

    @Override
    public void showSuccess(String message) {
        Logger.i(TAG, "Succès: " + message);
        updateStatus("✅ " + message);
    }

    /**
     * Initialise l'état par défaut
     */
    public void initializeDefaultState() {
        activity.runOnUiThread(() -> {
            updateStatus("Interface vocale prête");
            updateScore(0, 0);

            // Activer le debug temporairement pour voir ce qui se passe
            if (debugOverlay != null) {
                debugOverlay.setVisibility(View.VISIBLE);
                Logger.d(TAG, "Debug overlay activé temporairement");
            }
        });
        Logger.i(TAG, "État par défaut initialisé - Interface 100% vocale");
    }

    /**
     * Active/désactive l'affichage du debug
     */
    public void setDebugVisible(boolean visible) {
        if (debugOverlay != null) {
            activity.runOnUiThread(() -> {
                debugOverlay.setVisibility(visible ? View.VISIBLE : View.GONE);
            });
        }
    }
}