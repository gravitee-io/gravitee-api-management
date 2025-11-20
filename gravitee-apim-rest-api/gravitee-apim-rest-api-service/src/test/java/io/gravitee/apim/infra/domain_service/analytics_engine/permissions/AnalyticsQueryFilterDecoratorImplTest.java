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

import static io.gravitee.rest.api.model.permissions.RolePermission.API_ANALYTICS;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.READ;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.analytics_engine.model.Filter;
import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.rest.api.model.permissions.RolePermission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.security.core.GrantedAuthority;

/**
 * @author GraviteeSource Team
 */
public class AnalyticsQueryFilterDecoratorImplTest extends AbstractPermissionsTest {

    List<String> allApiIds = List.of(
        UUID.randomUUID().toString(),
        UUID.randomUUID().toString(),
        UUID.randomUUID().toString(),
        UUID.randomUUID().toString(),
        UUID.randomUUID().toString(),
        UUID.randomUUID().toString(),
        UUID.randomUUID().toString(),
        UUID.randomUUID().toString(),
        UUID.randomUUID().toString(),
        UUID.randomUUID().toString()
    );

    @Test
    public void should_not_modify_non_enty_filter() {
        var EqualityFilter = new Filter(FilterSpec.Name.API, FilterSpec.Operator.EQ, "43985673406573406");
        var filters = new ArrayList<>(Arrays.asList(EqualityFilter));

        var updatedFilters = analyticsQueryFilterDecorator.getUpdatedFilters(filters);

        assertThat(updatedFilters).isEqualTo(filters);
    }

    @Test
    public void should_modify_empty_filter_when_user_is_org_admin() {
        GrantedAuthority organizationAdmin = () -> "ORGANIZATION:ADMIN";
        setAuthorities(organizationAdmin);

        when(apiAuthorizationService.findIdsByEnvironment("DEFAULT")).thenReturn(new HashSet<>(allApiIds));

        var emptyFilters = new ArrayList<Filter>();
        var updatedFilters = analyticsQueryFilterDecorator.getUpdatedFilters(emptyFilters);

        assertThat(updatedFilters).singleElement();

        var filter = updatedFilters.get(0);
        assertThat(filter.name()).isEqualByComparingTo(FilterSpec.Name.API);
        assertThat(filter.operator()).isEqualByComparingTo(FilterSpec.Operator.IN);
        assertThat((Iterable<String>) filter.value()).containsExactlyInAnyOrderElementsOf(allApiIds);
    }

    @ParameterizedTest
    @MethodSource("nonAdminRoles")
    public void should_modify_empty_filter_when_user_is_not_org_admin(String role) {
        var user1 = "user1";

        setAuthenticatedUsername(user1);
        GrantedAuthority environmentUser = () -> role;
        setAuthorities(environmentUser);

        var user1ApiIds = allApiIds.subList(1, 6);
        when(apiAuthorizationService.findIdsByUser(any(), eq(user1), anyBoolean())).thenReturn(new HashSet<>(user1ApiIds));

        var apisUser1CanRead = user1ApiIds.subList(1, 3);
        when(
            permissionService.hasPermission(any(), eq(API_ANALYTICS), argThat(apiId -> apisUser1CanRead.contains(apiId)), eq(READ))
        ).thenReturn(true);

        var emptyFilters = new ArrayList<Filter>();
        var updatedFilters = analyticsQueryFilterDecorator.getUpdatedFilters(emptyFilters);

        assertThat(updatedFilters).singleElement();

        var filter = updatedFilters.get(0);
        assertThat(filter.name()).isEqualByComparingTo(FilterSpec.Name.API);
        assertThat(filter.operator()).isEqualByComparingTo(FilterSpec.Operator.IN);
        assertThat((Iterable<String>) filter.value()).containsExactlyInAnyOrderElementsOf(apisUser1CanRead);
    }

    static Stream<String> nonAdminRoles() {
        Set<String> roles = Arrays.stream(RolePermission.values())
            .map(rolePermission -> String.format("%s:%s", rolePermission.getScope(), rolePermission.getPermission()))
            .collect(Collectors.toSet());

        roles.add("ENVIRONMENT:ADMIN");

        return roles.stream();
    }
}
