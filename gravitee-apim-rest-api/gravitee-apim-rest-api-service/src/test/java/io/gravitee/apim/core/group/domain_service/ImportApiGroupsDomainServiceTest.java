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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.core.model.GroupFixtures;
import inmemory.GroupCrudServiceInMemory;
import inmemory.GroupQueryServiceInMemory;
import inmemory.GroupServiceTestDelegate;
import io.gravitee.rest.api.model.GroupEntity;
import io.gravitee.rest.api.model.NewGroupEntity;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.GroupNameAlreadyExistsException;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ImportApiGroupsDomainServiceTest {

    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final ExecutionContext EXECUTION_CONTEXT = new ExecutionContext(ORGANIZATION_ID, ENVIRONMENT_ID);

    private final GroupQueryServiceInMemory groupQueryService = new GroupQueryServiceInMemory();
    private final GroupCrudServiceInMemory groupCrudService = new GroupCrudServiceInMemory(groupQueryService);

    @Mock
    private GroupService groupService;

    private ImportApiGroupsDomainService service;

    @BeforeEach
    void setUp() {
        service = new ImportApiGroupsDomainService(groupQueryService, groupService);
    }

    @AfterEach
    void tearDown() {
        groupQueryService.reset();
        groupCrudService.reset();
    }

    @Test
    void should_resolve_existing_group_by_id() {
        groupQueryService.initWith(
            List.of(GroupFixtures.aGroup("group-1").toBuilder().name("Helios").environmentId(ENVIRONMENT_ID).build())
        );

        var resolved = service.resolveOrCreateGroupIds(Set.of("group-1"), EXECUTION_CONTEXT);

        assertThat(resolved).containsExactly("group-1");
        verify(groupService, never()).create(any(), any());
    }

    @Test
    void should_resolve_existing_group_by_name() {
        groupQueryService.initWith(
            List.of(GroupFixtures.aGroup("group-1").toBuilder().name("Helios").environmentId(ENVIRONMENT_ID).build())
        );

        var resolved = service.resolveOrCreateGroupIds(Set.of("Helios"), EXECUTION_CONTEXT);

        assertThat(resolved).containsExactly("group-1");
        verify(groupService, never()).create(any(), any());
    }

    @Test
    void should_create_missing_group_by_name_and_return_its_id() {
        when(groupService.findByName(ENVIRONMENT_ID, "Helios")).thenReturn(List.of());
        when(groupService.create(any(ExecutionContext.class), any(NewGroupEntity.class))).thenAnswer(invocation -> {
            NewGroupEntity newGroupEntity = invocation.getArgument(1);
            return GroupEntity.builder().id("created-group-id").name(newGroupEntity.getName()).build();
        });

        var resolved = service.resolveOrCreateGroupIds(Set.of("Helios"), EXECUTION_CONTEXT);

        assertThat(resolved).containsExactly("created-group-id");
        verify(groupService).create(eq(EXECUTION_CONTEXT), argThat(entity -> "Helios".equals(entity.getName())));
    }

    @Test
    void should_reuse_group_when_name_already_exists_on_create() {
        when(groupService.findByName(ENVIRONMENT_ID, "Helios")).thenReturn(
            List.of(),
            List.of(GroupEntity.builder().id("existing-group-id").name("Helios").build())
        );
        when(groupService.create(any(ExecutionContext.class), any(NewGroupEntity.class))).thenThrow(
            new GroupNameAlreadyExistsException("Helios")
        );

        var resolved = service.resolveOrCreateGroupIds(Set.of("Helios"), EXECUTION_CONTEXT);

        assertThat(resolved).containsExactly("existing-group-id");
    }

    @Test
    void should_keep_matching_group_and_create_missing_one() {
        groupQueryService.initWith(
            List.of(GroupFixtures.aGroup("developers-id").toBuilder().name("Developers").environmentId(ENVIRONMENT_ID).build())
        );
        when(groupService.findByName(ENVIRONMENT_ID, "Helios")).thenReturn(List.of());
        when(groupService.create(any(ExecutionContext.class), any(NewGroupEntity.class))).thenReturn(
            GroupEntity.builder().id("helios-id").name("Helios").build()
        );

        var resolved = service.resolveOrCreateGroupIds(Set.of("Developers", "Helios"), EXECUTION_CONTEXT);

        assertThat(resolved).hasSize(2).contains("developers-id", "helios-id");
    }

    @Test
    void should_delegate_to_group_service_with_duplicate_name_checks_in_tests() {
        service = new ImportApiGroupsDomainService(groupQueryService, GroupServiceTestDelegate.create(groupQueryService, groupCrudService));

        var resolved = service.resolveOrCreateGroupIds(Set.of("Helios"), EXECUTION_CONTEXT);

        assertThat(resolved).hasSize(1);
        assertThat(groupCrudService.storage().values()).anyMatch(group -> "Helios".equals(group.getName()));
    }

    @Test
    void should_return_null_when_group_refs_are_null() {
        assertThat(service.resolveOrCreateGroupIds(null, EXECUTION_CONTEXT)).isNull();
    }

    @Test
    void should_return_empty_when_group_refs_are_empty() {
        assertThat(service.resolveOrCreateGroupIds(Set.of(), EXECUTION_CONTEXT)).isEmpty();
    }

    private static NewGroupEntity newGroupEntity(String name) {
        var newGroupEntity = new NewGroupEntity();
        newGroupEntity.setName(name);
        return newGroupEntity;
    }
}
