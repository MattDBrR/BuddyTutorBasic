package com.bfr.helloworld.buddy;

import android.os.RemoteException;
import com.bfr.buddy.speech.shared.ISTTCallback;
import com.bfr.buddy.speech.shared.ITTSCallback;
import com.bfr.buddy.speech.shared.STTResult;
import com.bfr.buddy.speech.shared.STTResultsData;
import com.bfr.buddysdk.BuddySDK;
import com.bfr.buddysdk.services.speech.STTTask;
import com.bfr.helloworld.utils.Logger;
import java.util.Locale;

/**
 * Gestionnaire de la parole et reconnaissance vocale de Buddy
 */
public class BuddySpeechManager {
    private static final String TAG = "BuddySpeechManager";

    // Interface avec méthodes par défaut pour éviter l'erreur
    public interface SpeechCallback {
        default void onSpeechFinished() {}
        default void onSpeechError(String error) {}
    }

    public interface ListeningCallback {
        void onSpeechRecognized(String utterance, float confidence);
        void onListeningError(String error);
    }

    private STTTask currentSTTTask;
    private boolean isListening = false;
    private boolean isSpeaking = false;

    public BuddySpeechManager() {
        Logger.i(TAG, "BuddySpeechManager initialisé");
    }

    /**
     * Fait parler Buddy
     */
    public void speak(String message) {
        speak(message, (SpeechCallback) null);
    }

    /**
     * Fait parler Buddy avec callback
     */
    public void speak(String message, SpeechCallback callback) {
        if (isSpeaking) {
            Logger.w(TAG, "Parole déjà en cours");
            return;
        }

        if (message == null || message.trim().isEmpty()) {
            Logger.w(TAG, "Message vide, ignoré");
            if (callback != null) {
                callback.onSpeechError("Message vide");
            }
            return;
        }

        Logger.d(TAG, "Début parole: '" + message + "'");
        isSpeaking = true;

        try {
            BuddySDK.Speech.startSpeaking(message, new ITTSCallback.Stub() {
                @Override
                public void onSuccess(String s) throws RemoteException {
                    Logger.d(TAG, "Parole terminée avec succès");
                    isSpeaking = false;
                    if (callback != null) {
                        callback.onSpeechFinished();
                    }
                }

                @Override
                public void onError(String error) throws RemoteException {
                    Logger.e(TAG, "Erreur TTS: " + error);
                    isSpeaking = false;
                    if (callback != null) {
                        callback.onSpeechError(error);
                    }
                }

                @Override
                public void onPause() throws RemoteException {
                    Logger.d(TAG, "Parole en pause");
                }

                @Override
                public void onResume() throws RemoteException {
                    Logger.d(TAG, "Parole reprise");
                }
            });
        } catch (Exception e) {
            Logger.e(TAG, "Exception lors de la parole", e);
            isSpeaking = false;
            if (callback != null) {
                callback.onSpeechError("Exception TTS: " + e.getMessage());
            }
        }
    }

    /**
     * Fait parler Buddy avec Runnable simple
     */
    public void speak(String message, Runnable onFinished) {
        speak(message, new SpeechCallback() {
            @Override
            public void onSpeechFinished() {
                if (onFinished != null) {
                    onFinished.run();
                }
            }

            @Override
            public void onSpeechError(String error) {
                Logger.e(TAG, "Erreur parole: " + error);
                if (onFinished != null) {
                    onFinished.run();
                }
            }
        });
    }

    /**
     * Démarre l'écoute avec Cerence FreeSpeech
     */
    public void startListening(ListeningCallback callback) {
        if (isListening) {
            Logger.w(TAG, "Écoute déjà en cours");
            return;
        }

        Logger.i(TAG, "Début écoute Cerence FreeSpeech");
        isListening = true;

        try {
            currentSTTTask = BuddySDK.Speech.createCerenceFreeSpeechTask(Locale.FRENCH);

            currentSTTTask.start(false, new ISTTCallback.Stub() {
                @Override
                public void onSuccess(STTResultsData sttResultsData) throws RemoteException {
                    Logger.i(TAG, "STT Success - traitement des résultats");
                    isListening = false;

                    if (!sttResultsData.getResults().isEmpty()) {
                        STTResult bestResult = sttResultsData.getResults().get(0);
                        String utterance = bestResult.getUtterance();
                        float confidence = bestResult.getConfidence();

                        Logger.i(TAG, "Reconnu: '" + utterance + "' (confiance: " + confidence + ")");

                        if (callback != null) {
                            callback.onSpeechRecognized(utterance, confidence);
                        }
                    } else {
                        Logger.w(TAG, "STT Success mais aucun résultat");
                        if (callback != null) {
                            callback.onListeningError("Aucune parole reconnue");
                        }
                    }
                }

                @Override
                public void onError(String error) throws RemoteException {
                    Logger.e(TAG, "Erreur STT: " + error);
                    isListening = false;
                    if (callback != null) {
                        callback.onListeningError(error);
                    }
                }
            });

        } catch (Exception e) {
            Logger.e(TAG, "Exception lors de la création STT", e);
            isListening = false;
            if (callback != null) {
                callback.onListeningError("Impossible de créer STT: " + e.getMessage());
            }
        }
    }

