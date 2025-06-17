package com.bfr.helloworld.buddy;

import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import com.bfr.buddy.usb.shared.IUsbCommadRsp;
import com.bfr.buddy.vision.shared.Tracking;
import com.bfr.buddysdk.BuddySDK;
import com.bfr.helloworld.utils.Logger;

/**
 * Gestionnaire du suivi de tête automatique de Buddy
 * Suit automatiquement les personnes détectées par la vision
 */
public class BuddyHeadTracker {
    private static final String TAG = "BuddyHeadTracker";

    // Configuration du suivi
    private static final long TRACKING_REFRESH_INTERVAL = 100; // 100ms = 10 FPS
    private static final float HEAD_MOVEMENT_THRESHOLD = 0.10f; // Seuil de déclenchement (15% de l'écran)
    private static final float HEAD_SPEED = 40.0f; // Vitesse des mouvements
    private static final float HEAD_ANGLE_STEP = 10.0f; // Angle par étape
    private static final long MOVEMENT_COOLDOWN = 300; // Délai entre mouvements (ms)

    // État du tracker
    private boolean isTrackingActive = false;
    private boolean areMotorsEnabled = false;
    private Thread trackingThread;
    private final Handler handler;
    private long lastMovementTime = 0;

    // Callback pour les événements
    public interface HeadTrackerCallback {
        void onTrackingStarted();
        void onTrackingStopped();
        void onPersonDetected(float centerX, float centerY);
        void onPersonLost();
        void onHeadMovement(String direction, float angle);
        void onError(String error);
    }

    private HeadTrackerCallback callback;

    public BuddyHeadTracker() {
        this.handler = new Handler(Looper.getMainLooper());
        Logger.i(TAG, "BuddyHeadTracker initialisé");
    }

    /**
     * Définit le callback pour les événements
     */
    public void setCallback(HeadTrackerCallback callback) {
        this.callback = callback;
    }

    /**
     * Démarre le suivi de tête
     */
    public void startTracking() {
        if (isTrackingActive) {
            Logger.w(TAG, "Suivi déjà actif");
            return;
        }

        Logger.i(TAG, "🎯 Démarrage du suivi de tête");

        // Activer les moteurs d'abord
        enableHeadMotors(() -> {
            // Une fois les moteurs activés, démarrer le suivi
            startTrackingLoop();
        });
    }

    /**
     * Arrête le suivi de tête
     */
    public void stopTracking() {
        if (!isTrackingActive) {
            Logger.d(TAG, "Suivi déjà arrêté");
            return;
        }

        Logger.i(TAG, "🛑 Arrêt du suivi de tête");
        isTrackingActive = false;

        // Arrêter le thread
        if (trackingThread != null && trackingThread.isAlive()) {
            trackingThread.interrupt();
            try {
                trackingThread.join(2000); // Attendre maximum 2 secondes
                Logger.d(TAG, "Thread de suivi arrêté");
            } catch (InterruptedException e) {
                Logger.w(TAG, "Interruption lors de l'arrêt du thread");
                Thread.currentThread().interrupt();
            }
        }

        // Callback
        if (callback != null) {
            handler.post(() -> callback.onTrackingStopped());
        }
    }

    /**
     * Active les moteurs de tête
     */
    private void enableHeadMotors(Runnable onComplete) {
        Logger.d(TAG, "Activation des moteurs de tête...");

        try {
            // Activer le moteur "Yes" (haut/bas)
            BuddySDK.USB.enableYesMove(true, new IUsbCommadRsp.Stub() {
                @Override
                public void onSuccess(String success) throws RemoteException {
                    Logger.d(TAG, "✅ Moteur YES activé");

                    // Activer le moteur "No" (gauche/droite)
                    try {
                        BuddySDK.USB.enableNoMove(true, new IUsbCommadRsp.Stub() {
                            @Override
                            public void onSuccess(String success) throws RemoteException {
                                Logger.d(TAG, "✅ Moteur NO activé");
                                areMotorsEnabled = true;

                                // Callback et démarrage
                                if (onComplete != null) {
                                    onComplete.run();
                                }
                            }

                            @Override
                            public void onFailed(String error) throws RemoteException {
                                Logger.e(TAG, "❌ Échec activation moteur NO: " + error);
                                notifyError("Échec activation moteur NO: " + error);
                            }
                        });
                    } catch (Exception e) {
                        Logger.e(TAG, "Exception activation moteur NO", e);
                        notifyError("Exception moteur NO: " + e.getMessage());
                    }
                }

                @Override
                public void onFailed(String error) throws RemoteException {
                    Logger.e(TAG, "❌ Échec activation moteur YES: " + error);
                    notifyError("Échec activation moteur YES: " + error);
                }
            });
        } catch (Exception e) {
            Logger.e(TAG, "Exception activation moteurs", e);
            notifyError("Exception activation moteurs: " + e.getMessage());
        }
    }

