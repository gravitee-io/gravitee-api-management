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

import java.util.List;
import java.util.Set;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author Florent CHAMFROY (forent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface GroupService {

    void                    addUserToGroup                      (String groupId, String username, String... roleIds);
    GroupEntity             create                              (NewGroupEntity group);
    void                    delete                              (String groupId);
    void                    deleteUserFromGroup                 (String groupId, String username);
    List<GroupEntity>       findAll                             ();
    GroupEntity             findById                            (String groupId);
    Set<GroupEntity>        findByIds                           (Set<String> groupIds);
    void                    associate(String groupId, String associationType);
    Set<GroupEntity>        findByEvent                         (GroupEvent event);
    List<GroupEntity>       findByName                          (String name);
    Set<GroupEntity>        findByUser                          (String username);
    List<ApiEntity>         getApis                             (String groupId);
    List<ApplicationEntity> getApplications                     (String groupId);
    int                     getNumberOfMembers                  (String groupId);
    boolean                 isUserAuthorizedToAccessApiData     (ApiEntity api, List<String> excludedGroups, String username);
    boolean                 isUserAuthorizedToAccessPortalData  (List<String> excludedGroups, String username);
    GroupEntity             update                              (String groupId, UpdateGroupEntity group);
}