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
package io.gravitee.rest.api.service;

import io.gravitee.rest.api.model.OrganizationEntity;
import io.gravitee.rest.api.model.UpdateOrganizationEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Collection;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface OrganizationService {
    Long count();

    OrganizationEntity findById(String organizationId);

    OrganizationEntity createOrUpdate(
        ExecutionContext executionContext,
        String organizationId,
        UpdateOrganizationEntity organizationEntity
    );

    OrganizationEntity update(ExecutionContext executionContext, String organizationId, UpdateOrganizationEntity organizationEntity);

    void delete(String organizationId);

    void initialize();

    /**
     * Find all existing organizations.
     *
     * @return the list of all organizations.
     */
    Collection<OrganizationEntity> findAll();
}
