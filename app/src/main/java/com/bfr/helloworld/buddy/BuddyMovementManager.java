package com.bfr.helloworld.buddy;

import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import com.bfr.buddy.usb.shared.IUsbCommadRsp;
import com.bfr.buddysdk.BuddySDK;
import com.bfr.helloworld.utils.Logger;

/**
 * Gestionnaire des mouvements de Buddy (tête, roues) - VERSION OPTIMISÉE
 */
public class BuddyMovementManager {
    private static final String TAG = "BuddyMovementManager";

    private final Handler handler;
    private boolean isMoving = false;
    private boolean headMotorsEnabled = false;

    public BuddyMovementManager() {
        this.handler = new Handler(Looper.getMainLooper());
        Logger.i(TAG, "BuddyMovementManager initialisé");
    }

    /**
     * Active les moteurs de tête (à faire une seule fois au démarrage)
     */
    public void enableHeadMotors() {
        if (headMotorsEnabled) {
            Logger.d(TAG, "Moteurs de tête déjà activés");
            return;
        }

        Logger.i(TAG, "Activation des moteurs de tête...");

        try {
            // Activer le moteur YES (hochement)
            BuddySDK.USB.enableYesMove(true, new IUsbCommadRsp.Stub() {
                @Override
                public void onSuccess(String s) throws RemoteException {
                    Logger.i(TAG, "✅ Moteur YES activé avec succès");

                    // Activer aussi le moteur NO
                    try {
                        BuddySDK.USB.enableNoMove(1, new IUsbCommadRsp.Stub() {
                            @Override
                            public void onSuccess(String s) throws RemoteException {
                                Logger.i(TAG, "✅ Moteur NO activé avec succès");
                                headMotorsEnabled = true;
                            }

                            @Override
                            public void onFailed(String s) throws RemoteException {
                                Logger.e(TAG, "❌ Échec activation moteur NO: " + s);
                            }
                        });
                    } catch (Exception e) {
                        Logger.e(TAG, "Exception activation moteur NO", e);
                    }
                }

                @Override
                public void onFailed(String s) throws RemoteException {
                    Logger.e(TAG, "❌ Échec activation moteur YES: " + s);
                }
            });
        } catch (Exception e) {
            Logger.e(TAG, "Erreur activation moteurs de tête", e);
        }
    }

    /**
     * Hochement de tête naturel et rapide - OPTIMISÉ POUR LA PAROLE
     * À déclencher PENDANT que Buddy parle, pas après
     */
    public void performYesNod() {
        Logger.i(TAG, "🎯 HOCHEMENT OUI - Démarrage immédiat");

        if (isMoving) {
            Logger.w(TAG, "⚠️ Mouvement déjà en cours, abandon");
            return;
        }

        // Vérifier que les moteurs sont activés
        if (!headMotorsEnabled) {
            Logger.w(TAG, "⚠️ Moteurs non activés, activation et réessai");
            enableHeadMotors();
            // Réessayer après délai court
            handler.postDelayed(this::performYesNod, 800);
            return;
        }

        isMoving = true;

        try {
            Logger.d(TAG, "→ Mouvement 1: Descente rapide (50°/s, 20°)");

            // MOUVEMENT 1: Descente rapide et naturelle
            BuddySDK.USB.buddySayYes(50.0f, 20.0f, new IUsbCommadRsp.Stub() {
                @Override
                public void onSuccess(String s) throws RemoteException {
                    Logger.d(TAG, "✓ Descente terminée: " + s);

                    if (s.equals("YES_MOVE_FINISHED")) {
                        // DÉLAI COURT pour mouvement naturel
                        handler.postDelayed(() -> {
                            try {
                                Logger.d(TAG, "→ Mouvement 2: Remontée rapide (60°/s, -20°)");

                                // MOUVEMENT 2: Remontée plus rapide
                                BuddySDK.USB.buddySayYes(60.0f, -20.0f, new IUsbCommadRsp.Stub() {
                                    @Override
                                    public void onSuccess(String s2) throws RemoteException {
                                        Logger.d(TAG, "✓ Remontée terminée: " + s2);

                                        if (s2.equals("YES_MOVE_FINISHED")) {
                                            Logger.i(TAG, "🎉 HOCHEMENT OUI TERMINÉ - Durée ~1.5s");
                                            isMoving = false;
                                        }
                                    }

                                    @Override
                                    public void onFailed(String s2) throws RemoteException {
                                        Logger.e(TAG, "❌ Échec remontée: " + s2);
                                        isMoving = false;
                                    }
                                });
                            } catch (Exception e) {
                                Logger.e(TAG, "Exception remontée", e);
                                isMoving = false;
                            }
                        }, 200); // DÉLAI RÉDUIT: 200ms au lieu de 500ms
                    }
                }

                @Override
                public void onFailed(String s) throws RemoteException {
                    Logger.e(TAG, "❌ Échec descente: " + s);
                    isMoving = false;
                }
            });
        } catch (Exception e) {
            Logger.e(TAG, "❌ Exception hochement", e);
            isMoving = false;
        }
    }

