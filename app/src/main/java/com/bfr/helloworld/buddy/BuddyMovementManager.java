package com.bfr.helloworld.buddy;

import android.os.Handler;
import android.os.RemoteException;
import com.bfr.buddy.usb.shared.IUsbCommadRsp;
import com.bfr.buddysdk.BuddySDK;
import com.bfr.helloworld.utils.Logger;

/**
 * Gestionnaire des mouvements de Buddy (tête, roues)
 */
public class BuddyMovementManager {
    private static final String TAG = "BuddyMovementManager";

    private final Handler handler;
    private boolean isMoving = false;

    public BuddyMovementManager() {
        this.handler = new Handler();
        Logger.i(TAG, "BuddyMovementManager initialisé");
    }

    /**
     * Fait hocher la tête pour dire "OUI"
     */
    public void performYesNod() {
        if (isMoving) {
            Logger.w(TAG, "Mouvement déjà en cours");
            return;
        }

        Logger.i(TAG, "Début hochement de tête 'OUI'");
        isMoving = true;

        try {
            // Premier mouvement : tête vers le bas
            BuddySDK.USB.buddySayYes(30.0f, 25.0f, new IUsbCommadRsp.Stub() {
                @Override
                public void onSuccess(String s) throws RemoteException {
                    if (s.equals("YES_MOVE_FINISHED")) {
                        Logger.d(TAG, "Premier hochement terminé, retour position");

                        // Retour à la position neutre
                        handler.post(() -> {
                            try {
                                BuddySDK.USB.buddySayYes(40.0f, -25.0f, new IUsbCommadRsp.Stub() {
                                    @Override
                                    public void onSuccess(String s2) throws RemoteException {
                                        if (s2.equals("YES_MOVE_FINISHED")) {
                                            Logger.i(TAG, "Hochement de tête 'OUI' terminé");
                                            isMoving = false;
                                        }
                                    }

                                    @Override
                                    public void onFailed(String s2) throws RemoteException {
                                        Logger.e(TAG, "Échec retour position: " + s2);
                                        isMoving = false;
                                    }
                                });
                            } catch (Exception e) {
                                Logger.e(TAG, "Erreur retour position", e);
                                isMoving = false;
                            }
                        });
                    }
                }

                @Override
                public void onFailed(String s) throws RemoteException {
                    Logger.e(TAG, "Échec hochement de tête: " + s);
                    isMoving = false;
                }
            });
        } catch (Exception e) {
            Logger.e(TAG, "Erreur hochement de tête", e);
            isMoving = false;
        }
    }

    /**
     * Effectue un demi-tour
     */
    public void performHalfTurn() {
        if (isMoving) {
            Logger.w(TAG, "Mouvement déjà en cours");
            return;
        }

        Logger.i(TAG, "Début demi-tour");
        isMoving = true;

        try {
            BuddySDK.USB.rotateBuddy(45.0f, 180.0f, new IUsbCommadRsp.Stub() {
                @Override
                public void onSuccess(String s) throws RemoteException {
                    Logger.i(TAG, "Demi-tour terminé");
                    isMoving = false;
                }

                @Override
                public void onFailed(String s) throws RemoteException {
                    Logger.e(TAG, "Échec demi-tour: " + s);
                    isMoving = false;
                }
            });
        } catch (Exception e) {
            Logger.e(TAG, "Erreur demi-tour", e);
            isMoving = false;
        }
    }

    /**
     * Effectue une danse de victoire
     */
    public void performVictoryDance() {
        if (isMoving) {
            Logger.w(TAG, "Mouvement déjà en cours");
            return;
        }

        Logger.i(TAG, "Début danse de victoire");
        isMoving = true;

        try {
            // Première rotation
            BuddySDK.USB.rotateBuddy(60.0f, 90.0f, new IUsbCommadRsp.Stub() {
                @Override
                public void onSuccess(String s) throws RemoteException {
                    // Rotation dans l'autre sens
                    handler.postDelayed(() -> performSecondDanceMove(), 1000);
                }

                @Override
                public void onFailed(String s) throws RemoteException {
                    Logger.e(TAG, "Échec danse 1: " + s);
                    isMoving = false;
                }
            });
        } catch (Exception e) {
            Logger.e(TAG, "Erreur danse de victoire", e);
            isMoving = false;
        }
    }

