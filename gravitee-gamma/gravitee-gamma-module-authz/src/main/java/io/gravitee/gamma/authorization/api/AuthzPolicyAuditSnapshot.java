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
package io.gravitee.gamma.authorization.api;

import io.gravitee.gamma.authorization.domain.AuthzPolicy;
import io.gravitee.gamma.authorization.domain.AuthzPolicyKind;
import io.gravitee.gamma.authorization.domain.AuthzPolicyStatus;
import java.time.Instant;

public record AuthzPolicyAuditSnapshot(
    String id,
    String name,
    AuthzPolicyKind kind,
    String entityId,
    AuthzPolicyStatus status,
    String environmentId,
    Instant createdAt,
    Instant updatedAt
) implements AuthzAuditSnapshot {
    public static AuthzPolicyAuditSnapshot of(AuthzPolicy policy) {
        if (policy == null) {
            return null;
        }
        return new AuthzPolicyAuditSnapshot(
            policy.id(),
            policy.name(),
            policy.kind(),
            policy.entityId(),
            policy.status(),
            policy.environmentId(),
            policy.createdAt(),
            policy.updatedAt()
        );
    }
}
