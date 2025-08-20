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
package io.gravitee.apim.infra.query_service.membership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.model.Membership;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MembershipQueryServiceImplTest {

    MembershipRepository membershipRepository;

    MembershipQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        membershipRepository = mock(MembershipRepository.class);
        service = new MembershipQueryServiceImpl(membershipRepository);
    }

    @Nested
    class FindByReferenceAndRoleId {

        @Test
        @SneakyThrows
        void should_return_all_memberships_matching() {
            when(membershipRepository.findByReferenceAndRoleId(any(), any(), any()))
                .thenAnswer(invocation ->
                    Set.of(
                        Membership
                            .builder()
                            .referenceType(invocation.getArgument(0))
                            .referenceId(invocation.getArgument(1))
                            .roleId(invocation.getArgument(2))
                            .id("membership-id")
                            .memberType(io.gravitee.repository.management.model.MembershipMemberType.USER)
                            .memberId("user-id")
                            .createdAt(Date.from(Instant.parse("2020-02-01T20:22:02.00Z")))
                            .updatedAt(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")))
                            .source("system")
                            .build()
                    )
                );

            var result = service.findByReferenceAndRoleId(
                io.gravitee.apim.core.membership.model.Membership.ReferenceType.API,
                "api-id",
                "role-id"
            );

            assertThat(result)
                .hasSize(1)
                .containsExactly(
                    io.gravitee.apim.core.membership.model.Membership
                        .builder()
                        .id("membership-id")
                        .referenceType(io.gravitee.apim.core.membership.model.Membership.ReferenceType.API)
                        .referenceId("api-id")
                        .createdAt(Instant.parse("2020-02-01T20:22:02.00Z").atZone(ZoneId.systemDefault()))
                        .updatedAt(Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneId.systemDefault()))
                        .memberId("user-id")
                        .memberType(io.gravitee.apim.core.membership.model.Membership.Type.USER)
                        .roleId("role-id")
                        .source("system")
                        .build()
                );
        }

        @Test
        @SneakyThrows
        void should_return_empty_collection_when_no_match() {
            when(membershipRepository.findByReferenceAndRoleId(any(), any(), any())).thenAnswer(invocation -> Set.of());

            var result = service.findByReferenceAndRoleId(
                io.gravitee.apim.core.membership.model.Membership.ReferenceType.API,
                "api-id",
                "role-id"
            );

            assertThat(result).isEmpty();
        }

        @Test
        void should_throw_when_technical_exception_occurs() throws TechnicalException {
            // Given
            when(membershipRepository.findByReferenceAndRoleId(any(), any(), any())).thenThrow(TechnicalException.class);

            // When
            Throwable throwable = catchThrowable(() ->
                service.findByReferenceAndRoleId(io.gravitee.apim.core.membership.model.Membership.ReferenceType.API, "api-id", "role-id")
            );

            // Then
            assertThat(throwable)
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage("An error occurs while trying to find API membership");
        }
    }

    @Nested
    class FindByReferencesAndRoleId {

        @Test
        @SneakyThrows
        void should_return_all_memberships_matching() {
            when(membershipRepository.findByReferencesAndRoleId(any(), anyList(), any()))
                .thenAnswer(invocation -> {
                    return invocation
                        .getArgument(1, List.class)
                        .stream()
                        .map(referenceId ->
                            Membership
                                .builder()
                                .referenceType(invocation.getArgument(0))
                                .referenceId((String) referenceId)
                                .roleId(invocation.getArgument(2))
                                .id("membership-id-" + referenceId)
                                .memberType(io.gravitee.repository.management.model.MembershipMemberType.USER)
                                .memberId("user-id")
                                .createdAt(Date.from(Instant.parse("2020-02-01T20:22:02.00Z")))
                                .updatedAt(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")))
                                .source("system")
                                .build()
                        )
                        .collect(Collectors.toSet());
                });

            var result = service.findByReferencesAndRoleId(
                io.gravitee.apim.core.membership.model.Membership.ReferenceType.API,
                List.of("api1", "api2"),
                "role-id"
            );

            assertThat(result)
                .hasSize(2)
                .containsExactlyInAnyOrder(
                    io.gravitee.apim.core.membership.model.Membership
                        .builder()
                        .id("membership-id-api1")
                        .referenceType(io.gravitee.apim.core.membership.model.Membership.ReferenceType.API)
                        .referenceId("api1")
                        .createdAt(Instant.parse("2020-02-01T20:22:02.00Z").atZone(ZoneId.systemDefault()))
                        .updatedAt(Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneId.systemDefault()))
                        .memberId("user-id")
                        .memberType(io.gravitee.apim.core.membership.model.Membership.Type.USER)
                        .roleId("role-id")
                        .source("system")
                        .build(),
                    io.gravitee.apim.core.membership.model.Membership
                        .builder()
                        .id("membership-id-api2")
                        .referenceType(io.gravitee.apim.core.membership.model.Membership.ReferenceType.API)
                        .referenceId("api2")
                        .createdAt(Instant.parse("2020-02-01T20:22:02.00Z").atZone(ZoneId.systemDefault()))
                        .updatedAt(Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneId.systemDefault()))
                        .memberId("user-id")
                        .memberType(io.gravitee.apim.core.membership.model.Membership.Type.USER)
                        .roleId("role-id")
                        .source("system")
                        .build()
                );
        }

        @Test
        @SneakyThrows
        void should_return_empty_collection_when_no_match() {
            when(membershipRepository.findByReferencesAndRoleId(any(), any(), any())).thenAnswer(invocation -> Set.of());

            var result = service.findByReferencesAndRoleId(
                io.gravitee.apim.core.membership.model.Membership.ReferenceType.API,
                List.of("api1", "api2"),
                "role-id"
            );

            assertThat(result).isEmpty();
        }

        @Test
        void should_throw_when_technical_exception_occurs() throws TechnicalException {
            // Given
            when(membershipRepository.findByReferencesAndRoleId(any(), any(), any())).thenThrow(TechnicalException.class);

            // When
            Throwable throwable = catchThrowable(() ->
                service.findByReferencesAndRoleId(
                    io.gravitee.apim.core.membership.model.Membership.ReferenceType.API,
                    List.of("api1", "api2"),
                    "role-id"
                )
            );

            // Then
            assertThat(throwable)
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage("An error occurs while trying to find API membership");
        }
    }

    @Nested
    class FindGroupsThatUserBelongsTo {

        @Test
        @SneakyThrows
        void should_return_all_group_memberships_where_user_is_a_member() {
            when(membershipRepository.findByMemberIdAndMemberTypeAndReferenceType(any(), any(), any()))
                .thenAnswer(invocation ->
                    Set.of(
                        Membership
                            .builder()
                            .referenceType(invocation.getArgument(2))
                            .referenceId("group-1")
                            .roleId("role-id")
                            .id("membership-1")
                            .memberType(io.gravitee.repository.management.model.MembershipMemberType.USER)
                            .memberId(invocation.getArgument(0))
                            .createdAt(Date.from(Instant.parse("2020-02-01T20:22:02.00Z")))
                            .updatedAt(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")))
                            .source("system")
                            .build()
                    )
                );

            var result = service.findGroupsThatUserBelongsTo("user-id");

            assertThat(result)
                .hasSize(1)
                .containsExactly(
                    io.gravitee.apim.core.membership.model.Membership
                        .builder()
                        .id("membership-1")
                        .referenceType(io.gravitee.apim.core.membership.model.Membership.ReferenceType.GROUP)
                        .referenceId("group-1")
                        .createdAt(Instant.parse("2020-02-01T20:22:02.00Z").atZone(ZoneId.systemDefault()))
                        .updatedAt(Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneId.systemDefault()))
                        .memberId("user-id")
                        .memberType(io.gravitee.apim.core.membership.model.Membership.Type.USER)
                        .roleId("role-id")
                        .source("system")
                        .build()
                );
        }

        @Test
        @SneakyThrows
        void should_return_empty_collection_when_no_match() {
            when(membershipRepository.findByMemberIdAndMemberTypeAndReferenceType(any(), any(), any())).thenAnswer(invocation -> Set.of());

            var result = service.findGroupsThatUserBelongsTo("user-id");

            assertThat(result).isEmpty();
        }

        @Test
        void should_throw_when_technical_exception_occurs() throws TechnicalException {
            // Given
            when(membershipRepository.findByMemberIdAndMemberTypeAndReferenceType(any(), any(), any())).thenThrow(TechnicalException.class);

            // When
            Throwable throwable = catchThrowable(() -> service.findGroupsThatUserBelongsTo("user-id"));

            // Then
            assertThat(throwable)
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage("An error occurs while trying to find Group memberships of member user-id");
        }
    }

    @Nested
    class FindClustersIdsThatUserBelongsTo {

        @Test
        @SneakyThrows
        void should_return_all_cluster_ids_where_user_is_a_member() {
            when(membershipRepository.findByMemberIdAndMemberTypeAndReferenceType(any(), any(), any()))
                .thenAnswer(invocation ->
                    Set.of(
                        Membership
                            .builder()
                            .referenceType(invocation.getArgument(2))
                            .referenceId("cluster-1")
                            .roleId("role-id")
                            .id("membership-1")
                            .memberType(io.gravitee.repository.management.model.MembershipMemberType.USER)
                            .memberId(invocation.getArgument(0))
                            .createdAt(Date.from(Instant.parse("2020-02-01T20:22:02.00Z")))
                            .updatedAt(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")))
                            .source("system")
                            .build()
                    )
                );

            var result = service.findClustersIdsThatUserBelongsTo("user-id");

            assertThat(result).hasSize(1).containsExactly("cluster-1");
        }

        @Test
        @SneakyThrows
        void should_return_empty_collection_when_no_match() {
            when(membershipRepository.findByMemberIdAndMemberTypeAndReferenceType(any(), any(), any())).thenAnswer(invocation -> Set.of());
            var result = service.findClustersIdsThatUserBelongsTo("user-id");
            assertThat(result).isEmpty();
        }

        @Test
        void should_throw_when_technical_exception_occurs() throws TechnicalException {
            // Given
            when(membershipRepository.findByMemberIdAndMemberTypeAndReferenceType(any(), any(), any())).thenThrow(TechnicalException.class);
            // When
            Throwable throwable = catchThrowable(() -> service.findClustersIdsThatUserBelongsTo("user-id"));
            // Then
            assertThat(throwable)
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage("An error occured while trying to find clusters ids of member user-id");
        }
    }
}
