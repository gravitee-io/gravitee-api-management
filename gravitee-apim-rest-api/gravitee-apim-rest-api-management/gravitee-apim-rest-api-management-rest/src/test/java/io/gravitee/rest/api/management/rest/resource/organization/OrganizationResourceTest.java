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
package io.gravitee.rest.api.management.rest.resource.organization;

import static jakarta.ws.rs.client.Entity.json;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.management.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.OrganizationEntity;
import io.gravitee.rest.api.model.UpdateOrganizationEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.ExecutionContext;
import jakarta.ws.rs.core.Response;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class OrganizationResourceTest extends AbstractResourceTest {

    private static final String ORGANIZATION_ID = "DEFAULT";

    private Set<RolePermissionAction> grantedPolicyActions = EnumSet.noneOf(RolePermissionAction.class);

    @Override
    protected String contextPath() {
        return "";
    }

    @BeforeEach
    public void setUpOrganization() {
        grantedPolicyActions = EnumSet.noneOf(RolePermissionAction.class);
        when(
            permissionService.hasPermission(
                any(ExecutionContext.class),
                any(RolePermission.class),
                anyString(),
                any(RolePermissionAction[].class)
            )
        ).thenAnswer(invocation -> {
            final Object[] arguments = invocation.getArguments();
            if (!RolePermission.ORGANIZATION_POLICIES.equals(arguments[1])) {
                return false;
            }
            return requiredActions(arguments).anyMatch(grantedPolicyActions::contains);
        });

        final OrganizationEntity organization = new OrganizationEntity();
        organization.setId(ORGANIZATION_ID);
        organization.setName("Default organization");
        when(organizationService.findById(ORGANIZATION_ID)).thenReturn(organization);
        when(organizationService.updateOrganizationAndFlows(eq(ORGANIZATION_ID), any(UpdateOrganizationEntity.class))).thenReturn(
            organization
        );
    }

    @Test
    public void should_get_organization_with_policies_read_permission() {
        userIsGrantedOnPolicies(RolePermissionAction.READ);

        final Response response = orgTarget().request().get();

        assertEquals(HttpStatusCode.OK_200, response.getStatus());
    }

    @Test
    public void should_not_update_organization_with_policies_read_permission() {
        userIsGrantedOnPolicies(RolePermissionAction.READ);

        final Response response = orgTarget().request().put(json(anUpdateOrganization()));

        assertEquals(HttpStatusCode.FORBIDDEN_403, response.getStatus());
    }

    @Test
    public void should_not_get_organization_without_policies_permission() {
        final Response response = orgTarget().request().get();

        assertEquals(HttpStatusCode.FORBIDDEN_403, response.getStatus());
    }

    @Test
    public void should_update_organization_with_policies_update_permission() {
        userIsGrantedOnPolicies(RolePermissionAction.UPDATE);

        final Response response = orgTarget().request().put(json(anUpdateOrganization()));

        assertEquals(HttpStatusCode.NO_CONTENT_204, response.getStatus());
    }

    @Test
    public void should_update_organization_with_policies_create_permission() {
        userIsGrantedOnPolicies(RolePermissionAction.CREATE);

        final Response response = orgTarget().request().put(json(anUpdateOrganization()));

        assertEquals(HttpStatusCode.NO_CONTENT_204, response.getStatus());
    }

    private void userIsGrantedOnPolicies(final RolePermissionAction... actions) {
        grantedPolicyActions = EnumSet.copyOf(Arrays.asList(actions));
    }

    /**
     * Mockito expands the varargs of {@link io.gravitee.rest.api.service.PermissionService#hasPermission} inconsistently:
     * they may show up either as trailing arguments or as a single array argument.
     */
    private Stream<Object> requiredActions(final Object[] arguments) {
        return Arrays.stream(arguments, 3, arguments.length).flatMap(argument ->
            argument instanceof Object[] array ? Arrays.stream(array) : Stream.of(argument)
        );
    }

    private UpdateOrganizationEntity anUpdateOrganization() {
        final UpdateOrganizationEntity organization = new UpdateOrganizationEntity();
        organization.setName("Default organization");
        return organization;
    }
}
