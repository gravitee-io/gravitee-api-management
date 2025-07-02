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

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.ApplicationStatus;
import io.gravitee.rest.api.model.ApiKeyMode;
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.InlinePictureEntity;
import io.gravitee.rest.api.model.NewApplicationEntity;
import io.gravitee.rest.api.model.UpdateApplicationEntity;
import io.gravitee.rest.api.model.application.ApplicationListItem;
import io.gravitee.rest.api.model.application.ApplicationQuery;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.common.Sortable;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface ApplicationService {
    ApplicationEntity findById(final ExecutionContext executionContext, String applicationId);

    Set<ApplicationListItem> findByIds(final ExecutionContext executionContext, Collection<String> applicationIds);

    Set<ApplicationListItem> findByIdsAndStatus(
        final ExecutionContext executionContext,
        Collection<String> applicationIds,
        ApplicationStatus applicationStatus
    );

    default Set<ApplicationListItem> findByUser(final ExecutionContext executionContext, String username) {
        return findByUser(executionContext, username, null);
    }

    Set<ApplicationListItem> findByUser(final ExecutionContext executionContext, String username, Sortable sortable);

    default Set<String> findIdsByUser(final ExecutionContext executionContext, String username) {
        return findIdsByUser(executionContext, username, null);
    }

    Set<String> findIdsByUser(final ExecutionContext executionContext, String username, Sortable sortable);

    Set<String> findIdsByUserAndPermission(
        ExecutionContext executionContext,
        String username,
        Sortable sortable,
        RolePermission rolePermission,
        RolePermissionAction... acl
    );

    Set<String> findIdsByOrganization(String organizationId);

    Set<String> findIdsByEnvironment(final ExecutionContext executionContext);

    Set<ApplicationListItem> findByGroups(ExecutionContext executionContext, List<String> groupId);

    Set<ApplicationListItem> findByGroupsAndStatus(ExecutionContext executionContext, List<String> groupId, String status);

    ApplicationEntity create(final ExecutionContext executionContext, NewApplicationEntity application, String username);

    ApplicationEntity update(final ExecutionContext executionContext, String applicationId, UpdateApplicationEntity application);

    ApplicationEntity updateApiKeyMode(final ExecutionContext executionContext, String applicationId, ApiKeyMode apiKeyMode);

    ApplicationEntity renewClientSecret(final ExecutionContext executionContext, String applicationId);

    ApplicationEntity restore(final ExecutionContext executionContext, String applicationId);

    void archive(final ExecutionContext executionContext, String applicationId);

    InlinePictureEntity getPicture(final ExecutionContext executionContext, String apiId);

    InlinePictureEntity getBackground(final ExecutionContext executionContext, String application);

    Map<String, Object> findByIdAsMap(String id) throws TechnicalException;

    Page<ApplicationListItem> search(
        final ExecutionContext executionContext,
        ApplicationQuery applicationQuery,
        Sortable sortable,
        Pageable pageable
    );

    Set<String> searchIds(final ExecutionContext executionContext, ApplicationQuery applicationQuery, Sortable sortable);

    Set<String> findUserApplicationsIds(ExecutionContext executionContext, String username, ApplicationStatus status);
}
