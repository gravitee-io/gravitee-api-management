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

import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.portal.rest.model.Subscription;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import org.springframework.stereotype.Component;

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
        subscriptionItem.setCreatedAt(getDate(subscriptionEntity.getCreatedAt()));
        subscriptionItem.setEndAt(getDate(subscriptionEntity.getEndingAt()));
        subscriptionItem.setProcessedAt(getDate(subscriptionEntity.getProcessedAt()));
        subscriptionItem.setStartAt(getDate(subscriptionEntity.getStartingAt()));
        subscriptionItem.setPausedAt(getDate(subscriptionEntity.getPausedAt()));
        subscriptionItem.setClosedAt(getDate(subscriptionEntity.getClosedAt()));
        subscriptionItem.setPausedAt(getDate(subscriptionEntity.getPausedAt()));
        subscriptionItem.setPlan(subscriptionEntity.getPlan());
        subscriptionItem.setRequest(subscriptionEntity.getRequest());
        subscriptionItem.setReason(subscriptionEntity.getReason());
        subscriptionItem.setStatus(Subscription.StatusEnum.fromValue(subscriptionEntity.getStatus().name()));
        subscriptionItem.setSubscribedBy(subscriptionEntity.getSubscribedBy());
        return subscriptionItem;
    }

    private OffsetDateTime getDate(final Date date) {
        if (date != null) {
            return date.toInstant().atOffset(ZoneOffset.UTC);
        }
        return null;
    }
}
