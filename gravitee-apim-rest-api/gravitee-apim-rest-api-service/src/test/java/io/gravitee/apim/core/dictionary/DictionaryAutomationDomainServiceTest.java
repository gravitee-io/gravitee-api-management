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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.dictionary.domain_service.DictionaryAutomationDomainService;
import io.gravitee.apim.core.dictionary.model.Dictionary;
import io.gravitee.apim.core.dictionary.model.DictionaryProvider;
import io.gravitee.apim.core.dictionary.model.DictionaryTrigger;
import io.gravitee.apim.core.dictionary.model.DictionaryType;
import io.gravitee.apim.infra.domain_service.dictionary.DictionaryAutomationDomainServiceLegacyWrapper;
import io.gravitee.common.component.Lifecycle;
import io.gravitee.rest.api.model.configuration.dictionary.DictionaryEntity;
import io.gravitee.rest.api.model.configuration.dictionary.NewDictionaryEntity;
import io.gravitee.rest.api.model.configuration.dictionary.UpdateDictionaryEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.configuration.dictionary.DictionaryService;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DictionaryAutomationDomainServiceTest {

    private final DictionaryService dictionaryService = mock(DictionaryService.class);
    private final DictionaryAutomationDomainService domainService = new DictionaryAutomationDomainServiceLegacyWrapper(dictionaryService);
    private final ExecutionContext executionContext = new ExecutionContext("org", "env");

    @Nested
    class Create {

        @Test
        void should_convert_dictionary_to_new_entity_with_key() {
            var dictionary = Dictionary.builder()
                .hrid("my-hrid")
                .name("My Dict")
                .description("A description")
                .type(DictionaryType.MANUAL)
                .properties(Map.of("k", "v"))
                .build();
            var created = DictionaryEntity.builder().id("new-id").build();
            when(dictionaryService.create(any(), any())).thenReturn(created);

            var result = domainService.create(executionContext, dictionary);

            assertThat(result).isSameAs(created);
            var captor = ArgumentCaptor.forClass(NewDictionaryEntity.class);
            verify(dictionaryService).create(eq(executionContext), captor.capture());
            var entity = captor.getValue();
            assertThat(entity.getKey()).isEqualTo("my-hrid");
            assertThat(entity.getName()).isEqualTo("My Dict");
            assertThat(entity.getDescription()).isEqualTo("A description");
            assertThat(entity.getType()).isEqualTo(io.gravitee.rest.api.model.configuration.dictionary.DictionaryType.MANUAL);
            assertThat(entity.getProperties()).containsEntry("k", "v");
        }
    }

    @Nested
    class Update {

        @Test
        void should_convert_dictionary_to_update_entity_without_key() {
            var trigger = DictionaryTrigger.builder().rate(10L).unit(TimeUnit.MINUTES).build();
            var provider = DictionaryProvider.builder()
                .type("HTTP")
                .configuration(new ObjectMapper().createObjectNode().put("url", "http://example.com"))
                .build();

            var dictionary = Dictionary.builder()
                .hrid("my-hrid")
                .name("Updated Dict")
                .description("Updated desc")
                .type(DictionaryType.DYNAMIC)
                .provider(provider)
                .trigger(trigger)
                .build();
            var updated = DictionaryEntity.builder().id("existing-id").build();
            when(dictionaryService.update(any(), any(), any())).thenReturn(updated);

            var result = domainService.update(executionContext, "existing-id", dictionary);

            assertThat(result).isSameAs(updated);
            var captor = ArgumentCaptor.forClass(UpdateDictionaryEntity.class);
            verify(dictionaryService).update(eq(executionContext), eq("existing-id"), captor.capture());
            var entity = captor.getValue();
            assertThat(entity.getName()).isEqualTo("Updated Dict");
            assertThat(entity.getDescription()).isEqualTo("Updated desc");
            assertThat(entity.getType()).isEqualTo(io.gravitee.rest.api.model.configuration.dictionary.DictionaryType.DYNAMIC);
            assertThat(entity.getProvider().getType()).isEqualTo("HTTP");
            assertThat(entity.getTrigger().getRate()).isEqualTo(10L);
            assertThat(entity.getTrigger().getUnit()).isEqualTo(TimeUnit.MINUTES);
        }
    }

    @Nested
    class HandleDeployment_Manual {

        private final DictionaryEntity dictionary = DictionaryEntity.builder()
            .id("dict-id")
            .type(io.gravitee.rest.api.model.configuration.dictionary.DictionaryType.MANUAL)
            .build();

        @Test
        void should_deploy_when_deploy_is_true() {
            var deployed = DictionaryEntity.builder()
                .id("dict-id")
                .type(io.gravitee.rest.api.model.configuration.dictionary.DictionaryType.MANUAL)
                .build();
            when(dictionaryService.deploy(executionContext, "dict-id")).thenReturn(deployed);

            var result = domainService.handleDeployment(executionContext, dictionary, true);

            assertThat(result).isSameAs(deployed);
            verify(dictionaryService).deploy(executionContext, "dict-id");
            verify(dictionaryService, never()).undeploy(executionContext, "dict-id");
        }

        @Test
        void should_undeploy_when_deploy_is_false() {
            var undeployed = DictionaryEntity.builder()
                .id("dict-id")
                .type(io.gravitee.rest.api.model.configuration.dictionary.DictionaryType.MANUAL)
                .build();
            when(dictionaryService.undeploy(executionContext, "dict-id")).thenReturn(undeployed);

            var result = domainService.handleDeployment(executionContext, dictionary, false);

            assertThat(result).isSameAs(undeployed);
            verify(dictionaryService).undeploy(executionContext, "dict-id");
            verify(dictionaryService, never()).deploy(executionContext, "dict-id");
        }
    }

    @Nested
    class HandleDeployment_Dynamic {

        @Test
        void should_start_when_deploy_is_true_and_not_started() {
            var dictionary = DictionaryEntity.builder()
                .id("dict-id")
                .type(io.gravitee.rest.api.model.configuration.dictionary.DictionaryType.DYNAMIC)
                .state(Lifecycle.State.STOPPED)
                .build();
            var started = DictionaryEntity.builder()
                .id("dict-id")
                .type(io.gravitee.rest.api.model.configuration.dictionary.DictionaryType.DYNAMIC)
                .state(Lifecycle.State.STARTED)
                .build();
            when(dictionaryService.start(executionContext, "dict-id")).thenReturn(started);

            var result = domainService.handleDeployment(executionContext, dictionary, true);

            assertThat(result).isSameAs(started);
            verify(dictionaryService).start(executionContext, "dict-id");
        }

        @Test
        void should_not_start_when_deploy_is_true_and_already_started() {
            var dictionary = DictionaryEntity.builder()
                .id("dict-id")
                .type(io.gravitee.rest.api.model.configuration.dictionary.DictionaryType.DYNAMIC)
                .state(Lifecycle.State.STARTED)
                .build();

            var result = domainService.handleDeployment(executionContext, dictionary, true);

            assertThat(result).isSameAs(dictionary);
            verify(dictionaryService, never()).start(executionContext, "dict-id");
        }

        @Test
        void should_stop_and_undeploy_when_deploy_is_false_and_started() {
            var dictionary = DictionaryEntity.builder()
                .id("dict-id")
                .type(io.gravitee.rest.api.model.configuration.dictionary.DictionaryType.DYNAMIC)
                .state(Lifecycle.State.STARTED)
                .build();
            var stopped = DictionaryEntity.builder()
                .id("dict-id")
                .type(io.gravitee.rest.api.model.configuration.dictionary.DictionaryType.DYNAMIC)
                .state(Lifecycle.State.STOPPED)
                .build();
            var undeployed = DictionaryEntity.builder()
                .id("dict-id")
                .type(io.gravitee.rest.api.model.configuration.dictionary.DictionaryType.DYNAMIC)
                .state(Lifecycle.State.STOPPED)
                .build();
            when(dictionaryService.stop(executionContext, "dict-id")).thenReturn(stopped);
            when(dictionaryService.undeploy(executionContext, "dict-id")).thenReturn(undeployed);

            var result = domainService.handleDeployment(executionContext, dictionary, false);

            assertThat(result).isSameAs(undeployed);
            verify(dictionaryService).stop(executionContext, "dict-id");
            verify(dictionaryService).undeploy(executionContext, "dict-id");
        }

        @Test
        void should_not_stop_when_deploy_is_false_and_not_started() {
            var dictionary = DictionaryEntity.builder()
                .id("dict-id")
                .type(io.gravitee.rest.api.model.configuration.dictionary.DictionaryType.DYNAMIC)
                .state(Lifecycle.State.STOPPED)
                .build();

            var result = domainService.handleDeployment(executionContext, dictionary, false);

            assertThat(result).isSameAs(dictionary);
            verify(dictionaryService, never()).stop(executionContext, "dict-id");
        }

        @Test
        void should_start_when_state_is_null_and_deploy_is_true() {
            var dictionary = DictionaryEntity.builder()
                .id("dict-id")
                .type(io.gravitee.rest.api.model.configuration.dictionary.DictionaryType.DYNAMIC)
                .build();
            var started = DictionaryEntity.builder()
                .id("dict-id")
                .type(io.gravitee.rest.api.model.configuration.dictionary.DictionaryType.DYNAMIC)
                .state(Lifecycle.State.STARTED)
                .build();
            when(dictionaryService.start(executionContext, "dict-id")).thenReturn(started);

            var result = domainService.handleDeployment(executionContext, dictionary, true);

            assertThat(result).isSameAs(started);
            verify(dictionaryService).start(executionContext, "dict-id");
        }

        @Test
        void should_not_stop_when_state_is_null_and_deploy_is_false() {
            var dictionary = DictionaryEntity.builder()
                .id("dict-id")
                .type(io.gravitee.rest.api.model.configuration.dictionary.DictionaryType.DYNAMIC)
                .build();

            var result = domainService.handleDeployment(executionContext, dictionary, false);

            assertThat(result).isSameAs(dictionary);
            verifyNoInteractions(dictionaryService);
        }
    }
}
