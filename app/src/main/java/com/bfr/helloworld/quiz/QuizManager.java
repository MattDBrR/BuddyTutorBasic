package com.bfr.helloworld.quiz;

import android.os.Handler;
import com.bfr.helloworld.ui.UICallback;
import com.bfr.helloworld.utils.Logger;

/**
 * Gestionnaire principal du quiz de mathématiques
 */
public class QuizManager {
    private static final String TAG = "QuizManager";
    private static final int DEFAULT_TOTAL_QUESTIONS = 5;

    public enum QuizState {
        NOT_STARTED,
        RUNNING,
        WAITING_FOR_ANSWER,
        PROCESSING_ANSWER,
        FINISHED
    }

    public interface QuizCallback {
        void onQuizStarted();
        void onQuestionReady(String question, int questionNumber, int totalQuestions);
        void onAnswerProcessed(AnswerProcessor.ProcessedAnswer processedAnswer, int correctAnswer);
        void onQuizFinished(ScoreManager scoreManager);
        void onQuizError(String error);
    }

    private final UICallback uiCallback;
    private final QuizCallback quizCallback;
    private final QuestionGenerator questionGenerator;
    private final ScoreManager scoreManager;
    private final Handler handler;

    private QuizState currentState;
    private int currentQuestionIndex;

    public QuizManager(UICallback uiCallback, QuizCallback quizCallback) {
        this(uiCallback, quizCallback, DEFAULT_TOTAL_QUESTIONS);
    }

    public QuizManager(UICallback uiCallback, QuizCallback quizCallback, int totalQuestions) {
        this.uiCallback = uiCallback;
        this.quizCallback = quizCallback;
        this.questionGenerator = new QuestionGenerator(totalQuestions);
        this.scoreManager = new ScoreManager(totalQuestions);
        this.handler = new Handler();
        this.currentState = QuizState.NOT_STARTED;
        this.currentQuestionIndex = 0;

        Logger.i(TAG, "QuizManager initialisé avec " + totalQuestions + " questions");
    }

    /**
     * Démarre le quiz
     */
    public void startQuiz() {
        if (currentState == QuizState.RUNNING) {
            Logger.w(TAG, "Quiz déjà en cours");
            return;
        }

        Logger.i(TAG, "Démarrage du quiz");
        currentState = QuizState.RUNNING;
        currentQuestionIndex = 0;
        scoreManager.reset();

        uiCallback.setStartQuizEnabled(false);
        uiCallback.setListenAnswerEnabled(false);
        uiCallback.updateStatus("Démarrage du quiz...");

        // Notifier le callback du démarrage
        quizCallback.onQuizStarted();

        // Lancer la première question après un délai
        handler.postDelayed(this::askCurrentQuestion, 2000);
    }

    /**
     * Pose la question actuelle
     */
    public void askCurrentQuestion() {
        if (currentState != QuizState.RUNNING) {
            Logger.w(TAG, "Tentative de poser une question alors que le quiz n'est pas en cours");
            return;
        }

        if (scoreManager.isQuizComplete()) {
            finishQuiz();
            return;
        }

        String question = questionGenerator.getQuestion(currentQuestionIndex);
        int questionNumber = currentQuestionIndex + 1;
        int totalQuestions = questionGenerator.getTotalQuestions();

        Logger.d(TAG, "Pose de la question " + questionNumber + "/" + totalQuestions + ": " + question);

        currentState = QuizState.WAITING_FOR_ANSWER;

        // Mettre à jour l'UI
        uiCallback.updateQuestion(question, questionNumber, totalQuestions);
        uiCallback.updateScore(scoreManager.getCorrectAnswers(), scoreManager.getCurrentQuestion());
        uiCallback.setListenAnswerEnabled(false);
        uiCallback.updateStatus("Écoute la question...");

        // Notifier le callback pour la parole
        quizCallback.onQuestionReady(question, questionNumber, totalQuestions);
    }

    /**
     * Active l'écoute de la réponse (appelé après que la question a été prononcée)
     */
    public void enableAnswerListening() {
        if (currentState != QuizState.WAITING_FOR_ANSWER) {
            Logger.w(TAG, "Tentative d'activation de l'écoute dans un état incorrect: " + currentState);
            return;
        }

        Logger.d(TAG, "Activation de l'écoute des réponses");

        handler.postDelayed(() -> {
            uiCallback.setListenAnswerEnabled(true);
            uiCallback.updateStatus("Maintenant, appuie sur 'Écouter' et dis ta réponse !");
        }, 1000);
    }

