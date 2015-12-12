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
package io.gravitee.gateway.services.sync;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.core.definition.Api;
import io.gravitee.gateway.core.manager.ApiManager;
import io.gravitee.gateway.services.sync.builder.RepositoryApiBuilder;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.mockito.Mockito.*;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
@RunWith(MockitoJUnitRunner.class)
public class SyncManagerTest {

    @InjectMocks
    private SyncManager syncManager = new SyncManager();

    @Mock
    private ApiRepository apiRepository;

    @Mock
    private ApiManager apiManager;

    @Mock
    private ObjectMapper objectMapper;

    @Test
    public void test_empty() throws TechnicalException {
        when(apiRepository.findAll()).thenReturn(Collections.emptySet());

        syncManager.refresh();

        verify(apiManager, never()).deploy(any(Api.class));
        verify(apiManager, never()).update(any(Api.class));
        verify(apiManager, never()).undeploy(any(String.class));
    }

    @Test
    public void test_newApi() throws Exception {
        io.gravitee.repository.management.model.Api api =
                new RepositoryApiBuilder().id("api-test").updatedAt(new Date()).definition("test").build();

        final Api mockApi = mockApi(api);

        when(apiRepository.findAll()).thenReturn(Collections.singleton(api));

        syncManager.refresh();

        verify(apiManager).deploy(mockApi);
        verify(apiManager, never()).update(any(Api.class));
        verify(apiManager, never()).undeploy(any(String.class));
    }

    @Test
    public void test_twiceWithSameApi() throws Exception {
        io.gravitee.repository.management.model.Api api =
                new RepositoryApiBuilder().id("api-test").updatedAt(new Date()).definition("test").build();

        final Api mockApi = mockApi(api);

        when(apiRepository.findAll()).thenReturn(Collections.singleton(api));
        when(apiManager.get(api.getId())).thenReturn(null);

        syncManager.refresh();

        when(apiManager.get(api.getId())).thenReturn(mockApi);

        syncManager.refresh();

        verify(apiManager).deploy(mockApi);
        verify(apiManager, never()).update(any(Api.class));
        verify(apiManager, never()).undeploy(any(String.class));
    }

    @Test
    public void test_twiceWithTwoApis() throws Exception {
        io.gravitee.repository.management.model.Api api =
                new RepositoryApiBuilder().id("api-test").updatedAt(new Date()).definition("test").build();
        io.gravitee.repository.management.model.Api api2 =
                new RepositoryApiBuilder().id("api-test-2").updatedAt(new Date()).definition("test2").build();

        final Api mockApi = mockApi(api);

        final Api mockApi2 = mockApi(api2);

        when(apiRepository.findAll()).thenReturn(Collections.singleton(api));

        syncManager.refresh();

        Set<io.gravitee.repository.management.model.Api> apis = new HashSet<>();
        apis.add(api);
        apis.add(api2);

        when(apiRepository.findAll()).thenReturn(apis);

        syncManager.refresh();

        verify(apiManager, times(2)).deploy(argThat(new ArgumentMatcher<Api>() {
            @Override
            public boolean matches(Object argument) {
                final Api api = (Api) argument;
                return api.getId().equals(mockApi.getId()) || api2.getId().equals(mockApi2.getId());
            }
        }));
        verify(apiManager, never()).update(any(Api.class));
        verify(apiManager, never()).undeploy(any(String.class));
    }

    @Test
    public void test_twiceWithTwoApis_apiToRemove() throws Exception {
        io.gravitee.repository.management.model.Api api =
                new RepositoryApiBuilder().id("api-test").updatedAt(new Date()).definition("test").build();
        io.gravitee.repository.management.model.Api api2 =
                new RepositoryApiBuilder().id("api-test-2").updatedAt(new Date()).definition("test2").build();

        final Api mockApi = mockApi(api);

        final Api mockApi2 = mockApi(api2);

        when(apiRepository.findAll()).thenReturn(Collections.singleton(api));

        syncManager.refresh();

        when(apiRepository.findAll()).thenReturn(Collections.singleton(api2));
        when(apiManager.apis()).thenReturn(Collections.singleton(mockApi));

        syncManager.refresh();

        verify(apiManager, times(2)).deploy(argThat(new ArgumentMatcher<Api>() {
            @Override
            public boolean matches(Object argument) {
                final Api api = (Api) argument;
                return api.getId().equals(mockApi.getId()) || api2.getId().equals(mockApi2.getId());
            }
        }));
        verify(apiManager, never()).update(any(Api.class));
        verify(apiManager).undeploy(api.getId());
        verify(apiManager, never()).undeploy(api2.getId());
    }

