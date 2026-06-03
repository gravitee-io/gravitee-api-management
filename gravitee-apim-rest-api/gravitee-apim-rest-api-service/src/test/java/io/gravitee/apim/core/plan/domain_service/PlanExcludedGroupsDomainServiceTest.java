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
package io.gravitee.apim.core.plan.domain_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import inmemory.GroupQueryServiceInMemory;
import inmemory.MembershipCrudServiceInMemory;
import inmemory.MembershipQueryServiceInMemory;
import inmemory.RoleQueryServiceInMemory;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.group.model.Group;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.membership.model.Role;
import io.gravitee.rest.api.service.exceptions.GroupNotFoundException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PlanExcludedGroupsDomainServiceTest {

    private static final String ENV_ID = "env-1";
    private static final String OTHER_ENV_ID = "env-2";
    private static final String USER_ID = "user";
    private static final String API_PRODUCT_ID = "ap-1";
    private static final String EXCLUDED_GROUP_ID = "grp-excluded";
    private static final String ALLOWED_GROUP_ID = "grp-allowed";
    private static final String API_PRODUCT_ROLE_ID = "api-product-role-id";
    private static final String API_ROLE_ID = "api-role-id";

    private GroupQueryServiceInMemory groupQueryService;
    private MembershipCrudServiceInMemory membershipCrudService;
    private MembershipQueryServiceInMemory membershipQueryService;
    private RoleQueryServiceInMemory roleQueryService;
    private PlanExcludedGroupsDomainService domainService;

    @BeforeEach
    void setUp() {
        groupQueryService = new GroupQueryServiceInMemory();
        membershipCrudService = new MembershipCrudServiceInMemory();
        membershipQueryService = new MembershipQueryServiceInMemory(membershipCrudService);
        roleQueryService = new RoleQueryServiceInMemory();
        domainService = new PlanExcludedGroupsDomainService(groupQueryService, membershipQueryService, roleQueryService);

        roleQueryService.initWith(
            List.of(
                Role.builder().id(API_PRODUCT_ROLE_ID).scope(Role.Scope.API_PRODUCT).name("API_PRODUCT_USER").build(),
                Role.builder().id(API_ROLE_ID).scope(Role.Scope.API).name("API_USER").build()
            )
        );
    }

    @Nested
    class ValidateExcludedGroups {

        @Test
        void should_do_nothing_when_excluded_groups_is_empty() {
            domainService.validateExcludedGroups(ENV_ID, List.of());
            domainService.validateExcludedGroups(ENV_ID, null);
        }

        @Test
        void should_pass_when_all_groups_exist_in_environment() {
            groupQueryService.initWith(List.of(group(EXCLUDED_GROUP_ID, ENV_ID), group(ALLOWED_GROUP_ID, ENV_ID)));

            domainService.validateExcludedGroups(ENV_ID, List.of(EXCLUDED_GROUP_ID, ALLOWED_GROUP_ID));
        }

        @Test
        void should_throw_when_group_does_not_exist() {
            assertThatThrownBy(() -> domainService.validateExcludedGroups(ENV_ID, List.of("missing-group"))).isInstanceOf(
                GroupNotFoundException.class
            );
        }

        @Test
        void should_throw_when_group_exists_in_another_environment() {
            groupQueryService.initWith(List.of(group(EXCLUDED_GROUP_ID, OTHER_ENV_ID)));

            assertThatThrownBy(() -> domainService.validateExcludedGroups(ENV_ID, List.of(EXCLUDED_GROUP_ID))).isInstanceOf(
                GroupNotFoundException.class
            );
        }
    }

    @Nested
    class IsUserAuthorizedToAccessApiProductPlan {

        @Test
        void should_allow_anonymous_user_when_no_excluded_groups() {
            assertThat(
                domainService.isUserAuthorizedToAccessApiProductPlan(apiProduct(Set.of(ALLOWED_GROUP_ID)), List.of(), null)
            ).isTrue();
        }

        @Test
        void should_deny_anonymous_user_when_excluded_groups_present() {
            assertThat(
                domainService.isUserAuthorizedToAccessApiProductPlan(apiProduct(Set.of(ALLOWED_GROUP_ID)), List.of(EXCLUDED_GROUP_ID), null)
            ).isFalse();
        }

        @Test
        void should_allow_any_user_when_no_excluded_groups() {
            assertThat(domainService.isUserAuthorizedToAccessApiProductPlan(apiProduct(Set.of(ALLOWED_GROUP_ID)), null, USER_ID)).isTrue();
        }

        @Test
        void should_allow_direct_api_product_member() {
            membershipCrudService.initWith(List.of(directApiProductMembership(API_PRODUCT_ID)));

            assertThat(
                domainService.isUserAuthorizedToAccessApiProductPlan(
                    apiProduct(Set.of(ALLOWED_GROUP_ID)),
                    List.of(EXCLUDED_GROUP_ID),
                    USER_ID
                )
            ).isTrue();
        }

        @Test
        void should_deny_user_in_excluded_group_only() {
            membershipCrudService.initWith(List.of(groupMembership(EXCLUDED_GROUP_ID, API_PRODUCT_ROLE_ID)));

            assertThat(
                domainService.isUserAuthorizedToAccessApiProductPlan(
                    apiProduct(new HashSet<>(Set.of(EXCLUDED_GROUP_ID, ALLOWED_GROUP_ID))),
                    List.of(EXCLUDED_GROUP_ID),
                    USER_ID
                )
            ).isFalse();
        }

        @Test
        void should_allow_user_in_non_excluded_group() {
            membershipCrudService.initWith(List.of(groupMembership(ALLOWED_GROUP_ID, API_PRODUCT_ROLE_ID)));

            assertThat(
                domainService.isUserAuthorizedToAccessApiProductPlan(
                    apiProduct(new HashSet<>(Set.of(EXCLUDED_GROUP_ID, ALLOWED_GROUP_ID))),
                    List.of(EXCLUDED_GROUP_ID),
                    USER_ID
                )
            ).isTrue();
        }

        @Test
        void should_deny_user_with_api_role_on_excluded_group_when_product_has_no_groups() {
            membershipCrudService.initWith(List.of(groupMembership(EXCLUDED_GROUP_ID, API_ROLE_ID)));

            assertThat(
                domainService.isUserAuthorizedToAccessApiProductPlan(apiProduct(null), List.of(EXCLUDED_GROUP_ID), USER_ID)
            ).isTrue();
        }

        @Test
        void should_deny_user_with_api_product_role_on_excluded_group_when_product_has_no_groups() {
            membershipCrudService.initWith(List.of(groupMembership(EXCLUDED_GROUP_ID, API_PRODUCT_ROLE_ID)));

            assertThat(
                domainService.isUserAuthorizedToAccessApiProductPlan(apiProduct(null), List.of(EXCLUDED_GROUP_ID), USER_ID)
            ).isFalse();
        }
    }

    private static ApiProduct apiProduct(Set<String> groups) {
        ApiProduct apiProduct = ApiProduct.builder().id(API_PRODUCT_ID).environmentId(ENV_ID).build();
        apiProduct.setGroups(groups);
        return apiProduct;
    }

    private static Group group(String id, String environmentId) {
        return Group.builder().id(id).environmentId(environmentId).name(id).build();
    }

    private static Membership directApiProductMembership(String apiProductId) {
        return Membership.builder()
            .id("direct-membership")
            .memberId(USER_ID)
            .memberType(Membership.Type.USER)
            .referenceType(Membership.ReferenceType.API_PRODUCT)
            .referenceId(apiProductId)
            .roleId(API_PRODUCT_ROLE_ID)
            .build();
    }

    private static Membership groupMembership(String groupId, String roleId) {
        return Membership.builder()
            .id("group-membership-" + groupId)
            .memberId(USER_ID)
            .memberType(Membership.Type.USER)
            .referenceType(Membership.ReferenceType.GROUP)
            .referenceId(groupId)
            .roleId(roleId)
            .build();
    }
}
