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
package io.gravitee.rest.api.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.model.MembershipMemberType;
import io.gravitee.repository.management.model.MembershipReferenceType;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
public class MembershipService_GetMembershipsTest {

    private static final String API_ID = "api-id-1";

    private MembershipService membershipService;

    @Mock
    private MembershipRepository membershipRepository;

    private String memberId = "memberId";

    @BeforeEach
    public void setUp() throws Exception {
        membershipService =
            new MembershipServiceImpl(
                null,
                null,
                null,
                null,
                null,
                null,
                membershipRepository,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
            );
    }

    @Test
    public void shouldGetEmptyMembersWithMembership() throws Exception {
        when(
            membershipRepository.findRefIdsByMemberIdAndMemberTypeAndReferenceType(
                memberId,
                MembershipMemberType.USER,
                MembershipReferenceType.APPLICATION
            )
        )
            .thenReturn(Stream.of());

        Set<String> referenceIds = membershipService.getReferenceIdsByMemberAndReference(
            io.gravitee.rest.api.model.MembershipMemberType.USER,
            memberId,
            io.gravitee.rest.api.model.MembershipReferenceType.APPLICATION
        );

        assertThat(referenceIds).as("references must be empty").isEmpty();
        verify(membershipRepository, times(1))
            .findRefIdsByMemberIdAndMemberTypeAndReferenceType(memberId, MembershipMemberType.USER, MembershipReferenceType.APPLICATION);
    }

    @Test
    public void shouldGetMembersWithMembership() throws Exception {
        when(
            membershipRepository.findRefIdsByMemberIdAndMemberTypeAndReferenceType(
                memberId,
                MembershipMemberType.USER,
                MembershipReferenceType.APPLICATION
            )
        )
            .thenReturn(Stream.of("ref-1", "m2"));

        Set<String> referenceIds = membershipService.getReferenceIdsByMemberAndReference(
            io.gravitee.rest.api.model.MembershipMemberType.USER,
            memberId,
            io.gravitee.rest.api.model.MembershipReferenceType.APPLICATION
        );

        assertThat(referenceIds).hasSize(2);
        verify(membershipRepository, times(1))
            .findRefIdsByMemberIdAndMemberTypeAndReferenceType(memberId, MembershipMemberType.USER, MembershipReferenceType.APPLICATION);
    }

    @Test
    public void shouldThrowTechnicalManagementException() throws Exception {
        when(
            membershipRepository.findRefIdsByMemberIdAndMemberTypeAndReferenceType(
                memberId,
                MembershipMemberType.USER,
                MembershipReferenceType.APPLICATION
            )
        )
            .thenThrow(new TechnicalException());

        assertThatThrownBy(() ->
                membershipService.getReferenceIdsByMemberAndReference(
                    io.gravitee.rest.api.model.MembershipMemberType.USER,
                    memberId,
                    io.gravitee.rest.api.model.MembershipReferenceType.APPLICATION
                )
            )
            .isInstanceOf(TechnicalManagementException.class);
    }
}
