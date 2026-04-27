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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.dictionary.domain_service.DictionaryAutomationDomainService;
import io.gravitee.apim.core.dictionary.use_case.DeleteDictionaryUseCase;
import io.gravitee.apim.rest.api.automation.model.DictionaryState;
import io.gravitee.apim.rest.api.automation.resource.base.AbstractResourceTest;
import io.gravitee.common.component.Lifecycle;
import io.gravitee.rest.api.model.configuration.dictionary.DictionaryEntity;
import io.gravitee.rest.api.model.configuration.dictionary.DictionaryProviderEntity;
import io.gravitee.rest.api.model.configuration.dictionary.DictionaryTriggerEntity;
import io.gravitee.rest.api.model.configuration.dictionary.DictionaryType;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.HRIDToUUID;
import io.gravitee.rest.api.service.impl.configuration.dictionary.DictionaryNotFoundException;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DictionaryResourceTest extends AbstractResourceTest {

    @Inject
    private DictionaryAutomationDomainService dictionaryAutomationDomainService;

    @Inject
    private DeleteDictionaryUseCase deleteDictionaryUseCase;

    @AfterEach
    void tearDown() {
        reset(dictionaryAutomationDomainService, deleteDictionaryUseCase);
    }

    @Override
    protected String contextPath() {
        return "/organizations/" + ORGANIZATION + "/environments/" + ENVIRONMENT + "/dictionaries";
    }

    @Nested
    class GetByHrid {

        @Test
        void should_return_dictionary_state() {
            var id = HRIDToUUID.dictionary().context(new ExecutionContext(ORGANIZATION, ENVIRONMENT)).hrid("my-dict").id();
            var entity = DictionaryEntity.builder()
                .id(id)
                .name("My Dictionary")
                .key("my-dict")
                .type(DictionaryType.MANUAL)
                .state(Lifecycle.State.STOPPED)
                .properties(Map.of("key1", "value1"))
                .createdAt(new Date())
                .updatedAt(new Date())
                .deployedAt(new Date())
                .build();
            when(dictionaryAutomationDomainService.findById(any(), eq(id))).thenReturn(Optional.of(entity));

            try (var response = rootTarget("my-dict").request().accept(MediaType.APPLICATION_JSON_TYPE).get()) {
                assertThat(response.getStatus()).isEqualTo(200);
                var state = response.readEntity(DictionaryState.class);
                SoftAssertions.assertSoftly(soft -> {
                    soft.assertThat(state.getId()).isEqualTo(id);
                    soft.assertThat(state.getOrganizationId()).isEqualTo(ORGANIZATION);
                    soft.assertThat(state.getEnvironmentId()).isEqualTo(ENVIRONMENT);
                    soft.assertThat(state.getHrid()).isEqualTo("my-dict");
                    soft.assertThat(state.getName()).isEqualTo("My Dictionary");
                    soft.assertThat(state.getDeployed()).isTrue();
                    soft.assertThat(state.getManual()).isNotNull();
                    soft.assertThat(state.getManual().getProperties()).isNotNull();
                    soft.assertThat(state.getManual().getProperties()).containsEntry("key1", "value1");
                });
            }
        }

        @Test
        void should_strip_dynamic_fields_for_readonly_user() {
            var trigger = new DictionaryTriggerEntity();
            trigger.setRate(5L);
            trigger.setUnit(TimeUnit.SECONDS);
            var provider = new DictionaryProviderEntity();
            provider.setType("HTTP");
            provider.setConfiguration(new ObjectMapper().createObjectNode().put("url", "http://example.com").put("method", "GET"));

            var entity = DictionaryEntity.builder()
                .id("dyn-id")
                .name("Dynamic Dict")
                .key("dyn-dict")
                .type(DictionaryType.DYNAMIC)
                .state(Lifecycle.State.STARTED)
                .provider(provider)
                .trigger(trigger)
                .createdAt(new Date())
                .updatedAt(new Date())
                .build();
            when(dictionaryAutomationDomainService.findById(any(), any())).thenReturn(Optional.of(entity));
            when(
                permissionService.hasPermission(
                    any(),
                    eq(RolePermission.ENVIRONMENT_DICTIONARY),
                    any(),
                    eq(RolePermissionAction.CREATE),
                    eq(RolePermissionAction.UPDATE),
                    eq(RolePermissionAction.DELETE)
                )
            ).thenReturn(false);

            try (var response = rootTarget("dyn-dict").request().accept(MediaType.APPLICATION_JSON_TYPE).get()) {
                assertThat(response.getStatus()).isEqualTo(200);
                var state = response.readEntity(DictionaryState.class);
                SoftAssertions.assertSoftly(soft -> {
                    soft.assertThat(state.getId()).isEqualTo("dyn-id");
                    soft.assertThat(state.getDynamic()).isNull();
                });
            }
        }

        @Test
        void should_return_404_when_not_found() {
            when(dictionaryAutomationDomainService.findById(any(), any())).thenReturn(Optional.empty());

            try (var response = rootTarget("unknown").request().accept(MediaType.APPLICATION_JSON_TYPE).get()) {
                assertThat(response.getStatus()).isEqualTo(404);
            }
        }
    }

    @Nested
    class DeleteByHrid {

        @Test
        void should_delete_dictionary() {
            var id = HRIDToUUID.dictionary().context(new ExecutionContext(ORGANIZATION, ENVIRONMENT)).hrid("my-dict").id();
            var entity = DictionaryEntity.builder()
                .id("dict-id")
                .name("My Dictionary")
                .key("my-dict")
                .type(DictionaryType.MANUAL)
                .state(Lifecycle.State.STOPPED)
                .createdAt(new Date())
                .updatedAt(new Date())
                .build();
            when(dictionaryAutomationDomainService.findById(any(), eq(id))).thenReturn(Optional.of(entity));

            try (var response = rootTarget("my-dict").request().delete()) {
                assertThat(response.getStatus()).isEqualTo(204);
                verify(deleteDictionaryUseCase).execute(any(DeleteDictionaryUseCase.Input.class));
            }
        }

        @Test
        void should_return_404_when_not_found() {
            Mockito.doThrow(new DictionaryNotFoundException("unknown")).when(deleteDictionaryUseCase).execute(any());

            try (var response = rootTarget("unknown").request().delete()) {
                assertThat(response.getStatus()).isEqualTo(404);
            }
        }
    }
}
