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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.subscription;

import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.services.sync.process.common.model.SubscriptionDeployable;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Builder
@Getter
@Setter
@Accessors(fluent = true)
@EqualsAndHashCode
@ToString
public class SingleSubscriptionDeployable implements SubscriptionDeployable {

    @NonNull
    private Subscription subscription;

    private SyncAction syncAction;

    public String id() {
        return subscription.getId();
    }

    public String apiId() {
        return subscription.getApi();
    }

    public Set<String> plans() {
        return subscribablePlans();
    }

    public Set<String> subscribablePlans() {
        return Set.of(subscription.getPlan());
    }

    @Override
    public List<Subscription> subscriptions() {
        return List.of(subscription);
    }

    public SingleSubscriptionDeployable subscriptions(final List<Subscription> subscriptions) {
        throw new UnsupportedOperationException(
            String.format("Unable to override subscriptions for %s", SingleSubscriptionDeployable.class.getName())
        );
    }
}
