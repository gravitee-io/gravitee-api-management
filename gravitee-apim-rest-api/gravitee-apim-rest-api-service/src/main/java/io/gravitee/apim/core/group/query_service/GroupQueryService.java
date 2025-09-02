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
package io.gravitee.apim.core.group.query_service;

import io.gravitee.apim.core.group.model.Group;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.GroupEntity;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface GroupQueryService {
    Optional<Group> findById(String id);
    Set<Group> findByIds(Set<String> ids);
    Set<Group> findByEvent(String environmentId, Group.GroupEvent event);
    List<Group> findByNames(String environmentId, Set<String> name);
    Page<Group> searchGroups(ExecutionContext executionContext, Set<String> groupIds, Pageable pageable);
}
