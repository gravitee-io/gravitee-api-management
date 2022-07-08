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
package io.gravitee.gateway.services.sync.cache.repository;

import static org.mockito.Mockito.*;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.gravitee.repository.management.api.search.SubscriptionCriteria;
import io.gravitee.repository.management.model.Subscription;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class SubscriptionRepositoryWrapperTest {

    private SubscriptionRepositoryWrapper repository;

    @Mock
    private SubscriptionRepository wrappedRepository;

    @Mock
    private Map<String, Subscription> cache;

    @Mock
    private SubscriptionCriteria subscriptionCriteria;

    @Before
    public void setUp() {
        repository = new SubscriptionRepositoryWrapper(wrappedRepository, cache);
    }

    @Test
    public void search_should_call_wrapped_repository_cause_no_client_id() throws TechnicalException {
        repository.search(subscriptionCriteria);

        verify(wrappedRepository).search(subscriptionCriteria);
        verifyNoInteractions(cache);
    }

    @Test
    public void search_should_get_from_cache_cause_has_client_id() throws TechnicalException {
        when(subscriptionCriteria.getClientId()).thenReturn("myClientId");
        when(subscriptionCriteria.getApis()).thenReturn(List.of("myApi"));
        when(subscriptionCriteria.getPlans()).thenReturn(List.of("myPlan"));

        repository.search(subscriptionCriteria);

        verify(cache).get("myApi.myClientId.myPlan");
        verifyNoInteractions(wrappedRepository);
    }
}
