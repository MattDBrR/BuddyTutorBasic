package com.bfr.helloworld.buddy;

import android.app.Activity;
import android.os.RemoteException;
import com.bfr.buddy.ui.shared.FacialExpression;
import com.bfr.buddy.usb.shared.IUsbCommadRsp;
import com.bfr.buddysdk.BuddySDK;
import com.bfr.helloworld.utils.Logger;

/**
 * Contrôleur principal pour l'interaction avec le robot Buddy
 */
public class BuddyController {
    private static final String TAG = "BuddyController";

    public interface BuddyInitCallback {
        void onBuddyReady();
        void onBuddyError(String error);
    }

    private final Activity activity;
    private final BuddyMovementManager movementManager;
    private final BuddyExpressionManager expressionManager;
    private final BuddySpeechManager speechManager;

    private boolean isInitialized = false;

    public BuddyController(Activity activity) {
        this.activity = activity;
        this.movementManager = new BuddyMovementManager();
        this.expressionManager = new BuddyExpressionManager();
        this.speechManager = new BuddySpeechManager();

        Logger.i(TAG, "BuddyController créé");
    }

    /**
     * Initialise Buddy (à appeler dans onSDKReady)
     */
    public void initialize(BuddyInitCallback callback) {
        if (isInitialized) {
            Logger.w(TAG, "Buddy déjà initialisé");
            callback.onBuddyReady();
            return;
        }

        Logger.i(TAG, "Initialisation de Buddy...");

        try {
            // 1. Configuration de l'interface transparente
            setupTransparentInterface();

            // 2. Configuration de l'expression par défaut
            setupDefaultExpression();

            // 3. Activation des roues
            enableWheels(callback);

        } catch (Exception e) {
            Logger.e(TAG, "Erreur lors de l'initialisation de Buddy", e);
            callback.onBuddyError("Erreur d'initialisation: " + e.getMessage());
        }
    }

    /**
     * Configure l'interface transparente
     */
    private void setupTransparentInterface() {
        try {
            BuddySDK.UI.setViewAsFace(activity.findViewById(com.bfr.helloworld.R.id.view_face));
            Logger.d(TAG, "Interface transparente configurée");
        } catch (Exception e) {
            Logger.e(TAG, "Erreur configuration interface transparente", e);
            throw new RuntimeException("Impossible de configurer l'interface transparente", e);
        }
    }

    /**
     * Configure l'expression faciale par défaut
     */
    private void setupDefaultExpression() {
        try {
            BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL);
            Logger.d(TAG, "Expression par défaut configurée");
        } catch (Exception e) {
            Logger.w(TAG, "Impossible de configurer l'expression par défaut");
            // Non critique, continuer l'initialisation
        }
    }

    /**
     * Active les roues de Buddy
     */
    private void enableWheels(BuddyInitCallback callback) {
        try {
            BuddySDK.USB.enableWheels(1, 1, new IUsbCommadRsp.Stub() {
                @Override
                public void onSuccess(String s) throws RemoteException {
                    Logger.i(TAG, "Roues activées avec succès");
                    finishInitialization(callback);
                }

                @Override
                public void onFailed(String s) throws RemoteException {
                    Logger.e(TAG, "Échec activation roues: " + s);
                    // Non critique pour le quiz, continuer
                    finishInitialization(callback);
                }
            });
        } catch (Exception e) {
            Logger.e(TAG, "Erreur activation roues", e);
            // Non critique, continuer
            finishInitialization(callback);
        }
    }

    /**
     * Finalise l'initialisation
     */
    private void finishInitialization(BuddyInitCallback callback) {
        isInitialized = true;
        Logger.i(TAG, "Buddy initialisé avec succès - FreeSpeech prêt");

        activity.runOnUiThread(() -> callback.onBuddyReady());
    }

    /**
     * Vérifie si Buddy est prêt
     */
    public boolean isReady() {
        return isInitialized;
    }

    /**
     * Fait parler Buddy avec une expression
     */
    public void speakWithExpression(String message, FacialExpression expression) {
        if (!isInitialized) {
            Logger.w(TAG, "Tentative de parole avant initialisation");
            return;
        }

        expressionManager.setExpression(expression);
        speechManager.speak(message, () -> {
            // Retour à l'expression neutre après la parole
            expressionManager.setExpression(FacialExpression.NEUTRAL);
        });
    }

    /**
     * Fait parler Buddy (expression neutre)
     */
    public void speak(String message) {
        speakWithExpression(message, FacialExpression.NEUTRAL);
    }

    // Getters pour les managers
    public BuddyMovementManager getMovementManager() {
        return movementManager;
    }

    public BuddyExpressionManager getExpressionManager() {
        return expressionManager;
    }

    public BuddySpeechManager getSpeechManager() {
        return speechManager;
    }

    /**
     * Nettoyage des ressources
     */
    public void cleanup() {
        speechManager.cleanup();
        Logger.i(TAG, "BuddyController nettoyé");
    }
}