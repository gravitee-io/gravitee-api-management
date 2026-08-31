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

<<<<<<< HEAD
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

=======
import static jakarta.ws.rs.client.Entity.json;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.HttpStatusCode;
>>>>>>> eff65d4 (fix(rest-api): require POLICIES[READ] to get the organization)
import io.gravitee.rest.api.management.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.OrganizationEntity;
import io.gravitee.rest.api.model.UpdateOrganizationEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.ExecutionContext;
<<<<<<< HEAD
import jakarta.ws.rs.client.Entity;
=======
>>>>>>> eff65d4 (fix(rest-api): require POLICIES[READ] to get the organization)
import jakarta.ws.rs.core.Response;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
<<<<<<< HEAD
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author GraviteeSource Team
 */
=======
import java.util.stream.Stream;
import org.junit.Before;
import org.junit.Test;

>>>>>>> eff65d4 (fix(rest-api): require POLICIES[READ] to get the organization)
public class OrganizationResourceTest extends AbstractResourceTest {

    private static final String ORGANIZATION_ID = "DEFAULT";

<<<<<<< HEAD
    @Override
    protected String contextPath() {
        return "organizations/" + ORGANIZATION_ID;
    }

    @BeforeEach
    public void init() {
        reset(organizationService);

        OrganizationEntity organization = new OrganizationEntity();
        organization.setId(ORGANIZATION_ID);
        organization.setName("Default organization");
        when(organizationService.findById(ORGANIZATION_ID)).thenReturn(organization);
    }

    @Test
    public void should_get_organization_when_holding_only_policies_read() {
        givenUserHolds(RolePermissionAction.READ);

        final Response response = rootTarget("").request().get();

        assertThat(response.getStatus()).isEqualTo(200);
        verify(organizationService).findById(ORGANIZATION_ID);
    }

    @Test
    public void should_get_organization_when_holding_only_policies_update() {
        givenUserHolds(RolePermissionAction.UPDATE);

        final Response response = rootTarget("").request().get();

        assertThat(response.getStatus()).isEqualTo(200);
        verify(organizationService).findById(ORGANIZATION_ID);
    }

    @Test
    public void should_not_get_organization_when_holding_no_policies_acl() {
        givenUserHolds();

        final Response response = rootTarget("").request().get();

        assertThat(response.getStatus()).isEqualTo(403);
        verify(organizationService, never()).findById(anyString());
    }

    @Test
    public void should_not_update_organization_when_holding_only_policies_read() {
        givenUserHolds(RolePermissionAction.READ);

        final Response response = rootTarget("").request().put(Entity.json(new UpdateOrganizationEntity()));

        assertThat(response.getStatus()).isEqualTo(403);
        verify(organizationService, never()).updateOrganizationAndFlows(anyString(), any(UpdateOrganizationEntity.class));
    }

    /**
     * Answers the ORGANIZATION_POLICIES check the way the role service does: the user passes only when one of the acls
     * the endpoint declares is one it actually holds, so an acl the endpoint omits can never be granted.
     */
    private void givenUserHolds(RolePermissionAction... held) {
        Set<RolePermissionAction> heldAcls = held.length == 0
            ? EnumSet.noneOf(RolePermissionAction.class)
            : EnumSet.copyOf(Arrays.asList(held));

        when(
            permissionService.hasPermission(
                any(ExecutionContext.class),
                eq(RolePermission.ORGANIZATION_POLICIES),
=======
    private Set<RolePermissionAction> grantedPolicyActions = EnumSet.noneOf(RolePermissionAction.class);

    @Override
    protected String contextPath() {
        return "";
    }

    @Before
    public void setUpOrganization() {
        grantedPolicyActions = EnumSet.noneOf(RolePermissionAction.class);
        when(
            permissionService.hasPermission(
                any(ExecutionContext.class),
                any(RolePermission.class),
>>>>>>> eff65d4 (fix(rest-api): require POLICIES[READ] to get the organization)
                anyString(),
                any(RolePermissionAction[].class)
            )
        ).thenAnswer(invocation -> {
<<<<<<< HEAD
            // Mockito expands varargs, so the declared acls are the arguments after the reference id.
            Object[] arguments = invocation.getArguments();
            return Arrays.stream(arguments, 3, arguments.length).map(RolePermissionAction.class::cast).anyMatch(heldAcls::contains);
        });
=======
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
>>>>>>> eff65d4 (fix(rest-api): require POLICIES[READ] to get the organization)
    }
}
