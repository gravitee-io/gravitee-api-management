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
package io.gravitee.apim.infra.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import fixtures.repository.ApiFixtures;
import io.gravitee.apim.infra.plugin.ManagementApiServicesManager;
import io.gravitee.common.event.EventManager;
import io.gravitee.common.event.impl.EventManagerImpl;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.service.event.ApiEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class ApiEventListenerTest {

    @Mock
    private ManagementApiServicesManager managementApiServicesManager;

    private EventManager eventManager;

    @BeforeEach
    void setUp() {
        eventManager = new EventManagerImpl();
        // just need to instantiate the class as subscription to listener is done at instantiation time.
        new ApiEventListener(eventManager, managementApiServicesManager);
    }

    @ParameterizedTest
    @EnumSource(value = DefinitionVersion.class, names = { "V1", "V2" })
    void should_do_nothing_on_not_v4_api_deploy(DefinitionVersion definitionVersion) {
        final Api eventApi = ApiFixtures.anApi().withDefinitionVersion(definitionVersion);
        eventManager.publishEvent(ApiEvent.DEPLOY, eventApi);
        verify(managementApiServicesManager, never()).deployServices(any());
    }

    @Test
    void should_do_actions_on_v4_api_deploy() {
        final Api eventApi = ApiFixtures.anApi();
        eventManager.publishEvent(ApiEvent.DEPLOY, eventApi);
        final ArgumentCaptor<io.gravitee.apim.core.api.model.Api> coreApiCaptor = ArgumentCaptor.forClass(
            io.gravitee.apim.core.api.model.Api.class
        );
        verify(managementApiServicesManager).deployServices(coreApiCaptor.capture());

        assertThat(coreApiCaptor.getValue()).satisfies(api -> {
            assertThat(api.getId()).isEqualTo(eventApi.getId());
            assertThat(api.getName()).isEqualTo(eventApi.getName());
        });
    }

    @ParameterizedTest
    @EnumSource(value = DefinitionVersion.class, names = { "V1", "V2" })
    void should_do_nothing_on_not_v4_api_undeploy(DefinitionVersion definitionVersion) {
        final Api eventApi = ApiFixtures.anApi().withDefinitionVersion(definitionVersion);
        eventManager.publishEvent(ApiEvent.UNDEPLOY, eventApi);
        verify(managementApiServicesManager, never()).undeployServices(any());
    }

    @Test
    void should_do_actions_on_v4_api_undeploy() {
        final Api eventApi = ApiFixtures.anApi();
        eventManager.publishEvent(ApiEvent.UNDEPLOY, eventApi);
        final ArgumentCaptor<io.gravitee.apim.core.api.model.Api> coreApiCaptor = ArgumentCaptor.forClass(
            io.gravitee.apim.core.api.model.Api.class
        );
        verify(managementApiServicesManager).undeployServices(coreApiCaptor.capture());

        assertThat(coreApiCaptor.getValue()).satisfies(api -> {
            assertThat(api.getId()).isEqualTo(eventApi.getId());
            assertThat(api.getName()).isEqualTo(eventApi.getName());
        });
    }

    @ParameterizedTest
    @EnumSource(value = DefinitionVersion.class, names = { "V1", "V2" })
    void should_do_nothing_on_not_v4_api_update(DefinitionVersion definitionVersion) {
        final Api eventApi = ApiFixtures.anApi().withDefinitionVersion(definitionVersion);
        eventManager.publishEvent(ApiEvent.UPDATE, eventApi);
        verify(managementApiServicesManager, never()).updateServices(any());
    }

    @Test
    void should_do_actions_on_v4_api_update() {
        final Api eventApi = ApiFixtures.anApi();
        eventManager.publishEvent(ApiEvent.UPDATE, eventApi);
        final ArgumentCaptor<io.gravitee.apim.core.api.model.Api> coreApiCaptor = ArgumentCaptor.forClass(
            io.gravitee.apim.core.api.model.Api.class
        );
        verify(managementApiServicesManager).updateServices(coreApiCaptor.capture());

        assertThat(coreApiCaptor.getValue()).satisfies(api -> {
            assertThat(api.getId()).isEqualTo(eventApi.getId());
            assertThat(api.getName()).isEqualTo(eventApi.getName());
        });
    }
}
