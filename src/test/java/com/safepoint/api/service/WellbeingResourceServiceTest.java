package com.safepoint.api.service;

import com.safepoint.api.entity.WellbeingResource;
import com.safepoint.api.repository.WellbeingResourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WellbeingResourceServiceTest {

    @Mock
    private WellbeingResourceRepository repository;

    @Mock
    private RestTemplate mlRestTemplate;

    @Mock
    private RestTemplate urlCheckRestTemplate;

    private WellbeingResourceService service;

    @BeforeEach
    void setUp() {
        service = new WellbeingResourceService(repository, mlRestTemplate, urlCheckRestTemplate);
    }

    // ── getAll ────────────────────────────────────────────────────────────────

    @Test @DisplayName("lang=en → queries AVAILABLE resources with lang 'en'")
    void getAll_en_queries_available_en() {
        when(repository.findByStatusAndLanguageOrderByCategoryAscTitleAsc("AVAILABLE", "en"))
                .thenReturn(List.of(resource("en")));

        List<WellbeingResource> result = service.getAll("en");

        verify(repository).findByStatusAndLanguageOrderByCategoryAscTitleAsc("AVAILABLE", "en");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLanguage()).isEqualTo("en");
    }

    @Test @DisplayName("lang=es → queries AVAILABLE resources with lang 'es'")
    void getAll_es_queries_available_es() {
        when(repository.findByStatusAndLanguageOrderByCategoryAscTitleAsc("AVAILABLE", "es"))
                .thenReturn(List.of(resource("es")));

        service.getAll("es");

        verify(repository).findByStatusAndLanguageOrderByCategoryAscTitleAsc("AVAILABLE", "es");
    }

    @Test @DisplayName("lang=null → defaults to 'en'")
    void getAll_null_lang_defaults_to_en() {
        when(repository.findByStatusAndLanguageOrderByCategoryAscTitleAsc("AVAILABLE", "en"))
                .thenReturn(List.of());

        service.getAll(null);

        verify(repository).findByStatusAndLanguageOrderByCategoryAscTitleAsc("AVAILABLE", "en");
    }

    @Test @DisplayName("lang=fr (unsupported) → defaults to 'en'")
    void getAll_unsupported_lang_defaults_to_en() {
        when(repository.findByStatusAndLanguageOrderByCategoryAscTitleAsc("AVAILABLE", "en"))
                .thenReturn(List.of());

        service.getAll("fr");

        verify(repository).findByStatusAndLanguageOrderByCategoryAscTitleAsc("AVAILABLE", "en");
    }

    // ── checkAllUrlAvailability ───────────────────────────────────────────────

    @Test @DisplayName("checkAllUrlAvailability — marks UNAVAILABLE when URL returns 4xx")
    void checkAllUrlAvailability_marks_unavailable_on_4xx() {
        WellbeingResource r = resource("en");
        when(repository.findAll()).thenReturn(List.of(r));
        when(urlCheckRestTemplate.headForHeaders(r.getUrl()))
                .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        service.checkAllUrlAvailability();

        assertThat(r.getStatus()).isEqualTo("UNAVAILABLE");
        verify(repository).save(r);
    }

    @Test @DisplayName("checkAllUrlAvailability — marks UNAVAILABLE on connection failure")
    void checkAllUrlAvailability_marks_unavailable_on_connection_error() {
        WellbeingResource r = resource("en");
        when(repository.findAll()).thenReturn(List.of(r));
        when(urlCheckRestTemplate.headForHeaders(r.getUrl()))
                .thenThrow(new ResourceAccessException("Connection refused"));

        service.checkAllUrlAvailability();

        assertThat(r.getStatus()).isEqualTo("UNAVAILABLE");
        verify(repository).save(r);
    }

    @Test @DisplayName("checkAllUrlAvailability — marks AVAILABLE when previously UNAVAILABLE URL recovers")
    void checkAllUrlAvailability_marks_available_when_url_recovers() {
        WellbeingResource r = resource("en");
        r.setStatus("UNAVAILABLE");
        when(repository.findAll()).thenReturn(List.of(r));

        service.checkAllUrlAvailability();

        assertThat(r.getStatus()).isEqualTo("AVAILABLE");
        verify(repository).save(r);
    }

    @Test @DisplayName("checkAllUrlAvailability — skips save when status is unchanged")
    void checkAllUrlAvailability_skips_save_when_already_available() {
        WellbeingResource r = resource("en"); // default status is AVAILABLE
        when(repository.findAll()).thenReturn(List.of(r));

        service.checkAllUrlAvailability();

        verify(repository, never()).save(any());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private WellbeingResource resource(String lang) {
        WellbeingResource r = new WellbeingResource();
        r.setTitle("Test");
        r.setUrl("https://example.com/" + lang);
        r.setCategory("Anxiety");
        r.setLanguage(lang);
        return r;
    }
}
