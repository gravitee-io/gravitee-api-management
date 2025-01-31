package io.gravitee.apim.core.application_dictionary.use_case;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import inmemory.ApplicationCrudServiceInMemory;
import inmemory.ApplicationDictionaryCrudServiceInMemory;
import io.gravitee.repository.management.model.Dictionary;
import io.gravitee.repository.management.model.DictionaryType;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class UpdateApplicationDictionaryUseCaseTest {

    UpdateApplicationDictionaryUseCase useCase;
    private static final String APPLICATION_ID = "application-id";
    private static final ExecutionContext EXECUTION_CONTEXT = new ExecutionContext();
    private ApplicationDictionaryCrudServiceInMemory applicationDictionaryCrudService;

    @BeforeEach
    void setUp() {
        ApplicationCrudServiceInMemory applicationCrudService = new ApplicationCrudServiceInMemory();
        applicationCrudService.initWith(List.of(BaseApplicationEntity.builder().id(APPLICATION_ID).build()));
        applicationDictionaryCrudService = new ApplicationDictionaryCrudServiceInMemory();
        Dictionary dictionary = new Dictionary();
        dictionary.setId(APPLICATION_ID);
        applicationDictionaryCrudService.initWith(List.of(dictionary));
        useCase = new UpdateApplicationDictionaryUseCase(applicationCrudService, applicationDictionaryCrudService);
    }

    @Test
    void should_create_dictionary() {
        UpdateApplicationDictionaryUseCase.Input input = new UpdateApplicationDictionaryUseCase.Input(
            APPLICATION_ID,
            true,
            EXECUTION_CONTEXT,
            DictionaryType.MANUAL,
            Map.of("key", "value"),
            "description"
        );

        UpdateApplicationDictionaryUseCase.Output output = useCase.execute(input);

        assertTrue(output.enabled());
        assertNotNull(output.dictionary());
        assertEquals(APPLICATION_ID, output.dictionary().getId());
        assertEquals("value", output.dictionary().getProperties().get("key"));
    }

    @Test
    void should_update_dictionary() {
        Dictionary existingDictionary = applicationDictionaryCrudService.findById(APPLICATION_ID).orElseThrow();
        existingDictionary.setProperties(Map.of("oldKey", "oldValue"));
        applicationDictionaryCrudService.update(EXECUTION_CONTEXT, existingDictionary);

        UpdateApplicationDictionaryUseCase.Input input = new UpdateApplicationDictionaryUseCase.Input(
            APPLICATION_ID,
            true,
            EXECUTION_CONTEXT,
            DictionaryType.MANUAL,
            Map.of("newKey", "newValue"),
            "new description"
        );

        UpdateApplicationDictionaryUseCase.Output output = useCase.execute(input);

        assertTrue(output.enabled());
        assertNotNull(output.dictionary());
        assertEquals(APPLICATION_ID, output.dictionary().getId());
        assertEquals("newValue", output.dictionary().getProperties().get("newKey"));
        assertEquals("new description", output.dictionary().getDescription());
    }

    @Test
    void should_disable_dictionary() {
        UpdateApplicationDictionaryUseCase.Input input = new UpdateApplicationDictionaryUseCase.Input(
            APPLICATION_ID,
            false,
            EXECUTION_CONTEXT,
            DictionaryType.MANUAL,
            Map.of(),
            null
        );

        UpdateApplicationDictionaryUseCase.Output output = useCase.execute(input);

        assertFalse(output.enabled());
        assertNull(output.dictionary());
        assertTrue(applicationDictionaryCrudService.findById(APPLICATION_ID).isEmpty());
    }

    @Test
    void should_throw_exception_when_creating_dynamic_dictionary() {
        applicationDictionaryCrudService.reset();
        UpdateApplicationDictionaryUseCase.Input input = new UpdateApplicationDictionaryUseCase.Input(
            APPLICATION_ID,
            true,
            EXECUTION_CONTEXT,
            DictionaryType.DYNAMIC,
            Map.of(),
            null
        );

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> useCase.execute(input));
        assertEquals("Dynamic dictionary is not supported", exception.getMessage());
    }
}
