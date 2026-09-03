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
package inmemory;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.group.model.Group;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.model.GroupEntity;
import io.gravitee.rest.api.model.NewGroupEntity;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.exceptions.GroupNameAlreadyExistsException;
import java.util.List;
import java.util.Set;

/**
 * Test delegate that mirrors {@link io.gravitee.rest.api.service.impl.GroupServiceImpl} group creation semantics
 * using in-memory stores (including duplicate-name checks).
 */
public final class GroupServiceTestDelegate {

    private GroupServiceTestDelegate() {}

    public static GroupService create(GroupQueryServiceInMemory groupQueryService, GroupCrudServiceInMemory groupCrudService) {
        GroupService groupService = mock(GroupService.class);

        when(groupService.findByName(any(), any())).thenAnswer(invocation -> {
            String environmentId = invocation.getArgument(0);
            String name = invocation.getArgument(1);
            return groupQueryService
                .findByNames(environmentId, Set.of(name))
                .stream()
                .map(GroupServiceTestDelegate::toGroupEntity)
                .toList();
        });

        when(groupService.create(any(ExecutionContext.class), any(NewGroupEntity.class))).thenAnswer(invocation -> {
            ExecutionContext executionContext = invocation.getArgument(0);
            NewGroupEntity newGroupEntity = invocation.getArgument(1);
            String environmentId = executionContext.getEnvironmentId();
            String name = newGroupEntity.getName();

            if (!groupService.findByName(environmentId, name).isEmpty()) {
                throw new GroupNameAlreadyExistsException(name);
            }

            var now = TimeProvider.now();
            var created = groupCrudService.create(
                Group.builder()
                    .id(UuidString.generateRandom())
                    .environmentId(environmentId)
                    .name(name)
                    .createdAt(now)
                    .updatedAt(now)
                    .build()
            );
            return toGroupEntity(created);
        });

        return groupService;
    }

    private static GroupEntity toGroupEntity(Group group) {
        return GroupEntity.builder().id(group.getId()).name(group.getName()).build();
    }
}