    @Test
    public void test_twiceWithTwoApis_apiToUpdate() throws Exception {
        io.gravitee.repository.management.model.Api api =
                new RepositoryApiBuilder().id("api-test").updatedAt(new Date()).definition("test").build();

        Instant updateDateInst = api.getUpdatedAt().toInstant().plus(Duration.ofHours(1));
        io.gravitee.repository.management.model.Api api2 =
                new RepositoryApiBuilder().id("api-test").updatedAt(Date.from(updateDateInst)).definition("test2").build();

        final Api mockApi = mockApi(api);
        mockApi(api2);

        when(apiRepository.findAll()).thenReturn(Collections.singleton(api));

        syncManager.refresh();

        when(apiRepository.findAll()).thenReturn(Collections.singleton(api2));
        when(apiManager.get(api.getId())).thenReturn(mockApi);

        syncManager.refresh();

        verify(apiManager).deploy(mockApi);
        verify(apiManager).update(mockApi);
        verify(apiManager, never()).undeploy(any(String.class));
    }

    @Test
    public void test_twiceWithTwoApis_api_noUpdate() throws Exception {
        io.gravitee.repository.management.model.Api api =
                new RepositoryApiBuilder().id("api-test").updatedAt(new Date()).definition("test").build();
        io.gravitee.repository.management.model.Api api2 =
                new RepositoryApiBuilder().id("api-test").updatedAt(api.getUpdatedAt()).definition("test").build();

        final Api mockApi = mockApi(api);

        when(apiRepository.findAll()).thenReturn(Collections.singleton(api));

        syncManager.refresh();

        when(apiRepository.findAll()).thenReturn(Collections.singleton(api2));

        syncManager.refresh();

        verify(apiManager, times(2)).deploy(mockApi);
        verify(apiManager, never()).update(any(Api.class));
        verify(apiManager, never()).undeploy(any(String.class));
    }

    @Test
    public void test_throwTechnicalException() throws TechnicalException {
        when(apiRepository.findAll()).thenThrow(TechnicalException.class);

        syncManager.refresh();

        verify(apiManager, never()).deploy(any(Api.class));
        verify(apiManager, never()).update(any(Api.class));
        verify(apiManager, never()).undeploy(any(String.class));
    }

    @Test
    public void test_deployApiWithTag() throws Exception {
        shouldDeployApiWithTags(new String[]{"test"});
    }

    @Test
    public void test_deployApiWithUpperCasedTag() throws Exception {
        shouldDeployApiWithTags(new String[]{"Test"});
    }

    @Test
    public void test_deployApiWithAccentTag() throws Exception {
        shouldDeployApiWithTags(new String[]{"tést"});
    }

    @Test
    public void test_deployApiWithUpperCasedAndAccentTag() throws Exception {
        shouldDeployApiWithTags(new String[]{"Tést"});
    }

    public void shouldDeployApiWithTags(final String[] apiTags) throws Exception {
        System.setProperty(SyncManager.TAGS_PROP, "test,toto");

        io.gravitee.repository.management.model.Api api =
                new RepositoryApiBuilder().id("api-test").updatedAt(new Date()).definition("test").build();

        final Api mockApi = mockApi(api, apiTags);

        when(apiRepository.findAll()).thenReturn(Collections.singleton(api));

        syncManager.refresh();

        verify(apiManager).deploy(mockApi);
        verify(apiManager, never()).update(any(Api.class));
        verify(apiManager, never()).undeploy(any(String.class));
        System.clearProperty(SyncManager.TAGS_PROP);
    }

    @Test
    public void test_not_deployApiWithoutTag() throws Exception {
        System.setProperty(SyncManager.TAGS_PROP, "test,toto");

        io.gravitee.repository.management.model.Api api =
                new RepositoryApiBuilder().id("api-test").updatedAt(new Date()).definition("test").build();

        final Api mockApi = mockApi(api);

        when(apiRepository.findAll()).thenReturn(Collections.singleton(api));

        syncManager.refresh();

        verify(apiManager, never()).deploy(mockApi);
        verify(apiManager, never()).update(any(Api.class));
        verify(apiManager, never()).undeploy(any(String.class));
        System.clearProperty(SyncManager.TAGS_PROP);
    }

    private Api mockApi(final io.gravitee.repository.management.model.Api api, final String[] tags) throws Exception {
        final Api mockApi = mockApi(api);
        mockApi.setTags(new HashSet<>(Arrays.asList(tags)));
        return mockApi;
    }

    private Api mockApi(final io.gravitee.repository.management.model.Api api) throws Exception {
        final Api mockApi = new Api();
        mockApi.setId(api.getId());
        mockApi.setDeployedAt(api.getUpdatedAt());
        when(objectMapper.readValue(api.getDefinition(), Api.class)).thenReturn(mockApi);
        return mockApi;
    }
}
