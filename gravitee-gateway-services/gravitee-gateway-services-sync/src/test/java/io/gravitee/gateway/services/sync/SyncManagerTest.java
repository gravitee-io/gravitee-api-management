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

import io.gravitee.common.event.EventManager;
import io.gravitee.gateway.core.definition.Api;
import io.gravitee.gateway.core.manager.ApiManager;
import io.gravitee.gateway.core.manager.impl.ApiManagerImpl;
import io.gravitee.gateway.services.sync.builder.RepositoryApiBuilder;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import org.junit.Before;
import org.junit.Ignore;
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
@Ignore
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

        syncManager = spy(new SyncManager());
        syncManager.setApiRepository(apiRepository);
        syncManager.setApiManager(apiManager);
    }

    @Test
    public void test_empty() throws TechnicalException {
        when(apiRepository.findAll()).thenReturn(Collections.<io.gravitee.repository.management.model.Api>emptySet());

        syncManager.refresh();

        verify(apiManager, never()).deploy(any(Api.class));
        verify(apiManager, never()).update(any(Api.class));
        verify(apiManager, never()).undeploy(any(String.class));
    }

    @Test
    public void test_newApi() throws TechnicalException {
        io.gravitee.repository.management.model.Api api = new RepositoryApiBuilder().name("api-test").updatedAt(new Date()).build();

        when(apiRepository.findAll()).thenReturn(Collections.singleton(api));

        syncManager.refresh();

        verify(apiManager).deploy(convert(api));
        verify(apiManager, never()).update(any(Api.class));
        verify(apiManager, never()).undeploy(any(String.class));
    }

    @Test
    public void test_twiceWithSameApi() throws TechnicalException {
        io.gravitee.repository.management.model.Api api = new RepositoryApiBuilder().name("api-test").updatedAt(new Date()).build();

        when(apiRepository.findAll()).thenReturn(Collections.singleton(api));

        syncManager.refresh();

        syncManager.refresh();

        verify(apiManager).deploy(convert(api));
        verify(apiManager, never()).update(any(Api.class));
        verify(apiManager, never()).undeploy(any(String.class));
    }

    @Test
    public void test_twiceWithTwoApis() throws TechnicalException {
        io.gravitee.repository.management.model.Api api = new RepositoryApiBuilder().name("api-test").updatedAt(new Date()).build();
        io.gravitee.repository.management.model.Api api2 = new RepositoryApiBuilder().name("api-test-2").updatedAt(new Date()).build();

        when(apiRepository.findAll()).thenReturn(Collections.singleton(api));

        syncManager.refresh();

        Set<io.gravitee.repository.management.model.Api> apis = new HashSet<>();
        apis.add(api);
        apis.add(api2);

        when(apiRepository.findAll()).thenReturn(apis);

        syncManager.refresh();

        verify(apiManager).deploy(convert(api));
        verify(apiManager).deploy(convert(api2));
        verify(apiManager, never()).update(any(Api.class));
        verify(apiManager, never()).undeploy(any(String.class));
    }

    @Test
    public void test_twiceWithTwoApis_apiToRemove() throws TechnicalException {
        io.gravitee.repository.management.model.Api api = new RepositoryApiBuilder().name("api-test").updatedAt(new Date()).build();
        io.gravitee.repository.management.model.Api api2 = new RepositoryApiBuilder().name("api-test-2").updatedAt(new Date()).build();

        when(apiRepository.findAll()).thenReturn(Collections.singleton(api));

        syncManager.refresh();

        when(apiRepository.findAll()).thenReturn(Collections.singleton(api2));

        syncManager.refresh();

        verify(apiManager).deploy(convert(api));
        verify(apiManager).deploy(convert(api2));
        verify(apiManager, never()).update(any(Api.class));
        verify(apiManager).undeploy(api.getName());
        verify(apiManager, never()).undeploy(api2.getName());
    }

    @Test
    public void test_twiceWithTwoApis_apiToUpdate() throws TechnicalException {
        io.gravitee.repository.management.model.Api api = new RepositoryApiBuilder().name("api-test").updatedAt(new Date()).build();

        Instant updateDateInst = api.getUpdatedAt().toInstant().plus(Duration.ofHours(1));
        io.gravitee.repository.management.model.Api api2 = new RepositoryApiBuilder().name("api-test").updatedAt(Date.from(updateDateInst)).build();

        when(apiRepository.findAll()).thenReturn(Collections.singleton(api));

        syncManager.refresh();

        when(apiRepository.findAll()).thenReturn(Collections.singleton(api2));

        syncManager.refresh();

        verify(apiManager).deploy(convert(api));
        verify(apiManager).update(convert(api));
        verify(apiManager, never()).undeploy(any(String.class));
    }

    @Test
    public void test_twiceWithTwoApis_api_noUpdate() throws TechnicalException {
        io.gravitee.repository.management.model.Api api = new RepositoryApiBuilder().name("api-test").updatedAt(new Date()).build();
        io.gravitee.repository.management.model.Api api2 = new RepositoryApiBuilder().name("api-test").updatedAt(api.getUpdatedAt()).build();

        when(apiRepository.findAll()).thenReturn(Collections.singleton(api));

        syncManager.refresh();

        when(apiRepository.findAll()).thenReturn(Collections.singleton(api2));

        syncManager.refresh();

        verify(apiManager).deploy(convert(api));
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

    private Api convert(io.gravitee.repository.management.model.Api remoteApi) {
        Api api = new Api();

        api.setName(remoteApi.getName());

        /*
        ProxyDefinition proxy = new ProxyDefinition();
        proxy.setContextPath(remoteApi.getPublicURI().getPath());
        proxy.setTarget(remoteApi.getTargetURI());
        proxy.setStripContextPath(false);
        */
        /*
        api.setCreatedAt(remoteApi.getCreatedAt());
        api.setUpdatedAt(remoteApi.getUpdatedAt());
        api.setState(ApiLifecycleState.STARTED);
        */

        return api;
    }
}
