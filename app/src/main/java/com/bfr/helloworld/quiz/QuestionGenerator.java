package com.bfr.helloworld.quiz;

import com.bfr.helloworld.utils.Logger;
import java.util.Random;

/**
 * Générateur de questions de mathématiques
 */
public class QuestionGenerator {
    private static final String TAG = "QuestionGenerator";

    private final String[] questions;
    private final int[] answers;
    private final Random random;

    public QuestionGenerator(int totalQuestions) {
        this.questions = new String[totalQuestions];
        this.answers = new int[totalQuestions];
        this.random = new Random();

        generateQuestions();
        Logger.i(TAG, "Générateur initialisé avec " + totalQuestions + " questions");
    }

    /**
     * Génère toutes les questions du quiz
     */
    private void generateQuestions() {
        for (int i = 0; i < questions.length; i++) {
            generateSingleQuestion(i);
        }
        Logger.i(TAG, "Toutes les questions générées");
    }

    /**
     * Génère une seule question
     */
    private void generateSingleQuestion(int index) {
        int a = random.nextInt(10) + 1; // 1-10
        int b = random.nextInt(10) + 1; // 1-10
        int operation = random.nextInt(2); // 0 = addition, 1 = soustraction

        if (operation == 0) {
            // Addition
            questions[index] = "Combien font " + a + " plus " + b + " ?";
            answers[index] = a + b;
        } else {
            // Soustraction (s'assurer que le résultat est positif)
            if (a < b) {
                int temp = a;
                a = b;
                b = temp;
            }
            questions[index] = "Combien font " + a + " moins " + b + " ?";
            answers[index] = a - b;
        }

        Logger.d(TAG, "Question " + (index + 1) + " générée: " + questions[index] + " (réponse: " + answers[index] + ")");
    }

    /**
     * Régénère toutes les questions
     */
    public void regenerateQuestions() {
        generateQuestions();
        Logger.i(TAG, "Questions régénérées");
    }

    /**
     * Récupère une question par index
     */
    public String getQuestion(int index) {
        if (index >= 0 && index < questions.length) {
            return questions[index];
        }
        Logger.w(TAG, "Index de question invalide: " + index);
        return "";
    }

    /**
     * Récupère la réponse d'une question par index
     */
    public int getAnswer(int index) {
        if (index >= 0 && index < answers.length) {
            return answers[index];
        }
        Logger.w(TAG, "Index de réponse invalide: " + index);
        return -1;
    }

    /**
     * Récupère le nombre total de questions
     */
    public int getTotalQuestions() {
        return questions.length;
    }
}