package io.gravitee.rest.api.management.v2.rest.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.rest.api.management.v2.rest.model.SourceConfigurationConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PageMapperTest extends AbstractMapperTest {

    private PageMapper pageMapper;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        pageMapper = PageMapper.INSTANCE;
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("Should map SourceConfigurationConfiguration to JSON string")
    void shouldMapSourceConfigurationConfigurationToJsonString() throws JsonProcessingException {
        SourceConfigurationConfiguration sourceConfiguration = new SourceConfigurationConfiguration();
        sourceConfiguration.setUseSystemProxy(true);

        String expectedJson = objectMapper.writeValueAsString(sourceConfiguration);

        String actualJson = pageMapper.map(sourceConfiguration);

        assertEquals(expectedJson, actualJson);
    }

    @Test
    @DisplayName("Should throw RuntimeException when serialization fails")
    void shouldThrowRuntimeExceptionWhenSerializationFails() throws JsonProcessingException {
        SourceConfigurationConfiguration sourceConfiguration = null;

        String expectedJson = objectMapper.writeValueAsString(null);

        String actualJson = pageMapper.map(sourceConfiguration);

        assertEquals(expectedJson, actualJson);

    }
}
