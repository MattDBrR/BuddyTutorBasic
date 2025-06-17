package com.bfr.helloworld.ui;

import android.app.Activity;
import android.widget.Button;
import android.widget.TextView;
import com.bfr.helloworld.R;
import com.bfr.helloworld.utils.Logger;

/**
 * Contrôleur de l'interface utilisateur
 */

public class UIController implements UICallback {
    private static final String TAG = "UIController";

    private final Activity activity;
    private final TextView txtStatus;
    private final TextView txtQuestion;
    private final TextView txtScore;
    private final Button btnStartQuiz;
    private final Button btnListenAnswer;

    public UIController(Activity activity) {
        this.activity = activity;

        // Initialisation des vues
        txtStatus = activity.findViewById(R.id.txtStatus);
        txtQuestion = activity.findViewById(R.id.txtQuestion);
        txtScore = activity.findViewById(R.id.txtScore);
        btnStartQuiz = activity.findViewById(R.id.btnStartQuiz);
        btnListenAnswer = activity.findViewById(R.id.btnListenAnswer);

        Logger.i(TAG, "UIController initialisé");
    }

    @Override
    public void updateStatus(String message) {
        activity.runOnUiThread(() -> {
            txtStatus.setText(message);
            Logger.d(TAG, "Status mis à jour: " + message);
        });
    }

    @Override
    public void updateQuestion(String question, int questionNumber, int totalQuestions) {
        activity.runOnUiThread(() -> {
            String displayText = "Question " + questionNumber + "/" + totalQuestions + ":\n" + question;
            txtQuestion.setText(displayText);
            Logger.d(TAG, "Question mise à jour: " + displayText);
        });
    }

    @Override
    public void updateScore(int correctAnswers, int currentQuestion) {
        activity.runOnUiThread(() -> {
            String scoreText = "Score: " + correctAnswers + "/" + currentQuestion;
            txtScore.setText(scoreText);
            Logger.d(TAG, "Score mis à jour: " + scoreText);
        });
    }

    @Override
    public void setStartQuizEnabled(boolean enabled) {
        activity.runOnUiThread(() -> {
            btnStartQuiz.setEnabled(enabled);
            Logger.d(TAG, "Bouton Start Quiz " + (enabled ? "activé" : "désactivé"));
        });
    }

    @Override
    public void setListenAnswerEnabled(boolean enabled) {
        activity.runOnUiThread(() -> {
            btnListenAnswer.setEnabled(enabled);
            Logger.d(TAG, "Bouton Listen Answer " + (enabled ? "activé" : "désactivé"));
        });
    }

    @Override
    public void showError(String error) {
        activity.runOnUiThread(() -> {
            txtStatus.setText("❌ " + error);
            Logger.e(TAG, "Erreur affichée: " + error);
        });
    }

    @Override
    public void showSuccess(String message) {
        activity.runOnUiThread(() -> {
            txtStatus.setText("✅ " + message);
            Logger.i(TAG, "Succès affiché: " + message);
        });
    }

    /**
     * Configure les listeners pour les boutons
     */
    public void setButtonListeners(Runnable onStartQuiz, Runnable onListenAnswer) {
        btnStartQuiz.setOnClickListener(v -> {
            Logger.d(TAG, "Bouton Start Quiz cliqué");
            onStartQuiz.run();
        });

        btnListenAnswer.setOnClickListener(v -> {
            Logger.d(TAG, "Bouton Listen Answer cliqué");
            onListenAnswer.run();
        });
    }

    /**
     * Initialise l'état par défaut de l'interface
     */
    public void initializeDefaultState() {
        activity.runOnUiThread(() -> {
            setStartQuizEnabled(false);
            setListenAnswerEnabled(false);
            updateStatus("Initialisation en cours...");
            txtQuestion.setText("Prêt pour le quiz ?");
            txtScore.setText("Score: 0/0");
        });
    }
}