    /**
     * Démarre la boucle de suivi
     */
    private void startTrackingLoop() {
        if (!areMotorsEnabled) {
            Logger.e(TAG, "Impossible de démarrer - moteurs non activés");
            notifyError("Moteurs non activés");
            return;
        }

        Logger.i(TAG, "🎯 Démarrage du tracking de vision...");

        try {
            // ÉTAPE CRITIQUE : Démarrer le tracking de vision d'abord
            BuddySDK.Vision.startTracking();
            Logger.d(TAG, "✅ Vision tracking démarré");

            // Délai pour laisser le temps au tracking de s'initialiser
            Thread.sleep(500);

        } catch (Exception e) {
            Logger.e(TAG, "❌ Échec démarrage vision tracking", e);
            notifyError("Impossible de démarrer le tracking de vision: " + e.getMessage());
            return;
        }

        isTrackingActive = true;
        lastMovementTime = 0;

        // Callback
        if (callback != null) {
            handler.post(() -> callback.onTrackingStarted());
        }

        trackingThread = new Thread(() -> {
            Logger.i(TAG, "🔄 Thread de suivi démarré");

            while (isTrackingActive && !Thread.currentThread().isInterrupted()) {
                try {
                    // Obtenir les données de tracking
                    Tracking trackingData = BuddySDK.Vision.getTracking();

                    if (!trackingData.isTrackingSuccessfull()) {
                        // Personne perdue
                        handlePersonLost();
                        Thread.sleep(TRACKING_REFRESH_INTERVAL);
                        continue;
                    }

                    // Calculer la position du centre du visage
                    float centerX = (trackingData.getLeftPos() + trackingData.getRightPos()) / 2.0f;
                    float centerY = (trackingData.getTopPos() + trackingData.getBottomPos()) / 2.0f;

                    // Calculer les écarts par rapport au centre de l'écran
                    float deltaX = centerX - 0.5f; // -0.5 à 0.5
                    float deltaY = centerY - 0.5f; // -0.5 à 0.5

                    Logger.d(TAG, String.format("Position: X=%.2f, Y=%.2f, ΔX=%.2f, ΔY=%.2f",
                            centerX, centerY, deltaX, deltaY));

                    // Notifier la détection
                    if (callback != null) {
                        handler.post(() -> callback.onPersonDetected(centerX, centerY));
                    }

                    // Vérifier et effectuer les mouvements
                    checkAndMoveHead(deltaX, deltaY);

                    // Attendre avant la prochaine itération
                    Thread.sleep(TRACKING_REFRESH_INTERVAL);

                } catch (InterruptedException e) {
                    Logger.i(TAG, "Thread de suivi interrompu");
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    Logger.e(TAG, "Erreur dans la boucle de suivi", e);
                    notifyError("Erreur de suivi: " + e.getMessage());

                    try {
                        Thread.sleep(TRACKING_REFRESH_INTERVAL);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }

            Logger.i(TAG, "🏁 Thread de suivi terminé");
        });

        trackingThread.start();
    }

    /**
     * Gère la perte de la personne
     */
    private void handlePersonLost() {
        if (callback != null) {
            handler.post(() -> callback.onPersonLost());
        }
    }

    /**
     * Vérifie les seuils et effectue les mouvements appropriés
     */
    private void checkAndMoveHead(float deltaX, float deltaY) {
        long currentTime = System.currentTimeMillis();

        // Vérifier le cooldown pour éviter les mouvements trop fréquents
        if (currentTime - lastMovementTime < MOVEMENT_COOLDOWN) {
            return;
        }

        try {
            boolean hasMoved = false;

            // Mouvement horizontal (gauche/droite) - PRIORITÉ
            if (Math.abs(deltaX) > HEAD_MOVEMENT_THRESHOLD) {
                if (deltaX > 0) {
                    // Personne à droite
                    Logger.d(TAG, "➡️ Mouvement tête DROITE");
                    moveHeadNo(HEAD_SPEED, HEAD_ANGLE_STEP);
                    notifyMovement("DROITE", HEAD_ANGLE_STEP);
                } else {
                    // Personne à gauche
                    Logger.d(TAG, "⬅️ Mouvement tête GAUCHE");
                    moveHeadNo(HEAD_SPEED, -HEAD_ANGLE_STEP);
                    notifyMovement("GAUCHE", -HEAD_ANGLE_STEP);
                }
                hasMoved = true;
            }
            // Mouvement vertical seulement si pas de mouvement horizontal
            else if (Math.abs(deltaY) > HEAD_MOVEMENT_THRESHOLD) {
                if (deltaY > 0) {
                    // Personne en bas
                    Logger.d(TAG, "⬇️ Mouvement tête BAS");
                    moveHeadYes(HEAD_SPEED, -HEAD_ANGLE_STEP);
                    notifyMovement("BAS", -HEAD_ANGLE_STEP);
                } else {
                    // Personne en haut
                    Logger.d(TAG, "⬆️ Mouvement tête HAUT");
                    moveHeadYes(HEAD_SPEED, HEAD_ANGLE_STEP);
                    notifyMovement("HAUT", HEAD_ANGLE_STEP);
                }
                hasMoved = true;
            }

            // Mettre à jour le timestamp si mouvement effectué
            if (hasMoved) {
                lastMovementTime = currentTime;
            }

        } catch (Exception e) {
            Logger.e(TAG, "Erreur lors du mouvement de tête", e);
            notifyError("Erreur mouvement: " + e.getMessage());
        }
    }

    /**
     * Effectue un mouvement horizontal (No movement)
     */
    private void moveHeadNo(float speed, float angle) {
        try {
            BuddySDK.USB.buddySayNo(speed, angle, new IUsbCommadRsp.Stub() {
                @Override
                public void onSuccess(String success) throws RemoteException {
                    if ("NO_MOVE_FINISHED".equals(success)) {
                        Logger.d(TAG, "Mouvement horizontal terminé");
                    }
                }

                @Override
                public void onFailed(String error) throws RemoteException {
                    Logger.e(TAG, "Échec mouvement horizontal: " + error);
                }
            });
        } catch (Exception e) {
            Logger.e(TAG, "Exception mouvement horizontal", e);
        }
    }

    /**
     * Effectue un mouvement vertical (Yes movement)
     */
    private void moveHeadYes(float speed, float angle) {
        try {
            BuddySDK.USB.buddySayYes(speed, angle, new IUsbCommadRsp.Stub() {
                @Override
                public void onSuccess(String success) throws RemoteException {
                    if ("YES_MOVE_FINISHED".equals(success)) {
                        Logger.d(TAG, "Mouvement vertical terminé");
                    }
                }

                @Override
                public void onFailed(String error) throws RemoteException {
                    Logger.e(TAG, "Échec mouvement vertical: " + error);
                }
            });
        } catch (Exception e) {
            Logger.e(TAG, "Exception mouvement vertical", e);
        }
    }

    /**
     * Centre la tête (position neutre)
     */
    public void centerHead() {
        if (!areMotorsEnabled) {
            Logger.w(TAG, "Impossible de centrer - moteurs non activés");
            return;
        }

        Logger.i(TAG, "🎯 Centrage de la tête");

        try {
            // Retour position neutre horizontale
            BuddySDK.USB.buddySayNo(HEAD_SPEED, 0, new IUsbCommadRsp.Stub() {
                @Override
                public void onSuccess(String success) throws RemoteException {
                    // Retour position neutre verticale
                    try {
                        BuddySDK.USB.buddySayYes(HEAD_SPEED, 0, new IUsbCommadRsp.Stub() {
                            @Override
                            public void onSuccess(String success) throws RemoteException {
                                Logger.i(TAG, "✅ Tête centrée");
                            }

                            @Override
                            public void onFailed(String error) throws RemoteException {
                                Logger.e(TAG, "Échec centrage vertical: " + error);
                            }
                        });
                    } catch (Exception e) {
                        Logger.e(TAG, "Exception centrage vertical", e);
                    }
                }

                @Override
                public void onFailed(String error) throws RemoteException {
                    Logger.e(TAG, "Échec centrage horizontal: " + error);
                }
            });
        } catch (Exception e) {
            Logger.e(TAG, "Exception centrage tête", e);
        }
    }

    /**
     * UTILITAIRES ET GETTERS
     */
    public boolean isTrackingActive() {
        return isTrackingActive;
    }

    public boolean areMotorsEnabled() {
        return areMotorsEnabled;
    }

    /**
     * Configure les paramètres de suivi
     */
    public void setTrackingParameters(float threshold, float speed, float angleStep) {
        // Note: Ces paramètres pourraient être rendus configurables
        // Pour l'instant, ils sont constants
        Logger.i(TAG, "Configuration: seuil=" + threshold + ", vitesse=" + speed + ", angle=" + angleStep);
    }

    /**
     * CALLBACKS HELPERS
     */
    private void notifyError(String error) {
        if (callback != null) {
            handler.post(() -> callback.onError(error));
        }
    }

    private void notifyMovement(String direction, float angle) {
        if (callback != null) {
            handler.post(() -> callback.onHeadMovement(direction, angle));
        }
    }

    /**
     * Nettoyage des ressources
     */
    public void cleanup() {
        Logger.i(TAG, "🧹 Nettoyage BuddyHeadTracker");
        stopTracking();

        // Centrer la tête avant arrêt
        if (areMotorsEnabled) {
            centerHead();
        }
    }
}
