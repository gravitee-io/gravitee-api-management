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
package io.gravitee.apim.infra.query_service.group;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.group.model.Group;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.GroupRepository;
import io.gravitee.repository.management.model.GroupEvent;
import io.gravitee.rest.api.model.GroupEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class GroupQueryServiceImplTest {

    GroupRepository groupRepository;

    GroupQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        groupRepository = mock(GroupRepository.class);
        service = new GroupQueryServiceImpl(groupRepository);
    }

    @Nested
    class FindById {

        @Test
        @SneakyThrows
        void should_find_groups_matching_the_ids_provided() {
            when(groupRepository.findById(any())).thenAnswer(invocation -> Optional.of(aGroup(invocation.getArgument(0)).build()));

            var result = service.findById("1");

            Assertions
                .assertThat(result)
                .contains(
                    Group
                        .builder()
                        .id("1")
                        .name("group-1")
                        .environmentId("environment-id")
                        .eventRules(List.of(new Group.GroupEventRule(Group.GroupEvent.API_CREATE)))
                        .createdAt(Instant.parse("2020-02-01T20:22:02.00Z").atZone(ZoneId.systemDefault()))
                        .updatedAt(Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneId.systemDefault()))
                        .maxInvitation(12)
                        .lockApiRole(true)
                        .lockApplicationRole(true)
                        .systemInvitation(true)
                        .emailInvitation(true)
                        .disableMembershipNotifications(true)
                        .apiPrimaryOwner("api-po-id")
                        .build()
                );
        }

        @Test
        @SneakyThrows
        void should_return_empty_when_no_group_match() {
            when(groupRepository.findById(any())).thenAnswer(invocation -> Optional.empty());

            var result = service.findById("1");

            Assertions.assertThat(result).isEmpty();
        }

        @Test
        void should_throw_when_technical_exception_occurs() throws TechnicalException {
            // Given
            when(groupRepository.findById(any())).thenThrow(TechnicalException.class);

            // When
            Throwable throwable = catchThrowable(() -> service.findById("123"));

            // Then
            assertThat(throwable)
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage("An error occurs while trying to find group by id: 123");
        }
    }

    @Nested
    class FindByIds {

        @Test
        @SneakyThrows
        void should_find_groups_matching_the_ids_provided() {
            when(groupRepository.findByIds(anySet()))
                .thenAnswer(invocation ->
                    invocation.getArgument(0, Set.class).stream().map(id -> aGroup((String) id).build()).collect(Collectors.toSet())
                );

            var groups = service.findByIds(Set.of("1", "2", "3"));

            Assertions.assertThat(groups).hasSize(3).extracting(Group::getId).containsExactly("1", "2", "3");
        }

        @Test
        @SneakyThrows
        void should_adapt_groups() {
            when(groupRepository.findByIds(anySet())).thenReturn(Set.of(aGroup("1").build()));

            var groups = service.findByIds(Set.of("1", "2", "3"));

            Assertions
                .assertThat(groups)
                .hasSize(1)
                .containsExactly(
                    Group
                        .builder()
                        .id("1")
                        .name("group-1")
                        .environmentId("environment-id")
                        .eventRules(List.of(new Group.GroupEventRule(Group.GroupEvent.API_CREATE)))
                        .createdAt(Instant.parse("2020-02-01T20:22:02.00Z").atZone(ZoneId.systemDefault()))
                        .updatedAt(Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneId.systemDefault()))
                        .maxInvitation(12)
                        .lockApiRole(true)
                        .lockApplicationRole(true)
                        .systemInvitation(true)
                        .emailInvitation(true)
                        .disableMembershipNotifications(true)
                        .apiPrimaryOwner("api-po-id")
                        .build()
                );
        }
    }

    @Nested
    class FindByEvent {

        @Test
        @SneakyThrows
        void should_find_groups_matching_the_event_provided() {
            when(groupRepository.findAllByEnvironment(any(String.class)))
                .thenAnswer(invocation ->
                    Set.of(
                        aGroup("1")
                            .environmentId(invocation.getArgument(0))
                            .eventRules(List.of(new io.gravitee.repository.management.model.GroupEventRule(GroupEvent.API_CREATE)))
                            .build(),
                        aGroup("2")
                            .environmentId(invocation.getArgument(0))
                            .eventRules(
                                List.of(
                                    new io.gravitee.repository.management.model.GroupEventRule(GroupEvent.API_CREATE),
                                    new io.gravitee.repository.management.model.GroupEventRule(GroupEvent.APPLICATION_CREATE)
                                )
                            )
                            .build(),
                        aGroup("3")
                            .environmentId(invocation.getArgument(0))
                            .eventRules(List.of(new io.gravitee.repository.management.model.GroupEventRule(GroupEvent.APPLICATION_CREATE)))
                            .build()
                    )
                );

            var groups = service.findByEvent("environment-id", Group.GroupEvent.API_CREATE);

            Assertions.assertThat(groups).hasSize(2).extracting(Group::getId).containsExactly("1", "2");
        }

        @Test
        @SneakyThrows
        void should_adapt_groups() {
            when(groupRepository.findAllByEnvironment(any(String.class)))
                .thenAnswer(invocation ->
                    Set.of(
                        aGroup("1")
                            .environmentId(invocation.getArgument(0))
                            .eventRules(List.of(new io.gravitee.repository.management.model.GroupEventRule(GroupEvent.API_CREATE)))
                            .build()
                    )
                );

            var groups = service.findByEvent("environment-id", Group.GroupEvent.API_CREATE);

            Assertions
                .assertThat(groups)
                .hasSize(1)
                .containsExactly(
                    Group
                        .builder()
                        .id("1")
                        .name("group-1")
                        .environmentId("environment-id")
                        .eventRules(List.of(new Group.GroupEventRule(Group.GroupEvent.API_CREATE)))
                        .createdAt(Instant.parse("2020-02-01T20:22:02.00Z").atZone(ZoneId.systemDefault()))
                        .updatedAt(Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneId.systemDefault()))
                        .maxInvitation(12)
                        .lockApiRole(true)
                        .lockApplicationRole(true)
                        .systemInvitation(true)
                        .emailInvitation(true)
                        .disableMembershipNotifications(true)
                        .apiPrimaryOwner("api-po-id")
                        .build()
                );
        }
    }

    @Nested
    class FindByNames {

        @Test
        @SneakyThrows
        void should_find_groups_matching_the_event_provided() {
            when(groupRepository.findAllByEnvironment(any(String.class)))
                .thenAnswer(invocation ->
                    Set.of(
                        aGroup("1").environmentId(invocation.getArgument(0)).name("group-1").build(),
                        aGroup("2").environmentId(invocation.getArgument(0)).name("group-2").build()
                    )
                );

            var groups = service.findByNames("environment-id", Set.of("group-2"));

            Assertions.assertThat(groups).hasSize(1).extracting(Group::getId).containsExactly("2");
        }

        @Test
        @SneakyThrows
        void should_adapt_groups() {
            when(groupRepository.findAllByEnvironment(any(String.class)))
                .thenAnswer(invocation -> Set.of(aGroup("1").environmentId(invocation.getArgument(0)).name("group-1").build()));

            var groups = service.findByNames("environment-id", Set.of("group-1"));

            Assertions
                .assertThat(groups)
                .hasSize(1)
                .containsExactly(
                    Group
                        .builder()
                        .id("1")
                        .name("group-1")
                        .environmentId("environment-id")
                        .eventRules(List.of(new Group.GroupEventRule(Group.GroupEvent.API_CREATE)))
                        .createdAt(Instant.parse("2020-02-01T20:22:02.00Z").atZone(ZoneId.systemDefault()))
                        .updatedAt(Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneId.systemDefault()))
                        .maxInvitation(12)
                        .lockApiRole(true)
                        .lockApplicationRole(true)
                        .systemInvitation(true)
                        .emailInvitation(true)
                        .disableMembershipNotifications(true)
                        .apiPrimaryOwner("api-po-id")
                        .build()
                );
        }
    }

    @Nested
    class FindGroupsInEnvironmentByIds {

        @Test
        @SneakyThrows
        void should_find_groups_matching_the_ids_and_environment() {
            when(groupRepository.findByIds(anySet()))
                .thenAnswer(invocation ->
                    Set.of(
                        aGroup("1").environmentId("environment-id").build(),
                        aGroup("2").environmentId("environment-id").build(),
                        aGroup("3").environmentId("other-environment-id").build()
                    )
                );

            var executionContext = mock(io.gravitee.rest.api.service.common.ExecutionContext.class);
            when(executionContext.hasEnvironmentId()).thenReturn(true);
            when(executionContext.getEnvironmentId()).thenReturn("environment-id");

            var groupEntities = service.findGroupsInEnvironmentByIds(executionContext, Set.of("1", "2", "3"));

            Assertions.assertThat(groupEntities).hasSize(2).extracting(GroupEntity::getId).containsExactly("1", "2");
        }

        @Test
        @SneakyThrows
        void should_return_all_groups_when_no_environment_specified() {
            when(groupRepository.findByIds(anySet()))
                .thenAnswer(invocation ->
                    Set.of(
                        aGroup("1").environmentId("environment-id").build(),
                        aGroup("2").environmentId("environment-id").build(),
                        aGroup("3").environmentId("other-environment-id").build()
                    )
                );

            var executionContext = mock(io.gravitee.rest.api.service.common.ExecutionContext.class);
            when(executionContext.hasEnvironmentId()).thenReturn(false);

            var groupEntities = service.findGroupsInEnvironmentByIds(executionContext, Set.of("1", "2", "3"));

            Assertions.assertThat(groupEntities).hasSize(3).extracting(GroupEntity::getId).containsExactly("1", "2", "3");
        }

        @Test
        @SneakyThrows
        void should_adapt_groups_to_entities() {
            when(groupRepository.findByIds(anySet())).thenAnswer(invocation -> Set.of(aGroup("1").environmentId("environment-id").build()));

            var executionContext = mock(io.gravitee.rest.api.service.common.ExecutionContext.class);
            when(executionContext.hasEnvironmentId()).thenReturn(true);
            when(executionContext.getEnvironmentId()).thenReturn("environment-id");

            var groupEntities = service.findGroupsInEnvironmentByIds(executionContext, Set.of("1"));

            Assertions
                .assertThat(groupEntities)
                .hasSize(1)
                .extracting(
                    GroupEntity::getId,
                    GroupEntity::getName,
                    GroupEntity::getMaxInvitation,
                    GroupEntity::isLockApiRole,
                    GroupEntity::isLockApplicationRole,
                    GroupEntity::isSystemInvitation,
                    GroupEntity::isEmailInvitation,
                    GroupEntity::isDisableMembershipNotifications
                )
                .containsExactly(org.assertj.core.api.Assertions.tuple("1", "group-1", 12, true, true, true, true, true));
        }

        @Test
        @SneakyThrows
        void should_handle_empty_group_ids() {
            var executionContext = mock(io.gravitee.rest.api.service.common.ExecutionContext.class);
            when(executionContext.hasEnvironmentId()).thenReturn(true);
            when(executionContext.getEnvironmentId()).thenReturn("environment-id");

            var groupEntities = service.findGroupsInEnvironmentByIds(executionContext, Set.of());

            Assertions.assertThat(groupEntities).isEmpty();
        }

        @Test
        @SneakyThrows
        void should_handle_case_insensitive_environment_id_comparison() {
            when(groupRepository.findByIds(anySet()))
                .thenAnswer(invocation ->
                    Set.of(
                        aGroup("1").environmentId("ENVIRONMENT-ID").build(),
                        aGroup("2").environmentId("environment-id").build(),
                        aGroup("3").environmentId("other-environment-id").build()
                    )
                );

            var executionContext = mock(io.gravitee.rest.api.service.common.ExecutionContext.class);
            when(executionContext.hasEnvironmentId()).thenReturn(true);
            when(executionContext.getEnvironmentId()).thenReturn("environment-id");

            var groupEntities = service.findGroupsInEnvironmentByIds(executionContext, Set.of("1", "2", "3"));

            Assertions.assertThat(groupEntities).hasSize(2).extracting(GroupEntity::getId).containsExactly("1", "2");
        }

        @Test
        @SneakyThrows
        void should_handle_null_environment_id_in_group() {
            when(groupRepository.findByIds(anySet()))
                .thenAnswer(invocation ->
                    Set.of(aGroup("1").environmentId(null).build(), aGroup("2").environmentId("environment-id").build())
                );

            var executionContext = mock(io.gravitee.rest.api.service.common.ExecutionContext.class);
            when(executionContext.hasEnvironmentId()).thenReturn(true);
            when(executionContext.getEnvironmentId()).thenReturn("environment-id");

            var groupEntities = service.findGroupsInEnvironmentByIds(executionContext, Set.of("1", "2"));

            Assertions.assertThat(groupEntities).hasSize(1).extracting(GroupEntity::getId).containsExactly("2");
        }

        @Test
        void should_throw_when_technical_exception_occurs() throws TechnicalException {
            when(groupRepository.findByIds(anySet())).thenThrow(TechnicalException.class);
            var executionContext = mock(io.gravitee.rest.api.service.common.ExecutionContext.class);

            Throwable throwable = catchThrowable(() -> service.findGroupsInEnvironmentByIds(executionContext, Set.of("1", "2")));

            assertThat(throwable)
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage("An error occurs while trying to find groups by ids and environment");
        }
    }

    io.gravitee.repository.management.model.Group.GroupBuilder aGroup(String id) {
        return io.gravitee.repository.management.model.Group
            .builder()
            .id(id)
            .name("group-" + id)
            .environmentId("environment-id")
            .eventRules(List.of(new io.gravitee.repository.management.model.GroupEventRule(GroupEvent.API_CREATE)))
            .createdAt(Date.from(Instant.parse("2020-02-01T20:22:02.00Z")))
            .updatedAt(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")))
            .maxInvitation(12)
            .lockApiRole(true)
            .lockApplicationRole(true)
            .systemInvitation(true)
            .emailInvitation(true)
            .disableMembershipNotifications(true)
            .apiPrimaryOwner("api-po-id");
    }
}