    /**
     * Hochement de tête SYNCHRONISÉ - démarrage en même temps que la parole
     */
    public void performSynchronizedYesNod() {
        Logger.i(TAG, "🎯 HOCHEMENT SYNCHRONISÉ - Pour accompagner la parole");

        if (isMoving) {
            Logger.w(TAG, "Mouvement en cours, ignoré");
            return;
        }

        if (!headMotorsEnabled) {
            Logger.w(TAG, "Moteurs non activés");
            return;
        }

        isMoving = true;

        try {
            // Mouvement plus doux pour accompagner la parole
            Logger.d(TAG, "→ Hochement doux synchronisé");

            BuddySDK.USB.buddySayYes(40.0f, 15.0f, new IUsbCommadRsp.Stub() {
                @Override
                public void onSuccess(String s) throws RemoteException {
                    if (s.equals("YES_MOVE_FINISHED")) {
                        // Retour immédiat
                        handler.postDelayed(() -> {
                            try {
                                BuddySDK.USB.buddySayYes(45.0f, -15.0f, new IUsbCommadRsp.Stub() {
                                    @Override
                                    public void onSuccess(String s2) throws RemoteException {
                                        if (s2.equals("YES_MOVE_FINISHED")) {
                                            Logger.i(TAG, "✅ Hochement synchronisé terminé");
                                            isMoving = false;
                                        }
                                    }

                                    @Override
                                    public void onFailed(String s2) throws RemoteException {
                                        Logger.e(TAG, "Échec retour synchronisé: " + s2);
                                        isMoving = false;
                                    }
                                });
                            } catch (Exception e) {
                                Logger.e(TAG, "Exception retour synchronisé", e);
                                isMoving = false;
                            }
                        }, 150); // Délai très court
                    }
                }

                @Override
                public void onFailed(String s) throws RemoteException {
                    Logger.e(TAG, "Échec hochement synchronisé: " + s);
                    isMoving = false;
                }
            });
        } catch (Exception e) {
            Logger.e(TAG, "Exception hochement synchronisé", e);
            isMoving = false;
        }
    }

    /**
     * Hochement triple pour emphase (ex: très bonne réponse)
     */
    public void performTripleYesNod() {
        Logger.i(TAG, "🎉 TRIPLE HOCHEMENT - Célébration");

        if (isMoving) return;
        if (!headMotorsEnabled) return;

        isMoving = true;
        performTripleNodStep(1);
    }

    private void performTripleNodStep(int step) {
        if (step > 3) {
            Logger.i(TAG, "Triple hochement terminé");
            isMoving = false;
            return;
        }

        try {
            Logger.d(TAG, "→ Hochement " + step + "/3");

            BuddySDK.USB.buddySayYes(55.0f, 18.0f, new IUsbCommadRsp.Stub() {
                @Override
                public void onSuccess(String s) throws RemoteException {
                    if (s.equals("YES_MOVE_FINISHED")) {
                        handler.postDelayed(() -> {
                            try {
                                BuddySDK.USB.buddySayYes(55.0f, -18.0f, new IUsbCommadRsp.Stub() {
                                    @Override
                                    public void onSuccess(String s2) throws RemoteException {
                                        if (s2.equals("YES_MOVE_FINISHED")) {
                                            // Passer au hochement suivant
                                            handler.postDelayed(() -> performTripleNodStep(step + 1), 100);
                                        }
                                    }

                                    @Override
                                    public void onFailed(String s2) throws RemoteException {
                                        Logger.e(TAG, "Échec triple hochement: " + s2);
                                        isMoving = false;
                                    }
                                });
                            } catch (Exception e) {
                                Logger.e(TAG, "Exception triple hochement", e);
                                isMoving = false;
                            }
                        }, 100);
                    }
                }

                @Override
                public void onFailed(String s) throws RemoteException {
                    Logger.e(TAG, "Échec triple hochement: " + s);
                    isMoving = false;
                }
            });
        } catch (Exception e) {
            Logger.e(TAG, "Exception triple hochement", e);
            isMoving = false;
        }
    }

