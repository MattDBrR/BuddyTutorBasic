package com.bfr.helloworld.ui;

/**
 * Interface pour les callbacks de l'interface utilisateur
 */
public interface UICallback {

    /**
     * Met à jour le statut affiché
     */
    void updateStatus(String message);

    /**
     * Met à jour la question affichée
     */
    void updateQuestion(String question, int questionNumber, int totalQuestions);

    /**
     * Met à jour le score affiché
     */
    void updateScore(int correctAnswers, int currentQuestion);

    /**
     * Active/désactive le bouton de démarrage du quiz
     */
    void setStartQuizEnabled(boolean enabled);

    /**
     * Active/désactive le bouton d'écoute
     */
    void setListenAnswerEnabled(boolean enabled);

    /**
     * Affiche un message d'erreur
     */
    void showError(String error);

    /**
     * Affiche un message de succès
     */
    void showSuccess(String message);
}