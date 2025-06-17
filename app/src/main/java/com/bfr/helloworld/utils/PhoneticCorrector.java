package com.bfr.helloworld.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Utilitaire pour corriger les erreurs phonétiques de reconnaissance vocale
 */
public class PhoneticCorrector {

    private static final Map<String, Integer> DIRECT_NUMBERS = new HashMap<>();
    private static final Map<String, Integer> PHONETIC_CORRECTIONS = new HashMap<>();

    static {
        initializeDirectNumbers();
        initializePhoneticCorrections();
    }

    private static void initializeDirectNumbers() {
        // Chiffres de base
        DIRECT_NUMBERS.put("zéro", 0);
        DIRECT_NUMBERS.put("zero", 0);
        DIRECT_NUMBERS.put("un", 1);
        DIRECT_NUMBERS.put("deux", 2);
        DIRECT_NUMBERS.put("trois", 3);
        DIRECT_NUMBERS.put("quatre", 4);
        DIRECT_NUMBERS.put("cinq", 5);
        DIRECT_NUMBERS.put("six", 6);
        DIRECT_NUMBERS.put("sept", 7);
        DIRECT_NUMBERS.put("huit", 8);
        DIRECT_NUMBERS.put("neuf", 9);

        // Nombres de 10 à 20
        DIRECT_NUMBERS.put("dix", 10);
        DIRECT_NUMBERS.put("onze", 11);
        DIRECT_NUMBERS.put("douze", 12);
        DIRECT_NUMBERS.put("treize", 13);
        DIRECT_NUMBERS.put("quatorze", 14);
        DIRECT_NUMBERS.put("quinze", 15);
        DIRECT_NUMBERS.put("seize", 16);
        DIRECT_NUMBERS.put("dix-sept", 17);
        DIRECT_NUMBERS.put("dix sept", 17);
        DIRECT_NUMBERS.put("dix-huit", 18);
        DIRECT_NUMBERS.put("dix huit", 18);
        DIRECT_NUMBERS.put("dix-neuf", 19);
        DIRECT_NUMBERS.put("dix neuf", 19);
        DIRECT_NUMBERS.put("vingt", 20);
    }

    private static void initializePhoneticCorrections() {
        // Corrections phonétiques observées
        PHONETIC_CORRECTIONS.put("c'est", 7);
        PHONETIC_CORRECTIONS.put("sait", 7);
        PHONETIC_CORRECTIONS.put("set", 7);
        PHONETIC_CORRECTIONS.put("cette", 7);
        PHONETIC_CORRECTIONS.put("ses", 7);

        PHONETIC_CORRECTIONS.put("hein", 1);
        PHONETIC_CORRECTIONS.put("an", 1);
        PHONETIC_CORRECTIONS.put("en", 1);
        PHONETIC_CORRECTIONS.put("han", 1);

        PHONETIC_CORRECTIONS.put("de", 2);
        PHONETIC_CORRECTIONS.put("d'eux", 2);
        PHONETIC_CORRECTIONS.put("du", 2);

        PHONETIC_CORRECTIONS.put("toi", 3);
        PHONETIC_CORRECTIONS.put("toit", 3);
        PHONETIC_CORRECTIONS.put("troie", 3);
        PHONETIC_CORRECTIONS.put("troit", 3);

        PHONETIC_CORRECTIONS.put("cat", 4);
        PHONETIC_CORRECTIONS.put("cat'", 4);
        PHONETIC_CORRECTIONS.put("catre", 4);
        PHONETIC_CORRECTIONS.put("carte", 4);

        PHONETIC_CORRECTIONS.put("saint", 5);
        PHONETIC_CORRECTIONS.put("sain", 5);
        PHONETIC_CORRECTIONS.put("sein", 5);
        PHONETIC_CORRECTIONS.put("seing", 5);

        PHONETIC_CORRECTIONS.put("sis", 6);
        PHONETIC_CORRECTIONS.put("cis", 6);
        PHONETIC_CORRECTIONS.put("si", 6);

        PHONETIC_CORRECTIONS.put("wi", 8);
        PHONETIC_CORRECTIONS.put("oui", 8);
        PHONETIC_CORRECTIONS.put("wii", 8);
        PHONETIC_CORRECTIONS.put("ouïe", 8);

        PHONETIC_CORRECTIONS.put("nerf", 9);
        PHONETIC_CORRECTIONS.put("nerve", 9);
        PHONETIC_CORRECTIONS.put("neu", 9);

        PHONETIC_CORRECTIONS.put("dis", 10);
        PHONETIC_CORRECTIONS.put("dit", 10);
        PHONETIC_CORRECTIONS.put("die", 10);
    }

