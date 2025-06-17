package com.bfr.helloworld.quiz;

import com.bfr.helloworld.utils.Logger;

/**
 * Gestionnaire du score du quiz
 */
public class ScoreManager {
    private static final String TAG = "ScoreManager";

    private int correctAnswers;
    private int currentQuestion;
    private final int totalQuestions;

    public ScoreManager(int totalQuestions) {
        this.totalQuestions = totalQuestions;
        reset();
        Logger.i(TAG, "ScoreManager initialisé pour " + totalQuestions + " questions");
    }

    /**
     * Remet à zéro le score
     */
    public void reset() {
        correctAnswers = 0;
        currentQuestion = 0;
        Logger.i(TAG, "Score remis à zéro");
    }

    /**
     * Ajoute une réponse correcte
     */
    public void addCorrectAnswer() {
        correctAnswers++;
        Logger.d(TAG, "Réponse correcte ajoutée. Score: " + correctAnswers + "/" + getCurrentQuestionNumber());
    }

    /**
     * Passe à la question suivante
     */
    public void nextQuestion() {
        currentQuestion++;
        Logger.d(TAG, "Question suivante: " + getCurrentQuestionNumber() + "/" + totalQuestions);
    }

    /**
     * Vérifie si le quiz est terminé
     */
    public boolean isQuizComplete() {
        return currentQuestion >= totalQuestions;
    }

    /**
     * Calcule le pourcentage de réussite
     */
    public double getPercentage() {
        if (currentQuestion == 0) return 0.0;
        return (double) correctAnswers / currentQuestion * 100;
    }

    /**
     * Détermine si l'étudiant a la moyenne
     */
    public boolean hasPassingGrade() {
        return getPercentage() >= 50.0;
    }

    /**
     * Génère un message de fin basé sur le score
     */
    public String getFinalMessage() {
        double percentage = getPercentage();

        if (percentage >= 80) {
            return "Excellent ! Tu maîtrises parfaitement !";
        } else if (percentage >= 60) {
            return "Très bien ! Tu as une bonne compréhension !";
        } else if (percentage >= 50) {
            return "Bien ! Tu as la moyenne, continue comme ça !";
        } else {
            return "Continue tes efforts, tu vas y arriver !";
        }
    }

    // Getters
    public int getCorrectAnswers() {
        return correctAnswers;
    }

    public int getCurrentQuestion() {
        return currentQuestion;
    }

    public int getCurrentQuestionNumber() {
        return currentQuestion + 1;
    }

    public int getTotalQuestions() {
        return totalQuestions;
    }

    /**
     * Génère une chaîne résumant le score
     */
    public String getScoreString() {
        return correctAnswers + "/" + currentQuestion + " (" + Math.round(getPercentage()) + "%)";
    }

    /**
     * Affiche les statistiques finales
     */
    public void logFinalStats() {
        Logger.i(TAG, "=== STATISTIQUES FINALES ===");
        Logger.i(TAG, "Réponses correctes: " + correctAnswers + "/" + totalQuestions);
        Logger.i(TAG, "Pourcentage: " + Math.round(getPercentage()) + "%");
        Logger.i(TAG, "Moyenne atteinte: " + (hasPassingGrade() ? "OUI" : "NON"));
        Logger.i(TAG, "Message: " + getFinalMessage());
    }
}