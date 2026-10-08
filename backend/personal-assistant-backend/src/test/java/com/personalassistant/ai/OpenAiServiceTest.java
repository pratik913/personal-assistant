package com.personalassistant.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OpenAiServiceTest {

    @Test
    void constructor_shouldRejectMissingApiKey() {

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () ->
                                new OpenAiService("")
                );

        assertNotNull(exception);
    }

    @Test
    void constructor_shouldRejectBlankApiKey() {

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () ->
                                new OpenAiService("   ")
                );

        assertNotNull(exception);
    }

    @Test
    void constructor_shouldCreateServiceWithValidApiKey() {

        OpenAiService service =
                new OpenAiService(
                        "test-api-key"
                );

        assertNotNull(service);
    }
}