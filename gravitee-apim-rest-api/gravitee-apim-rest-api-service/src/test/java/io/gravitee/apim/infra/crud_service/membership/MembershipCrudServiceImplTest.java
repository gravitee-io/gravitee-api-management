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
package io.gravitee.apim.infra.crud_service.membership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.core.model.MembershipFixtures;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.MembershipRepository;
import java.time.Instant;
import java.util.Date;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class MembershipCrudServiceImplTest {

    MembershipRepository membershipRepository;

    MembershipCrudServiceImpl service;

    @BeforeEach
    void setUp() {
        membershipRepository = mock(MembershipRepository.class);

        service = new MembershipCrudServiceImpl(membershipRepository);
    }

    @Nested
    class Create {

        @Test
        @SneakyThrows
        void should_create_a_membership() {
            var membership = MembershipFixtures.anApiMembership("api-id");
            service.create(membership);

            var captor = ArgumentCaptor.forClass(io.gravitee.repository.management.model.Membership.class);
            verify(membershipRepository).create(captor.capture());

            assertThat(captor.getValue())
                .usingRecursiveComparison()
                .isEqualTo(
                    io.gravitee.repository.management.model.Membership.builder()
                        .id("membership-id")
                        .referenceType(io.gravitee.repository.management.model.MembershipReferenceType.API)
                        .referenceId("api-id")
                        .createdAt(Date.from(Instant.parse("2020-02-01T20:22:02.00Z")))
                        .updatedAt(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")))
                        .memberId("user-id")
                        .memberType(io.gravitee.repository.management.model.MembershipMemberType.USER)
                        .roleId("role-id")
                        .source("system")
                        .build()
                );
        }

        @Test
        @SneakyThrows
        void should_return_the_created_membership() {
            when(membershipRepository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

            var toCreate = MembershipFixtures.anApiMembership();
            var result = service.create(toCreate);

            assertThat(result).isEqualTo(toCreate);
        }

        @Test
        void should_throw_when_technical_exception_occurs() throws TechnicalException {
            // Given
            when(membershipRepository.create(any())).thenThrow(TechnicalException.class);

            // When
            Throwable throwable = catchThrowable(() -> service.create(MembershipFixtures.anApiMembership()));

            // Then
            assertThat(throwable)
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage("An error occurs while trying to create the membership: membership-id");
        }
    }

    @Nested
    class Delete {

        @Test
        void should_delete_membership() throws TechnicalException {
            service.delete("membership-id");
            verify(membershipRepository).delete("membership-id");
        }

        @Test
        void should_throw_if_deletion_problem_occurs() throws TechnicalException {
            doThrow(new TechnicalException("exception")).when(membershipRepository).delete("membership-id");
            assertThatThrownBy(() -> service.delete("membership-id"))
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage("An error occurs while trying to delete the membership: membership-id");
            verify(membershipRepository).delete("membership-id");
        }
    }
}
