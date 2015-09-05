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
package io.gravitee.gateway.core.manager;

import io.gravitee.common.event.EventManager;
import io.gravitee.gateway.core.builder.ApiBuilder;
import io.gravitee.gateway.core.manager.impl.ApiManagerImpl;
import io.gravitee.gateway.core.model.Api;
import io.gravitee.gateway.core.policy.PolicyDefinition;
import io.gravitee.gateway.core.policy.PolicyManager;
import io.gravitee.repository.api.ApiRepository;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.model.PolicyConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Method;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class ApiManagerTest {

    private ApiManager apiManager;

    private ApiRepository apiRepository;
    private EventManager eventManager;
    private PolicyManager policyManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        apiRepository = mock(ApiRepository.class);
        eventManager = mock(EventManager.class);
        policyManager = mock(PolicyManager.class);

        apiManager = spy(new ApiManagerImpl());
        ((ApiManagerImpl)apiManager).setApiRepository(apiRepository);
        ((ApiManagerImpl)apiManager).setEventManager(eventManager);
        ((ApiManagerImpl)apiManager).setPolicyManager(policyManager);
    }

    @Test
    public void add_simpleApi() {
        Api api = new ApiBuilder().name("api-test").origin("http://localhost/team").target("http://localhost/target").build();
        apiManager.add(api);

        assertEquals(0, apiManager.apis().get(api.getName()).getPolicies().size());
        verify(eventManager, only()).publishEvent(ApiEvent.CREATE, api);
    }

    @Test
    public void add_apiWithPolicy_technicalException() throws TechnicalException {
        Api api = new ApiBuilder().name("api-test").origin("http://localhost/team").target("http://localhost/target").build();

        // Prepare policy
        when(apiRepository.findPoliciesByApi(api.getName())).thenThrow(TechnicalException.class);

        apiManager.add(api);

        verify(eventManager, never()).publishEvent(ApiEvent.CREATE, api);
    }

    @Test
    public void add_apiWithPolicy_notFound() throws TechnicalException {
        Api api = new ApiBuilder().name("api-test").origin("http://localhost/team").target("http://localhost/target").build();

        // Prepare policy
        PolicyConfiguration policy = new PolicyConfiguration();
        policy.setPolicy("my-policy");

        when(apiRepository.findPoliciesByApi(api.getName())).thenReturn(Collections.singletonList(policy));

        // Run tests
        apiManager.add(api);

        verify(eventManager, never()).publishEvent(ApiEvent.CREATE, api);
    }

    @Test
    public void add_apiWithPolicy() throws TechnicalException {
        Api api = new ApiBuilder().name("api-test").origin("http://localhost/team").target("http://localhost/target").build();

        // Prepare policy
        PolicyConfiguration policy = new PolicyConfiguration();
        policy.setPolicy("my-policy");

        when(apiRepository.findPoliciesByApi(api.getName())).thenReturn(Collections.singletonList(policy));
        when(policyManager.getPolicyDefinition("my-policy")).thenReturn(mock(PolicyDefinition.class));

        // Run tests
        apiManager.add(api);

        assertEquals(0, apiManager.apis().get(api.getName()).getPolicies().size());
        verify(eventManager, only()).publishEvent(ApiEvent.CREATE, api);
    }
}
