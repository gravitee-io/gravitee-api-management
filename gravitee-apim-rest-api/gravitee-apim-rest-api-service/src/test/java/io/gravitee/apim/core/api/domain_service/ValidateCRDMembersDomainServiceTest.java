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
package io.gravitee.apim.core.api.domain_service;

import static io.gravitee.apim.core.member.model.SystemRole.PRIMARY_OWNER;
import static org.assertj.core.api.Assertions.assertThat;

import inmemory.MembershipQueryServiceInMemory;
import inmemory.RoleQueryServiceInMemory;
import inmemory.UserDomainServiceInMemory;
import io.gravitee.apim.core.member.domain_service.ValidateCRDMembersDomainService;
import io.gravitee.apim.core.member.model.MembershipReferenceType;
import io.gravitee.apim.core.member.model.crd.MemberCRD;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.membership.model.Role;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.apim.core.validation.Validator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

class ValidateCRDMembersDomainServiceTest {

    static final String ORG_ID = "TEST";

    UserDomainServiceInMemory userDomainService = new UserDomainServiceInMemory();
    RoleQueryServiceInMemory roleQueryService = new RoleQueryServiceInMemory();
    MembershipQueryServiceInMemory membershipQueryService = new MembershipQueryServiceInMemory();

    ValidateCRDMembersDomainService cut = new ValidateCRDMembersDomainService(userDomainService, roleQueryService, membershipQueryService);

    @BeforeEach
    void setUp() {
        userDomainService.initWith(
            List.of(
                BaseUserEntity.builder().id("user-id").source("memory").sourceId("user-id").organizationId(ORG_ID).build(),
                BaseUserEntity.builder().id("primary_owner_id").source("memory").sourceId("primary_owner_id").organizationId(ORG_ID).build()
            )
        );
        roleQueryService.initWith(
            List.of(
                Role
                    .builder()
                    .name(PRIMARY_OWNER.name())
                    .referenceType(Role.ReferenceType.ORGANIZATION)
                    .referenceId(ORG_ID)
                    .id("primary_owner_id")
                    .scope(Role.Scope.APPLICATION)
                    .build(),
                Role
                    .builder()
                    .name("USER")
                    .referenceType(Role.ReferenceType.ORGANIZATION)
                    .referenceId(ORG_ID)
                    .id("user_role_id")
                    .scope(Role.Scope.APPLICATION)
                    .build(),
                Role
                    .builder()
                    .name(PRIMARY_OWNER.name())
                    .referenceType(Role.ReferenceType.ORGANIZATION)
                    .referenceId(ORG_ID)
                    .id("primary_owner_id")
                    .scope(Role.Scope.API)
                    .build(),
                Role
                    .builder()
                    .name("USER")
                    .referenceType(Role.ReferenceType.ORGANIZATION)
                    .referenceId(ORG_ID)
                    .id("user_role_id")
                    .scope(Role.Scope.API)
                    .build()
            )
        );
    }

    @Test
    void should_return_no_warning_with_existing_member() {
        var members = Set.of(MemberCRD.builder().source("memory").sourceId("user-id").role("USER").build());
        var expectedMembers = Set.of(MemberCRD.builder().id("user-id").source("memory").sourceId("user-id").role("USER").build());
        var result = cut.validateAndSanitize(
            new ValidateCRDMembersDomainService.Input(ORG_ID, "app_id", MembershipReferenceType.APPLICATION, members)
        );

        result.peek(
            sanitized -> assertThat(Set.copyOf(sanitized.members())).isEqualTo(expectedMembers),
            errors -> assertThat(errors).isEmpty()
        );
    }

    @Test
    void should_return_warning_with_unknown_member() {
        var members = Set.of(
            MemberCRD.builder().source("memory").sourceId("user-id").role("USER").build(),
            MemberCRD.builder().source("memory").sourceId("unknown-id").role("USER").build()
        );
        var expectedMembers = Set.of(MemberCRD.builder().id("user-id").source("memory").sourceId("user-id").role("USER").build());
        var result = cut.validateAndSanitize(
            new ValidateCRDMembersDomainService.Input(ORG_ID, "", MembershipReferenceType.APPLICATION, members)
        );

        result.peek(
            sanitized -> assertThat(Set.copyOf(sanitized.members())).isEqualTo(expectedMembers),
            errors ->
                assertThat(errors)
                    .isEqualTo(
                        List.of(Validator.Error.warning("member [unknown-id] of source [memory] could not be found in organization [TEST]"))
                    )
        );
    }

    @ParameterizedTest
    @EnumSource(value = MembershipReferenceType.class, names = { "APPLICATION", "API" })
    void should_return_warning_with_unknown_member_role(MembershipReferenceType referenceType) {
        var members = Set.of(MemberCRD.builder().source("memory").sourceId("user-id").role("UNKNOWN").build());
        var expectedMembers = Set.copyOf(members);
        var result = cut.validateAndSanitize(new ValidateCRDMembersDomainService.Input(ORG_ID, "", referenceType, members));

        result.peek(
            sanitized -> assertThat(Set.copyOf(sanitized.members())).isEqualTo(expectedMembers),
            errors -> assertThat(errors).isEqualTo(List.of(Validator.Error.warning("member role [UNKNOWN] doesn't exist")))
        );
    }

    @ParameterizedTest
    @EnumSource(value = MembershipReferenceType.class, names = { "APPLICATION", "API" })
    void should_return_error_with_primary_owner_role(MembershipReferenceType referenceType) {
        var members = Set.of(MemberCRD.builder().source("memory").sourceId("user-id").role("PRIMARY_OWNER").build());
        var expectedMembers = Set.of();
        var result = cut.validateAndSanitize(new ValidateCRDMembersDomainService.Input(ORG_ID, "", referenceType, members));

        result.peek(
            sanitized -> assertThat(Set.copyOf(sanitized.members())).isEqualTo(expectedMembers),
            errors ->
                assertThat(errors).isEqualTo(List.of(Validator.Error.severe("you can not change primary owner once resource is created")))
        );
    }

    @ParameterizedTest
    @EnumSource(value = MembershipReferenceType.class, names = { "APPLICATION", "API" })
    void should_return_error_changing_primary_owner_role(MembershipReferenceType referenceType) {
        membershipQueryService.initWith(
            List.of(
                Membership
                    .builder()
                    .memberType(Membership.Type.USER)
                    .referenceType(Membership.ReferenceType.valueOf(referenceType.name()))
                    .referenceId("test")
                    .roleId("primary_owner_id")
                    .memberId("primary_owner_id")
                    .build()
            )
        );
        var members = Set.of(MemberCRD.builder().source("memory").sourceId("primary_owner_id").role("USER").build());
        var expectedMembers = Set.of();
        var result = cut.validateAndSanitize(new ValidateCRDMembersDomainService.Input(ORG_ID, "test", referenceType, members));

        result.peek(
            sanitized -> assertThat(Set.copyOf(sanitized.members())).isEqualTo(expectedMembers),
            errors ->
                assertThat(errors)
                    .isEqualTo(List.of(Validator.Error.severe("can not change the role of exiting primary owner [primary_owner_id]")))
        );
    }

    @Test
    void should_return_no_warning_with_null_members() {
        var expectedMembers = Set.of();
        var result = cut.validateAndSanitize(
            new ValidateCRDMembersDomainService.Input(ORG_ID, "", MembershipReferenceType.APPLICATION, null)
        );

        result.peek(sanitized -> assertThat(sanitized.members()).isEqualTo(expectedMembers), errors -> assertThat(errors).isEmpty());
    }
}
