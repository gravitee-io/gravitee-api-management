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
import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.GroupRepository;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.model.GroupEvent;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.time.Instant;
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

            Assertions.assertThat(result).contains(
                Group.builder()
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
            when(groupRepository.findByIds(anySet())).thenAnswer(invocation ->
                invocation
                    .getArgument(0, Set.class)
                    .stream()
                    .map(id -> aGroup((String) id).build())
                    .collect(Collectors.toSet())
            );

            var groups = service.findByIds(Set.of("1", "2", "3"));

            Assertions.assertThat(groups).hasSize(3).extracting(Group::getId).containsExactly("1", "2", "3");
        }

        @Test
        @SneakyThrows
        void should_adapt_groups() {
            when(groupRepository.findByIds(anySet())).thenReturn(Set.of(aGroup("1").build()));

            var groups = service.findByIds(Set.of("1", "2", "3"));

            Assertions.assertThat(groups)
                .hasSize(1)
                .containsExactly(
                    Group.builder()
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
            when(groupRepository.findAllByEnvironment(any(String.class))).thenAnswer(invocation ->
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
            when(groupRepository.findAllByEnvironment(any(String.class))).thenAnswer(invocation ->
                Set.of(
                    aGroup("1")
                        .environmentId(invocation.getArgument(0))
                        .eventRules(List.of(new io.gravitee.repository.management.model.GroupEventRule(GroupEvent.API_CREATE)))
                        .build()
                )
            );

            var groups = service.findByEvent("environment-id", Group.GroupEvent.API_CREATE);

            Assertions.assertThat(groups)
                .hasSize(1)
                .containsExactly(
                    Group.builder()
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
            when(groupRepository.findAllByEnvironment(any(String.class))).thenAnswer(invocation ->
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
            when(groupRepository.findAllByEnvironment(any(String.class))).thenAnswer(invocation ->
                Set.of(aGroup("1").environmentId(invocation.getArgument(0)).name("group-1").build())
            );

            var groups = service.findByNames("environment-id", Set.of("group-1"));

            Assertions.assertThat(groups)
                .hasSize(1)
                .containsExactly(
                    Group.builder()
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

    io.gravitee.repository.management.model.Group.GroupBuilder aGroup(String id) {
        return io.gravitee.repository.management.model.Group.builder()
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

    @Nested
    class SearchGroups {

        @Test
        @SneakyThrows
        void should_search_groups_with_pagination() {
            Set<String> groupIds = Set.of("1", "2", "3");
            io.gravitee.rest.api.model.common.Pageable pageable = new PageableImpl(1, 10);
            io.gravitee.repository.management.api.search.Pageable repoPageable = new PageableBuilder().pageNumber(0).pageSize(10).build();

            List<io.gravitee.repository.management.model.Group> groups = List.of(aGroup("1").build(), aGroup("2").build());

            Page<io.gravitee.repository.management.model.Group> page = new Page<>(groups, 0, groups.size(), 2);

            when(groupRepository.search(any(), any())).thenReturn(page);

            ExecutionContext executionContext = mock(ExecutionContext.class);
            Page<Group> result = service.searchGroups(executionContext, groupIds, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent().get(0).getId()).isEqualTo("1");
            assertThat(result.getContent().get(1).getId()).isEqualTo("2");
            assertThat(result.getPageNumber()).isEqualTo(0);
            assertThat(result.getPageElements()).isEqualTo(2);
            assertThat(result.getTotalElements()).isEqualTo(2);
        }

        @Test
        @SneakyThrows
        void should_return_empty_page_when_no_results() {
            Set<String> groupIds = Set.of("1", "2", "3");
            io.gravitee.rest.api.model.common.Pageable pageable = new PageableImpl(1, 10);

            Page<io.gravitee.repository.management.model.Group> page = new Page<>(List.of(), 0, 0, 0);

            when(groupRepository.search(any(), any())).thenReturn(page);

            ExecutionContext executionContext = mock(ExecutionContext.class);
            Page<Group> result = service.searchGroups(executionContext, groupIds, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getPageNumber()).isEqualTo(0);
            assertThat(result.getPageElements()).isEqualTo(0);
            assertThat(result.getTotalElements()).isEqualTo(0);
        }

        @Test
        void should_throw_when_technical_exception_occurs() {
            Set<String> groupIds = Set.of("1", "2", "3");
            io.gravitee.rest.api.model.common.Pageable pageable = new PageableImpl(1, 10);

            when(groupRepository.search(any(), any())).thenThrow(new RuntimeException("Test exception"));

            ExecutionContext executionContext = mock(ExecutionContext.class);
            Throwable throwable = catchThrowable(() -> service.searchGroups(executionContext, groupIds, pageable));

            assertThat(throwable).isInstanceOf(RuntimeException.class).hasMessage("Test exception");
        }
    }
}