    /**
     * Deuxième mouvement de la danse
     */
    private void performSecondDanceMove() {
        try {
            BuddySDK.USB.rotateBuddy(-60.0f, 90.0f, new IUsbCommadRsp.Stub() {
                @Override
                public void onSuccess(String s) throws RemoteException {
                    // Troisième rotation
                    handler.postDelayed(() -> performThirdDanceMove(), 1000);
                }

                @Override
                public void onFailed(String s) throws RemoteException {
                    Logger.e(TAG, "Échec danse 2: " + s);
                    isMoving = false;
                }
            });
        } catch (Exception e) {
            Logger.e(TAG, "Erreur danse 2", e);
            isMoving = false;
        }
    }

    /**
     * Troisième mouvement de la danse
     */
    private void performThirdDanceMove() {
        try {
            BuddySDK.USB.rotateBuddy(60.0f, 90.0f, new IUsbCommadRsp.Stub() {
                @Override
                public void onSuccess(String s) throws RemoteException {
                    Logger.i(TAG, "Danse de victoire terminée !");
                    isMoving = false;
                }

                @Override
                public void onFailed(String s) throws RemoteException {
                    Logger.e(TAG, "Échec danse 3: " + s);
                    isMoving = false;
                }
            });
        } catch (Exception e) {
            Logger.e(TAG, "Erreur danse 3", e);
            isMoving = false;
        }
    }

    /**
     * Effectue un mouvement avant simple
     */
    public void moveForward(float distance) {
        if (isMoving) {
            Logger.w(TAG, "Mouvement déjà en cours");
            return;
        }

        Logger.d(TAG, "Mouvement avant: " + distance + "m");
        isMoving = true;

        try {
            BuddySDK.USB.moveBuddy(0.2f, distance, new IUsbCommadRsp.Stub() {
                @Override
                public void onSuccess(String s) throws RemoteException {
                    if (s.equals("WHEEL_MOVE_FINISHED")) {
                        Logger.d(TAG, "Mouvement avant terminé");
                        isMoving = false;
                    }
                }

                @Override
                public void onFailed(String s) throws RemoteException {
                    Logger.e(TAG, "Échec mouvement avant: " + s);
                    isMoving = false;
                }
            });
        } catch (Exception e) {
            Logger.e(TAG, "Erreur mouvement avant", e);
            isMoving = false;
        }
    }

    /**
     * Arrête d'urgence tous les mouvements
     */
    public void emergencyStop() {
        Logger.w(TAG, "Arrêt d'urgence des mouvements");

        try {
            BuddySDK.USB.emergencyStopMotors(new IUsbCommadRsp.Stub() {
                @Override
                public void onSuccess(String s) throws RemoteException {
                    Logger.i(TAG, "Arrêt d'urgence réussi");
                    isMoving = false;
                }

                @Override
                public void onFailed(String s) throws RemoteException {
                    Logger.e(TAG, "Échec arrêt d'urgence: " + s);
                    isMoving = false;
                }
            });
        } catch (Exception e) {
            Logger.e(TAG, "Erreur arrêt d'urgence", e);
            isMoving = false;
        }
    }

    /**
     * Vérifie si Buddy est en train de bouger
     */
    public boolean isMoving() {
        return isMoving;
    }

    /**
     * Effectue une séquence de mouvements de célébration
     */
    public void performCelebrationSequence() {
        Logger.i(TAG, "Début séquence de célébration");

        // Hochement de tête suivi d'un demi-tour
        performYesNod();

        handler.postDelayed(() -> {
            if (!isMoving) {
                performHalfTurn();
            }
        }, 3000);
    }
}