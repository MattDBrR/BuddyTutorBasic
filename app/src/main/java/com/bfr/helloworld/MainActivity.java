package com.bfr.helloworld;

import android.os.Bundle;
import android.os.Handler;
import com.bfr.buddy.ui.shared.FacialExpression;
import com.bfr.buddy.utils.events.EventItem;
import com.bfr.buddysdk.BuddyActivity;
import com.bfr.buddysdk.BuddySDK;
import com.bfr.helloworld.buddy.BuddyController;
import com.bfr.helloworld.buddy.BuddySpeechManager;
import com.bfr.helloworld.quiz.AnswerProcessor;
import com.bfr.helloworld.quiz.QuizManager;
import com.bfr.helloworld.quiz.ScoreManager;
import com.bfr.helloworld.ui.UIController;
import com.bfr.helloworld.utils.Logger;
import com.bfr.helloworld.buddy.BuddyHeadTracker;

/**
 * Activité principale - Interface 100% Vocale
 * Interaction uniquement par reconnaissance vocale, pas de boutons
 */
public class MainActivity extends BuddyActivity
        implements QuizManager.QuizCallback, BuddyController.BuddyInitCallback {

    private static final String TAG = "MainActivity";

    //Tracker
    private BuddyHeadTracker headTracker;

    // Contrôleurs principaux
    private UIController uiController;
    private BuddyController buddyController;
    private QuizManager quizManager;
    private Handler handler;

    // État de l'application
    private boolean isSDKReady = false;
    private boolean isQuizStarted = false;
    private boolean isWaitingForQuizConfirmation = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        headTracker = new BuddyHeadTracker();
        setContentView(R.layout.activity_main);

        Logger.i(TAG, "=== DÉMARRAGE QUIZ VOCAL BUDDY ===");

        // CORRECTION : Interface COMPLÈTEMENT transparente
        getWindow().setStatusBarColor(0x00000000);
        getWindow().setNavigationBarColor(0x00000000);

        // Rendre l'activité complètement transparente
        getWindow().getDecorView().setSystemUiVisibility(
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                        android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        android.view.View.SYSTEM_UI_FLAG_FULLSCREEN |
                        android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );

        initializeControllers();
    }

    /**
     * Initialise tous les contrôleurs
     */
    private void initializeControllers() {
        // Contrôleur UI compatible vocal
        uiController = new UIController(this);
        uiController.initializeDefaultState();

        // Contrôleur Buddy
        buddyController = new BuddyController(this);

        // Handler pour les délais
        handler = new Handler();

        Logger.i(TAG, "Contrôleurs initialisés");
    }

    @Override
    public void onSDKReady() {
        Logger.i(TAG, "SDK Buddy prêt - initialisation interface vocale");

        // Initialisation directe de Buddy
        uiController.updateStatus("Initialisation de Buddy...");
        buddyController.initialize(this);
    }

    // ========== CALLBACKS BUDDY ==========

    @Override
    public void onBuddyReady() {
        Logger.i(TAG, "Buddy prêt pour interface vocale !");
        isSDKReady = true;

        // CORRECTION : Activer les moteurs de tête
        buddyController.getMovementManager().enableHeadMotors();

        // Initialiser le gestionnaire de quiz
        quizManager = new QuizManager(uiController, this);

        uiController.updateStatus("✅ Prêt pour quiz vocal !");

        // SÉQUENCE VOCALE DE DÉMARRAGE
        startVocalQuizSequence();
    }

    @Override
    public void onBuddyError(String error) {
        Logger.e(TAG, "Erreur Buddy: " + error);
        uiController.showError("Erreur Buddy: " + error);

        // Réessayer dans 5 secondes
        handler.postDelayed(this::startVocalQuizSequence, 5000);
    }

    // ========== SÉQUENCE VOCALE PRINCIPALE ==========

    /**
     * Démarre la séquence vocale de proposition de quiz
     */
    private void startVocalQuizSequence() {
        Logger.i(TAG, "Démarrage séquence vocale");

        isWaitingForQuizConfirmation = true;

        headTracker.startTracking();

        // Message de bienvenue + proposition de quiz
        buddyController.getSpeechManager().speak(
                "Bonjour ! Je suis ton professeur de maths Buddy ! " +
                        "Es-tu prêt pour un quiz de mathématiques ? " +
                        "Dis 'oui' pour commencer ou 'non' si tu préfères attendre.",
                new BuddySpeechManager.SpeechCallback() {
                    @Override
                    public void onSpeechFinished() {
                        Logger.d(TAG, "Proposition de quiz terminée - activation écoute");

                        // Petit délai puis activation écoute
                        handler.postDelayed(() -> {
                            buddyController.getSpeechManager().speak("Je t'écoute...",
                                    new BuddySpeechManager.SpeechCallback() {
                                        @Override
                                        public void onSpeechFinished() {
                                            activateQuizConfirmationListening();
                                        }
                                    });
                        }, 1000);
                    }
                }
        );
    }

    /**
     * Active l'écoute pour la confirmation de quiz
     */
    private void activateQuizConfirmationListening() {
        Logger.d(TAG, "Activation écoute confirmation quiz");

        // Expression d'écoute
        buddyController.getExpressionManager().showListening(null);

        // Démarrer l'écoute
        buddyController.getSpeechManager().startListening(new BuddySpeechManager.ListeningCallback() {
            @Override
            public void onSpeechRecognized(String utterance, float confidence) {
                Logger.i(TAG, "Confirmation reçue: '" + utterance + "' (confiance: " + confidence + ")");

                runOnUiThread(() -> {
                    processQuizConfirmation(utterance.toLowerCase().trim());
                });
            }

            @Override
            public void onListeningError(String error) {
                Logger.e(TAG, "Erreur écoute confirmation: " + error);

                runOnUiThread(() -> {
                    buddyController.getSpeechManager().speak(
                            "Je n'ai pas bien entendu. Peux-tu répéter ? Dis 'oui' ou 'non'.",
                            new BuddySpeechManager.SpeechCallback() {
                                @Override
                                public void onSpeechFinished() {
                                    // Réessayer l'écoute
                                    handler.postDelayed(() -> activateQuizConfirmationListening(), 1000);
                                }
                            });
                });
            }
        });
    }

    /**
     * Traite la réponse de confirmation du quiz
     */
    private void processQuizConfirmation(String utterance) {
        Logger.d(TAG, "Traitement confirmation: '" + utterance + "'");

        isWaitingForQuizConfirmation = false;

        // Analyse de la réponse
        if (utterance.contains("oui") || utterance.contains("ok") ||
                utterance.contains("d'accord") || utterance.contains("allons-y") ||
                utterance.contains("allez") || utterance.contains("vas-y")) {

            // L'utilisateur accepte le quiz
            Logger.i(TAG, "Quiz accepté par l'utilisateur");

            buddyController.getExpressionManager().showHappiness(() -> {
                buddyController.getSpeechManager().speak(
                        "Super ! Commençons le quiz de mathématiques !",
                        new BuddySpeechManager.SpeechCallback() {
                            @Override
                            public void onSpeechFinished() {
                                // Démarrer le quiz après 1 seconde
                                handler.postDelayed(() -> quizManager.startQuiz(), 1000);
                            }
                        });
            });

        } else if (utterance.contains("non") || utterance.contains("pas") ||
                utterance.contains("attendre") || utterance.contains("plus tard")) {

            // L'utilisateur refuse le quiz
            Logger.i(TAG, "Quiz refusé par l'utilisateur");

            buddyController.getExpressionManager().showNeutral(() -> {
                buddyController.getSpeechManager().speak(
                        "Pas de problème ! Quand tu seras prêt, dis-moi 'je veux faire le quiz' et on commencera !",
                        new BuddySpeechManager.SpeechCallback() {
                            @Override
                            public void onSpeechFinished() {
                                // Attendre une nouvelle demande
                                handler.postDelayed(() -> activateQuizRequestListening(), 2000);
                            }
                        });
            });

        } else {
            // Réponse non comprise
            Logger.w(TAG, "Réponse non comprise: '" + utterance + "'");

            buddyController.getSpeechManager().speak(
                    "Je n'ai pas compris. Dis simplement 'oui' pour commencer le quiz ou 'non' si tu préfères attendre.",
                    new BuddySpeechManager.SpeechCallback() {
                        @Override
                        public void onSpeechFinished() {
                            // Réessayer la confirmation
                            handler.postDelayed(() -> activateQuizConfirmationListening(), 1000);
                        }
                    });
        }
    }

    /**
     * Active l'écoute pour une demande de quiz différée
     */
    private void activateQuizRequestListening() {
        Logger.d(TAG, "Écoute en attente d'une demande de quiz");

        buddyController.getExpressionManager().showNeutral(null);

        buddyController.getSpeechManager().startListening(new BuddySpeechManager.ListeningCallback() {
            @Override
            public void onSpeechRecognized(String utterance, float confidence) {
                String cleanUtterance = utterance.toLowerCase().trim();

                if (cleanUtterance.contains("quiz") || cleanUtterance.contains("commencer") ||
                        cleanUtterance.contains("allons") || cleanUtterance.contains("prêt")) {

                    Logger.i(TAG, "Demande de quiz détectée: '" + utterance + "'");
                    runOnUiThread(() -> {
                        buddyController.getSpeechManager().speak("Parfait ! Commençons !",
                                new BuddySpeechManager.SpeechCallback() {
                                    @Override
                                    public void onSpeechFinished() {
                                        quizManager.startQuiz();
                                    }
                                });
                    });
                } else {
                    // Continuer l'écoute en silence
                    handler.postDelayed(() -> activateQuizRequestListening(), 1000);
                }
            }

            @Override
            public void onListeningError(String error) {
                // Relancer l'écoute en continu
                handler.postDelayed(() -> activateQuizRequestListening(), 2000);
            }
        });
    }

    // ========== CALLBACKS QUIZ ==========

    @Override
    public void onQuizStarted() {
        Logger.i(TAG, "Quiz démarré en mode vocal");
        isQuizStarted = true;

        buddyController.getSpeechManager().speakQuizStart(
                quizManager.getQuestionGenerator().getTotalQuestions(),
                new BuddySpeechManager.SpeechCallback() {
                    @Override
                    public void onSpeechFinished() {
                        Logger.d(TAG, "Introduction quiz terminée - déclenchement première question");
                        // CORRECTION : Délai plus long pour éviter conflit de parole
                        handler.postDelayed(() -> {
                            if (quizManager != null) {
                                Logger.d(TAG, "Tentative de lancement première question");
                                quizManager.askCurrentQuestion();
                            }
                        }, 2000); // 2 secondes au lieu de 1
                    }
                }
        );
    }

    @Override
    public void onQuestionReady(String question, int questionNumber, int totalQuestions) {
        Logger.i(TAG, "Question prête: " + questionNumber + "/" + totalQuestions);

        // CORRECTION : Arrêter toute parole en cours avant de commencer
        buddyController.getSpeechManager().stopSpeaking();

        // Délai pour s'assurer que la parole précédente est arrêtée
        handler.postDelayed(() -> {
            // Expression neutre pour poser la question
            buddyController.getExpressionManager().showNeutral(() -> {

                Logger.d(TAG, "Début prononciation question: " + question);
                buddyController.getSpeechManager().speakQuestion(question, questionNumber,
                        new BuddySpeechManager.SpeechCallback() {
                            @Override
                            public void onSpeechFinished() {
                                Logger.d(TAG, "Question prononcée - activation écoute automatique");

                                // Délai puis "je t'écoute" et activation automatique
                                handler.postDelayed(() -> {
                                    buddyController.getSpeechManager().speak("Je t'écoute...",
                                            new BuddySpeechManager.SpeechCallback() {
                                                @Override
                                                public void onSpeechFinished() {
                                                    activateAnswerListening();
                                                }
                                            });
                                }, 1000);
                            }

                            @Override
                            public void onSpeechError(String error) {
                                Logger.e(TAG, "Erreur prononciation question: " + error);
                                // Réessayer après délai
                                handler.postDelayed(() -> activateAnswerListening(), 2000);
                            }
                        }
                );
            });
        }, 500); // 500ms pour s'assurer que la parole précédente est arrêtée
    }

    /**
     * Active l'écoute pour la réponse à la question
     */
    private void activateAnswerListening() {
        Logger.d(TAG, "Activation écoute réponse");

        // Expression d'écoute
        buddyController.getExpressionManager().showListening(null);

        // Démarrer l'écoute
        buddyController.getSpeechManager().startListening(new BuddySpeechManager.ListeningCallback() {
            @Override
            public void onSpeechRecognized(String utterance, float confidence) {
                Logger.i(TAG, "Réponse reçue: '" + utterance + "' (confiance: " + confidence + ")");

                runOnUiThread(() -> {
                    quizManager.processVocalAnswer(utterance);
                });
            }

            @Override
            public void onListeningError(String error) {
                Logger.e(TAG, "Erreur écoute réponse: " + error);

                runOnUiThread(() -> {
                    // CORRECTION : Gestion appropriée des erreurs d'écoute
                    buddyController.getExpressionManager().showThinking(() -> {
                        buddyController.getSpeechManager().speak(
                                "Je n'ai pas bien entendu ta réponse. Peux-tu répéter plus clairement ?",
                                new BuddySpeechManager.SpeechCallback() {
                                    @Override
                                    public void onSpeechFinished() {
                                        Logger.d(TAG, "Message d'aide prononcé - relance écoute");
                                        // Redire "Je t'écoute" et relancer l'écoute
                                        handler.postDelayed(() -> {
                                            buddyController.getSpeechManager().speak("Je t'écoute...",
                                                    new BuddySpeechManager.SpeechCallback() {
                                                        @Override
                                                        public void onSpeechFinished() {
                                                            activateAnswerListening();
                                                        }
                                                    });
                                        }, 1000);
                                    }
                                });
                    });
                });
            }
        });
    }

