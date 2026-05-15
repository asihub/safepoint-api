package com.safepoint.api.config;

import com.safepoint.api.service.WellbeingResourceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WellbeingCheckEndpointTest {

    @Mock
    private WellbeingResourceService service;

    @InjectMocks
    private WellbeingCheckEndpoint endpoint;

    @Test @DisplayName("run() triggers URL availability check and returns 'triggered'")
    void run_triggers_url_check_and_returns_triggered() {
        String result = endpoint.run();

        verify(service).checkAllUrlAvailability();
        assertThat(result).isEqualTo("triggered");
    }
}
