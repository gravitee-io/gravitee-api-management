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
package io.gravitee.apim.core.group.domain_service;

import static io.gravitee.apim.core.utils.CollectionUtils.isEmpty;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.group.crud_service.GroupCrudService;
import io.gravitee.apim.core.group.model.Group;
import io.gravitee.apim.core.group.query_service.GroupQueryService;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.service.common.UuidString;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

/**
 * Resolves API membership groups for import/promotion:
 * match groups in the target environment by name; auto-create an empty group when missing.
 * <p>
 * Group refs in the definition are treated as names (not IDs), matching V2.
 */
@RequiredArgsConstructor
@CustomLog
@DomainService
public class ImportApiGroupsDomainService {

    private final GroupQueryService groupQueryService;
    private final GroupCrudService groupCrudService;

    public Set<String> resolveOrCreateGroupIds(Set<String> groupRefs, String environmentId) {
        if (isEmpty(groupRefs)) {
            return groupRefs;
        }

        var resolved = new HashSet<Group>();
        var missingNames = new HashSet<>(groupRefs);

        resolveExistingByName(resolved, missingNames, environmentId);

        for (String missingGroupName : missingNames) {
            var created = createEmptyGroup(missingGroupName, environmentId);
            resolved.add(created);
            log.warn(
                "Group [{}] did not exist in environment [{}] and was created empty during API import/promotion. ",
                missingGroupName,
                environmentId
            );
        }

        return resolved.stream().map(Group::getId).collect(Collectors.toSet());
    }

    private void resolveExistingByName(Set<Group> resolved, Set<String> missingNames, String environmentId) {
        if (missingNames.isEmpty()) {
            return;
        }
        var foundByNames = groupQueryService.findByNames(environmentId, missingNames);
        resolved.addAll(foundByNames);
        missingNames.removeAll(foundByNames.stream().map(Group::getName).collect(Collectors.toSet()));
    }

    private Group createEmptyGroup(String name, String environmentId) {
        var now = TimeProvider.now();
        return groupCrudService.create(
            Group.builder().id(UuidString.generateRandom()).environmentId(environmentId).name(name).createdAt(now).updatedAt(now).build()
        );
    }
}
