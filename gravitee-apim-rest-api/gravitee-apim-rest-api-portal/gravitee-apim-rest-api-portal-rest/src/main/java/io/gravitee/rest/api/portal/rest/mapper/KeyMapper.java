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
package io.gravitee.rest.api.portal.rest.mapper;

import io.gravitee.rest.api.model.ApiKeyEntity;
import io.gravitee.rest.api.model.ApiKeyMode;
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.portal.rest.model.ApiKeyModeEnum;
import io.gravitee.rest.api.portal.rest.model.Application;
import io.gravitee.rest.api.portal.rest.model.Key;
import io.gravitee.rest.api.portal.rest.model.Subscription;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */

@Component
public class KeyMapper {

    public Key convert(ApiKeyEntity apiKeyEntity) {
        final Key keyItem = new Key();
        keyItem.setId(apiKeyEntity.getId());
        keyItem.setApplication(convert(apiKeyEntity.getApplication()));
        keyItem.setSubscriptions(convert(apiKeyEntity.getSubscriptions()));
        keyItem.setCreatedAt(apiKeyEntity.getCreatedAt().toInstant().atOffset(ZoneOffset.UTC));
        keyItem.setKey(apiKeyEntity.getKey());
        keyItem.setPaused(apiKeyEntity.isPaused());
        keyItem.setRevoked(apiKeyEntity.isRevoked());
        if (apiKeyEntity.isRevoked()) {
            keyItem.setRevokedAt(apiKeyEntity.getRevokedAt().toInstant().atOffset(ZoneOffset.UTC));
        }
        keyItem.setExpired(apiKeyEntity.isExpired());
        if (apiKeyEntity.getExpireAt() != null) {
            keyItem.setExpireAt(apiKeyEntity.getExpireAt().toInstant().atOffset(ZoneOffset.UTC));
        }
        return keyItem;
    }

    public Application convert(ApplicationEntity applicationEntity) {
        Application application = new Application();
        application.setId(applicationEntity.getId());
        application.setName(applicationEntity.getName());
        if (application.getApiKeyMode() != null) {
            application.setApiKeyMode(ApiKeyModeEnum.valueOf(applicationEntity.getApiKeyMode().name()));
        }
        return application;
    }

    public List<Subscription> convert(Collection<SubscriptionEntity> subscriptionEntities) {
        return subscriptionEntities.stream().map(this::convert).collect(Collectors.toList());
    }

    public Subscription convert(SubscriptionEntity subscriptionEntity) {
        Subscription subscription = new Subscription();
        subscription.setApi(subscriptionEntity.getApi());
        subscription.setApplication(subscriptionEntity.getApplication());
        subscription.setPlan(subscription.getPlan());
        return subscription;
    }
}
