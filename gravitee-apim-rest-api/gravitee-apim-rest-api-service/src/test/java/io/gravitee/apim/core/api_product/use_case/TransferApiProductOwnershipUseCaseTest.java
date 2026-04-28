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
package io.gravitee.apim.core.api_product.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import inmemory.AbstractUseCaseTest;
import inmemory.MembershipDomainServiceInMemory;
import io.gravitee.apim.core.api_product.domain_service.ApiProductIndexerDomainService;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.api_product.query_service.ApiProductQueryService;
import io.gravitee.apim.core.member.model.MembershipMemberType;
import io.gravitee.apim.core.membership.domain_service.ApiProductPrimaryOwnerDomainService;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.apim.core.membership.model.TransferOwnership;
import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.permissions.RoleScope;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class TransferApiProductOwnershipUseCaseTest extends AbstractUseCaseTest {

    private static final String API_PRODUCT_ID = "my-api-product";

    private TransferApiProductOwnershipUseCase transferApiProductOwnershipUseCase;
    private final MembershipDomainServiceInMemory membershipDomainService = new MembershipDomainServiceInMemory();

    @Mock
    private ApiProductQueryService apiProductQueryService;

    @Mock
    private ApiProductIndexerDomainService apiProductIndexerDomainService;

    @Mock
    private ApiProductPrimaryOwnerDomainService apiProductPrimaryOwnerDomainService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        transferApiProductOwnershipUseCase = new TransferApiProductOwnershipUseCase(
            membershipDomainService,
            apiProductQueryService,
            apiProductIndexerDomainService,
            apiProductPrimaryOwnerDomainService
        );
        initData();
        stubApiProductInIndex();
    }

    @Test
    void should_transfer_ownership_to_existing_api_product_member() {
        assertAll(
            () -> assertThat(currentRoleOf("user-1")).isEqualTo("PRIMARY_OWNER"),
            () -> assertThat(currentRoleOf("user-2")).isEqualTo("USER")
        );

        transferApiProductOwnershipUseCase.execute(
            new TransferApiProductOwnershipUseCase.Input(
                TransferOwnership.builder().currentPrimaryOwnerNewRole("USER").newPrimaryOwnerId("user-2").build(),
                API_PRODUCT_ID,
                AUDIT_INFO
            )
        );

        assertAll(
            () -> assertThat(currentRoleOf("user-1")).isEqualTo("USER"),
            () -> assertThat(currentRoleOf("user-2")).isEqualTo("PRIMARY_OWNER")
        );
        verify(apiProductIndexerDomainService).index(any(ApiProductIndexerDomainService.Context.class), any(ApiProduct.class), any());
    }

    @Test
    void should_transfer_ownership_to_new_user_not_yet_a_member() {
        assertThat(currentRoleOf("user-1")).isEqualTo("PRIMARY_OWNER");

        transferApiProductOwnershipUseCase.execute(
            new TransferApiProductOwnershipUseCase.Input(
                TransferOwnership.builder().currentPrimaryOwnerNewRole("OWNER").newPrimaryOwnerId("user-new").build(),
                API_PRODUCT_ID,
                AUDIT_INFO
            )
        );

        assertAll(
            () -> assertThat(currentRoleOf("user-1")).isEqualTo("OWNER"),
            () -> assertThat(currentRoleOf("user-new")).isEqualTo("PRIMARY_OWNER")
        );
        verify(apiProductIndexerDomainService).index(any(ApiProductIndexerDomainService.Context.class), any(ApiProduct.class), any());
    }

    @Test
    void should_transfer_ownership_to_group() {
        assertThat(currentRoleOf("user-1")).isEqualTo("PRIMARY_OWNER");

        transferApiProductOwnershipUseCase.execute(
            new TransferApiProductOwnershipUseCase.Input(
                TransferOwnership.builder()
                    .currentPrimaryOwnerNewRole("USER")
                    .newPrimaryOwnerId("group-1")
                    .memberType(MembershipMemberType.GROUP)
                    .build(),
                API_PRODUCT_ID,
                AUDIT_INFO
            )
        );

        assertAll(
            () -> assertThat(currentRoleOf("user-1")).isEqualTo("USER"),
            () -> assertThat(currentRoleOf("group-1")).isEqualTo("PRIMARY_OWNER"),
            () -> assertThat(memberTypeOf("group-1")).isEqualTo(io.gravitee.rest.api.model.MembershipMemberType.GROUP)
        );
        verify(apiProductIndexerDomainService).index(any(ApiProductIndexerDomainService.Context.class), any(ApiProduct.class), any());
    }

    @Test
    void should_reindex_with_resolved_primary_owner_after_transfer() {
        var newPo = PrimaryOwnerEntity.builder()
            .id("user-2")
            .displayName("User Two")
            .email("u2@example.com")
            .type(PrimaryOwnerEntity.Type.USER)
            .build();
        when(apiProductPrimaryOwnerDomainService.getApiProductPrimaryOwner(ORG_ID, API_PRODUCT_ID)).thenReturn(newPo);

        transferApiProductOwnershipUseCase.execute(
            new TransferApiProductOwnershipUseCase.Input(
                TransferOwnership.builder().currentPrimaryOwnerNewRole("USER").newPrimaryOwnerId("user-2").build(),
                API_PRODUCT_ID,
                AUDIT_INFO
            )
        );

        verify(apiProductIndexerDomainService).index(
            any(ApiProductIndexerDomainService.Context.class),
            eq(ApiProduct.builder().id(API_PRODUCT_ID).environmentId(ENV_ID).name("product-under-test").build()),
            eq(newPo)
        );
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void stubApiProductInIndex() {
        var product = ApiProduct.builder().id(API_PRODUCT_ID).environmentId(ENV_ID).name("product-under-test").build();
        when(apiProductQueryService.findById(API_PRODUCT_ID)).thenReturn(Optional.of(product));
        when(apiProductPrimaryOwnerDomainService.getApiProductPrimaryOwner(ORG_ID, API_PRODUCT_ID)).thenReturn(
            PrimaryOwnerEntity.builder().id("user-1").displayName("User One").type(PrimaryOwnerEntity.Type.USER).build()
        );
    }

    private String currentRoleOf(String userId) {
        var member = membershipDomainService
            .storage()
            .stream()
            .filter(m -> m.getId().equals(userId))
            .findFirst();
        assertThat(member).as("No member found with id: " + userId).isPresent();
        var role = member.get().getRoles().stream().findFirst();
        assertThat(role).as("Member '" + userId + "' has no roles assigned").isPresent();
        return role.get().getName();
    }

    private io.gravitee.rest.api.model.MembershipMemberType memberTypeOf(String memberId) {
        var member = membershipDomainService
            .storage()
            .stream()
            .filter(m -> m.getId().equals(memberId))
            .findFirst();
        assertThat(member).as("No member found with id: " + memberId).isPresent();
        return member.get().getType();
    }

    private void initData() {
        membershipDomainService.initWith(
            List.of(
                MemberEntity.builder()
                    .id("user-1")
                    .referenceId(API_PRODUCT_ID)
                    .referenceType(MembershipReferenceType.API_PRODUCT)
                    .roles(List.of(RoleEntity.builder().name("PRIMARY_OWNER").scope(RoleScope.API_PRODUCT).build()))
                    .build(),
                MemberEntity.builder()
                    .id("user-2")
                    .referenceId(API_PRODUCT_ID)
                    .referenceType(MembershipReferenceType.API_PRODUCT)
                    .roles(List.of(RoleEntity.builder().name("USER").scope(RoleScope.API_PRODUCT).build()))
                    .build()
            )
        );
    }
}