    /**
     * Extrait un nombre d'un texte avec corrections phonétiques
     */
    public static int extractNumberWithPhoneticCorrection(String text) {
        if (text == null || text.trim().isEmpty()) {
            return -1;
        }

        String cleanText = text.toLowerCase().trim();
        Logger.d("PhoneticCorrector", "Analyse du texte: '" + cleanText + "'");

        // 1. Mapping direct
        if (DIRECT_NUMBERS.containsKey(cleanText)) {
            Logger.d("PhoneticCorrector", "Correspondance directe: " + cleanText + " -> " + DIRECT_NUMBERS.get(cleanText));
            return DIRECT_NUMBERS.get(cleanText);
        }

        // 2. Corrections phonétiques
        if (PHONETIC_CORRECTIONS.containsKey(cleanText)) {
            Logger.d("PhoneticCorrector", "Correction phonétique: " + cleanText + " -> " + PHONETIC_CORRECTIONS.get(cleanText));
            return PHONETIC_CORRECTIONS.get(cleanText);
        }

        // 3. Recherche dans les sous-chaînes
        for (Map.Entry<String, Integer> entry : DIRECT_NUMBERS.entrySet()) {
            if (cleanText.contains(entry.getKey())) {
                Logger.d("PhoneticCorrector", "Trouvé dans sous-chaîne: " + entry.getKey() + " -> " + entry.getValue());
                return entry.getValue();
            }
        }

        // 4. Corrections phonétiques dans les sous-chaînes
        for (Map.Entry<String, Integer> entry : PHONETIC_CORRECTIONS.entrySet()) {
            if (cleanText.contains(entry.getKey())) {
                Logger.d("PhoneticCorrector", "Correction phonétique dans sous-chaîne: " + entry.getKey() + " -> " + entry.getValue());
                return entry.getValue();
            }
        }

        // 5. Parsing numérique
        try {
            return Integer.parseInt(cleanText);
        } catch (NumberFormatException e) {
            // Continue
        }

        // 6. Recherche floue
        return findClosestNumberMatch(cleanText);
    }

    /**
     * Recherche floue avec distance de Levenshtein
     */
    private static int findClosestNumberMatch(String input) {
        String bestMatch = null;
        int minDistance = Integer.MAX_VALUE;

        for (String numberWord : DIRECT_NUMBERS.keySet()) {
            int distance = levenshteinDistance(input, numberWord);
            if (distance < minDistance && distance <= 2) {
                minDistance = distance;
                bestMatch = numberWord;
            }
        }

        if (bestMatch != null) {
            Logger.d("PhoneticCorrector", "Correspondance floue: " + input + " -> " + bestMatch + " (distance: " + minDistance + ")");
            return DIRECT_NUMBERS.get(bestMatch);
        }

        return -1;
    }

    /**
     * Calcule la distance de Levenshtein entre deux chaînes
     */
    private static int levenshteinDistance(String a, String b) {
        if (a == null || b == null) return Integer.MAX_VALUE;

        int[][] dp = new int[a.length() + 1][b.length() + 1];

        for (int i = 0; i <= a.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= b.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = (a.charAt(i - 1) == b.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(
                                dp[i - 1][j] + 1,        // deletion
                                dp[i][j - 1] + 1),       // insertion
                        dp[i - 1][j - 1] + cost  // substitution
                );
            }
        }

        return dp[a.length()][b.length()];
    }
}
