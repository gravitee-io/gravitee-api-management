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
package io.gravitee.gateway.handlers.api.manager.impl;

import io.gravitee.gamma.definition.approval.ApprovalOpenKey;
import io.gravitee.gateway.handlers.api.manager.DeployedApprovalState;
import java.util.Objects;

/** What a time-based grant covers: the same caller calling the same rule of the same API with the same arguments. */
record GrantKey(String environmentId, String apiId, String ruleId, String applicationId, String subject, String argumentsDigest) {
    GrantKey {
        subject = Objects.requireNonNullElse(subject, "");
    }

    static GrantKey of(DeployedApprovalState state) {
        DeployedApprovalState.Grant grant = state.grant();
        return new GrantKey(
            state.environmentId(),
            state.apiId(),
            state.ruleId(),
            grant.applicationId(),
            grant.subject(),
            grant.argumentsDigest()
        );
    }

    /** The key a grant is cached under: a digest, so that no end-user id appears in a cache key. */
    String digest() {
        return ApprovalOpenKey.of(environmentId, apiId, ruleId, applicationId, subject, argumentsDigest);
    }
}
