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
package io.gravitee.apim.core.api_product.domain_service;

import static org.assertj.core.api.Assertions.assertThat;

import inmemory.ApiProductQueryServiceInMemory;
import inmemory.MembershipCrudServiceInMemory;
import inmemory.MembershipQueryServiceInMemory;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.membership.model.Membership;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ApiProductAccessibleIdsDomainServiceTest {

    private static final String ENV_ID = "env-1";
    private static final String OTHER_ENV_ID = "env-2";
    private static final String USER_ID = "sam";
    private static final String GROUP_ID = "s-group";
    private static final String OTHER_GROUP_ID = "other-group";
    private static final String ROLE_ID = "role-id";

    private ApiProductQueryServiceInMemory apiProductQueryService;
    private MembershipCrudServiceInMemory membershipCrudService;
    private MembershipQueryServiceInMemory membershipQueryService;
    private ApiProductAccessibleIdsDomainService domainService;

    @BeforeEach
    void setUp() {
        apiProductQueryService = new ApiProductQueryServiceInMemory();
        membershipCrudService = new MembershipCrudServiceInMemory();
        membershipQueryService = new MembershipQueryServiceInMemory(membershipCrudService);
        domainService = new ApiProductAccessibleIdsDomainService(apiProductQueryService, membershipQueryService);
    }

    @Test
    void should_return_empty_set_when_user_has_no_memberships() {
        apiProductQueryService.initWith(List.of(apiProduct("ap-1", ENV_ID, null), apiProduct("ap-2", ENV_ID, null)));

        Set<String> result = domainService.findAccessibleApiProductIds(ENV_ID, USER_ID);

        assertThat(result).isEmpty();
    }

    @Test
    void should_return_only_directly_owned_api_products() {
        apiProductQueryService.initWith(List.of(apiProduct("ap-1", ENV_ID, null), apiProduct("ap-2", ENV_ID, null)));
        membershipCrudService.initWith(List.of(directApiProductMembership("m-1", "ap-1")));

        Set<String> result = domainService.findAccessibleApiProductIds(ENV_ID, USER_ID);

        assertThat(result).containsExactly("ap-1");
    }

    @Test
    void should_return_only_group_inherited_api_products_when_user_has_no_direct_memberships() {
        apiProductQueryService.initWith(List.of(apiProduct("ap-1", ENV_ID, null), apiProduct("ap-2", ENV_ID, Set.of(GROUP_ID))));
        membershipCrudService.initWith(List.of(groupMembership("m-1", GROUP_ID)));

        Set<String> result = domainService.findAccessibleApiProductIds(ENV_ID, USER_ID);

        assertThat(result).containsExactly("ap-2");
    }

    @Test
    void should_union_direct_and_group_inherited_api_products() {
        apiProductQueryService.initWith(
            List.of(apiProduct("ap-1", ENV_ID, null), apiProduct("ap-2", ENV_ID, Set.of(GROUP_ID)), apiProduct("ap-3", ENV_ID, null))
        );
        membershipCrudService.initWith(List.of(directApiProductMembership("m-1", "ap-1"), groupMembership("m-2", GROUP_ID)));

        Set<String> result = domainService.findAccessibleApiProductIds(ENV_ID, USER_ID);

        assertThat(result).containsExactlyInAnyOrder("ap-1", "ap-2");
    }

    @Test
    void should_dedupe_when_direct_and_group_inherited_overlap() {
        apiProductQueryService.initWith(List.of(apiProduct("ap-1", ENV_ID, Set.of(GROUP_ID))));
        membershipCrudService.initWith(List.of(directApiProductMembership("m-1", "ap-1"), groupMembership("m-2", GROUP_ID)));

        Set<String> result = domainService.findAccessibleApiProductIds(ENV_ID, USER_ID);

        assertThat(result).containsExactly("ap-1");
    }

    @Test
    void should_scope_group_inherited_lookup_to_the_requested_environment() {
        // ap-other-env is in a different environment but attached to the same group: must NOT leak.
        apiProductQueryService.initWith(
            List.of(apiProduct("ap-here", ENV_ID, Set.of(GROUP_ID)), apiProduct("ap-other-env", OTHER_ENV_ID, Set.of(GROUP_ID)))
        );
        membershipCrudService.initWith(List.of(groupMembership("m-1", GROUP_ID)));

        Set<String> result = domainService.findAccessibleApiProductIds(ENV_ID, USER_ID);

        assertThat(result).containsExactly("ap-here");
    }

    @Test
    void should_ignore_groups_the_user_does_not_belong_to() {
        // ap-other is attached to a different group than the one sam belongs to.
        apiProductQueryService.initWith(
            List.of(apiProduct("ap-mine", ENV_ID, Set.of(GROUP_ID)), apiProduct("ap-other", ENV_ID, Set.of(OTHER_GROUP_ID)))
        );
        membershipCrudService.initWith(List.of(groupMembership("m-1", GROUP_ID)));

        Set<String> result = domainService.findAccessibleApiProductIds(ENV_ID, USER_ID);

        assertThat(result).containsExactly("ap-mine");
    }

    private static ApiProduct apiProduct(String id, String environmentId, Set<String> groups) {
        ApiProduct apiProduct = ApiProduct.builder().id(id).name(id).environmentId(environmentId).build();
        apiProduct.setGroups(groups == null ? null : new HashSet<>(groups));
        return apiProduct;
    }

    private static Membership directApiProductMembership(String id, String apiProductId) {
        return Membership.builder()
            .id(id)
            .memberId(USER_ID)
            .memberType(Membership.Type.USER)
            .referenceType(Membership.ReferenceType.API_PRODUCT)
            .referenceId(apiProductId)
            .roleId(ROLE_ID)
            .build();
    }

    private static Membership groupMembership(String id, String groupId) {
        return Membership.builder()
            .id(id)
            .memberId(USER_ID)
            .memberType(Membership.Type.USER)
            .referenceType(Membership.ReferenceType.GROUP)
            .referenceId(groupId)
            .roleId(ROLE_ID)
            .build();
    }
}
