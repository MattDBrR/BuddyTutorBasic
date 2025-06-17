package com.bfr.helloworld;

import android.os.Bundle;
import com.bfr.buddy.ui.shared.FacialExpression;
import com.bfr.buddy.utils.events.EventItem;
import com.bfr.buddysdk.BuddyActivity;
import com.bfr.helloworld.buddy.BuddyController;
import com.bfr.helloworld.buddy.BuddySpeechManager;
import com.bfr.helloworld.quiz.AnswerProcessor;
import com.bfr.helloworld.quiz.QuizManager;
import com.bfr.helloworld.quiz.ScoreManager;
import com.bfr.helloworld.ui.UIController;
import com.bfr.helloworld.utils.Logger;

/**
 * Activité principale - Version modulaire avec FreeSpeech
 */
public class MainActivity extends BuddyActivity
        implements QuizManager.QuizCallback, BuddyController.BuddyInitCallback {

    private static final String TAG = "MainActivity";

    // Contrôleurs principaux
    private UIController uiController;
    private BuddyController buddyController;
    private QuizManager quizManager;

    // État de l'application
    private boolean isSDKReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Logger.i(TAG, "=== DÉMARRAGE QUIZ DE MATHS BUDDY ===");

        initializeControllers();
        setupUI();
    }

    /**
     * Initialise tous les contrôleurs
     */
    private void initializeControllers() {
        // Contrôleur UI
        uiController = new UIController(this);
        uiController.initializeDefaultState();

        // Contrôleur Buddy
        buddyController = new BuddyController(this);

        Logger.i(TAG, "Contrôleurs initialisés");
    }

    /**
     * Configure l'interface utilisateur
     */
    private void setupUI() {
        uiController.setButtonListeners(
                this::onStartQuizClicked,
                this::onListenAnswerClicked
        );

        Logger.i(TAG, "Interface utilisateur configurée");
    }

    @Override
    public void onSDKReady() {
        Logger.i(TAG, "SDK Buddy prêt - initialisation directe avec FreeSpeech");

        // Initialisation directe de Buddy - plus besoin de permissions
        uiController.updateStatus("Initialisation de Buddy...");
        buddyController.initialize(this);
    }

    // ========== CALLBACKS BUDDY ==========

    @Override
    public void onBuddyReady() {
        Logger.i(TAG, "Buddy prêt avec FreeSpeech !");
        isSDKReady = true;

        // Initialiser le gestionnaire de quiz
        quizManager = new QuizManager(uiController, this);

        // Mettre à jour l'UI
        uiController.setStartQuizEnabled(true);
        uiController.updateStatus("✅ Prêt ! Appuie sur 'Commencer le Quiz'");

        // Message de bienvenue - CORRECTION ICI
        buddyController.getSpeechManager().speakWelcome(new BuddySpeechManager.SpeechCallback() {
            @Override
            public void onSpeechFinished() {
                Logger.d(TAG, "Message de bienvenue terminé");
            }
        });
    }

    @Override
    public void onBuddyError(String error) {
        Logger.e(TAG, "Erreur Buddy: " + error);
        uiController.showError("Erreur Buddy: " + error);
    }

    // ========== CALLBACKS QUIZ ==========

    @Override
    public void onQuizStarted() {
        Logger.i(TAG, "Quiz démarré - notification reçue");

        // Faire parler Buddy pour introduire le quiz - CORRECTION ICI
        buddyController.getSpeechManager().speakQuizStart(
                quizManager.getQuestionGenerator().getTotalQuestions(),
                new BuddySpeechManager.SpeechCallback() {
                    @Override
                    public void onSpeechFinished() {
                        Logger.d(TAG, "Introduction terminée");
                        // Le QuizManager s'occupera de poser la première question
                    }
                }
        );
    }

    @Override
    public void onQuestionReady(String question, int questionNumber, int totalQuestions) {
        Logger.i(TAG, "Question prête: " + questionNumber + "/" + totalQuestions);

        // Expression d'écoute
        buddyController.getExpressionManager().showListening(null);

        // Faire parler Buddy - CORRECTION ICI
        buddyController.getSpeechManager().speakQuestion(question, questionNumber,
                new BuddySpeechManager.SpeechCallback() {
                    @Override
                    public void onSpeechFinished() {
                        Logger.d(TAG, "Question prononcée, activation écoute");
                        // Activer l'écoute après que Buddy ait fini de parler
                        quizManager.enableAnswerListening();
                    }
                }
        );
    }

    @Override
    public void onAnswerProcessed(AnswerProcessor.ProcessedAnswer processedAnswer, int correctAnswer) {
        Logger.i(TAG, "Réponse traitée: " + processedAnswer.getResult());

        if (processedAnswer.isCorrect()) {
            // Bonne réponse
            buddyController.getExpressionManager().performCorrectAnswerSequence();

            // CORRECTION ICI
            buddyController.getSpeechManager().speakCorrectAnswer(correctAnswer,
                    new BuddySpeechManager.SpeechCallback() {
                        @Override
                        public void onSpeechFinished() {
                            // Hochement de tête après la parole
                            buddyController.getMovementManager().performYesNod();
                        }
                    }
            );

        } else {
            // Mauvaise réponse
            buddyController.getExpressionManager().performIncorrectAnswerSequence();

            // CORRECTION ICI
            buddyController.getSpeechManager().speakIncorrectAnswer(
                    processedAnswer.getExtractedNumber(),
                    correctAnswer,
                    new BuddySpeechManager.SpeechCallback() {
                        @Override
                        public void onSpeechFinished() {
                            Logger.d(TAG, "Explication terminée");
                        }
                    }
            );
        }
    }

    @Override
    public void onQuizFinished(ScoreManager scoreManager) {
        Logger.i(TAG, "Quiz terminé - score final: " + scoreManager.getScoreString());

        boolean hasPassingGrade = scoreManager.hasPassingGrade();

        // Expression selon le résultat
        buddyController.getExpressionManager().performEndQuizSequence(hasPassingGrade);

        // Parole de fin - CORRECTION ICI
        buddyController.getSpeechManager().speakFinalScore(
                scoreManager.getCorrectAnswers(),
                scoreManager.getTotalQuestions(),
                hasPassingGrade,
                new BuddySpeechManager.SpeechCallback() {
                    @Override
                    public void onSpeechFinished() {
                        // Mouvement de célébration si réussite
                        if (hasPassingGrade) {
                            buddyController.getMovementManager().performVictoryDance();
                        }

                        // Proposer un nouveau quiz après 10 secondes
                        android.os.Handler handler = new android.os.Handler();
                        handler.postDelayed(() -> {
                            buddyController.speak("Veux-tu refaire un quiz ?");
                            quizManager.regenerateQuestions();
                        }, 10000);
                    }
                }
        );
    }

    @Override
    public void onQuizError(String error) {
        Logger.e(TAG, "Erreur quiz: " + error);
        uiController.showError("Erreur quiz: " + error);

        // CORRECTION ICI
        buddyController.getSpeechManager().speakTechnicalError(new BuddySpeechManager.SpeechCallback() {
            @Override
            public void onSpeechFinished() {
                Logger.d(TAG, "Message d'erreur technique prononcé");
            }
        });
    }

    // ========== ÉVÉNEMENTS UI ==========

    /**
     * Appelé quand le bouton "Commencer Quiz" est cliqué
     */
    private void onStartQuizClicked() {
        if (!isSDKReady) {
            Logger.w(TAG, "SDK pas prêt, impossible de démarrer le quiz");
            uiController.showError("Buddy n'est pas encore prêt");
            return;
        }

        if (quizManager == null) {
            Logger.e(TAG, "QuizManager non initialisé");
            uiController.showError("Erreur interne - redémarrer l'app");
            return;
        }

        Logger.i(TAG, "Bouton Start Quiz cliqué");
        buddyController.getExpressionManager().setExpression(FacialExpression.HAPPY);
        quizManager.startQuiz();
    }

    /**
     * Appelé quand le bouton "Écouter" est cliqué
     */
    private void onListenAnswerClicked() {
        if (!isSDKReady) {
            Logger.w(TAG, "SDK pas prêt, impossible d'écouter");
            uiController.showError("Buddy n'est pas encore prêt");
            return;
        }

        if (quizManager == null) {
            Logger.e(TAG, "QuizManager non initialisé");
            uiController.showError("Erreur interne - redémarrer l'app");
            return;
        }

        Logger.i(TAG, "Bouton Listen Answer cliqué - FreeSpeech activé");

        // Expression d'écoute
        buddyController.getExpressionManager().showListening(null);

        // Démarrer l'écoute via le quiz manager
        quizManager.startListening();

        // Démarrer l'écoute vocale avec FreeSpeech - CORRECTION ICI
        buddyController.getSpeechManager().startListening(new BuddySpeechManager.ListeningCallback() {
            @Override
            public void onSpeechRecognized(String utterance, float confidence) {
                Logger.i(TAG, "FreeSpeech - Parole reconnue: '" + utterance + "' (confiance: " + confidence + ")");

                runOnUiThread(() -> {
                    uiController.updateStatus("FreeSpeech: '" + utterance + "'");

                    // Traiter la réponse via le quiz manager
                    quizManager.processVocalAnswer(utterance);
                });
            }

            @Override
            public void onListeningError(String error) {
                Logger.e(TAG, "Erreur FreeSpeech: " + error);

                runOnUiThread(() -> {
                    uiController.showError("Erreur FreeSpeech: " + error);
                    uiController.setListenAnswerEnabled(true);

                    buddyController.getSpeechManager().speakParsingError(new BuddySpeechManager.SpeechCallback() {
                        @Override
                        public void onSpeechFinished() {
                            Logger.d(TAG, "Message d'erreur prononcé");
                        }
                    });
                });
            }
        });
    }

    // ========== GESTION DU CYCLE DE VIE ==========

    @Override
    public void onEvent(EventItem iEvent) {
        super.onEvent(iEvent);
        Logger.d(TAG, "Événement reçu: " + iEvent.getType());
    }

    @Override
    protected void onPause() {
        super.onPause();
        Logger.d(TAG, "Application en pause");

        // Arrêter les activités en cours
        if (buddyController != null) {
            buddyController.getSpeechManager().stopListening();
            buddyController.getSpeechManager().stopSpeaking();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Logger.d(TAG, "Application reprise");

        // Remettre l'expression neutre si nécessaire
        if (isSDKReady && buddyController != null) {
            buddyController.getExpressionManager().showNeutral(null);
        }
    }

    @Override
    protected void onDestroy() {
        Logger.i(TAG, "=== FERMETURE QUIZ DE MATHS BUDDY ===");

        // Nettoyer les ressources
        if (buddyController != null) {
            buddyController.cleanup();
        }

        super.onDestroy();
    }
}