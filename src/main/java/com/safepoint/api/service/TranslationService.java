package com.safepoint.api.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * Translates text to English using the MyMemory API (free, no key required, 500 req/day).
 * Used to normalize Spanish user input before ML analysis.
 * API docs: https://mymemory.translated.net/doc/spec.php
 */
@Service
@Slf4j
public class TranslationService {

    private final RestTemplate restTemplate;

    private static final String MYMEMORY_URL = "https://api.mymemory.translated.net/get";

    // Minimum text length to attempt translation — very short texts skip translation
    private static final int MIN_TRANSLATE_LENGTH = 5;

    public TranslationService(@Qualifier("samhsaRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Detects whether text is likely Spanish using simple heuristics.
     * Checks for common Spanish words and diacritics.
     */
    public boolean isLikelySpanish(String text) {
        if (text == null || text.isBlank()) return false;

        String lower = text.toLowerCase();

        // Spanish-specific characters
        boolean hasDiacritics = lower.chars().anyMatch(c ->
            "áéíóúüñ¿¡".indexOf(c) >= 0
        );
        if (hasDiacritics) return true;

        // Common Spanish words not found in English
        String[] spanishMarkers = {
            " yo ", " mi ", " me ", " no ", " es ", " que ",
            " de ", " la ", " el ", " los ", " las ",
            " con ", " por ", " para ", " pero ", " soy ",
            " estoy ", " tengo ", " quiero ", " puedo ",
            " muy ", " bien ", " mal ", " todo ", " nada ",
        };

        int matchCount = 0;
        for (String marker : spanishMarkers) {
            if (lower.contains(marker)) matchCount++;
            if (matchCount >= 2) return true;
        }

        return false;
    }

    /**
     * Translates text from Spanish to English via MyMemory API.
     * Returns the original text if translation fails or text is too short.
     *
     * @param text       input text (may be Spanish)
     * @param sourceLang source language code (e.g. "es")
     * @return translated text in English, or original if translation unavailable
     */
    public String translateToEnglish(String text, String sourceLang) {
        if (text == null || text.length() < MIN_TRANSLATE_LENGTH) return text;

        try {
            String url = UriComponentsBuilder.fromUriString(MYMEMORY_URL)
                .queryParam("q", text)
                .queryParam("langpair", sourceLang + "|en")
                .toUriString();

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response == null) {
                log.warn("MyMemory returned null response");
                return text;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> responseData = (Map<String, Object>) response.get("responseData");

            if (responseData == null) return text;

            String translated = (String) responseData.get("translatedText");
            Double matchScore = responseData.get("match") instanceof Number
                ? ((Number) responseData.get("match")).doubleValue() : 0.0;

            // Only use translation if confidence is reasonable
            if (translated != null && !translated.isBlank() && matchScore > 0.1) {
                log.info("Translated {} chars from {} to en (match={})", text.length(), sourceLang, matchScore);
                return translated;
            }

            return text;

        } catch (Exception e) {
            log.warn("Translation failed, using original text: {}", e.getMessage());
            return text;
        }
    }

    /**
     * Auto-detects language and translates to English if needed.
     * Currently supports Spanish detection only.
     */
    public String normalizeToEnglish(String text) {
        if (text == null || text.isBlank()) return text;
        if (isLikelySpanish(text)) {
            return translateToEnglish(text, "es");
        }
        return text;
    }
}
