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
package io.gravitee.gamma.authorization.domain;

import io.gravitee.gamma.definition.authz.AuthzEntityIdConstants;

public enum AuthzEntityKind {
    PRINCIPAL,
    RESOURCE;

    /**
     * Default engine type name for entities whose REST request did not carry an explicit
     * {@code entityType}. Used as a backwards-compatible fallback so existing integrations
     * keep producing {@code Principal::"<id>"} / {@code Resource::"<id>"} UIDs in the PDP.
     */
    public String defaultEntityType() {
        return switch (this) {
            case PRINCIPAL -> AuthzEntityIdConstants.ENGINE_TYPE_PRINCIPAL;
            case RESOURCE -> AuthzEntityIdConstants.ENGINE_TYPE_RESOURCE;
        };
    }
}
