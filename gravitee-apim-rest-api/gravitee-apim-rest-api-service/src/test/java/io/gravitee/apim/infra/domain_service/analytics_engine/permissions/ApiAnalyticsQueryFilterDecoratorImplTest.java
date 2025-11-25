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
import static io.gravitee.apim.core.analytics_engine.model.FilterSpec.Operator.GTE;
import static io.gravitee.apim.core.analytics_engine.model.FilterSpec.Operator.IN;
import static io.gravitee.apim.core.analytics_engine.model.FilterSpec.Operator.LTE;
import static io.gravitee.rest.api.model.permissions.RolePermission.API_ANALYTICS;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.READ;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.analytics_engine.model.Filter;
import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.rest.api.model.api.ApiQuery;
import io.gravitee.rest.api.model.permissions.RolePermission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Builder;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.stubbing.Answer;
import org.springframework.security.core.GrantedAuthority;

/**
 * @author GraviteeSource Team
 */
@DisplayName("analytics filters")
class ApiAnalyticsQueryFilterDecoratorImplTest extends AbstractPermissionsTest {

    static final String apiId1 = "api-id-1";
    static final String apiId2 = "api-id-2";
    static final String apiId3 = "api-id-3";
    static final String apiId4 = "api-id-4";
    static final String apiId5 = "api-id-5";
    static final String apiId6 = "api-id-6";
    static final String apiId7 = "api-id-7";
    static final String apiId8 = "api-id-8";
    static final String apiId9 = "api-id-9";

    static final String organizationAdminRole = "ORGANIZATION:ADMIN";
    static final String environmentAdminRole = "ENVIRONMENT:ADMIN";

    static List<String> allApiIds = List.of(apiId1, apiId2, apiId3, apiId4, apiId5, apiId6, apiId7, apiId8, apiId9);

    @Nested
    @DisplayName("administrators can see all environment metrics")
    class AdministratorUsers {

        @BeforeEach
        void setUp() {
            var set = new HashSet<>(allApiIds);
            when(apiAuthorizationService.findIdsByEnvironment("DEFAULT")).thenReturn((Set<String>) set.clone(), (Set<String>) set.clone());
        }

        @ParameterizedTest
        @ValueSource(strings = { organizationAdminRole, environmentAdminRole })
        void should_update_empty_filter_to_include_all_environment_ids(String role) {
            GrantedAuthority organizationAdmin = () -> role;
            setAuthorities(organizationAdmin);

            var emptyFilters = new ArrayList<Filter>();
            var updatedFilters = apiAnalyticsQueryFilterDecorator.applyPermissionBasedFilters(emptyFilters);

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

        @ParameterizedTest
        @ValueSource(strings = { organizationAdminRole, environmentAdminRole })
        void should_allow_access_to_single_api(String role) {
            GrantedAuthority organizationAdmin = () -> role;
            setAuthorities(organizationAdmin);

            var EqualityFilter = new Filter(API, EQ, apiId7);
            List<Filter> filters = List.of(EqualityFilter);

            var updatedFilters = apiAnalyticsQueryFilterDecorator.applyPermissionBasedFilters(filters);

            assertThat(updatedFilters).isEqualTo(filters);
        }

        @ParameterizedTest
        @ValueSource(strings = { organizationAdminRole, environmentAdminRole })
        void should_not_allow_access_to_unknown_api_id(String role) {
            GrantedAuthority organizationAdmin = () -> role;
            setAuthorities(organizationAdmin);

            var EqualityFilter = new Filter(API, EQ, "invalid-api-id");
            var filters = List.of(EqualityFilter);

            var updatedFilters = apiAnalyticsQueryFilterDecorator.applyPermissionBasedFilters(filters);

            var emptyFilter = new Filter(API, IN, List.of());
            assertThat(updatedFilters).containsExactly(emptyFilter);
        }

        @ParameterizedTest
        @ValueSource(strings = { organizationAdminRole, environmentAdminRole })
        void should_update_filter_list_to_include_only_valid_api_ids(String role) {
            GrantedAuthority organizationAdmin = () -> role;
            setAuthorities(organizationAdmin);

            var filters = List.of(new Filter(API, IN, List.of(apiId1, apiId3, apiId7, "invalid-api-id")));
            var updatedFilters = apiAnalyticsQueryFilterDecorator.applyPermissionBasedFilters(filters);

            var allowedApiIds = List.of(apiId1, apiId3, apiId7);
            assertThat(updatedFilters)
                .singleElement()
                .satisfies(filter -> {
                    assertThat(filter.name()).isEqualTo(API);
                    assertThat(filter.operator()).isEqualTo(IN);
                    assertThat(filter.value())
                        .asInstanceOf(InstanceOfAssertFactories.ITERABLE)
                        .containsExactlyInAnyOrderElementsOf(allowedApiIds);
                });
        }

        @ParameterizedTest
        @ValueSource(strings = { organizationAdminRole, environmentAdminRole })
        void should_update_multiple_filters(String role) {
            GrantedAuthority organizationAdmin = () -> role;
            setAuthorities(organizationAdmin);

            var equalityFilter1 = new Filter(API, EQ, apiId5);
            var listFilter = new Filter(API, IN, List.of(apiId7, "invalid-api-id"));
            var equalityFilter2 = new Filter(API, EQ, "invalid-api-id");

            var filters = List.of(equalityFilter1, listFilter, equalityFilter2);

            var updatedFilters = apiAnalyticsQueryFilterDecorator.applyPermissionBasedFilters(filters);

            assertThat(updatedFilters).containsExactly(
                equalityFilter1,
                new Filter(API, IN, List.of(apiId7)),
                new Filter(API, IN, List.of())
            );
        }

        @Test
        void should_not_change_unsupported_filters() {
            GrantedAuthority organizationAdmin = () -> environmentAdminRole;
            setAuthorities(organizationAdmin);

            var filter1 = new Filter(API, LTE, "some-value");
            var filter2 = new Filter(API, GTE, "other-value");

            var filters = List.of(filter1, filter2);

            var updatedFilters = apiAnalyticsQueryFilterDecorator.applyPermissionBasedFilters(filters);

            assertThat(updatedFilters).isEqualTo(filters);
        }
    }

