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
package io.gravitee.apim.core.membership.query_service;

import io.gravitee.apim.core.membership.exception.RoleNotFoundException;
import io.gravitee.apim.core.membership.model.Role;
import io.gravitee.rest.api.service.common.ReferenceContext;
import java.util.Optional;
import java.util.Set;

public interface RoleQueryService {
    Optional<Role> findApiRole(String name, ReferenceContext referenceContext);
    Optional<Role> findApplicationRole(String name, ReferenceContext referenceContext);
    Optional<Role> findIntegrationRole(String name, ReferenceContext referenceContext);
    Set<Role> findByIds(Set<String> ids);

    default Role getApiRole(String name, ReferenceContext referenceContext) {
        return findApiRole(name, referenceContext).orElseThrow(() -> new RoleNotFoundException(name, referenceContext));
    }
}
