package com.safepoint.api;

import com.safepoint.api.entity.WellbeingResource;
import com.safepoint.api.repository.WellbeingResourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WellbeingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WellbeingResourceRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        repository.save(resource("Managing Anxiety",   "Anxiety", "en", "https://example.com/anxiety-en"));
        repository.save(resource("Manejo de Ansiedad", "Ansiedad","es", "https://example.com/anxiety-es"));
    }

    private WellbeingResource resource(String title, String category, String lang, String url) {
        WellbeingResource r = new WellbeingResource();
        r.setTitle(title);
        r.setCategory(category);
        r.setLanguage(lang);
        r.setUrl(url);
        return r;
    }

    @Test @DisplayName("GET /api/v1/wellbeing?lang=en → returns only English resources")
    void get_en_resources() throws Exception {
        mockMvc.perform(get("/api/v1/wellbeing").param("lang", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].language").value("en"))
                .andExpect(jsonPath("$[0].title").value("Managing Anxiety"));
    }

    @Test @DisplayName("GET /api/v1/wellbeing?lang=es → returns only Spanish resources")
    void get_es_resources() throws Exception {
        mockMvc.perform(get("/api/v1/wellbeing").param("lang", "es"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].language").value("es"))
                .andExpect(jsonPath("$[0].title").value("Manejo de Ansiedad"));
    }

    @Test @DisplayName("GET /api/v1/wellbeing (no param) → defaults to English")
    void get_no_param_defaults_to_en() throws Exception {
        mockMvc.perform(get("/api/v1/wellbeing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].language").value("en"));
    }

    @Test @DisplayName("GET /api/v1/wellbeing?lang=fr → defaults to English, not empty")
    void get_unsupported_lang_defaults_to_en() throws Exception {
        mockMvc.perform(get("/api/v1/wellbeing").param("lang", "fr"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].language").value("en"));
    }
}
