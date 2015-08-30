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
package io.gravitee.gateway.core.sync;

import io.gravitee.gateway.core.builder.RepositoryApiBuilder;
import io.gravitee.gateway.core.event.EventManager;
import io.gravitee.gateway.core.manager.ApiManager;
import io.gravitee.gateway.core.manager.impl.ApiManagerImpl;
import io.gravitee.gateway.core.model.ApiLifecycleState;
import io.gravitee.gateway.core.sync.impl.SyncManager;
import io.gravitee.repository.api.ApiRepository;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.model.Api;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.Mockito.*;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class SyncManagerTest {

    @Mock
    private ApiRepository apiRepository;

    private ApiManager apiManager;

    private SyncManager syncManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        apiManager = spy(new ApiManagerImpl());
        ((ApiManagerImpl)apiManager).setEventManager(mock(EventManager.class));
        doNothing().when((ApiManagerImpl) apiManager).enhance(any(io.gravitee.gateway.core.model.Api.class));

        syncManager = spy(new SyncManager());
        syncManager.setApiRepository(apiRepository);
        syncManager.setApiManager(apiManager);
    }

    @Test
    public void test_empty() throws TechnicalException {
        when(apiRepository.findAll()).thenReturn(Collections.<Api>emptySet());

        syncManager.refresh();

        verify(apiManager, never()).add(any(io.gravitee.gateway.core.model.Api.class));
        verify(apiManager, never()).update(any(io.gravitee.gateway.core.model.Api.class));
        verify(apiManager, never()).remove(any(String.class));
    }

    @Test
    public void test_newApi() throws TechnicalException {
        Api api = new RepositoryApiBuilder().name("api-test").updatedAt(new Date()).build();

        when(apiRepository.findAll()).thenReturn(Collections.singleton(api));

        syncManager.refresh();

        verify(apiManager).add(convert(api));
        verify(apiManager, never()).update(any(io.gravitee.gateway.core.model.Api.class));
        verify(apiManager, never()).remove(any(String.class));
    }

    @Test
    public void test_twiceWithSameApi() throws TechnicalException {
        Api api = new RepositoryApiBuilder().name("api-test").updatedAt(new Date()).build();

        when(apiRepository.findAll()).thenReturn(Collections.singleton(api));

        syncManager.refresh();

        syncManager.refresh();

        verify(apiManager).add(convert(api));
        verify(apiManager, never()).update(any(io.gravitee.gateway.core.model.Api.class));
        verify(apiManager, never()).remove(any(String.class));
    }

    @Test
    public void test_twiceWithTwoApis() throws TechnicalException {
        Api api = new RepositoryApiBuilder().name("api-test").updatedAt(new Date()).build();
        Api api2 = new RepositoryApiBuilder().name("api-test-2").updatedAt(new Date()).build();

        when(apiRepository.findAll()).thenReturn(Collections.singleton(api));

        syncManager.refresh();

        Set<Api> apis = new HashSet<>();
        apis.add(api);
        apis.add(api2);

        when(apiRepository.findAll()).thenReturn(apis);

        syncManager.refresh();

        verify(apiManager).add(convert(api));
        verify(apiManager).add(convert(api2));
        verify(apiManager, never()).update(any(io.gravitee.gateway.core.model.Api.class));
        verify(apiManager, never()).remove(any(String.class));
    }

    @Test
    public void test_twiceWithTwoApis_apiToRemove() throws TechnicalException {
        Api api = new RepositoryApiBuilder().name("api-test").updatedAt(new Date()).build();
        Api api2 = new RepositoryApiBuilder().name("api-test-2").updatedAt(new Date()).build();

        when(apiRepository.findAll()).thenReturn(Collections.singleton(api));

        syncManager.refresh();

        when(apiRepository.findAll()).thenReturn(Collections.singleton(api2));

        syncManager.refresh();

        verify(apiManager).add(convert(api));
        verify(apiManager).add(convert(api2));
        verify(apiManager, never()).update(any(io.gravitee.gateway.core.model.Api.class));
        verify(apiManager).remove(api.getName());
        verify(apiManager, never()).remove(api2.getName());
    }

    @Test
    public void test_twiceWithTwoApis_apiToUpdate() throws TechnicalException {
        Api api = new RepositoryApiBuilder().name("api-test").updatedAt(new Date()).build();

        Instant updateDateInst = api.getUpdatedAt().toInstant().plus(Duration.ofHours(1));
        Api api2 = new RepositoryApiBuilder().name("api-test").updatedAt(Date.from(updateDateInst)).build();

        when(apiRepository.findAll()).thenReturn(Collections.singleton(api));

        syncManager.refresh();

        when(apiRepository.findAll()).thenReturn(Collections.singleton(api2));

        syncManager.refresh();

        verify(apiManager).add(convert(api));
        verify(apiManager).update(convert(api));
        verify(apiManager, never()).remove(any(String.class));
    }

    @Test
    public void test_twiceWithTwoApis_api_noUpdate() throws TechnicalException {
        Api api = new RepositoryApiBuilder().name("api-test").updatedAt(new Date()).build();
        Api api2 = new RepositoryApiBuilder().name("api-test").updatedAt(api.getUpdatedAt()).build();

        when(apiRepository.findAll()).thenReturn(Collections.singleton(api));

        syncManager.refresh();

        when(apiRepository.findAll()).thenReturn(Collections.singleton(api2));

        syncManager.refresh();

        verify(apiManager).add(convert(api));
        verify(apiManager, never()).update(any(io.gravitee.gateway.core.model.Api.class));
        verify(apiManager, never()).remove(any(String.class));
    }

    @Test
    public void test_throwTechnicalException() throws TechnicalException {
        when(apiRepository.findAll()).thenThrow(TechnicalException.class);

        syncManager.refresh();

        verify(apiManager, never()).add(any(io.gravitee.gateway.core.model.Api.class));
        verify(apiManager, never()).update(any(io.gravitee.gateway.core.model.Api.class));
        verify(apiManager, never()).remove(any(String.class));
    }

    private io.gravitee.gateway.core.model.Api convert(io.gravitee.repository.model.Api remoteApi) {
        io.gravitee.gateway.core.model.Api api = new io.gravitee.gateway.core.model.Api();

        api.setName(remoteApi.getName());
        api.setPublicURI(remoteApi.getPublicURI());
        api.setTargetURI(remoteApi.getTargetURI());
        api.setCreatedAt(remoteApi.getCreatedAt());
        api.setUpdatedAt(remoteApi.getUpdatedAt());
        api.setState(ApiLifecycleState.STARTED);

        return api;
    }
}
