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

import static org.assertj.core.api.Assertions.assertThat;

import inmemory.RoleQueryServiceInMemory;
import inmemory.UserDomainServiceInMemory;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.member.domain_service.ValidateCRDMembersDomainService;
import io.gravitee.apim.core.member.model.MembershipReferenceType;
import io.gravitee.apim.core.member.model.crd.MemberCRD;
import io.gravitee.apim.core.membership.model.Role;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.apim.core.validation.Validator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ValidateCRDMembersDomainServiceTest {

    static final String ORG_ID = "TEST";
    static final String ENV_ID = "TEST";
    static final String USER_SOURCE = "MEMORY";
    static final String USER_ID = "USER";
    static final String ACTOR_USER_ID = "ACTOR";
    static final String ROLE_ID = UUID.randomUUID().toString();

    static final AuditInfo AUDIT_INFO = AuditInfo
        .builder()
        .actor(AuditActor.builder().userSource(USER_SOURCE).userSourceId(ACTOR_USER_ID).userId(ACTOR_USER_ID).build())
        .environmentId(ENV_ID)
        .organizationId(ORG_ID)
        .build();

    UserDomainServiceInMemory userDomainService = new UserDomainServiceInMemory();
    RoleQueryServiceInMemory roleQueryService = new RoleQueryServiceInMemory();

    ValidateCRDMembersDomainService cut = new ValidateCRDMembersDomainService(userDomainService, roleQueryService);

    @BeforeEach
    void setUp() {
        userDomainService.initWith(
            List.of(
                BaseUserEntity.builder().organizationId(ORG_ID).id(USER_ID).source(USER_SOURCE).sourceId(USER_ID).build(),
                BaseUserEntity.builder().organizationId(ORG_ID).id(ACTOR_USER_ID).source(USER_SOURCE).sourceId(ACTOR_USER_ID).build()
            )
        );
        roleQueryService.initWith(
            List.of(
                Role
                    .builder()
                    .name("USER")
                    .referenceType(Role.ReferenceType.ORGANIZATION)
                    .referenceId(ORG_ID)
                    .id(ROLE_ID)
                    .scope(Role.Scope.APPLICATION)
                    .build(),
                Role
                    .builder()
                    .name("USER")
                    .referenceType(Role.ReferenceType.ORGANIZATION)
                    .referenceId(ORG_ID)
                    .id(ROLE_ID)
                    .scope(Role.Scope.API)
                    .build(),
                Role
                    .builder()
                    .name("PRIMARY_OWNER")
                    .referenceType(Role.ReferenceType.ORGANIZATION)
                    .referenceId(ORG_ID)
                    .id("user_role_id")
                    .scope(Role.Scope.APPLICATION)
                    .build(),
                Role
                    .builder()
                    .name("PRIMARY_OWNER")
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
        var members = Set.of(MemberCRD.builder().source(USER_SOURCE).sourceId(USER_ID).role("USER").build());
        var expectedMembers = Set.of(MemberCRD.builder().id(USER_ID).source(USER_SOURCE).sourceId(USER_ID).role("USER").build());
        var result = cut.validateAndSanitize(
            new ValidateCRDMembersDomainService.Input(AUDIT_INFO, "app_id", MembershipReferenceType.APPLICATION, members)
        );

        result.peek(
            sanitized -> assertThat(Set.copyOf(sanitized.members())).isEqualTo(expectedMembers),
            errors -> assertThat(errors).isEmpty()
        );
    }

    @Test
    void should_return_warning_with_unknown_member() {
        var members = Set.of(
            MemberCRD.builder().source(USER_SOURCE).sourceId(USER_ID).role("USER").build(),
            MemberCRD.builder().source(USER_SOURCE).sourceId("unknown-id").role("USER").build()
        );
        var expectedMembers = Set.of(MemberCRD.builder().id(USER_ID).source(USER_SOURCE).sourceId(USER_ID).role("USER").build());
        var result = cut.validateAndSanitize(
            new ValidateCRDMembersDomainService.Input(AUDIT_INFO, "", MembershipReferenceType.APPLICATION, members)
        );

        result.peek(
            sanitized -> assertThat(Set.copyOf(sanitized.members())).isEqualTo(expectedMembers),
            errors ->
                assertThat(errors)
                    .isEqualTo(
                        List.of(Validator.Error.warning("member [unknown-id] of source [MEMORY] could not be found in organization [TEST]"))
                    )
        );
    }

    @ParameterizedTest
    @EnumSource(value = MembershipReferenceType.class, names = { "APPLICATION", "API" })
    void should_return_warning_with_unknown_member_role(MembershipReferenceType referenceType) {
        var members = Set.of(MemberCRD.builder().source(USER_SOURCE).sourceId(USER_ID).role("UNKNOWN").build());
        var expectedMembers = Set.copyOf(members);
        var result = cut.validateAndSanitize(new ValidateCRDMembersDomainService.Input(AUDIT_INFO, "", referenceType, members));

        result.peek(
            sanitized -> assertThat(Set.copyOf(sanitized.members())).isEqualTo(expectedMembers),
            errors -> assertThat(errors).isEqualTo(List.of(Validator.Error.warning("member role [UNKNOWN] doesn't exist")))
        );
    }

    @ParameterizedTest
    @EnumSource(value = MembershipReferenceType.class, names = { "APPLICATION", "API" })
    void should_not_return_warning_with_known_member_role(MembershipReferenceType referenceType) {
        var members = Set.of(MemberCRD.builder().source(USER_SOURCE).sourceId(USER_ID).role(ROLE_ID).build());
        var expectedMembers = Set.copyOf(members);
        var result = cut.validateAndSanitize(new ValidateCRDMembersDomainService.Input(AUDIT_INFO, "", referenceType, members));

        result.peek(
            sanitized -> assertThat(Set.copyOf(sanitized.members())).isEqualTo(expectedMembers),
            errors -> assertThat(errors).isEmpty()
        );
    }

    @ParameterizedTest
    @EnumSource(value = MembershipReferenceType.class, names = { "APPLICATION", "API" })
    void should_return_error_with_primary_owner_role(MembershipReferenceType referenceType) {
        var members = Set.of(MemberCRD.builder().source(USER_SOURCE).sourceId(USER_ID).role("PRIMARY_OWNER").build());
        var expectedMembers = Set.of();
        var result = cut.validateAndSanitize(new ValidateCRDMembersDomainService.Input(AUDIT_INFO, "", referenceType, members));

        result.peek(
            sanitized -> assertThat(Set.copyOf(sanitized.members())).isEqualTo(expectedMembers),
            errors ->
                assertThat(errors).isEqualTo(List.of(Validator.Error.severe("setting a member with the primary owner role is not allowed")))
        );
    }

    @ParameterizedTest
    @EnumSource(value = MembershipReferenceType.class, names = { "APPLICATION", "API" })
    void should_return_error_changing_primary_owner_role(MembershipReferenceType referenceType) {
        userDomainService.initWith(
            List.of(BaseUserEntity.builder().organizationId(ORG_ID).id(USER_ID).source(USER_SOURCE).sourceId(USER_ID).build())
        );
        var members = Set.of(MemberCRD.builder().source(USER_SOURCE).sourceId(ACTOR_USER_ID).role("USER").build());
        var expectedMembers = Set.of();
        var result = cut.validateAndSanitize(new ValidateCRDMembersDomainService.Input(AUDIT_INFO, "test", referenceType, members));

        result.peek(
            sanitized -> assertThat(Set.copyOf(sanitized.members())).isEqualTo(expectedMembers),
            errors -> assertThat(errors).isEqualTo(List.of(Validator.Error.severe("can not change the role of primary owner [ACTOR]")))
        );
    }

    @Test
    void should_return_no_warning_with_null_members() {
        var expectedMembers = Set.of();
        var result = cut.validateAndSanitize(
            new ValidateCRDMembersDomainService.Input(AUDIT_INFO, "", MembershipReferenceType.APPLICATION, null)
        );

        result.peek(sanitized -> assertThat(sanitized.members()).isEqualTo(expectedMembers), errors -> assertThat(errors).isEmpty());
    }
}