    /**
     * Arrête l'écoute en cours
     */
    public void stopListening() {
        if (!isListening) {
            Logger.d(TAG, "Aucune écoute en cours");
            return;
        }

        Logger.d(TAG, "Arrêt de l'écoute");

        if (currentSTTTask != null) {
            try {
                currentSTTTask.stop();
            } catch (Exception e) {
                Logger.e(TAG, "Erreur arrêt STT", e);
            }
            currentSTTTask = null;
        }

        isListening = false;
    }

    /**
     * Arrête la parole en cours
     */
    public void stopSpeaking() {
        if (!isSpeaking) {
            Logger.d(TAG, "Aucune parole en cours");
            return;
        }

        Logger.d(TAG, "Arrêt de la parole");

        try {
            BuddySDK.Speech.stopSpeaking();
            isSpeaking = false;
        } catch (Exception e) {
            Logger.e(TAG, "Erreur arrêt parole", e);
        }
    }

    /**
     * Vérifie si Buddy est en train de parler
     */
    public boolean isSpeaking() {
        return isSpeaking;
    }

    /**
     * Vérifie si Buddy est en train d'écouter
     */
    public boolean isListening() {
        return isListening;
    }

    /**
     * Messages prédéfinis pour le quiz
     */

    public void speakWelcome(SpeechCallback callback) {
        speak("Bonjour ! Je suis ton professeur de maths Buddy ! Prêt pour un quiz amusant ?", callback);
    }

    public void speakQuizStart(int totalQuestions, SpeechCallback callback) {
        speak("Super ! Commençons le quiz ! Tu vas avoir " + totalQuestions + " questions.", callback);
    }

    public void speakQuestion(String question, int questionNumber, SpeechCallback callback) {
        speak("Question numéro " + questionNumber + ". " + question, callback);
    }

    public void speakCorrectAnswer(int answer, SpeechCallback callback) {
        speak("Bravo ! C'est exact ! " + answer + " est la bonne réponse !", callback);
    }

    public void speakIncorrectAnswer(int userAnswer, int correctAnswer, SpeechCallback callback) {
        speak("Pas tout à fait ! Tu as dit " + userAnswer + ", mais la bonne réponse était " +
                correctAnswer + ". Mais ne t'inquiète pas, tu fais de ton mieux !", callback);
    }

    public void speakFinalScore(int correct, int total, boolean hasPassingGrade, SpeechCallback callback) {
        String message;
        if (hasPassingGrade) {
            message = "Fantastique ! Tu as " + correct + " bonnes réponses sur " + total +
                    " ! Tu as plus de la moyenne ! Je suis très fier de toi !";
        } else {
            message = "Tu as " + correct + " bonnes réponses sur " + total +
                    ". Ce n'est pas grave ! Avec de l'entraînement, tu vas progresser. Je crois en toi !";
        }
        speak(message, callback);
    }

    public void speakEncouragement(SpeechCallback callback) {
        speak("Appuie sur le bouton écouter et dis-moi ta réponse.", callback);
    }

    public void speakParsingError(SpeechCallback callback) {
        speak("Je n'ai pas compris le nombre. Dis clairement le chiffre : un, deux, trois...", callback);
    }

    public void speakTechnicalError(SpeechCallback callback) {
        speak("Problème technique avec la reconnaissance vocale. Réessaie de parler plus clairement.", callback);
    }

    /**
     * Nettoyage des ressources
     */
    public void cleanup() {
        stopListening();
        stopSpeaking();
        Logger.i(TAG, "BuddySpeechManager nettoyé");
    }
}