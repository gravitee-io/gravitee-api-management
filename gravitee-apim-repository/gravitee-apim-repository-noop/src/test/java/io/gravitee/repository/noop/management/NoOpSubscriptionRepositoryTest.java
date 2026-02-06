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
package io.gravitee.repository.noop.management;

import static org.junit.Assert.*;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.gravitee.repository.management.api.search.Order;
import io.gravitee.repository.management.api.search.Sortable;
import io.gravitee.repository.management.api.search.SubscriptionCriteria;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.api.search.builder.SortableBuilder;
import io.gravitee.repository.management.model.Subscription;
import io.gravitee.repository.management.model.SubscriptionReferenceType;
import io.gravitee.repository.noop.AbstractNoOpRepositoryTest;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NoOpSubscriptionRepositoryTest extends AbstractNoOpRepositoryTest {

    @Autowired
    private SubscriptionRepository cut;

    @Test
    public void search() throws TechnicalException {
        List<Subscription> subscriptions = cut.search(SubscriptionCriteria.builder().build());

        assertNotNull(subscriptions);
        assertTrue(subscriptions.isEmpty());
    }

    @Test
    public void searchWithReferenceIdsAndReferenceTypeCriteria() throws TechnicalException {
        List<Subscription> subscriptions = cut.search(
            SubscriptionCriteria.builder()
                .referenceIds(List.of("c45b8e66-4d2a-47ad-9b8e-664d2a97ad88"))
                .referenceType(SubscriptionReferenceType.API_PRODUCT)
                .build()
        );

        assertNotNull(subscriptions);
        assertTrue(subscriptions.isEmpty());
    }

    @Test
    public void testSortablePageableSearch() throws TechnicalException {
        Page<Subscription> subscriptions = cut.search(
            SubscriptionCriteria.builder().build(),
            new SortableBuilder().build(),
            new PageableBuilder().build()
        );

        assertNotNull(subscriptions);
        assertNotNull(subscriptions.getContent());
        assertTrue(subscriptions.getContent().isEmpty());
    }

    @Test
    public void testSortableSearch() throws TechnicalException {
        List<Subscription> subscriptions = cut.search(SubscriptionCriteria.builder().build(), new SortableBuilder().build());

        assertNotNull(subscriptions);
        assertTrue(subscriptions.isEmpty());
    }

    @Test
    public void findByIdIn() throws TechnicalException {
        List<Subscription> subscriptions = cut.findByIdIn(List.of("test_id"));

        assertNotNull(subscriptions);
        assertTrue(subscriptions.isEmpty());
    }

    @Test
    public void findReferenceIdsOrderByNumberOfSubscriptions() throws TechnicalException {
        Set<String> subscriptions = cut.findReferenceIdsOrderByNumberOfSubscriptions(SubscriptionCriteria.builder().build(), Order.ASC);

        assertNotNull(subscriptions);
        assertTrue(subscriptions.isEmpty());
    }

    @Test
    public void findByReferenceIdAndReferenceType() throws TechnicalException {
        Set<Subscription> subscriptions = cut.findByReferenceIdAndReferenceType(
            "test-api-product-id",
            SubscriptionReferenceType.API_PRODUCT
        );

        assertNotNull(subscriptions);
        assertTrue(subscriptions.isEmpty());
    }

    @Test
    public void findByIdAndReferenceIdAndReferenceType() throws TechnicalException {
        Optional<Subscription> subscription = cut.findByIdAndReferenceIdAndReferenceType(
            "test-subscription-id",
            "test-api-product-id",
            SubscriptionReferenceType.API_PRODUCT
        );

        assertNotNull(subscription);
        assertFalse(subscription.isPresent());
    }
}
