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
package io.gravitee.rest.api.service.impl.upgrade.upgrader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Environment;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class ApiEndpointWeightUpgraderTest {

    @Mock
    private ApiRepository apiRepository;

    @Mock
    private EnvironmentRepository environmentRepository;

    private ObjectMapper objectMapper;

    @InjectMocks
    private ApiEndpointWeightUpgrader upgrader;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
        // Inject ObjectMapper manually since it's not a mock
        org.springframework.test.util.ReflectionTestUtils.setField(upgrader, "objectMapper", objectMapper);
    }

    @Test
    void shouldUpgradeV2ApiWeight() throws Exception {
        // Mock environment
        Environment env = new Environment();
        env.setId("env1");
        when(environmentRepository.findAll()).thenReturn(Set.of(env));

        // Mock V2 API
        Api api = new Api();
        api.setId("api-v2");
        api.setDefinitionVersion(DefinitionVersion.V2);
        api.setDefinition("{\"proxy\":{\"groups\":[{\"endpoints\":[{\"name\":\"ep\",\"weight\":0}]}]}}");

        when(apiRepository.search(any(), any(), any())).thenReturn(Stream.of(api));

        // Run upgrader
        upgrader.upgrade();

        // Capture updated API
        ArgumentCaptor<Api> captor = ArgumentCaptor.forClass(Api.class);
        verify(apiRepository).update(captor.capture());

        Api updatedApi = captor.getValue();
        JsonNode rootNode = objectMapper.readTree(updatedApi.getDefinition());
        int weight = rootNode.at("/proxy/groups/0/endpoints/0/weight").intValue();

        assertEquals(1, weight, "V2 endpoint weight should be updated to 1");
    }

    @Test
    void shouldUpgradeV4ApiWeight() throws Exception {
        // Mock environment
        Environment env = new Environment();
        env.setId("env1");
        when(environmentRepository.findAll()).thenReturn(Set.of(env));

        // Mock V4 API
        Api api = new Api();
        api.setId("api-v4");
        api.setDefinitionVersion(DefinitionVersion.V4);
        api.setDefinition("{\"endpointGroups\":[{\"endpoints\":[{\"name\":\"ep\",\"weight\":0}]}]}");

        when(apiRepository.search(any(), any(), any())).thenReturn(Stream.of(api));

        // Run upgrader
        upgrader.upgrade();

        // Capture updated API
        ArgumentCaptor<Api> captor = ArgumentCaptor.forClass(Api.class);
        verify(apiRepository).update(captor.capture());

        Api updatedApi = captor.getValue();
        JsonNode rootNode = objectMapper.readTree(updatedApi.getDefinition());
        int weight = rootNode.at("/endpointGroups/0/endpoints/0/weight").intValue();

        assertEquals(1, weight, "V4 endpoint weight should be updated to 1");
    }
}
