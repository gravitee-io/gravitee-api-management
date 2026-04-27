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
package io.gravitee.apim.rest.api.automation.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.dictionary.domain_service.ValidateDictionaryDomainService;
import io.gravitee.apim.core.dictionary.use_case.CreateOrUpdateDictionaryUseCase;
import io.gravitee.apim.rest.api.automation.model.DictionaryState;
import io.gravitee.apim.rest.api.automation.resource.base.AbstractResourceTest;
import io.gravitee.common.component.Lifecycle;
import io.gravitee.rest.api.model.configuration.dictionary.DictionaryEntity;
import io.gravitee.rest.api.model.configuration.dictionary.DictionaryType;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import java.util.Date;
import java.util.Map;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DictionariesResourceTest extends AbstractResourceTest {

    @Inject
    private CreateOrUpdateDictionaryUseCase createOrUpdateDictionaryUseCase;

    @Inject
    private ValidateDictionaryDomainService validateDictionaryDomainService;

    @AfterEach
    void tearDown() {
        reset(createOrUpdateDictionaryUseCase, validateDictionaryDomainService);
    }

    @Override
    protected String contextPath() {
        return "/organizations/" + ORGANIZATION + "/environments/" + ENVIRONMENT + "/dictionaries";
    }

    @Nested
    class DryRun {

        @Test
        void should_return_spec_without_calling_use_case() {
            try (
                var response = rootTarget()
                    .queryParam("dryRun", true)
                    .request()
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .put(Entity.json(readJSON("manual-dictionary.json")))
            ) {
                assertThat(response.getStatus()).isEqualTo(200);
                verifyNoInteractions(createOrUpdateDictionaryUseCase);
            }
        }

        @Test
        void should_return_400_when_dictionary_is_invalid() {
            try (
                var response = rootTarget()
                    .queryParam("dryRun", true)
                    .request()
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .put(Entity.json(readJSON("invalid-dictionary.json")))
            ) {
                assertThat(response.getStatus()).isEqualTo(400);
                verifyNoInteractions(createOrUpdateDictionaryUseCase);
            }
        }
    }

    @Nested
    class Run {

        @Test
        void should_create_or_update_manual_dictionary() {
            var entity = DictionaryEntity.builder()
                .id("dict-id")
                .name("My Dictionary")
                .key("my-dict")
                .type(DictionaryType.MANUAL)
                .state(Lifecycle.State.STOPPED)
                .properties(Map.of("key1", "value1"))
                .createdAt(new Date())
                .updatedAt(new Date())
                .deployedAt(new Date())
                .build();
            when(createOrUpdateDictionaryUseCase.execute(any())).thenReturn(new CreateOrUpdateDictionaryUseCase.Output(entity));

            try (
                var response = rootTarget()
                    .request()
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .put(Entity.json(readJSON("manual-dictionary.json")))
            ) {
                assertThat(response.getStatus()).isEqualTo(200);
                verify(validateDictionaryDomainService).validate(any());
                verify(createOrUpdateDictionaryUseCase).execute(any(CreateOrUpdateDictionaryUseCase.Input.class));

                var state = response.readEntity(DictionaryState.class);
                SoftAssertions.assertSoftly(soft -> {
                    soft.assertThat(state.getId()).isEqualTo("dict-id");
                    soft.assertThat(state.getHrid()).isEqualTo("my-dict");
                    soft.assertThat(state.getOrganizationId()).isEqualTo(ORGANIZATION);
                    soft.assertThat(state.getEnvironmentId()).isEqualTo(ENVIRONMENT);
                    soft.assertThat(state.getDeployed()).isTrue();
                    soft.assertThat(state.getManual()).isNotNull();
                    soft.assertThat(state.getManual().getProperties()).containsEntry("key1", "value1");
                });
            }
        }

        @Test
        void should_create_or_update_dynamic_dictionary() {
            var entity = DictionaryEntity.builder()
                .id("dyn-dict-id")
                .name("My Dynamic Dictionary")
                .key("my-dynamic-dict")
                .type(DictionaryType.DYNAMIC)
                .state(Lifecycle.State.STOPPED)
                .createdAt(new Date())
                .updatedAt(new Date())
                .build();
            when(createOrUpdateDictionaryUseCase.execute(any())).thenReturn(new CreateOrUpdateDictionaryUseCase.Output(entity));

            try (
                var response = rootTarget()
                    .request()
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .put(Entity.json(readJSON("dynamic-dictionary.json")))
            ) {
                assertThat(response.getStatus()).isEqualTo(200);
                verify(createOrUpdateDictionaryUseCase).execute(any(CreateOrUpdateDictionaryUseCase.Input.class));
            }
        }
    }
}
