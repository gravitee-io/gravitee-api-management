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
package io.gravitee.apim.core.api_product.use_case.members;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

import inmemory.AbstractUseCaseTest;
import inmemory.MembershipDomainServiceInMemory;
import io.gravitee.apim.core.membership.model.AddMember;
import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import io.gravitee.rest.api.service.exceptions.SinglePrimaryOwnerException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AddApiProductMemberUseCaseTest extends AbstractUseCaseTest {

    private AddApiProductMemberUseCase addApiProductMemberUseCase;
    private final MembershipDomainServiceInMemory membershipDomainService = new MembershipDomainServiceInMemory();

    @BeforeEach
    void setUp() {
        addApiProductMemberUseCase = new AddApiProductMemberUseCase(membershipDomainService);
    }

    @Test
    void should_add_api_product_member() {
        AddMember addMember = AddMember.builder().roleName("API_PRODUCT_USER").userId("user-1").build();

        addApiProductMemberUseCase.execute(new AddApiProductMemberUseCase.Input(addMember, "ap-1"));

        var membership = membershipDomainService.storage().stream().findFirst().orElseThrow();
        assertAll(() ->
            assertThat(membership)
                .usingRecursiveComparison()
                .isEqualTo(
                    MemberEntity.builder()
                        .referenceType(MembershipReferenceType.API_PRODUCT)
                        .referenceId("ap-1")
                        .roles(List.of(RoleEntity.builder().name("API_PRODUCT_USER").build()))
                        .type(MembershipMemberType.USER)
                        .id("user-1")
                        .build()
                )
        );
    }

    @Test
    void should_throw_validation_error_when_role_is_primary_owner() {
        AddMember addMember = AddMember.builder().roleName("PRIMARY_OWNER").userId("u1").build();

        assertThrows(SinglePrimaryOwnerException.class, () ->
            addApiProductMemberUseCase.execute(new AddApiProductMemberUseCase.Input(addMember, "ap-1"))
        );
    }

    @Test
    void should_throw_when_user_id_and_external_reference_missing() {
        AddMember addMember = AddMember.builder().roleName("API_PRODUCT_USER").build();

        assertThrows(InvalidDataException.class, () ->
            addApiProductMemberUseCase.execute(new AddApiProductMemberUseCase.Input(addMember, "ap-1"))
        );
    }
}
