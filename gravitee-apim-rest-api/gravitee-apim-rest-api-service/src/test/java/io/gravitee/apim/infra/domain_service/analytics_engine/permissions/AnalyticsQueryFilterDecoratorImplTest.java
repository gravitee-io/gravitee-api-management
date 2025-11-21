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
package io.gravitee.apim.infra.domain_service.analytics_engine.permissions;

import static io.gravitee.apim.core.analytics_engine.model.FilterSpec.Name.API;
import static io.gravitee.apim.core.analytics_engine.model.FilterSpec.Operator.EQ;
import static io.gravitee.apim.core.analytics_engine.model.FilterSpec.Operator.IN;
import static io.gravitee.rest.api.model.permissions.RolePermission.API_ANALYTICS;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.READ;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.analytics_engine.model.Filter;
import io.gravitee.rest.api.model.api.ApiQuery;
import io.gravitee.rest.api.model.permissions.RolePermission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.stubbing.Answer;
import org.springframework.security.core.GrantedAuthority;

/**
 * @author GraviteeSource Team
 */
@DisplayName("analytics filters")
public class AnalyticsQueryFilterDecoratorImplTest extends AbstractPermissionsTest {

    static final String apiId1 = "api-id-1";
    static final String apiId2 = "api-id-2";
    static final String apiId3 = "api-id-3";
    static final String apiId4 = "api-id-4";
    static final String apiId5 = "api-id-5";
    static final String apiId6 = "api-id-6";
    static final String apiId7 = "api-id-7";
    static final String apiId8 = "api-id-8";
    static final String apiId9 = "api-id-9";

    static List<String> allApiIds = List.of(apiId1, apiId2, apiId3, apiId4, apiId5, apiId6, apiId7, apiId8, apiId9);

    @Nested
    @DisplayName("organization administrators can see all environment metrics")
    class OrganizationAdministratorUsers {

        @BeforeEach
        public void setUp() {
            GrantedAuthority organizationAdmin = () -> "ORGANIZATION:ADMIN";
            setAuthorities(organizationAdmin);

            when(apiAuthorizationService.findIdsByEnvironment("DEFAULT")).thenReturn(new HashSet<>(allApiIds));
        }

        @Test
        public void should_not_update_nonempty_filter() {
            var EqualityFilter = new Filter(API, EQ, "43985673406573406");
            List<Filter> filters = List.of(EqualityFilter);

            var updatedFilters = analyticsQueryFilterDecorator.getUpdatedFilters(filters);

            assertThat(updatedFilters).isEqualTo(filters);
        }

        @Test
        public void should_update_empty_filter_to_include_all_environment_ids() {
            var emptyFilters = new ArrayList<Filter>();
            var updatedFilters = analyticsQueryFilterDecorator.getUpdatedFilters(emptyFilters);

            assertThat(updatedFilters)
                .singleElement()
                .satisfies(filter -> {
                    assertThat(filter.name()).isEqualTo(API);
                    assertThat(filter.operator()).isEqualTo(IN);
                    assertThat(filter.value())
                        .asInstanceOf(InstanceOfAssertFactories.ITERABLE)
                        .containsExactlyInAnyOrderElementsOf(allApiIds);
                });
        }
    }

    @Nested
    @DisplayName("non-administrators see a subset of the metrics")
    class NonAdministratorUsers {

        record eqTestArguments(String role, String wantedApiId, List<String> expectedApiIds, String description) {}

        record inTestArguments(String role, List<String> wantedApiIds, List<String> expectedApiIds, String description) {}

        final String user = "testUser";

        final List<String> apisUserIsAMemberOf = Arrays.asList(apiId1, apiId2, apiId3, apiId4, apiId5);
        final List<String> apisUserCanRead = Arrays.asList(apiId3, apiId4, apiId5);

        @BeforeEach
        public void setUp() {
            setAuthenticatedUsername(user);

            when(permissionService.hasPermission(any(), eq(API_ANALYTICS), argThat(apisUserCanRead::contains), eq(READ))).thenReturn(true);

            when(apiAuthorizationService.findIdsByUser(any(), eq(user), any(), anyBoolean())).thenAnswer(
                (Answer<Set<String>>) invocation -> {
                    var query = (ApiQuery) invocation.getArgument(2);

                    if (query.getIds() == null) {
                        return new HashSet<>(apisUserIsAMemberOf);
                    }

                    if (query.getIds().isEmpty()) {
                        return new HashSet<>();
                    }

                    return new HashSet<>(query.getIds());
                }
            );
        }

