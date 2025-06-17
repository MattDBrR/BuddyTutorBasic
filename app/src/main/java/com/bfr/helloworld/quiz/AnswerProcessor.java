package com.bfr.helloworld.quiz;

import com.bfr.helloworld.utils.Logger;
import com.bfr.helloworld.utils.PhoneticCorrector;

/**
 * Processeur pour analyser et valider les réponses vocales
 */
public class AnswerProcessor {
    private static final String TAG = "AnswerProcessor";

    public enum AnswerResult {
        CORRECT,
        INCORRECT,
        INVALID_INPUT,
        PARSING_ERROR
    }

    public static class ProcessedAnswer {
        private final AnswerResult result;
        private final int extractedNumber;
        private final String originalText;
        private final String feedbackMessage;

        public ProcessedAnswer(AnswerResult result, int extractedNumber, String originalText, String feedbackMessage) {
            this.result = result;
            this.extractedNumber = extractedNumber;
            this.originalText = originalText;
            this.feedbackMessage = feedbackMessage;
        }

        // Getters
        public AnswerResult getResult() { return result; }
        public int getExtractedNumber() { return extractedNumber; }
        public String getOriginalText() { return originalText; }
        public String getFeedbackMessage() { return feedbackMessage; }

        public boolean isCorrect() { return result == AnswerResult.CORRECT; }
        public boolean isValid() { return result != AnswerResult.INVALID_INPUT && result != AnswerResult.PARSING_ERROR; }
    }

    /**
     * Traite une réponse vocale et vérifie sa correction
     */
    public static ProcessedAnswer processAnswer(String utterance, int expectedAnswer) {
        Logger.d(TAG, "Traitement de la réponse: '" + utterance + "' (attendu: " + expectedAnswer + ")");

        if (utterance == null || utterance.trim().isEmpty()) {
            Logger.w(TAG, "Réponse vide ou nulle");
            return new ProcessedAnswer(
                    AnswerResult.INVALID_INPUT,
                    -1,
                    utterance,
                    "Je n'ai pas entendu de réponse. Réessaie s'il te plaît."
            );
        }

        // Extraction du nombre avec correction phonétique
        int extractedNumber = PhoneticCorrector.extractNumberWithPhoneticCorrection(utterance);

        if (extractedNumber == -1) {
            Logger.w(TAG, "Impossible d'extraire un nombre de: '" + utterance + "'");
            return new ProcessedAnswer(
                    AnswerResult.PARSING_ERROR,
                    -1,
                    utterance,
                    "Je n'ai pas compris le nombre. Dis clairement : un, deux, trois..."
            );
        }

        Logger.i(TAG, "Nombre extrait: " + extractedNumber + " vs attendu: " + expectedAnswer);

        // Vérification de la correction
        if (extractedNumber == expectedAnswer) {
            return new ProcessedAnswer(
                    AnswerResult.CORRECT,
                    extractedNumber,
                    utterance,
                    generateCorrectFeedback(extractedNumber)
            );
        } else {
            return new ProcessedAnswer(
                    AnswerResult.INCORRECT,
                    extractedNumber,
                    utterance,
                    generateIncorrectFeedback(extractedNumber, expectedAnswer)
            );
        }
    }

    /**
     * Génère un message de félicitation pour une réponse correcte
     */
    private static String generateCorrectFeedback(int answer) {
        String[] positiveMessages = {
                "Bravo ! C'est exact !",
                "Parfait ! Tu as trouvé !",
                "Excellent ! C'est la bonne réponse !",
                "Super ! Tu es très fort !",
                "Fantastique ! C'est correct !"
        };

        int randomIndex = (int) (Math.random() * positiveMessages.length);
        return positiveMessages[randomIndex] + " " + answer + " est bien la bonne réponse !";
    }

    /**
     * Génère un message d'encouragement pour une réponse incorrecte
     */
    private static String generateIncorrectFeedback(int userAnswer, int correctAnswer) {
        String[] encouragementMessages = {
                "Pas tout à fait ! Tu as dit " + userAnswer + ", mais la bonne réponse était " + correctAnswer + ". Continue, tu vas y arriver !",
                "Presque ! Tu as répondu " + userAnswer + ", mais c'était " + correctAnswer + ". Ne t'inquiète pas, c'est en pratiquant qu'on apprend !",
                "Oops ! Tu as dit " + userAnswer + " mais la réponse était " + correctAnswer + ". Tu fais de ton mieux, c'est l'important !",
                "Pas grave ! Tu as répondu " + userAnswer + ", la bonne réponse était " + correctAnswer + ". Continue tes efforts !"
        };

        int randomIndex = (int) (Math.random() * encouragementMessages.length);
        return encouragementMessages[randomIndex];
    }

    /**
     * Valide si un nombre est dans la plage attendue pour le quiz
     */
    public static boolean isNumberInValidRange(int number) {
        // Pour notre quiz de maths simples, les réponses sont entre 0 et 20
        return number >= 0 && number <= 20;
    }

    /**
     * Analyse la confiance dans la reconnaissance vocale
     */
    public static boolean isConfidenceAcceptable(float confidence) {
        // Seuil de confiance acceptable pour la reconnaissance vocale
        return confidence >= 0.5f;
    }

    /**
     * Génère des suggestions d'amélioration pour l'utilisateur
     */
    public static String generateImprovementSuggestion(String originalUtterance) {
        if (originalUtterance == null || originalUtterance.trim().isEmpty()) {
            return "Parle plus fort et plus clairement.";
        }

        String cleanText = originalUtterance.toLowerCase().trim();

        // Suggestions basées sur les erreurs communes
        if (cleanText.length() < 3) {
            return "Essaie de dire le nombre plus distinctement : 'trois', 'cinq', 'sept'...";
        }

        if (cleanText.contains("euh") || cleanText.contains("hum")) {
            return "N'hésite pas ! Dis directement le nombre que tu penses.";
        }

        return "Parle clairement en disant juste le nombre : un, deux, trois...";
    }
}