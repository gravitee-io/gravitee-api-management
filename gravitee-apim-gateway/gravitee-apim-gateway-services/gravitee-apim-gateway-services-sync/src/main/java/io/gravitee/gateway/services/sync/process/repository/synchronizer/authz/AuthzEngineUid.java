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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.authz;

import static io.gravitee.gamma.definition.authz.AuthzEntityIdConstants.ENGINE_TYPE_PRINCIPAL;
import static io.gravitee.gamma.definition.authz.AuthzEntityIdConstants.ENGINE_TYPE_RESOURCE;

public final class AuthzEngineUid {

    private AuthzEngineUid() {}

    public static String of(AuthzEntityReactorDeployable.Kind kind, String entityType, String entityId) {
        // Explicit entityType wins so GAPL policies match verbatim; legacy publishers fall back to the kind default.
        String type = (entityType != null && !entityType.isBlank())
            ? entityType
            : (kind == AuthzEntityReactorDeployable.Kind.PRINCIPAL ? ENGINE_TYPE_PRINCIPAL : ENGINE_TYPE_RESOURCE);
        return type + "::\"" + entityId + "\"";
    }
}
