/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.apim.core.application_dictionary.use_case;

import static org.assertj.core.api.Assertions.assertThat;

import inmemory.ApplicationCrudServiceInMemory;
import inmemory.ApplicationDictionaryCrudServiceInMemory;
import io.gravitee.apim.core.application_dictionary.model.Dictionary;
import io.gravitee.apim.core.application_dictionary.model.ManualDictionary;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class UpdateApplicationDictionaryUseCaseTest {

    UpdateApplicationDictionaryUseCase useCase;
    private static final String APPLICATION_ID = "application-id";
    private static final String APPLICATION_ID_FOR_CREATE = "application-id-for-create";
    private static final ExecutionContext EXECUTION_CONTEXT = new ExecutionContext("environment", "environmentId");
    private ApplicationDictionaryCrudServiceInMemory applicationDictionaryCrudService;

    @BeforeEach
    void setUp() {
        ApplicationCrudServiceInMemory applicationCrudService = new ApplicationCrudServiceInMemory();
        applicationCrudService.initWith(
            List.of(
                BaseApplicationEntity.builder().id(APPLICATION_ID).build(),
                BaseApplicationEntity.builder().id(APPLICATION_ID_FOR_CREATE).build()
            )
        );
        applicationDictionaryCrudService = new ApplicationDictionaryCrudServiceInMemory();
        io.gravitee.apim.core.application_dictionary.model.Dictionary dictionary = new ManualDictionary();
        dictionary.setId(APPLICATION_ID);
        applicationDictionaryCrudService.initWith(List.of(dictionary));
        useCase = new UpdateApplicationDictionaryUseCase(applicationCrudService, applicationDictionaryCrudService);
    }

    @Test
    void should_create_dictionary() {
        var dictionary = new ManualDictionary();
        dictionary.setId(APPLICATION_ID_FOR_CREATE);
        dictionary.setProperties(Map.of("key", "value"));
        dictionary.setDescription("description");
        UpdateApplicationDictionaryUseCase.Input input = new UpdateApplicationDictionaryUseCase.Input(
            true,
            EXECUTION_CONTEXT,
            dictionary,
            APPLICATION_ID_FOR_CREATE
        );

        UpdateApplicationDictionaryUseCase.Output output = useCase.execute(input);

        assertThat(output)
            .isNotNull()
            .extracting(UpdateApplicationDictionaryUseCase.Output::enabled, UpdateApplicationDictionaryUseCase.Output::dictionary)
            .contains(true, dictionary);
        assertThat(output)
            .extracting(UpdateApplicationDictionaryUseCase.Output::dictionary)
            .asInstanceOf(InstanceOfAssertFactories.type(ManualDictionary.class))
            .extracting(ManualDictionary::getId, ManualDictionary::getProperties, ManualDictionary::getDescription)
            .contains(APPLICATION_ID_FOR_CREATE, Map.of("key", "value"), "description");
    }

    @Test
    void should_update_dictionary() {
        io.gravitee.apim.core.application_dictionary.model.Dictionary existingDictionary = applicationDictionaryCrudService
            .findById(APPLICATION_ID)
            .orElseThrow();
        ((ManualDictionary) existingDictionary).setProperties(Map.of("oldKey", "oldValue"));
        var updated = new ManualDictionary();
        updated.setId(APPLICATION_ID);
        updated.setProperties(Map.of("newKey", "newValue"));
        updated.setDescription("new description");
        applicationDictionaryCrudService.update(EXECUTION_CONTEXT, existingDictionary);

        UpdateApplicationDictionaryUseCase.Input input = new UpdateApplicationDictionaryUseCase.Input(
            true,
            EXECUTION_CONTEXT,
            updated,
            APPLICATION_ID
        );

        UpdateApplicationDictionaryUseCase.Output output = useCase.execute(input);

        assertThat(output)
            .isNotNull()
            .extracting(UpdateApplicationDictionaryUseCase.Output::enabled, UpdateApplicationDictionaryUseCase.Output::dictionary)
            .contains(true, updated);
        assertThat(output.dictionary())
            .asInstanceOf(InstanceOfAssertFactories.type(ManualDictionary.class))
            .extracting(ManualDictionary::getId, ManualDictionary::getProperties, ManualDictionary::getDescription)
            .contains(APPLICATION_ID, Map.of("newKey", "newValue"), "new description");
    }

    @Test
    void should_disable_dictionary() {
        Dictionary dictionary = null;
        UpdateApplicationDictionaryUseCase.Input input = new UpdateApplicationDictionaryUseCase.Input(
            false,
            EXECUTION_CONTEXT,
            dictionary,
            APPLICATION_ID
        );

        UpdateApplicationDictionaryUseCase.Output output = useCase.execute(input);

        assertThat(output)
            .isNotNull()
            .extracting(UpdateApplicationDictionaryUseCase.Output::enabled, UpdateApplicationDictionaryUseCase.Output::dictionary)
            .contains(false, dictionary);
        assertThat(applicationDictionaryCrudService.findById(APPLICATION_ID)).isEmpty();
    }
}