        @ParameterizedTest
        @MethodSource("nonOrganizationAdminRoles")
        public void should_update_empty_filter(String role) {
            GrantedAuthority environmentUser = () -> role;
            setAuthorities(environmentUser);

            var emptyFilters = new ArrayList<Filter>();
            var updatedFilters = analyticsQueryFilterDecorator.getUpdatedFilters(emptyFilters);

            assertThat(updatedFilters)
                .singleElement()
                .satisfies(filter -> {
                    assertThat(filter.name()).isEqualTo(API);
                    assertThat(filter.operator()).isEqualTo(IN);
                    assertThat(filter.value())
                        .asInstanceOf(InstanceOfAssertFactories.ITERABLE)
                        .containsExactlyInAnyOrderElementsOf(apisUserCanRead);
                });
        }

        @ParameterizedTest
        @MethodSource("eqTestParams")
        public void should_update_EQ_filter(eqTestArguments args) {
            GrantedAuthority environmentUser = () -> args.role;
            setAuthorities(environmentUser);

            var filter = new Filter(API, EQ, args.wantedApiId);
            var updatedFilters = analyticsQueryFilterDecorator.getUpdatedFilters(List.of(filter));

            assertThat(updatedFilters)
                .singleElement()
                .satisfies(f -> {
                    assertThat(f.name()).isEqualTo(API);
                    assertThat(f.operator()).isEqualTo(IN);
                    assertThat(f.value())
                        .asInstanceOf(InstanceOfAssertFactories.ITERABLE)
                        .containsExactlyInAnyOrderElementsOf(args.expectedApiIds);
                });
        }

        @ParameterizedTest
        @MethodSource("inTestParams")
        public void should_update_IN_filter(inTestArguments args) {
            GrantedAuthority environmentUser = () -> args.role;
            setAuthorities(environmentUser);

            var filter = new Filter(API, IN, args.wantedApiIds);
            var updatedFilters = analyticsQueryFilterDecorator.getUpdatedFilters(List.of(filter));

            assertThat(updatedFilters)
                .singleElement()
                .satisfies(f -> {
                    assertThat(f.name()).isEqualTo(API);
                    assertThat(f.operator()).isEqualTo(IN);
                    assertThat(f.value())
                        .asInstanceOf(InstanceOfAssertFactories.ITERABLE)
                        .containsExactlyInAnyOrderElementsOf(args.expectedApiIds);
                });
        }

        static Stream<eqTestArguments> eqTestParams() {
            return nonOrganizationAdminRoles().mapMulti((role, consumer) -> {
                consumer.accept(new eqTestArguments(role, apiId3, List.of(apiId3), "User has access to the ID requested"));
                consumer.accept(new eqTestArguments(role, apiId7, Collections.emptyList(), "User doesn't have access to the ID requested"));
            });
        }

        static Stream<inTestArguments> inTestParams() {
            return nonOrganizationAdminRoles().mapMulti((role, consumer) -> {
                consumer.accept(new inTestArguments(role, List.of(), List.of(), "User requesting an empty list should get an empty list"));
                consumer.accept(
                    new inTestArguments(role, List.of(apiId3, apiId5), List.of(apiId3, apiId5), "User has access to the IDs requested")
                );
                consumer.accept(
                    new inTestArguments(
                        role,
                        List.of(apiId7, apiId8),
                        Collections.emptyList(),
                        "User doesn't have access to the IDs requested"
                    )
                );
                consumer.accept(
                    new inTestArguments(role, List.of(apiId3, apiId7), List.of(apiId3), "User has access to some if the IDs requested")
                );
            });
        }

        // Returns all roles defined in RolePermission formated as SCOPE:PERMISSION, and also adds role ENVIRONMENT:ADMIN
        static Stream<String> nonOrganizationAdminRoles() {
            Set<String> roles = Arrays.stream(RolePermission.values())
                .map(rolePermission -> String.format("%s:%s", rolePermission.getScope(), rolePermission.getPermission()))
                .collect(Collectors.toSet());

            roles.add("ENVIRONMENT:ADMIN");

            return roles.stream();
        }
    }
}
