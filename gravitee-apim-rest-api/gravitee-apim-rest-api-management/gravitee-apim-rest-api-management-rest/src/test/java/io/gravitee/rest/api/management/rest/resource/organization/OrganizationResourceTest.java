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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.rest.api.management.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.OrganizationEntity;
import io.gravitee.rest.api.model.UpdateOrganizationEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.ExecutionContext;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author GraviteeSource Team
 */
public class OrganizationResourceTest extends AbstractResourceTest {

    private static final String ORGANIZATION_ID = "DEFAULT";

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
                anyString(),
                any(RolePermissionAction[].class)
            )
        ).thenAnswer(invocation -> {
            // Mockito expands varargs, so the declared acls are the arguments after the reference id.
            Object[] arguments = invocation.getArguments();
            return Arrays.stream(arguments, 3, arguments.length).map(RolePermissionAction.class::cast).anyMatch(heldAcls::contains);
        });
    }
}
