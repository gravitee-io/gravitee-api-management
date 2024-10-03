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
package io.gravitee.gateway.services.sync.process.repository.mapper;

import static io.gravitee.repository.management.model.Event.EventProperties.API_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.ExecutionMode;
import io.gravitee.definition.model.Proxy;
import io.gravitee.definition.model.VirtualHost;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.nativeapi.NativeApi;
import io.gravitee.gateway.services.sync.process.repository.service.EnvironmentService;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.OrganizationRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Environment;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.LifecycleState;
import io.gravitee.repository.management.model.Organization;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiMapperTest {

    private final ObjectMapper objectMapper = new GraviteeMapper();

    @Mock
    private EnvironmentRepository environmentRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    private ApiMapper cut;
    private io.gravitee.definition.model.Api apiV2;
    private Api repoApiV2;
    private io.gravitee.definition.model.v4.Api apiV4;
    private Api repoApiV4;

    @BeforeEach
    public void beforeEach() throws JsonProcessingException {
        cut = new ApiMapper(objectMapper, new EnvironmentService(environmentRepository, organizationRepository));
        apiV2 = new io.gravitee.definition.model.Api();
        apiV2.setId("id");
        apiV2.setDefinitionVersion(DefinitionVersion.V2);
        Proxy proxy = new Proxy();
        proxy.setVirtualHosts(List.of(new VirtualHost("/test")));
        apiV2.setProxy(proxy);

        repoApiV2 = new Api();
        repoApiV2.setId("id");
        repoApiV2.setName("name");
        repoApiV2.setLifecycleState(LifecycleState.STARTED);
        repoApiV2.setEnvironmentId("env");
        repoApiV2.setDefinitionVersion(DefinitionVersion.V2);
        repoApiV2.setDefinition(objectMapper.writeValueAsString(apiV2));

        apiV4 = new io.gravitee.definition.model.v4.Api();
        apiV4.setId("id");
        apiV4.setDefinitionVersion(DefinitionVersion.V4);

        repoApiV4 = new Api();
        repoApiV4.setId("id");
        repoApiV4.setName("name");
        repoApiV4.setLifecycleState(LifecycleState.STARTED);
        repoApiV4.setEnvironmentId("env");
        repoApiV4.setDefinitionVersion(DefinitionVersion.V4);
        repoApiV4.setType(ApiType.PROXY);
        repoApiV4.setDefinition(objectMapper.writeValueAsString(apiV4));
    }

    @Test
    void should_map_api_v2_with_jupiter_to_v4_emulation_engine() throws JsonProcessingException {
        Event event = new Event();
        // Force old jupiter mode
        ObjectNode apiV2Def = (ObjectNode) objectMapper.readTree(repoApiV2.getDefinition());
        apiV2Def.put("execution_mode", "jupiter");
        repoApiV2.setDefinition(objectMapper.writeValueAsString(apiV2Def));
        String payload = objectMapper.writeValueAsString(repoApiV2);
        event.setPayload(payload);
        cut
            .to(event)
            .test()
            .assertValue(reactableApi -> {
                assertThat(reactableApi.getId()).isEqualTo(apiV2.getId());
                io.gravitee.definition.model.Api eventApi = (io.gravitee.definition.model.Api) reactableApi.getDefinition();
                assertThat(eventApi.getExecutionMode()).isEqualTo(ExecutionMode.V4_EMULATION_ENGINE);
                return true;
            })
            .assertComplete();
    }

    @Test
    void should_map_api_v2() throws JsonProcessingException {
        Event event = new Event();
        event.setPayload(objectMapper.writeValueAsString(repoApiV2));
        cut
            .to(event)
            .test()
            .assertValue(reactableApi -> {
                assertThat(reactableApi.getId()).isEqualTo(apiV2.getId());
                assertThat(reactableApi.getDefinition()).isEqualTo(apiV2);
                return true;
            })
            .assertComplete();
    }

    @Test
    void should_map_api_v4() throws JsonProcessingException {
        Event event = new Event();
        event.setPayload(objectMapper.writeValueAsString(repoApiV4));
        cut
            .to(event)
            .test()
            .assertValue(reactableApi -> {
                assertThat(reactableApi.getId()).isEqualTo(apiV4.getId());
                assertThat(reactableApi.getDefinition()).isEqualTo(apiV4);
                return true;
            })
            .assertComplete();
    }

    @Test
    void should_map_native_api_v4() throws JsonProcessingException {
        NativeApi nativeApi = new NativeApi();
        nativeApi.setId("id");
        nativeApi.setType(ApiType.NATIVE);
        nativeApi.setDefinitionVersion(DefinitionVersion.V4);

        repoApiV4.setType(ApiType.NATIVE);
        repoApiV4.setDefinition(objectMapper.writeValueAsString(nativeApi));

        Event event = new Event();
        event.setPayload(objectMapper.writeValueAsString(repoApiV4));
        cut
            .to(event)
            .test()
            .assertValue(reactableApi -> {
                assertThat(reactableApi.getId()).isEqualTo(nativeApi.getId());
                assertThat(reactableApi.getDefinition()).isEqualTo(nativeApi);
                return true;
            })
            .assertComplete();
    }

    @Test
    void should_map_api_with_env_and_orga() throws JsonProcessingException, TechnicalException {
        Organization organization = new Organization();
        organization.setId("orga");
        when(organizationRepository.findById(organization.getId())).thenReturn(Optional.of(organization));
        Environment environment = new Environment();
        environment.setOrganizationId(organization.getId());
        when(environmentRepository.findById(repoApiV4.getEnvironmentId())).thenReturn(Optional.of(environment));
        Event event = new Event();
        event.setPayload(objectMapper.writeValueAsString(repoApiV4));
        cut
            .to(event)
            .test()
            .assertValue(reactableApi -> {
                assertThat(reactableApi.getId()).isEqualTo(apiV4.getId());
                assertThat(reactableApi.getDefinition()).isEqualTo(apiV4);
                assertThat(reactableApi.getOrganizationId()).isEqualTo(organization.getId());
                assertThat(reactableApi.getEnvironmentId()).isEqualTo(environment.getId());
                return true;
            })
            .assertComplete();
    }

    @Test
    void should_map_api_with_env_but_ignore_not_found_orga() throws JsonProcessingException, TechnicalException {
        Environment environment = new Environment();
        environment.setOrganizationId("not found");
        when(environmentRepository.findById(repoApiV4.getEnvironmentId())).thenReturn(Optional.of(environment));
        Event event = new Event();
        event.setPayload(objectMapper.writeValueAsString(repoApiV4));
        cut
            .to(event)
            .test()
            .assertValue(reactableApi -> {
                assertThat(reactableApi.getId()).isEqualTo(apiV4.getId());
                assertThat(reactableApi.getDefinition()).isEqualTo(apiV4);
                assertThat(reactableApi.getOrganizationId()).isNull();
                assertThat(reactableApi.getEnvironmentId()).isEqualTo(environment.getId());
                return true;
            })
            .assertComplete();
    }

    @Test
    void should_return_empty_with_mapping_error() throws JsonProcessingException {
        Event event = new Event();
        event.setPayload(objectMapper.writeValueAsString("wrong"));
        cut.to(event).test().assertNoValues().assertComplete();
    }

    @Test
    void should_map_event_to_id() {
        Event event = new Event();
        event.setProperties(Map.of(API_ID.getValue(), "id"));
        cut
            .toId(event)
            .test()
            .assertValue(id -> {
                assertThat(id).isEqualTo("id");
                return true;
            })
            .assertComplete();
    }

    @Test
    void should_map_event_to_empty_id() {
        Event event = new Event();
        cut.toId(event).test().assertNoValues().assertComplete();
    }
}
