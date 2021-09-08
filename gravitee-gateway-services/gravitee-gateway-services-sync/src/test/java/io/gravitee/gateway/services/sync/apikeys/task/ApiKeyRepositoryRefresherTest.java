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
package io.gravitee.gateway.services.sync.apikeys.task;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.gravitee.definition.model.Plan;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.node.api.cluster.ClusterManager;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.api.search.ApiKeyCriteria;
import io.gravitee.repository.management.model.ApiKey;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiKeyRepositoryRefresherTest {

    private ApiKeyRefresher refresher;

    @Mock
    private ClusterManager clusterManager;

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @Mock
    private Map<String, ApiKey> cache;

    @Mock
    private Api api;

    @Mock
    private Plan plan;

    @Before
    public void setUp() {
        refresher = new ApiKeyRefresher(api);
        refresher.setCache(cache);
        refresher.setApiKeyRepository(apiKeyRepository);
        refresher.setClusterManager(clusterManager);
        when(clusterManager.isMasterNode()).thenReturn(true);
    }

    @Test
    public void shouldNotSynchronize_notMasterNode() throws TechnicalException {
        when(clusterManager.isMasterNode()).thenReturn(false);
        refresher.setDistributed(true);

        when(plan.getSecurity()).thenReturn(io.gravitee.repository.management.model.Plan.PlanSecurityType.API_KEY.name());
        List<Plan> plans = Collections.singletonList(plan);
        when(api.getPlans()).thenReturn(plans);

        refresher.initialize();
        refresher.run();

        verify(apiKeyRepository, never()).findByCriteria(any());
        assertEquals(0, refresher.getCount());
    }

    @Test
    public void shouldSynchronize_notMasterNode_notDistributed() throws TechnicalException {
        when(clusterManager.isMasterNode()).thenReturn(false);
        refresher.setDistributed(false);

        when(plan.getSecurity()).thenReturn(io.gravitee.repository.management.model.Plan.PlanSecurityType.API_KEY.name());
        List<Plan> plans = Collections.singletonList(plan);
        when(api.getPlans()).thenReturn(plans);

        refresher.initialize();
        refresher.run();

        verify(apiKeyRepository)
            .findByCriteria(
                argThat(
                    criteria ->
                        !criteria.isIncludeRevoked() && criteria.getFrom() == 0 && criteria.getTo() == 0 && criteria.getPlans().size() == 1
                )
            );
        assertEquals(1, refresher.getCount());
        assertEquals(0, refresher.getErrorsCount());
    }

    @Test
    public void shouldCountError_technicalException() throws TechnicalException {
        when(plan.getSecurity()).thenReturn(io.gravitee.repository.management.model.Plan.PlanSecurityType.API_KEY.name());
        List<Plan> plans = Collections.singletonList(plan);
        when(api.getPlans()).thenReturn(plans);

        when(apiKeyRepository.findByCriteria(any())).thenThrow(TechnicalException.class);

        refresher.initialize();
        refresher.run();

        verify(apiKeyRepository)
            .findByCriteria(
                argThat(
                    criteria ->
                        !criteria.isIncludeRevoked() && criteria.getFrom() == 0 && criteria.getTo() == 0 && criteria.getPlans().size() == 1
                )
            );

        assertEquals(1, refresher.getCount());
        assertEquals(1, refresher.getErrorsCount());
    }

    @Test
    public void shouldInitWithNonRevokedApiKey() throws TechnicalException {
        when(plan.getSecurity()).thenReturn(io.gravitee.repository.management.model.Plan.PlanSecurityType.API_KEY.name());
        List<Plan> plans = Collections.singletonList(plan);
        when(api.getPlans()).thenReturn(plans);

        refresher.initialize();
        refresher.run();

        verify(apiKeyRepository)
            .findByCriteria(
                argThat(
                    criteria ->
                        !criteria.isIncludeRevoked() && criteria.getFrom() == 0 && criteria.getTo() == 0 && criteria.getPlans().size() == 1
                )
            );
    }

    @Test
    public void shouldInitWithNonRevokedApiKey_lowerCase() throws TechnicalException {
        when(plan.getSecurity()).thenReturn(io.gravitee.repository.management.model.Plan.PlanSecurityType.API_KEY.name().toLowerCase());
        List<Plan> plans = Collections.singletonList(plan);
        when(api.getPlans()).thenReturn(plans);

        refresher.initialize();
        refresher.run();

        verify(apiKeyRepository)
            .findByCriteria(
                argThat(
                    criteria ->
                        !criteria.isIncludeRevoked() && criteria.getFrom() == 0 && criteria.getTo() == 0 && criteria.getPlans().size() == 1
                )
            );
    }

    @Test
    public void shouldNotRefreshKeylessPlan() {
        when(plan.getSecurity()).thenReturn(io.gravitee.repository.management.model.Plan.PlanSecurityType.KEY_LESS.name());
        List<Plan> plans = Collections.singletonList(plan);
        when(api.getPlans()).thenReturn(plans);

        refresher.initialize();
        refresher.run();

        Mockito.verifyNoInteractions(apiKeyRepository);
    }

    @Test
    public void shouldRefreshWithRevokedApiKey() throws TechnicalException {
        when(plan.getSecurity()).thenReturn(io.gravitee.repository.management.model.Plan.PlanSecurityType.API_KEY.name());
        List<Plan> plans = Collections.singletonList(plan);
        when(api.getPlans()).thenReturn(plans);

        refresher.initialize();
        refresher.run();
        refresher.run();

        InOrder inOrder = Mockito.inOrder(apiKeyRepository, apiKeyRepository);

        inOrder
            .verify(apiKeyRepository)
            .findByCriteria(
                argThat(
                    criteria ->
                        !criteria.isIncludeRevoked() && criteria.getFrom() == 0 && criteria.getTo() == 0 && criteria.getPlans().size() == 1
                )
            );

        inOrder
            .verify(apiKeyRepository)
            .findByCriteria(
                argThat(
                    criteria ->
                        criteria.isIncludeRevoked() && criteria.getFrom() != 0 && criteria.getTo() != 0 && criteria.getPlans().size() == 1
                )
            );
    }

    @Test
    public void shouldRefreshWithRevokedApiKeyAndPutIntoCache() throws TechnicalException {
        when(plan.getSecurity()).thenReturn(io.gravitee.repository.management.model.Plan.PlanSecurityType.API_KEY.name());
        List<Plan> plans = Collections.singletonList(plan);
        when(api.getPlans()).thenReturn(plans);

        ApiKey apiKey1 = mock(ApiKey.class);
        when(apiKey1.getApi()).thenReturn("my-api");
        when(apiKey1.getKey()).thenReturn("my-key");
        when(apiKey1.isRevoked()).thenReturn(false);

        when(apiKeyRepository.findByCriteria(any(ApiKeyCriteria.class))).thenReturn(Collections.singletonList(apiKey1));

        refresher.initialize();
        refresher.run();
        refresher.run();

        InOrder inOrder = Mockito.inOrder(apiKeyRepository, apiKeyRepository);

        inOrder
            .verify(apiKeyRepository)
            .findByCriteria(
                argThat(
                    criteria ->
                        !criteria.isIncludeRevoked() && criteria.getFrom() == 0 && criteria.getTo() == 0 && criteria.getPlans().size() == 1
                )
            );

        inOrder
            .verify(apiKeyRepository)
            .findByCriteria(
                argThat(
                    criteria ->
                        criteria.isIncludeRevoked() && criteria.getFrom() != 0 && criteria.getTo() != 0 && criteria.getPlans().size() == 1
                )
            );

        verify(cache, Mockito.times(2)).put(eq("my-api.my-key"), any(ApiKey.class));
    }

    @Test
    public void shouldRefreshWithRevokedApiKeyAndRemoveFromCache() throws TechnicalException {
        String apiKey = "1234-4567-7890";

        when(plan.getSecurity()).thenReturn(io.gravitee.repository.management.model.Plan.PlanSecurityType.API_KEY.name());
        List<Plan> plans = Collections.singletonList(plan);
        when(api.getPlans()).thenReturn(plans);

        ApiKey apiKey1 = mock(ApiKey.class);
        when(apiKey1.getApi()).thenReturn("api-1");
        when(apiKey1.getKey()).thenReturn(apiKey);
        when(apiKey1.isRevoked()).thenReturn(false);

        ApiKey apiKey2 = mock(ApiKey.class);
        when(apiKey2.getApi()).thenReturn("api-2");
        when(apiKey2.getKey()).thenReturn(apiKey);
        when(apiKey2.isRevoked()).thenReturn(true);

        when(apiKeyRepository.findByCriteria(any(ApiKeyCriteria.class)))
            .thenReturn(Collections.singletonList(apiKey1))
            .thenReturn(Collections.singletonList(apiKey2));

        refresher.initialize();
        refresher.run();
        refresher.run();

        InOrder inOrder = Mockito.inOrder(apiKeyRepository, apiKeyRepository);

        inOrder
            .verify(apiKeyRepository)
            .findByCriteria(
                ArgumentMatchers.argThat(
                    criteria ->
                        !criteria.isIncludeRevoked() && criteria.getFrom() == 0 && criteria.getTo() == 0 && criteria.getPlans().size() == 1
                )
            );

        inOrder
            .verify(apiKeyRepository)
            .findByCriteria(
                ArgumentMatchers.argThat(
                    criteria ->
                        criteria.isIncludeRevoked() && criteria.getFrom() != 0 && criteria.getTo() != 0 && criteria.getPlans().size() == 1
                )
            );

        InOrder inOrderCache = Mockito.inOrder(cache, cache);

        inOrderCache.verify(cache).put(eq("api-1.1234-4567-7890"), any(ApiKey.class));
        inOrderCache.verify(cache).remove("api-2.1234-4567-7890");
    }
}