    /**
     * Secouement NON optimisé
     */
    public void performNoShake() {
        if (isMoving || !headMotorsEnabled) {
            Logger.w(TAG, "Secouement NON impossible");
            return;
        }

        Logger.i(TAG, "🚫 SECOUEMENT NON");
        isMoving = true;

        try {
            // Mouvement plus rapide et naturel pour le "non"
            BuddySDK.USB.buddySayNo(45.0f, 35.0f, new IUsbCommadRsp.Stub() {
                @Override
                public void onSuccess(String s) throws RemoteException {
                    if (s.equals("NO_MOVE_FINISHED")) {
                        Logger.i(TAG, "✅ Secouement NON terminé");
                        isMoving = false;
                    }
                }

                @Override
                public void onFailed(String s) throws RemoteException {
                    Logger.e(TAG, "❌ Échec secouement NON: " + s);
                    isMoving = false;
                }
            });
        } catch (Exception e) {
            Logger.e(TAG, "Exception secouement NON", e);
            isMoving = false;
        }
    }

    /**
     * Danse de victoire optimisée
     */
    public void performVictoryDance() {
        if (isMoving) {
            Logger.w(TAG, "Danse impossible - mouvement en cours");
            return;
        }

        Logger.i(TAG, "🎉 DANSE DE VICTOIRE");
        isMoving = true;

        try {
            // Rotation rapide
            BuddySDK.USB.rotateBuddy(100.0f, 360.0f, new IUsbCommadRsp.Stub() {
                @Override
                public void onSuccess(String s) throws RemoteException {
                    // Rotation inverse après délai court
                    handler.postDelayed(() -> performSecondDanceMove(), 800);
                }

                @Override
                public void onFailed(String s) throws RemoteException {
                    Logger.e(TAG, "Échec danse 1: " + s);
                    isMoving = false;
                }
            });
        } catch (Exception e) {
            Logger.e(TAG, "Exception danse de victoire", e);
            isMoving = false;
        }
    }

    private void performSecondDanceMove() {
        try {
            BuddySDK.USB.rotateBuddy(-80.0f, 120.0f, new IUsbCommadRsp.Stub() {
                @Override
                public void onSuccess(String s) throws RemoteException {
                    Logger.i(TAG, "🎉 Danse de victoire terminée !");
                    isMoving = false;
                }

                @Override
                public void onFailed(String s) throws RemoteException {
                    Logger.e(TAG, "Échec danse 2: " + s);
                    isMoving = false;
                }
            });
        } catch (Exception e) {
            Logger.e(TAG, "Exception danse 2", e);
            isMoving = false;
        }
    }

    /**
     * GETTERS ET UTILITAIRES
     */
    public boolean isMoving() {
        return isMoving;
    }

    public boolean areHeadMotorsEnabled() {
        return headMotorsEnabled;
    }

    /**
     * Arrêt d'urgence
     */
    public void emergencyStop() {
        Logger.w(TAG, "🛑 ARRÊT D'URGENCE");
        isMoving = false;

        try {
            // Utiliser les méthodes d'arrêt du SDK
            BuddySDK.USB.buddyStopYesMove(new IUsbCommadRsp.Stub() {
                @Override
                public void onSuccess(String s) throws RemoteException {
                    Logger.i(TAG, "Arrêt moteur YES réussi");
                }

                @Override
                public void onFailed(String s) throws RemoteException {
                    Logger.e(TAG, "Échec arrêt moteur YES: " + s);
                }
            });

            BuddySDK.USB.buddyStopNoMove(new IUsbCommadRsp.Stub() {
                @Override
                public void onSuccess(String s) throws RemoteException {
                    Logger.i(TAG, "Arrêt moteur NO réussi");
                }

                @Override
                public void onFailed(String s) throws RemoteException {
                    Logger.e(TAG, "Échec arrêt moteur NO: " + s);
                }
            });
        } catch (Exception e) {
            Logger.e(TAG, "Exception arrêt d'urgence", e);
        }
    }
}