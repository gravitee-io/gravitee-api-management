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
package io.gravitee.gamma.authorization.event;

import io.gravitee.gamma.authorization.api.AuthzEventPublisher;
import io.gravitee.gamma.repository.authorization.model.AuthorizationEntity;
import io.gravitee.gamma.repository.authorization.model.AuthorizationPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class RecordingAuthzEventPublisher implements AuthzEventPublisher {

    public enum EventKind {
        POLICY_PUBLISHED,
        POLICY_UNPUBLISHED,
        ENTITY_PUBLISHED,
        ENTITY_UNPUBLISHED,
    }

    public record Recorded(EventKind kind, String environmentId, String id, AuthorizationPolicy policy, AuthorizationEntity entity) {
        public Recorded {
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(environmentId, "environmentId");
            Objects.requireNonNull(id, "id");
        }
    }

    private final List<Recorded> recorded = new ArrayList<>();

    @Override
    public void publishPolicyDeployed(AuthorizationPolicy policy) {
        recorded.add(new Recorded(EventKind.POLICY_PUBLISHED, policy.environmentId(), policy.id(), policy, null));
    }

    @Override
    public void unpublishPolicy(AuthorizationPolicy policy) {
        recorded.add(new Recorded(EventKind.POLICY_UNPUBLISHED, policy.environmentId(), policy.id(), policy, null));
    }

    @Override
    public void publishEntityUpserted(AuthorizationEntity entity) {
        recorded.add(new Recorded(EventKind.ENTITY_PUBLISHED, entity.environmentId(), entity.entityId(), null, entity));
    }

    @Override
    public void unpublishEntity(AuthorizationEntity entity) {
        recorded.add(new Recorded(EventKind.ENTITY_UNPUBLISHED, entity.environmentId(), entity.entityId(), null, entity));
    }

    public List<Recorded> events() {
        return List.copyOf(recorded);
    }

    public void clear() {
        recorded.clear();
    }
}
