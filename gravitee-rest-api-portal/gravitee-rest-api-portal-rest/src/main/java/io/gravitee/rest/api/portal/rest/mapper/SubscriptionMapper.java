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

import java.time.ZoneOffset;

import org.springframework.stereotype.Component;

import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.portal.rest.model.Subscription;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */

@Component
public class SubscriptionMapper {
    
    public Subscription convert(SubscriptionEntity subscriptionEntity) {
        final Subscription subscriptionItem = new Subscription();

        subscriptionItem.setId(subscriptionEntity.getId());
        subscriptionItem.setApi(subscriptionEntity.getApi());
        subscriptionItem.setApplication(subscriptionEntity.getApplication());
        subscriptionItem.setCreatedAt(subscriptionEntity.getCreatedAt().toInstant().atOffset( ZoneOffset.UTC ));
        subscriptionItem.setEndAt(subscriptionEntity.getEndingAt().toInstant().atOffset( ZoneOffset.UTC ));
        subscriptionItem.setPlan(subscriptionEntity.getPlan());
        subscriptionItem.setProcessedAt(subscriptionEntity.getProcessedAt().toInstant().atOffset( ZoneOffset.UTC ));
        subscriptionItem.setRequest(subscriptionEntity.getRequest());
        subscriptionItem.setStartAt(subscriptionEntity.getStartingAt().toInstant().atOffset( ZoneOffset.UTC ));
        subscriptionItem.setStatus(Subscription.StatusEnum.fromValue(subscriptionEntity.getStatus().name()));
        
        return subscriptionItem;
    }

}
