package com.safepoint.api.repository;

import com.safepoint.api.entity.WellbeingResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class WellbeingResourceRepositoryTest {

    @Autowired
    private WellbeingResourceRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();

        repository.save(resource("Managing Anxiety",  "Anxiety",     "en", "https://example.com/anxiety-en"));
        repository.save(resource("Better Sleep",      "Sleep",       "en", "https://example.com/sleep-en"));
        repository.save(resource("Manejo de Ansiedad","Ansiedad",    "es", "https://example.com/anxiety-es"));
        repository.save(resource("Mejor Sueño",       "Sueño",       "es", "https://example.com/sleep-es"));
    }

    private WellbeingResource resource(String title, String category, String lang, String url) {
        WellbeingResource r = new WellbeingResource();
        r.setTitle(title);
        r.setCategory(category);
        r.setLanguage(lang);
        r.setUrl(url);
        return r;
    }

    @Test @DisplayName("findAllByLanguage en → only English resources")
    void find_by_language_en() {
        List<WellbeingResource> result = repository.findAllByLanguageOrderByCategoryAscTitleAsc("en");

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(r -> "en".equals(r.getLanguage()));
    }

    @Test @DisplayName("findAllByLanguage es → only Spanish resources")
    void find_by_language_es() {
        List<WellbeingResource> result = repository.findAllByLanguageOrderByCategoryAscTitleAsc("es");

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(r -> "es".equals(r.getLanguage()));
    }

    @Test @DisplayName("results are ordered by category then title")
    void results_ordered_by_category_then_title() {
        repository.save(resource("AAA Article", "Zzz",    "en", "https://example.com/zzz"));
        repository.save(resource("AAA Article", "Anxiety", "en", "https://example.com/aaa"));

        List<WellbeingResource> result = repository.findAllByLanguageOrderByCategoryAscTitleAsc("en");

        assertThat(result.get(0).getCategory()).isEqualTo("Anxiety");
        assertThat(result.get(result.size() - 1).getCategory()).isEqualTo("Zzz");
    }

    @Test @DisplayName("unknown language → empty list")
    void unknown_language_returns_empty() {
        List<WellbeingResource> result = repository.findAllByLanguageOrderByCategoryAscTitleAsc("fr");

        assertThat(result).isEmpty();
    }

    @Test @DisplayName("new resource defaults to language 'en'")
    void new_resource_defaults_to_en() {
        WellbeingResource r = new WellbeingResource();
        r.setTitle("Default Lang");
        r.setCategory("Test");
        r.setUrl("https://example.com/default");
        repository.save(r);

        WellbeingResource saved = repository.findById(r.getId()).orElseThrow();
        assertThat(saved.getLanguage()).isEqualTo("en");
    }

    @Test @DisplayName("findByStatusAndLanguage — excludes UNAVAILABLE resources")
    void find_by_status_and_language_excludes_unavailable() {
        WellbeingResource broken = resource("Broken Article", "Anxiety", "en", "https://broken.example.com");
        broken.setStatus("UNAVAILABLE");
        repository.save(broken);

        List<WellbeingResource> result =
                repository.findByStatusAndLanguageOrderByCategoryAscTitleAsc("AVAILABLE", "en");

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(r -> "AVAILABLE".equals(r.getStatus()));
        assertThat(result).noneMatch(r -> "https://broken.example.com".equals(r.getUrl()));
    }

    @Test @DisplayName("findByStatusAndLanguage — returns UNAVAILABLE resources when queried with UNAVAILABLE")
    void find_by_status_and_language_returns_unavailable_when_requested() {
        WellbeingResource broken = resource("Broken Article", "Anxiety", "en", "https://broken.example.com");
        broken.setStatus("UNAVAILABLE");
        repository.save(broken);

        List<WellbeingResource> result =
                repository.findByStatusAndLanguageOrderByCategoryAscTitleAsc("UNAVAILABLE", "en");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUrl()).isEqualTo("https://broken.example.com");
    }
}
