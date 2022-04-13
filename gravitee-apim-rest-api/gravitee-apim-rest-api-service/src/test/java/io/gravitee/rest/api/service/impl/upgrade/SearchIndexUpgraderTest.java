/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.service.impl.upgrade;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Environment;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.service.PageService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.converter.ApiConverter;
import io.gravitee.rest.api.service.search.SearchEngineService;
import java.util.Optional;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SearchIndexUpgraderTest {

    @InjectMocks
    private SearchIndexUpgrader upgrader;

    @Mock
    private ApiRepository apiRepository;

    @Mock
    private PageService pageService;

    @Mock
    private UserService userService;

    @Mock
    private SearchEngineService searchEngineService;

    @Mock
    private EnvironmentRepository environmentRepository;

    @Mock
    private ApiConverter apiConverter;

    @Before
    public void setup() throws Exception {
        mockEnvironment("env1", "org1");
        mockEnvironment("env2", "org2");
        mockEnvironment("env3", "org1");
    }

    @Test
    public void upgrade_should_retrieve_environment_of_each_api() throws Exception {
        Set<Api> apis = mockTestApis();
        when(apiRepository.findAll()).thenReturn(apis);

        upgrader.upgrade(GraviteeContext.getExecutionContext());

        verify(environmentRepository, times(1)).findById("env1");
        verify(environmentRepository, times(1)).findById("env2");
        verify(environmentRepository, times(1)).findById("env3");
        verifyNoMoreInteractions(environmentRepository);
    }

    @Test
    public void upgrade_should_index_each_api() throws Exception {
        Set<Api> apis = mockTestApis();
        when(apiRepository.findAll()).thenReturn(apis);

        upgrader.upgrade(GraviteeContext.getExecutionContext());

        verify(searchEngineService, times(1))
            .index(
                argThat(e -> e.getEnvironmentId().equals("env1") && e.getOrganizationId().equals("org1")),
                argThat(api -> api.getId().equals("api1")),
                eq(true),
                eq(false)
            );
        verify(searchEngineService, times(1))
            .index(
                argThat(e -> e.getEnvironmentId().equals("env2") && e.getOrganizationId().equals("org2")),
                argThat(api -> api.getId().equals("api2")),
                eq(true),
                eq(false)
            );
        verify(searchEngineService, times(1))
            .index(
                argThat(e -> e.getEnvironmentId().equals("env1") && e.getOrganizationId().equals("org1")),
                argThat(api -> api.getId().equals("api3")),
                eq(true),
                eq(false)
            );
        verify(searchEngineService, times(1))
            .index(
                argThat(e -> e.getEnvironmentId().equals("env3") && e.getOrganizationId().equals("org1")),
                argThat(api -> api.getId().equals("api4")),
                eq(true),
                eq(false)
            );
    }

    private Set<Api> mockTestApis() {
        return Set.of(mockTestApi("api1", "env1"), mockTestApi("api2", "env2"), mockTestApi("api3", "env1"), mockTestApi("api4", "env3"));
    }

    private Api mockTestApi(String apiId, String environmentId) {
        Api api = new Api();
        api.setId(apiId);
        api.setEnvironmentId(environmentId);

        ApiEntity apiEntity = new ApiEntity();
        apiEntity.setId(apiId);
        when(apiConverter.toApiEntity(api)).thenReturn(apiEntity);

        return api;
    }

    private void mockEnvironment(String envId, String orgId) throws Exception {
        Environment environment1 = new Environment();
        environment1.setId(envId);
        environment1.setOrganizationId(orgId);
        when(environmentRepository.findById(envId)).thenReturn(Optional.of(environment1));
    }
}
