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
package io.gravitee.apim.core.dictionary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.dictionary.domain_service.DictionaryAutomationDomainService;
import io.gravitee.apim.core.dictionary.model.Dictionary;
import io.gravitee.apim.core.dictionary.model.DictionaryType;
import io.gravitee.apim.core.dictionary.use_case.CreateOrUpdateDictionaryUseCase;
import io.gravitee.rest.api.model.configuration.dictionary.DictionaryEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Optional;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class CreateOrUpdateDictionaryUseCaseTest {

    private final DictionaryAutomationDomainService domainService = mock(DictionaryAutomationDomainService.class);
    private final CreateOrUpdateDictionaryUseCase useCase = new CreateOrUpdateDictionaryUseCase(domainService);
    private final ExecutionContext executionContext = new ExecutionContext("org", "env");

    @Test
    void should_create_when_dictionary_does_not_exist() {
        var dictionary = Dictionary.builder().hrid("my-hrid").name("test").type(DictionaryType.MANUAL).build();
        var created = DictionaryEntity.builder()
            .id("new-id")
            .type(io.gravitee.rest.api.model.configuration.dictionary.DictionaryType.MANUAL)
            .build();
        var deployed = DictionaryEntity.builder()
            .id("new-id")
            .type(io.gravitee.rest.api.model.configuration.dictionary.DictionaryType.MANUAL)
            .build();

        when(domainService.findById(executionContext, "new-id")).thenReturn(Optional.empty());
        when(domainService.create(executionContext, dictionary)).thenReturn(created);
        when(domainService.handleDeployment(executionContext, created, true)).thenReturn(deployed);

        var output = useCase.execute(new CreateOrUpdateDictionaryUseCase.Input(executionContext, dictionary, true));

        assertThat(output.dictionary()).isSameAs(deployed);
        verify(domainService).create(executionContext, dictionary);
        verify(domainService, never()).update(any(), any(), any());
    }

    @Test
    void should_update_when_dictionary_already_exists() {
        var dictionary = Dictionary.builder().id("existing-id").name("test").type(DictionaryType.MANUAL).build();
        var existing = DictionaryEntity.builder()
            .id("existing-id")
            .type(io.gravitee.rest.api.model.configuration.dictionary.DictionaryType.MANUAL)
            .build();
        var updated = DictionaryEntity.builder()
            .id("existing-id")
            .type(io.gravitee.rest.api.model.configuration.dictionary.DictionaryType.MANUAL)
            .build();
        var deployed = DictionaryEntity.builder()
            .id("existing-id")
            .type(io.gravitee.rest.api.model.configuration.dictionary.DictionaryType.MANUAL)
            .build();

        when(domainService.findById(executionContext, "existing-id")).thenReturn(Optional.of(existing));
        when(domainService.update(executionContext, "existing-id", dictionary)).thenReturn(updated);
        when(domainService.handleDeployment(executionContext, updated, false)).thenReturn(deployed);

        var output = useCase.execute(new CreateOrUpdateDictionaryUseCase.Input(executionContext, dictionary, false));

        assertThat(output.dictionary()).isSameAs(deployed);
        verify(domainService).update(executionContext, "existing-id", dictionary);
        verify(domainService, never()).create(any(), any());
    }

    @Test
    void should_create_new_dictionary_when_hrid_changes() {
        var dictionary = Dictionary.builder().hrid("new-hrid").name("test").type(DictionaryType.MANUAL).build();
        var created = DictionaryEntity.builder()
            .id("new-id")
            .key("new-hrid")
            .type(io.gravitee.rest.api.model.configuration.dictionary.DictionaryType.MANUAL)
            .build();
        var deployed = DictionaryEntity.builder()
            .id("new-id")
            .key("new-hrid")
            .type(io.gravitee.rest.api.model.configuration.dictionary.DictionaryType.MANUAL)
            .build();

        when(domainService.findById(executionContext, "new-id")).thenReturn(Optional.empty());
        when(domainService.create(executionContext, dictionary)).thenReturn(created);
        when(domainService.handleDeployment(executionContext, created, true)).thenReturn(deployed);

        var output = useCase.execute(new CreateOrUpdateDictionaryUseCase.Input(executionContext, dictionary, true));

        assertThat(output.dictionary()).isSameAs(deployed);
        verify(domainService).create(executionContext, dictionary);
        verify(domainService, never()).update(any(), any(), any());
    }
}
