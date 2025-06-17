package com.bfr.helloworld.buddy;

import android.os.Handler;
import android.os.Looper;
import com.bfr.buddy.ui.shared.FacialExpression;
import com.bfr.buddy.ui.shared.IUIFaceAnimationCallback;
import com.bfr.buddysdk.BuddySDK;
import com.bfr.helloworld.utils.Logger;

/**
 * Gestionnaire des expressions faciales de Buddy
 */
public class BuddyExpressionManager {
    private static final String TAG = "BuddyExpressionManager";

    private FacialExpression currentExpression = FacialExpression.NEUTRAL;
    private final Handler handler = new Handler(Looper.getMainLooper());

    public BuddyExpressionManager() {
        Logger.i(TAG, "BuddyExpressionManager initialisé");
    }

    /**
     * Change l'expression faciale de Buddy
     */
    public void setExpression(FacialExpression expression) {
        setExpression(expression, null);
    }

    /**
     * Change l'expression faciale avec callback
     */
    public void setExpression(FacialExpression expression, Runnable onComplete) {
        if (expression == null) {
            Logger.w(TAG, "Expression null, ignorée");
            return;
        }

        if (expression == currentExpression) {
            Logger.d(TAG, "Expression déjà active: " + expression);
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }

        Logger.d(TAG, "Changement d'expression: " + currentExpression + " -> " + expression);

        try {
            // Utilisation de l'API avec callback correct
            BuddySDK.UI.setFacialExpression(expression, 1.0, new IUIFaceAnimationCallback.Stub() {
                @Override
                public void onAnimationEnd(String iType, String iValue) {
                    Logger.d(TAG, "Animation terminée - Type: " + iType + ", Value: " + iValue);
                    currentExpression = expression;
                    if (onComplete != null) {
                        onComplete.run();
                    }
                }
            });

        } catch (Exception e) {
            Logger.e(TAG, "Exception lors du changement d'expression", e);
            // En cas d'erreur, exécuter quand même le callback
            currentExpression = expression;
            if (onComplete != null) {
                onComplete.run();
            }
        }
    }

    /**
     * Expressions spécifiques pour le quiz
     */

    /**
     * Expression de joie pour une bonne réponse
     */
    public void showHappiness(Runnable onComplete) {
        Logger.i(TAG, "Expression de joie");
        setExpression(FacialExpression.LOVE, onComplete);
    }

    /**
     * Expression de réflexion pour une mauvaise réponse
     */
    public void showThinking(Runnable onComplete) {
        Logger.i(TAG, "Expression de réflexion");
        setExpression(FacialExpression.THINKING, onComplete);
    }

    /**
     * Expression d'écoute
     */
    public void showListening(Runnable onComplete) {
        Logger.i(TAG, "Expression d'écoute");
        setExpression(FacialExpression.LISTENING, onComplete);
    }

    /**
     * Expression de surprise pour la fin du quiz
     */
    public void showSurprise(Runnable onComplete) {
        Logger.i(TAG, "Expression de surprise");
        setExpression(FacialExpression.SURPRISED, onComplete);
    }

    /**
     * Expression neutre (par défaut)
     */
    public void showNeutral(Runnable onComplete) {
        Logger.d(TAG, "Expression neutre");
        setExpression(FacialExpression.NEUTRAL, onComplete);
    }

    /**
     * Séquence d'expressions pour une bonne réponse
     */
    public void performCorrectAnswerSequence() {
        Logger.i(TAG, "Séquence expression bonne réponse");

        // D'abord joie
        showHappiness(() -> {
            // Puis retour au neutre après 2 secondes
            handler.postDelayed(() -> showNeutral(null), 2000);
        });
    }

    /**
     * Séquence d'expressions pour une mauvaise réponse
     */
    public void performIncorrectAnswerSequence() {
        Logger.i(TAG, "Séquence expression mauvaise réponse");

        // D'abord réflexion
        showThinking(() -> {
            // Puis retour au neutre après 2 secondes
            handler.postDelayed(() -> showNeutral(null), 2000);
        });
    }

    /**
     * Séquence d'expressions pour la fin du quiz
     */
    public void performEndQuizSequence(boolean hasPassingGrade) {
        Logger.i(TAG, "Séquence expression fin de quiz (succès: " + hasPassingGrade + ")");

        if (hasPassingGrade) {
            // Surprise puis joie
            showSurprise(() -> {
                handler.postDelayed(() -> showHappiness(null), 1500);
            });
        } else {
            // Réflexion puis encouragement (neutre)
            showThinking(() -> {
                handler.postDelayed(() -> showNeutral(null), 2000);
            });
        }
    }

    /**
     * Récupère l'expression actuelle
     */
    public FacialExpression getCurrentExpression() {
        return currentExpression;
    }

    /**
     * Vérifie si Buddy exprime de la joie
     */
    public boolean isHappy() {
        return currentExpression == FacialExpression.HAPPY ||
                currentExpression == FacialExpression.LOVE ||
                currentExpression == FacialExpression.SURPRISED;
    }
}