    /**
     * Démarre l'écoute de la réponse
     */
    public void startListening() {
        if (currentState != QuizState.WAITING_FOR_ANSWER) {
            Logger.w(TAG, "Tentative d'écoute dans un état incorrect: " + currentState);
            return;
        }

        Logger.d(TAG, "Début de l'écoute");
        uiCallback.setListenAnswerEnabled(false);
        uiCallback.updateStatus("🎤 J'écoute ta réponse...");
    }

    /**
     * Traite une réponse vocale reçue
     */
    public void processVocalAnswer(String utterance) {
        if (currentState != QuizState.WAITING_FOR_ANSWER) {
            Logger.w(TAG, "Réponse reçue dans un état incorrect: " + currentState);
            return;
        }

        currentState = QuizState.PROCESSING_ANSWER;

        int expectedAnswer = questionGenerator.getAnswer(currentQuestionIndex);
        AnswerProcessor.ProcessedAnswer processedAnswer = AnswerProcessor.processAnswer(utterance, expectedAnswer);

        Logger.i(TAG, "Réponse traitée: " + processedAnswer.getResult() +
                " (extrait: " + processedAnswer.getExtractedNumber() +
                ", attendu: " + expectedAnswer + ")");

        // Mettre à jour l'UI selon le résultat
        if (processedAnswer.isValid()) {
            if (processedAnswer.isCorrect()) {
                scoreManager.addCorrectAnswer();
                uiCallback.updateStatus("✅ Bonne réponse !");
            } else {
                uiCallback.updateStatus("❌ Pas tout à fait...");
            }

            // Passer à la question suivante
            scoreManager.nextQuestion();
            currentQuestionIndex++;

            // Notifier le callback pour la parole et les mouvements
            quizCallback.onAnswerProcessed(processedAnswer, expectedAnswer);

            // Programmer la question suivante
            handler.postDelayed(() -> {
                currentState = QuizState.RUNNING;
                askCurrentQuestion();
            }, 3000);

        } else {
            // Réponse invalide, permettre de réessayer
            currentState = QuizState.WAITING_FOR_ANSWER;
            uiCallback.updateStatus("Pas compris: '" + utterance + "'. Réessaie !");
            uiCallback.setListenAnswerEnabled(true);

            // Donner un feedback pour améliorer
            String suggestion = AnswerProcessor.generateImprovementSuggestion(utterance);
            Logger.d(TAG, "Suggestion d'amélioration: " + suggestion);
        }
    }

    /**
     * Termine le quiz
     */
    public void finishQuiz() {
        Logger.i(TAG, "Fin du quiz");
        currentState = QuizState.FINISHED;

        scoreManager.logFinalStats();

        // Mettre à jour l'UI
        uiCallback.updateQuestion("Quiz terminé !", 0, 0);
        uiCallback.updateScore(scoreManager.getCorrectAnswers(), scoreManager.getTotalQuestions());
        uiCallback.setStartQuizEnabled(true);
        uiCallback.setListenAnswerEnabled(false);

        String finalMessage = scoreManager.hasPassingGrade() ?
                "🎉 Félicitations ! Tu as la moyenne !" :
                "💪 Continue tes efforts !";
        uiCallback.updateStatus(finalMessage);

        // Notifier le callback
        quizCallback.onQuizFinished(scoreManager);
    }

    /**
     * Régénère les questions pour un nouveau quiz
     */
    public void regenerateQuestions() {
        if (currentState == QuizState.RUNNING || currentState == QuizState.WAITING_FOR_ANSWER) {
            Logger.w(TAG, "Impossible de régénérer les questions pendant un quiz");
            return;
        }

        questionGenerator.regenerateQuestions();
        currentState = QuizState.NOT_STARTED;
        currentQuestionIndex = 0;

        Logger.i(TAG, "Questions régénérées pour un nouveau quiz");
    }

    /**
     * Gère les erreurs du quiz
     */
    public void handleError(String error) {
        Logger.e(TAG, "Erreur dans le quiz: " + error);
        currentState = QuizState.NOT_STARTED;

        uiCallback.showError(error);
        uiCallback.setStartQuizEnabled(true);
        uiCallback.setListenAnswerEnabled(false);

        quizCallback.onQuizError(error);
    }

    // Getters
    public QuizState getCurrentState() {
        return currentState;
    }

    public ScoreManager getScoreManager() {
        return scoreManager;
    }

    public QuestionGenerator getQuestionGenerator() {
        return questionGenerator;
    }

    public int getCurrentQuestionIndex() {
        return currentQuestionIndex;
    }
}