    @Nested
    @DisplayName("non-administrators see a subset of the metrics")
    class NonAdministratorUsers {

        @Builder
        record EqTestArguments(
            String role,
            String wantedApiId,
            FilterSpec.Operator expectedOperator,
            Object expectedApiIds,
            String description
        ) {}

        @Builder
        record InTestArguments(String role, List<String> wantedApiIds, List<String> expectedApiIds, String description) {}

        final String user = "testUser";

        final List<String> apisUserIsAMemberOf = Arrays.asList(apiId1, apiId2, apiId3, apiId4, apiId5);
        final List<String> apisUserCanRead = Arrays.asList(apiId3, apiId4, apiId5);

        @BeforeEach
        void setUp() {
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
        @MethodSource("nonAdminRoles")
        void should_update_empty_filter_to_include_allowed_ids(String role) {
            GrantedAuthority environmentUser = () -> role;
            setAuthorities(environmentUser);

            var emptyFilters = new ArrayList<Filter>();
            var updatedFilters = apiAnalyticsQueryFilterDecorator.applyPermissionBasedFilters(emptyFilters);

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
        void should_allow_access_to_single_api(EqTestArguments args) {
            GrantedAuthority environmentUser = () -> args.role;
            setAuthorities(environmentUser);

            var filter = new Filter(API, EQ, args.wantedApiId);

            var updatedFilters = apiAnalyticsQueryFilterDecorator.applyPermissionBasedFilters(List.of(filter));

            assertThat(updatedFilters)
                .singleElement()
                .satisfies(f -> {
                    assertThat(f.name()).isEqualTo(API);
                    assertThat(f.operator()).isEqualTo(args.expectedOperator);
                    assertThat(f.value()).isEqualTo(args.expectedApiIds);
                });
        }

        @ParameterizedTest
        @MethodSource("inTestParams")
        void should_update_filter_list_to_include_only_valid_api_ids(InTestArguments args) {
            GrantedAuthority environmentUser = () -> args.role;
            setAuthorities(environmentUser);

            var filter = new Filter(API, IN, args.wantedApiIds);

            var updatedFilters = apiAnalyticsQueryFilterDecorator.applyPermissionBasedFilters(List.of(filter));

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
        @MethodSource("nonAdminRoles")
        void should_update_multiple_filters(String role) {
            GrantedAuthority environmentUser = () -> role;
            setAuthorities(environmentUser);

            var equalityFilter1 = new Filter(API, EQ, apiId4);
            var equalityFilter2 = new Filter(API, EQ, "invalid-api-id");
            var listFilter = new Filter(API, IN, List.of(apiId3, apiId7));

            var filters = List.of(equalityFilter1, equalityFilter2, listFilter);

            var updatedFilters = apiAnalyticsQueryFilterDecorator.applyPermissionBasedFilters(filters);

            assertThat(updatedFilters).containsExactly(
                equalityFilter1,
                new Filter(API, IN, List.of()),
                new Filter(API, IN, List.of(apiId3))
            );
        }

        static Stream<EqTestArguments> eqTestParams() {
            return nonAdminRoles().mapMulti((role, consumer) -> {
                consumer.accept(
                    EqTestArguments.builder()
                        .description("User has access to the ID requested")
                        .role(role)
                        .wantedApiId(apiId3)
                        .expectedOperator(EQ)
                        .expectedApiIds(apiId3)
                        .build()
                );

                consumer.accept(
                    EqTestArguments.builder()
                        .description("User doesn't have access to the API ID requested")
                        .role(role)
                        .wantedApiId(apiId7)
                        .expectedOperator(IN)
                        .expectedApiIds(List.of())
                        .build()
                );

                consumer.accept(
                    EqTestArguments.builder()
                        .description("User requests an invalid API ID")
                        .role(role)
                        .wantedApiId("invalid-api-id")
                        .expectedOperator(IN)
                        .expectedApiIds(List.of())
                        .build()
                );
            });
        }

        static Stream<InTestArguments> inTestParams() {
            return nonAdminRoles().mapMulti((role, consumer) -> {
                consumer.accept(
                    InTestArguments.builder()
                        .description("User requesting an empty list should get an empty list")
                        .role(role)
                        .wantedApiIds(List.of())
                        .expectedApiIds(List.of())
                        .build()
                );

                consumer.accept(
                    InTestArguments.builder()
                        .description("User has access to the API IDs requested")
                        .role(role)
                        .wantedApiIds(List.of(apiId3, apiId5))
                        .expectedApiIds(List.of(apiId3, apiId5))
                        .build()
                );

                consumer.accept(
                    InTestArguments.builder()
                        .description("User doesn't have access to the API IDs requested")
                        .role(role)
                        .wantedApiIds(List.of(apiId7, apiId8))
                        .expectedApiIds(List.of())
                        .build()
                );

                consumer.accept(
                    InTestArguments.builder()
                        .description("User has access to some if the API IDs requested")
                        .role(role)
                        .wantedApiIds(List.of(apiId3, apiId5, "invalid-api-id"))
                        .expectedApiIds(List.of(apiId3, apiId5))
                        .build()
                );
            });
        }

        // Returns all roles defined in RolePermission formated as SCOPE:PERMISSION
        static Stream<String> nonAdminRoles() {
            Set<String> roles = Arrays.stream(RolePermission.values())
                .map(rolePermission -> String.format("%s:%s", rolePermission.getScope(), rolePermission.getPermission()))
                .collect(Collectors.toSet());

            return roles.stream();
        }
    }

    @Test
    void should_throw_exception_when_IN_filter_is_not_an_iterable() {
        GrantedAuthority grantedAuthority = () -> environmentAdminRole;
        setAuthorities(grantedAuthority);

        var filters = List.of(new Filter(API, IN, 1));

        assertThatThrownBy(() -> apiAnalyticsQueryFilterDecorator.applyPermissionBasedFilters(filters))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Filter value must be an Iterable");
    }

    @Test
    void should_throw_exception_when_IN_filter_contains_non_string_values() {
        var set = new HashSet<>(allApiIds);
        when(apiAuthorizationService.findIdsByEnvironment("DEFAULT")).thenReturn((Set<String>) set.clone(), (Set<String>) set.clone());

        GrantedAuthority grantedAuthority = () -> environmentAdminRole;
        setAuthorities(grantedAuthority);

        var filters = List.of(new Filter(API, IN, List.of(apiId2, true, 6)));

        var updatedFilters = apiAnalyticsQueryFilterDecorator.applyPermissionBasedFilters(filters);

        var expectedFilter = new Filter(API, IN, List.of(apiId2));
        assertThat(updatedFilters).containsExactly(expectedFilter);
    }
}
