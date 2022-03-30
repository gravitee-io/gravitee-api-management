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

import io.gravitee.rest.api.model.ApiKeyEntity;
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.key.ApiKeyQuery;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Collection;
import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface ApiKeyService {
    ApiKeyEntity generate(
        ExecutionContext executionContext,
        ApplicationEntity application,
        SubscriptionEntity subscription,
        String customApiKey
    );

    ApiKeyEntity renew(ExecutionContext executionContext, ApplicationEntity application);

    ApiKeyEntity renew(ExecutionContext executionContext, SubscriptionEntity subscription);

    ApiKeyEntity renew(ExecutionContext executionContext, SubscriptionEntity subscription, String customApiKey);

    void revoke(ExecutionContext executionContext, String keyId, boolean notify);

    void revoke(ExecutionContext executionContext, ApiKeyEntity apiKeyEntity, boolean notify);

    ApiKeyEntity reactivate(ExecutionContext executionContext, ApiKeyEntity apiKeyEntity);

    List<ApiKeyEntity> findBySubscription(ExecutionContext executionContext, String subscription);

    List<ApiKeyEntity> findByKey(ExecutionContext executionContext, String apiKey);

    ApiKeyEntity findById(ExecutionContext executionContext, String keyId);

    ApiKeyEntity findByKeyAndApi(ExecutionContext executionContext, String apiKey, String apiId);

    List<ApiKeyEntity> findByApplication(ExecutionContext executionContext, String applicationId);

    void delete(String apiKey);

    ApiKeyEntity update(ExecutionContext executionContext, ApiKeyEntity apiKeyEntity);

    ApiKeyEntity updateDaysToExpirationOnLastNotification(ExecutionContext executionContext, ApiKeyEntity apiKeyEntity, Integer value);

    boolean canCreate(ExecutionContext executionContext, String apiKey, SubscriptionEntity subscription);

    boolean canCreate(ExecutionContext executionContext, String apiKey, String apiId, String applicationId);

    Collection<ApiKeyEntity> search(ExecutionContext executionContext, ApiKeyQuery query);
}
