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

import inmemory.UserDomainServiceInMemory;
import io.gravitee.apim.core.api.model.crd.MemberCRD;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.apim.core.validation.Validator;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ValidateCRDMembersDomainServiceTest {

    static final String ORG_ID = "TEST";

    UserDomainServiceInMemory userDomainService = new UserDomainServiceInMemory();

    ValidateCRDMembersDomainService cut = new ValidateCRDMembersDomainService(userDomainService);

    @BeforeEach
    void setUp() {
        userDomainService.initWith(
            List.of(BaseUserEntity.builder().id("user-id").source("memory").sourceId("user-id").organizationId(ORG_ID).build())
        );
    }

    @Test
    void should_return_no_warning_with_existing_member() {
        var members = Set.of(MemberCRD.builder().source("memory").sourceId("user-id").build());
        var expectedMembers = Set.of(MemberCRD.builder().id("user-id").source("memory").sourceId("user-id").build());
        var result = cut.validateAndSanitize(new ValidateCRDMembersDomainService.Input(ORG_ID, members));

        result.peek(sanitized -> assertThat(sanitized.members()).isEqualTo(expectedMembers), errors -> assertThat(errors).isEmpty());
    }

    @Test
    void should_return_warning_with_unknown_member() {
        var members = Set.of(
            MemberCRD.builder().source("memory").sourceId("user-id").build(),
            MemberCRD.builder().source("memory").sourceId("unknown-id").build()
        );
        var expectedMembers = Set.of(MemberCRD.builder().id("user-id").source("memory").sourceId("user-id").build());
        var result = cut.validateAndSanitize(new ValidateCRDMembersDomainService.Input(ORG_ID, members));

        result.peek(
            sanitized -> assertThat(sanitized.members()).isEqualTo(expectedMembers),
            errors ->
                assertThat(errors)
                    .isEqualTo(
                        List.of(Validator.Error.warning("Member [unknown-id] of source [memory] could not be found in organization [TEST]"))
                    )
        );
    }

    @Test
    void should_return_no_warning_with_null_members() {
        var expectedMembers = Set.of();
        var result = cut.validateAndSanitize(new ValidateCRDMembersDomainService.Input(ORG_ID, null));

        result.peek(sanitized -> assertThat(sanitized.members()).isEqualTo(expectedMembers), errors -> assertThat(errors).isEmpty());
    }
}
