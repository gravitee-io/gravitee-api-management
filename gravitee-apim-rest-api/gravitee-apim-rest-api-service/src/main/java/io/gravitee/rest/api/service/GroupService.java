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

import io.gravitee.repository.management.model.GroupEvent;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.IndexableApi;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;
import java.util.Set;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author Florent CHAMFROY (forent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface GroupService {
    GroupEntity create(ExecutionContext executionContext, NewGroupEntity group);
    void delete(ExecutionContext executionContext, String groupId);
    void deleteUserFromGroup(ExecutionContext executionContext, String groupId, String username);
    List<GroupEntity> findAll(ExecutionContext executionContext);
    List<GroupSimpleEntity> findAllByOrganization(String organizationId);
    GroupEntity findById(ExecutionContext executionContext, String groupId);
    Set<GroupEntity> findByIds(Set<String> groupIds);
    void associate(final ExecutionContext executionContext, String groupId, String associationType);
    Set<GroupEntity> findByEvent(final String environmentId, GroupEvent event);
    List<GroupEntity> findByName(final String environmentId, String name);

    Set<GroupEntity> findByUser(String username);
    List<ApiEntity> getApis(final String environmentId, String groupId);
    List<ApplicationEntity> getApplications(String groupId);
    int getNumberOfMembers(ExecutionContext executionContext, String groupId);
    boolean isUserAuthorizedToAccessApiData(IndexableApi api, List<String> excludedGroups, String username);
    GroupEntity update(ExecutionContext executionContext, String groupId, UpdateGroupEntity group);
    void updateApiPrimaryOwner(String groupId, String newApiPrimaryOwner);
}
