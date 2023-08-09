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
package io.gravitee.rest.api.management.v2.rest.resource.api;

import io.gravitee.rest.api.management.v2.rest.model.ApiTransferOwnership;
import io.gravitee.rest.api.management.v2.rest.model.MembershipMemberType;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import org.junit.Test;

import java.util.List;
import java.util.Optional;

import static io.gravitee.common.http.HttpStatusCode.NOT_FOUND_404;
import static io.gravitee.common.http.HttpStatusCode.NO_CONTENT_204;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiResource_TransferOwnershipTest extends ApiResourceTest {

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis/" + API + "/_transfer-ownership";
    }

    @Test
    public void should_not_transfer_ownership_if_api_does_not_exist() {
        when(apiSearchServiceV4.exists(API)).thenReturn(false);
        try (Response response = rootTarget().request().post(null)) {
            assertThat(NOT_FOUND_404).isEqualTo(response.getStatus());
        }
    }

    @Test
    public void should_transfer_ownership_without_roles() {
        when(apiSearchServiceV4.exists(API)).thenReturn(true);

        final ApiTransferOwnership apiTransferOwnership = fakeApiTransferOwnership();
        apiTransferOwnership.setPoRole(null);
        try (Response response = rootTarget().request().post(Entity.json(apiTransferOwnership))) {
            verify(roleService, never()).findByScopeAndName(any(), any(), any());
            verify(membershipService).transferApiOwnership(
                   any(ExecutionContext.class),
                   eq(API),
                   argThat(arg ->
                          arg.getMemberId().equals(apiTransferOwnership.getUserId())
                          && arg.getReference().equals(apiTransferOwnership.getUserReference())
                          && arg.getMemberType().name().equals(apiTransferOwnership.getUserType().name())
                   ),
                   eq(List.of())
            );
            assertThat(NO_CONTENT_204).isEqualTo(response.getStatus());
        }
    }

    @Test
    public void should_transfer_ownership_with_roles() {
        when(apiSearchServiceV4.exists(API)).thenReturn(true);
        final ApiTransferOwnership apiTransferOwnership = fakeApiTransferOwnership();
        final RoleEntity roleEntity = new RoleEntity();
        roleEntity.setId("role");
        roleEntity.setName("a-role-from-db");
        when(roleService.findByScopeAndName(RoleScope.API, apiTransferOwnership.getPoRole(), GraviteeContext.getCurrentOrganization()))
               .thenReturn(
                      Optional.of(
                             roleEntity
                      )
               );

        try (Response response = rootTarget().request().post(Entity.json(apiTransferOwnership))) {
            verify(roleService).findByScopeAndName(any(), any(), any());
            verify(membershipService).transferApiOwnership(
                   any(ExecutionContext.class),
                   eq(API),
                   argThat(arg ->
                          arg.getMemberId().equals(apiTransferOwnership.getUserId())
                                 && arg.getReference().equals(apiTransferOwnership.getUserReference())
                                 && arg.getMemberType().name().equals(apiTransferOwnership.getUserType().name())
                   ),
                   eq(List.of(roleEntity))
            );
            assertThat(NO_CONTENT_204).isEqualTo(response.getStatus());
        }
    }

    // Fakers
    private ApiTransferOwnership fakeApiTransferOwnership() {
        var apiTransferOwnership = new ApiTransferOwnership();
        apiTransferOwnership.setUserId("user");
        apiTransferOwnership.setUserReference("reference");
        apiTransferOwnership.setUserType(MembershipMemberType.USER);
        apiTransferOwnership.setPoRole("role");
        return apiTransferOwnership;
    }
}
