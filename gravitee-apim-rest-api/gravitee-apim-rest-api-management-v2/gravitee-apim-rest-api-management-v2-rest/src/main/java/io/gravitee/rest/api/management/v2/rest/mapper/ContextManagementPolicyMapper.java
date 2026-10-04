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
package io.gravitee.rest.api.management.v2.rest.mapper;

import io.gravitee.rest.api.management.v2.rest.model.ContextManagementPolicy;

public final class ContextManagementPolicyMapper {

    private ContextManagementPolicyMapper() {}

    public static io.gravitee.definition.model.llm.ContextManagementPolicy toDomain(ContextManagementPolicy policy) {
        if (policy == null || policy.getMode() == null) {
            throw new IllegalArgumentException("Context management policy mode must not be null");
        }
        return new io.gravitee.definition.model.llm.ContextManagementPolicy(
            io.gravitee.definition.model.llm.ContextManagementPolicy.Mode.valueOf(policy.getMode().getValue()),
            policy.getCompactThreshold(),
            null
        );
    }

    public static ContextManagementPolicy toApi(io.gravitee.definition.model.llm.ContextManagementPolicy policy) {
        return new ContextManagementPolicy()
            .mode(ContextManagementPolicy.ModeEnum.valueOf(policy.getMode().name()))
            .compactThreshold(policy.getCompactThreshold());
    }
}
