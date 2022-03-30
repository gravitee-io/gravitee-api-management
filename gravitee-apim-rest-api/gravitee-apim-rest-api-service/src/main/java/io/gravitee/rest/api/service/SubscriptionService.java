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
package io.gravitee.rest.api.service;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.management.api.search.Order;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.pagedresult.Metadata;
import io.gravitee.rest.api.model.subscription.SubscriptionMetadataQuery;
import io.gravitee.rest.api.model.subscription.SubscriptionQuery;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface SubscriptionService {
    /**
     * Get a subscription from its ID.
     * @param subscription
     * @return
     */
    SubscriptionEntity findById(String subscription);

    Collection<SubscriptionEntity> findByApplicationAndPlan(ExecutionContext executionContext, String application, String plan);

    Collection<SubscriptionEntity> findByApi(ExecutionContext executionContext, String api);

    Collection<SubscriptionEntity> findByPlan(ExecutionContext executionContext, String plan);

    SubscriptionEntity create(ExecutionContext executionContext, NewSubscriptionEntity newSubscriptionEntity);

    SubscriptionEntity create(ExecutionContext executionContext, NewSubscriptionEntity newSubscriptionEntity, String customApiKey);

    SubscriptionEntity update(ExecutionContext executionContext, UpdateSubscriptionEntity subscription);

    SubscriptionEntity updateDaysToExpirationOnLastNotification(String subscriptionId, Integer value);

    SubscriptionEntity update(ExecutionContext executionContext, UpdateSubscriptionEntity subscription, String clientId);

    SubscriptionEntity process(ExecutionContext executionContext, ProcessSubscriptionEntity processSubscription, String userId);

    SubscriptionEntity pause(ExecutionContext executionContext, String subscription);

    SubscriptionEntity resume(ExecutionContext executionContext, String subscription);

    SubscriptionEntity restore(ExecutionContext executionContext, String subscription);

    SubscriptionEntity close(ExecutionContext executionContext, String subscription);

    void delete(ExecutionContext executionContext, String subscription);

    Collection<SubscriptionEntity> search(ExecutionContext executionContext, SubscriptionQuery query);

    Page<SubscriptionEntity> search(ExecutionContext executionContext, SubscriptionQuery query, Pageable pageable);

    Page<SubscriptionEntity> search(
        ExecutionContext executionContext,
        SubscriptionQuery query,
        Pageable pageable,
        boolean fillApiKeys,
        boolean fillPlansSecurityType
    );

    Metadata getMetadata(ExecutionContext executionContext, SubscriptionMetadataQuery query);

    SubscriptionEntity transfer(ExecutionContext executionContext, TransferSubscriptionEntity transferSubscription, String userId);

    String exportAsCsv(Collection<SubscriptionEntity> subscriptions, Map<String, Map<String, Object>> metadata);

    Set<String> findReferenceIdsOrderByNumberOfSubscriptions(SubscriptionQuery subscriptionQuery, Order order);

    Set<SubscriptionEntity> findByIdIn(Collection<String> subscriptions);
}
