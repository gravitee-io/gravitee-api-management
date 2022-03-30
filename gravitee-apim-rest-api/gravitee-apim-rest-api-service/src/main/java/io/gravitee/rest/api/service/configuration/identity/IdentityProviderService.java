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
package io.gravitee.rest.api.service.configuration.identity;

import io.gravitee.rest.api.model.configuration.identity.IdentityProviderEntity;
import io.gravitee.rest.api.model.configuration.identity.NewIdentityProviderEntity;
import io.gravitee.rest.api.model.configuration.identity.RoleMappingEntity;
import io.gravitee.rest.api.model.configuration.identity.UpdateIdentityProviderEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Collection;
import java.util.Set;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface IdentityProviderService {
    IdentityProviderEntity create(ExecutionContext executionContext, NewIdentityProviderEntity identityProvider);

    IdentityProviderEntity update(ExecutionContext executionContext, String id, UpdateIdentityProviderEntity identityProvider);

    IdentityProviderEntity findById(String id);

    void delete(ExecutionContext executionContext, String id);

    Set<IdentityProviderEntity> findAll(ExecutionContext executionContext);

    /**
     * Transform an inline roleMapping definition to a roleMappingEntity
     *
     * ORGANIZATION:ROLE_NAME
     * ENVIRONMENT:ROLE_NAME (old mapping style) => converted to ENVIRONMENT:DEFAULT:ROLE_NAME
     * ENVIRONMENT:ENVIRONMENT_ID:ROLE_NAME (new mapping style)
     *
     * @param inlineRoles
     * @return
     */
    RoleMappingEntity getRoleMappings(String condition, Collection<String> inlineRoles);
}
