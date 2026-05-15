package com.safepoint.api.service;

import com.safepoint.api.entity.WellbeingResource;
import com.safepoint.api.repository.WellbeingResourceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WellbeingResourceServiceTest {

    @Mock
    private WellbeingResourceRepository repository;

    @InjectMocks
    private WellbeingResourceService service;

    private WellbeingResource resource(String lang) {
        WellbeingResource r = new WellbeingResource();
        r.setTitle("Test");
        r.setUrl("https://example.com/" + lang);
        r.setCategory("Anxiety");
        r.setLanguage(lang);
        return r;
    }

    @Test @DisplayName("lang=en → queries repository with 'en'")
    void lang_en_queries_en() {
        when(repository.findAllByLanguageOrderByCategoryAscTitleAsc("en"))
                .thenReturn(List.of(resource("en")));

        List<WellbeingResource> result = service.getAll("en");

        verify(repository).findAllByLanguageOrderByCategoryAscTitleAsc("en");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLanguage()).isEqualTo("en");
    }

    @Test @DisplayName("lang=es → queries repository with 'es'")
    void lang_es_queries_es() {
        when(repository.findAllByLanguageOrderByCategoryAscTitleAsc("es"))
                .thenReturn(List.of(resource("es")));

        List<WellbeingResource> result = service.getAll("es");

        verify(repository).findAllByLanguageOrderByCategoryAscTitleAsc("es");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLanguage()).isEqualTo("es");
    }

    @Test @DisplayName("lang=null → defaults to 'en'")
    void lang_null_defaults_to_en() {
        when(repository.findAllByLanguageOrderByCategoryAscTitleAsc("en"))
                .thenReturn(List.of(resource("en")));

        service.getAll(null);

        verify(repository).findAllByLanguageOrderByCategoryAscTitleAsc("en");
    }

    @Test @DisplayName("lang=fr (unsupported) → defaults to 'en'")
    void lang_unknown_defaults_to_en() {
        when(repository.findAllByLanguageOrderByCategoryAscTitleAsc("en"))
                .thenReturn(List.of());

        service.getAll("fr");

        verify(repository).findAllByLanguageOrderByCategoryAscTitleAsc("en");
    }

    @Test @DisplayName("lang=ES (uppercase) → defaults to 'en' (case-sensitive)")
    void lang_uppercase_defaults_to_en() {
        when(repository.findAllByLanguageOrderByCategoryAscTitleAsc("en"))
                .thenReturn(List.of());

        service.getAll("ES");

        verify(repository).findAllByLanguageOrderByCategoryAscTitleAsc("en");
    }
}
