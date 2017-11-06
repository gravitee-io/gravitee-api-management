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
package io.gravitee.gateway.services.apikeyscache;

import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.handlers.api.definition.Plan;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.api.search.ApiKeyCriteria;
import io.gravitee.repository.management.model.ApiKey;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiKeyRepositoryRefresherTest {

    private ApiKeyRefresher refresher;

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @Mock
    private Ehcache cache;

    @Mock
    private Api api;

    @Mock
    private Plan plan;

    @Before
    public void setUp() {
        refresher = new ApiKeyRefresher(api);
        refresher.setCache(cache);
        refresher.setApiKeyRepository(apiKeyRepository);
    }

    @Test
    public void shouldInitWithNonRevokedApiKey() throws TechnicalException {
        Mockito.when(plan.getSecurity()).thenReturn(io.gravitee.repository.management.model.Plan.PlanSecurityType.API_KEY.name());
        List<Plan> plans = Collections.singletonList(plan);
        Mockito.when(api.getPlans()).thenReturn(plans);

        refresher.initialize();
        refresher.run();

        Mockito.verify(apiKeyRepository).findByCriteria(Matchers.argThat(new ArgumentMatcher<ApiKeyCriteria>() {
            @Override
            public boolean matches(Object arg) {
                ApiKeyCriteria criteria = (ApiKeyCriteria) arg;
                return !criteria.isIncludeRevoked() &&
                        criteria.getFrom() == 0 &&
                        criteria.getTo() == 0 &&
                        criteria.getPlans().size() == 1;
            }
        }));
    }

    @Test
    public void shouldNotRefreshKeylessPlan() throws TechnicalException {
        Mockito.when(plan.getSecurity()).thenReturn(io.gravitee.repository.management.model.Plan.PlanSecurityType.KEY_LESS.name());
        List<Plan> plans = Collections.singletonList(plan);
        Mockito.when(api.getPlans()).thenReturn(plans);

        refresher.initialize();
        refresher.run();

        Mockito.verifyZeroInteractions(apiKeyRepository);
    }

    @Test
    public void shouldRefreshWithRevokedApiKey() throws TechnicalException {
        Mockito.when(plan.getSecurity()).thenReturn(io.gravitee.repository.management.model.Plan.PlanSecurityType.API_KEY.name());
        List<Plan> plans = Collections.singletonList(plan);
        Mockito.when(api.getPlans()).thenReturn(plans);

        refresher.initialize();
        refresher.run();
        refresher.run();

        InOrder inOrder = Mockito.inOrder(apiKeyRepository, apiKeyRepository);

        inOrder.verify(apiKeyRepository).findByCriteria(Matchers.argThat(new ArgumentMatcher<ApiKeyCriteria>() {
                    @Override
                    public boolean matches(Object arg) {
                        ApiKeyCriteria criteria = (ApiKeyCriteria) arg;
                        return !criteria.isIncludeRevoked() &&
                                criteria.getFrom() == 0 &&
                                criteria.getTo() == 0 &&
                                criteria.getPlans().size() == 1;
                    }
                }));

        inOrder.verify(apiKeyRepository).findByCriteria(Matchers.argThat(new ArgumentMatcher<ApiKeyCriteria>() {
            @Override
            public boolean matches(Object arg) {
                ApiKeyCriteria criteria = (ApiKeyCriteria) arg;
                return criteria.isIncludeRevoked() &&
                        criteria.getFrom() != 0 &&
                        criteria.getTo() != 0 &&
                        criteria.getPlans().size() == 1;
            }
        }));
    }

    @Test
    public void shouldRefreshWithRevokedApiKeyAndPutIntoCache() throws TechnicalException {
        Mockito.when(plan.getSecurity()).thenReturn(io.gravitee.repository.management.model.Plan.PlanSecurityType.API_KEY.name());
        List<Plan> plans = Collections.singletonList(plan);
        Mockito.when(api.getPlans()).thenReturn(plans);

        ApiKey apiKey1 = Mockito.mock(ApiKey.class);
        Mockito.when(apiKey1.isRevoked()).thenReturn(false);

        Mockito.when(apiKeyRepository.findByCriteria(Mockito.any(ApiKeyCriteria.class)))
                .thenReturn(Collections.singletonList(apiKey1));

        refresher.initialize();
        refresher.run();
        refresher.run();

        InOrder inOrder = Mockito.inOrder(apiKeyRepository, apiKeyRepository);

        inOrder.verify(apiKeyRepository).findByCriteria(Matchers.argThat(new ArgumentMatcher<ApiKeyCriteria>() {
            @Override
            public boolean matches(Object arg) {
                ApiKeyCriteria criteria = (ApiKeyCriteria) arg;
                return !criteria.isIncludeRevoked() &&
                        criteria.getFrom() == 0 &&
                        criteria.getTo() == 0 &&
                        criteria.getPlans().size() == 1;
            }
        }));

        inOrder.verify(apiKeyRepository).findByCriteria(Matchers.argThat(new ArgumentMatcher<ApiKeyCriteria>() {
            @Override
            public boolean matches(Object arg) {
                ApiKeyCriteria criteria = (ApiKeyCriteria) arg;
                return criteria.isIncludeRevoked() &&
                        criteria.getFrom() != 0 &&
                        criteria.getTo() != 0 &&
                        criteria.getPlans().size() == 1;
            }
        }));

        Mockito.verify(cache, Mockito.times(2)).put(Matchers.any(Element.class));
    }

    @Test
    public void shouldRefreshWithRevokedApiKeyAndRemoveFromCache() throws TechnicalException {
        String apiKey = "1234-4567-7890";

        Mockito.when(plan.getSecurity()).thenReturn(io.gravitee.repository.management.model.Plan.PlanSecurityType.API_KEY.name());
        List<Plan> plans = Collections.singletonList(plan);
        Mockito.when(api.getPlans()).thenReturn(plans);

        ApiKey apiKey1 = Mockito.mock(ApiKey.class);
        Mockito.when(apiKey1.getKey()).thenReturn(apiKey);
        Mockito.when(apiKey1.isRevoked()).thenReturn(false);

        ApiKey apiKey2 = Mockito.mock(ApiKey.class);
        Mockito.when(apiKey2.getKey()).thenReturn(apiKey);
        Mockito.when(apiKey2.isRevoked()).thenReturn(true);

        Mockito.when(apiKeyRepository.findByCriteria(Mockito.any(ApiKeyCriteria.class)))
                .thenReturn(Collections.singletonList(apiKey1))
                .thenReturn(Collections.singletonList(apiKey2));

        refresher.initialize();
        refresher.run();
        refresher.run();

        InOrder inOrder = Mockito.inOrder(apiKeyRepository, apiKeyRepository);

        inOrder.verify(apiKeyRepository).findByCriteria(Matchers.argThat(new ArgumentMatcher<ApiKeyCriteria>() {
            @Override
            public boolean matches(Object arg) {
                ApiKeyCriteria criteria = (ApiKeyCriteria) arg;
                return !criteria.isIncludeRevoked() &&
                        criteria.getFrom() == 0 &&
                        criteria.getTo() == 0 &&
                        criteria.getPlans().size() == 1;
            }
        }));

        inOrder.verify(apiKeyRepository).findByCriteria(Matchers.argThat(new ArgumentMatcher<ApiKeyCriteria>() {
            @Override
            public boolean matches(Object arg) {
                ApiKeyCriteria criteria = (ApiKeyCriteria) arg;
                return criteria.isIncludeRevoked() &&
                        criteria.getFrom() != 0 &&
                        criteria.getTo() != 0 &&
                        criteria.getPlans().size() == 1;
            }
        }));

        InOrder inOrderCache = Mockito.inOrder(cache, cache);

        inOrderCache.verify(cache).put(Matchers.any(Element.class));
        inOrderCache.verify(cache).remove(apiKey);
    }
}
