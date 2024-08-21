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

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.isA;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Sets;
import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Application;
import io.gravitee.repository.management.model.Membership;
import io.gravitee.repository.management.model.MembershipMemberType;
import io.gravitee.repository.management.model.Visibility;
import io.gravitee.rest.api.model.GroupEntity;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.UserMembership;
import io.gravitee.rest.api.model.pagedresult.Metadata;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
public class MembershipService_FindUserMembershipMetadataTest {

    private static final String USER_MEMBERSHIP_REFERENCE = "ref-id";
    private static UserMembership USER_MEMBERSHIP;

    private MembershipService membershipService;

    @Mock
    private MembershipRepository mockMembershipRepository;

    @Mock
    private ApiRepository mockApiRepository;

    @Mock
    private ApplicationRepository mockApplicationRepository;

    @Mock
    private GroupService mockGroupService;

    @Mock
    private RoleService mockRoleService;

    @BeforeEach
    public void setUp() throws Exception {
        membershipService =
            new MembershipServiceImpl(
                null,
                null,
                mockApplicationRepository,
                null,
                null,
                null,
                mockMembershipRepository,
                mockRoleService,
                null,
                null,
                null,
                null,
                mockApiRepository,
                mockGroupService,
                null,
                null,
                null
            );
        USER_MEMBERSHIP = new UserMembership();
        USER_MEMBERSHIP.setReference(USER_MEMBERSHIP_REFERENCE);
    }

    @ParameterizedTest
    @MethodSource({ "emptyMetadataInputs" })
    void shouldBeEmpty(List<UserMembership> memberships, MembershipReferenceType type) {
        var result = membershipService.findUserMembershipMetadata(memberships, type);
        assertThat(result).isNotNull().satisfies(res -> assertThat(res.toMap()).isEmpty());
    }

    static Stream<Arguments> emptyMetadataInputs() {
        var validUserMembership = new UserMembership();
        validUserMembership.setReference(USER_MEMBERSHIP_REFERENCE);
        var validUserMembershipList = List.of(validUserMembership);

        return Stream.of(
            Arguments.of(null, MembershipReferenceType.API),
            Arguments.of(new ArrayList<UserMembership>(), MembershipReferenceType.API),
            Arguments.of(validUserMembershipList, null),
            Arguments.of(validUserMembershipList, MembershipReferenceType.ENVIRONMENT),
            Arguments.of(validUserMembershipList, MembershipReferenceType.PLATFORM),
            Arguments.of(validUserMembershipList, MembershipReferenceType.ORGANIZATION),
            Arguments.of(validUserMembershipList, MembershipReferenceType.API),
            Arguments.of(validUserMembershipList, MembershipReferenceType.APPLICATION)
        );
    }

    @Test
    void shouldReturnApiMetadata() {
        when(mockApiRepository.search(eq(new ApiCriteria.Builder().ids(List.of(USER_MEMBERSHIP_REFERENCE)).build()), any(), any()))
            .thenReturn(
                Stream.of(
                    Api
                        .builder()
                        .id(USER_MEMBERSHIP_REFERENCE)
                        .name("api-name")
                        .version("v11")
                        .visibility(Visibility.PUBLIC)
                        .environmentId("env-id")
                        .build()
                )
            );
        var result = membershipService.findUserMembershipMetadata(List.of(USER_MEMBERSHIP), MembershipReferenceType.API);
        assertThat(result)
            .isNotNull()
            .satisfies(res ->
                assertThat(res.toMap())
                    .isNotEmpty()
                    .extractingByKey(USER_MEMBERSHIP_REFERENCE)
                    .hasFieldOrPropertyWithValue("name", "api-name")
                    .hasFieldOrPropertyWithValue("version", "v11")
                    .hasFieldOrPropertyWithValue("visibility", Visibility.PUBLIC)
                    .hasFieldOrPropertyWithValue("environmentId", "env-id")
            );
    }

    @Test
    void shouldReturnApplicationMetadata() throws TechnicalException {
        when(mockApplicationRepository.findByIds(eq(List.of(USER_MEMBERSHIP_REFERENCE))))
            .thenReturn(
                Set.of(Application.builder().id(USER_MEMBERSHIP_REFERENCE).name("application-name").environmentId("env-id").build())
            );
        var result = membershipService.findUserMembershipMetadata(List.of(USER_MEMBERSHIP), MembershipReferenceType.APPLICATION);
        assertThat(result)
            .isNotNull()
            .satisfies(res ->
                assertThat(res.toMap())
                    .isNotEmpty()
                    .extractingByKey(USER_MEMBERSHIP_REFERENCE)
                    .hasFieldOrPropertyWithValue("name", "application-name")
                    .hasFieldOrPropertyWithValue("environmentId", "env-id")
            );
    }
}
