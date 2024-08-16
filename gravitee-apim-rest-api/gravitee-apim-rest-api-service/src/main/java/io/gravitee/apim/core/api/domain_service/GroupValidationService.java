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
package io.gravitee.apim.core.api.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.group.model.Group;
import io.gravitee.apim.core.group.query_service.GroupQueryService;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
@DomainService
public class GroupValidationService {

    private final GroupQueryService groupQueryService;

    public Set<String> validateAndSanitize(final Set<String> groupIds, String environmentId, final PrimaryOwnerEntity primaryOwner) {
        if (groupIds == null || groupIds.isEmpty()) {
            return groupIds;
        }

        var found = groupQueryService.findByIds(groupIds);
        if (found.size() != groupIds.size()) {
            var foundIds = found.stream().map(Group::getId).collect(Collectors.toSet());
            var groupsNotFound = groupIds.stream().filter(groupId -> !foundIds.contains(groupId)).collect(Collectors.toSet());
            throw new InvalidDataException("These groupIds [" + groupsNotFound + "] do not exist");
        }

        var defaultGroups = groupQueryService.findByEvent(environmentId, Group.GroupEvent.API_CREATE);
        found.addAll(defaultGroups);

        var sanitized = found.stream().filter(group -> group.getApiPrimaryOwner() == null).map(Group::getId).collect(Collectors.toSet());
        if (primaryOwner != null && io.gravitee.apim.core.membership.model.PrimaryOwnerEntity.Type.GROUP.equals(primaryOwner.type())) {
            sanitized.add(primaryOwner.id());
        }

        return sanitized;
    }
}
