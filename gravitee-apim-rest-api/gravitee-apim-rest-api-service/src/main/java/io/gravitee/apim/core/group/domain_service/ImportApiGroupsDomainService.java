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
import io.gravitee.apim.core.group.model.Group;
import io.gravitee.apim.core.group.query_service.GroupQueryService;
import io.gravitee.rest.api.model.NewGroupEntity;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.GroupNameAlreadyExistsException;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

/**
 * Resolves API membership groups for import/promotion the same way as V2 import:
 * match by ID then by name; create a group with that name when it does not exist in the target environment.
 */
@RequiredArgsConstructor
@CustomLog
@DomainService
public class ImportApiGroupsDomainService {

    private final GroupQueryService groupQueryService;
    private final GroupService groupService;

    public Set<String> resolveOrCreateGroupIds(Set<String> groupRefs, ExecutionContext executionContext) {
        if (isEmpty(groupRefs)) {
            return groupRefs;
        }

        var environmentId = executionContext.getEnvironmentId();
        var resolved = new HashSet<Group>();
        var pending = new HashSet<>(groupRefs);

        var foundByIds = groupQueryService.findByIds(pending);
        resolved.addAll(foundByIds);
        pending.removeAll(foundByIds.stream().map(Group::getId).collect(Collectors.toSet()));

        if (!pending.isEmpty()) {
            var foundByNames = groupQueryService.findByNames(environmentId, pending);
            resolved.addAll(foundByNames);
            pending.removeAll(foundByNames.stream().map(Group::getName).collect(Collectors.toSet()));
        }

        for (String missingGroupName : pending) {
            resolved.add(findOrCreateGroupByName(missingGroupName, executionContext));
        }

        return resolved.stream().map(Group::getId).collect(Collectors.toSet());
    }

    private Group findOrCreateGroupByName(String name, ExecutionContext executionContext) {
        var environmentId = executionContext.getEnvironmentId();

        var existing = groupService.findByName(environmentId, name);
        if (!existing.isEmpty()) {
            var groupEntity = existing.getFirst();
            return Group.builder().id(groupEntity.getId()).name(groupEntity.getName()).environmentId(environmentId).build();
        }

        try {
            var newGroupEntity = new NewGroupEntity();
            newGroupEntity.setName(name);
            var created = groupService.create(executionContext, newGroupEntity);
            log.info("Group [{}] did not exist in environment [{}] and was created during API import/promotion", name, environmentId);
            return Group.builder().id(created.getId()).name(created.getName()).environmentId(environmentId).build();
        } catch (GroupNameAlreadyExistsException e) {
            return groupService
                .findByName(environmentId, name)
                .stream()
                .findFirst()
                .map(groupEntity ->
                    Group.builder().id(groupEntity.getId()).name(groupEntity.getName()).environmentId(environmentId).build()
                )
                .orElseThrow(() -> e);
        }
    }
}
