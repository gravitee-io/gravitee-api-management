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
package io.gravitee.rest.api.service;

import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.UpdateEnvironmentEntity;
import java.util.List;
import java.util.Set;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface EnvironmentService {
    List<EnvironmentEntity> findByUser(final String organizationId, String userId);

    EnvironmentEntity findByOrgAndIdOrHrid(final String organizationId, String idOrHrid);

    List<EnvironmentEntity> findByOrganization(String organizationId);

    EnvironmentEntity findById(String environmentId);

    Set<String> findOrganizationIdsByEnvironments(Set<String> environmentIds);

    EnvironmentEntity createOrUpdate(String organizationId, String environmentId, UpdateEnvironmentEntity environment);

    void delete(String environmentId);

    EnvironmentEntity initialize();

    EnvironmentEntity findByCockpitId(String cockpitId);

    EnvironmentEntity getDefaultOrInitialize();

    Set<EnvironmentEntity> findAllOrInitialize();
}