// EXTRAIT DE MODIFICATION POUR MainActivity.java
// Remplacer la section onAnswerProcessed

    @Override
    public void onAnswerProcessed(AnswerProcessor.ProcessedAnswer processedAnswer, int correctAnswer) {
        Logger.i(TAG, "Réponse traitée: " + processedAnswer.getResult());

        if (processedAnswer.isCorrect()) {
            headTracker.stopTracking();
            // BONNE RÉPONSE - TIMING OPTIMISÉ
            Logger.d(TAG, "Bonne réponse - séquence optimisée");

            // 1. DÉMARRER L'EXPRESSION DE JOIE
            buddyController.getExpressionManager().performCorrectAnswerSequence();

            // 2. DÉMARRER LE HOCHEMENT EN MÊME TEMPS QUE LA PAROLE
            // CORRECTION: Hochement immédiat, pas après la parole
            handler.postDelayed(() -> {
                Logger.d(TAG, "🎯 Déclenchement hochement PENDANT la parole");
                buddyController.getMovementManager().performSynchronizedYesNod();
            }, 300); // Délai court pour que le hochement accompagne la parole

            // 3. PAROLE DE FÉLICITATION
            buddyController.getSpeechManager().speakCorrectAnswer(correctAnswer,
                    new BuddySpeechManager.SpeechCallback() {
                        @Override
                        public void onSpeechFinished() {
                            Logger.d(TAG, "Feedback positif terminé");

                            // CORRECTION: Retour expression neutre plus naturel
                            handler.postDelayed(() -> {
                                Logger.d(TAG, "Fin célébration - retour neutre");
                                buddyController.getExpressionManager().showNeutral(null);
                            }, 1500); // Délai réduit
                        }

                        @Override
                        public void onSpeechError(String error) {
                            Logger.e(TAG, "Erreur parole positive: " + error);
                            // Hochement de secours même si la parole échoue
                            buddyController.getMovementManager().performYesNod();
                        }
                    }
            );

        } else if (processedAnswer.isValid()) {
            headTracker.stopTracking();
            // MAUVAISE RÉPONSE - Pas de mouvement, juste expression et parole
            Logger.d(TAG, "Mauvaise réponse - expression triste seulement");

            buddyController.getExpressionManager().performIncorrectAnswerSequence();

            buddyController.getSpeechManager().speakIncorrectAnswer(
                    processedAnswer.getExtractedNumber(), correctAnswer,
                    new BuddySpeechManager.SpeechCallback() {
                        @Override
                        public void onSpeechFinished() {
                            Logger.d(TAG, "Feedback négatif terminé");
                            // Retour neutre plus rapide
                            handler.postDelayed(() -> {
                                buddyController.getExpressionManager().showNeutral(null);
                            }, 1000);
                        }
                    }
            );

        } else {
            headTracker.stopTracking();
            // RÉPONSE INVALIDE - Expression de réflexion
            Logger.w(TAG, "Réponse invalide, guidage utilisateur");

            buddyController.getExpressionManager().showThinking(() -> {
                buddyController.getSpeechManager().speak(
                        "Je n'ai pas compris. Peux-tu répéter le nombre plus clairement ?",
                        new BuddySpeechManager.SpeechCallback() {
                            @Override
                            public void onSpeechFinished() {
                                Logger.d(TAG, "Message d'aide prononcé - relance écoute");
                                // Relance immédiate de l'écoute
                                handler.postDelayed(() -> {
                                    buddyController.getSpeechManager().speak("Je t'écoute...",
                                            new BuddySpeechManager.SpeechCallback() {
                                                @Override
                                                public void onSpeechFinished() {
                                                    activateAnswerListening();
                                                }
                                            });
                                }, 500); // Délai réduit
                            }
                        }
                );
            });
        }
        headTracker.startTracking();
    }

    // MODIFICATION POUR onQuizFinished - Score exceptionnel = triple hochement
    @Override
    public void onQuizFinished(ScoreManager scoreManager) {
        Logger.i(TAG, "Quiz terminé - Score: " + scoreManager.getCorrectAnswers() + "/" + scoreManager.getTotalQuestions());
        isQuizStarted = false;

        boolean hasPassingGrade = scoreManager.hasPassingGrade();
        boolean isPerfectScore = (scoreManager.getCorrectAnswers() == scoreManager.getTotalQuestions());

        // Expression de fin
        buddyController.getExpressionManager().performEndQuizSequence(hasPassingGrade);

        // TIMING OPTIMISÉ: Mouvement pendant la parole de fin
        if (hasPassingGrade) {
            if (isPerfectScore) {
                headTracker.stopTracking();
                // SCORE PARFAIT = Triple hochement + danse
                Logger.i(TAG, "🏆 SCORE PARFAIT - Célébration maximale");

                handler.postDelayed(() -> {
                    buddyController.getMovementManager().performTripleYesNod();
                }, 500);

                handler.postDelayed(() -> {
                    buddyController.getMovementManager().performVictoryDance();
                }, 3000);

            } else {
                headTracker.stopTracking();
                // BON SCORE = Hochement simple + danse
                handler.postDelayed(() -> {
                    buddyController.getMovementManager().performYesNod();
                }, 500);

                handler.postDelayed(() -> {
                    buddyController.getMovementManager().performVictoryDance();
                }, 2500);

            }
            headTracker.startTracking();
        }

        // Parole de fin
        buddyController.getSpeechManager().speakFinalScore(
                scoreManager.getCorrectAnswers(),
                scoreManager.getTotalQuestions(),
                hasPassingGrade,
                new BuddySpeechManager.SpeechCallback() {
                    @Override
                    public void onSpeechFinished() {
                        Logger.d(TAG, "Score final annoncé");

                        // Proposer nouveau quiz après délai
                        handler.postDelayed(() -> {
                            buddyController.getSpeechManager().speak(
                                    "Veux-tu faire un autre quiz ? Dis 'oui' ou 'non'.",
                                    new BuddySpeechManager.SpeechCallback() {
                                        @Override
                                        public void onSpeechFinished() {
                                            activateQuizConfirmationListening();
                                        }
                                    });
                        }, 2000); // Délai raisonnable
                    }
                }
        );
    }

    @Override
    public void onQuizError(String error) {
        Logger.e(TAG, "Erreur quiz: " + error);

        buddyController.getSpeechManager().speak(
                "Il y a eu un problème. Veux-tu réessayer ?",
                new BuddySpeechManager.SpeechCallback() {
                    @Override
                    public void onSpeechFinished() {
                        activateQuizConfirmationListening();
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

            // Relancer l'écoute si nécessaire
            if (!isQuizStarted && !isWaitingForQuizConfirmation) {
                handler.postDelayed(() -> activateQuizRequestListening(), 1000);
            }
        }
    }

    @Override
    protected void onDestroy() {
        Logger.i(TAG, "=== FERMETURE QUIZ VOCAL BUDDY ===");

        // Nettoyer les ressources
        if (buddyController != null) {
            buddyController.cleanup();
        }

        super.onDestroy();
    }
}