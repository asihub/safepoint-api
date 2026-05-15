package com.safepoint.api.controller;

import com.safepoint.api.config.SecurityConfig;
import com.safepoint.api.entity.WellbeingResource;
import com.safepoint.api.service.WellbeingResourceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WellbeingResourceController.class)
@Import(SecurityConfig.class)
class WellbeingResourceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WellbeingResourceService service;

    private WellbeingResource resource(String lang) {
        WellbeingResource r = new WellbeingResource();
        r.setTitle("Test Article");
        r.setUrl("https://example.com/" + lang);
        r.setCategory("Anxiety");
        r.setLanguage(lang);
        return r;
    }

    @Test @DisplayName("GET /api/v1/wellbeing with no param → 200, delegates with null")
    void no_lang_param_returns_200() throws Exception {
        when(service.getAll(null)).thenReturn(List.of(resource("en")));

        mockMvc.perform(get("/api/v1/wellbeing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].language").value("en"));

        verify(service).getAll(null);
    }

    @Test @DisplayName("GET /api/v1/wellbeing?lang=en → 200 with en resources")
    void lang_en_returns_en_resources() throws Exception {
        when(service.getAll("en")).thenReturn(List.of(resource("en")));

        mockMvc.perform(get("/api/v1/wellbeing").param("lang", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].language").value("en"));

        verify(service).getAll("en");
    }

    @Test @DisplayName("GET /api/v1/wellbeing?lang=es → 200 with es resources")
    void lang_es_returns_es_resources() throws Exception {
        when(service.getAll("es")).thenReturn(List.of(resource("es")));

        mockMvc.perform(get("/api/v1/wellbeing").param("lang", "es"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].language").value("es"));

        verify(service).getAll("es");
    }

    @Test @DisplayName("GET /api/v1/wellbeing → 200 with empty list")
    void returns_empty_list() throws Exception {
        when(service.getAll(null)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/wellbeing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }
}
