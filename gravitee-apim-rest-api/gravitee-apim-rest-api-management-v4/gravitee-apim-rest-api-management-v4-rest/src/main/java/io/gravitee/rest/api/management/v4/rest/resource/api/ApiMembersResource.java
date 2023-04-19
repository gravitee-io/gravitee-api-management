/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.management.v4.rest.resource.api;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v4.rest.mapper.MemberMapper;
import io.gravitee.rest.api.management.v4.rest.model.Member;
import io.gravitee.rest.api.management.v4.rest.model.MembersResponse;
import io.gravitee.rest.api.management.v4.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.v4.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.management.v4.rest.security.Permission;
import io.gravitee.rest.api.management.v4.rest.security.Permissions;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.Comparator;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;

@Path("/environments/{envId}/apis/{apiId}/members")
public class ApiMembersResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @PathParam("apiId")
    private String apiId;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_MEMBER, acls = { RolePermissionAction.READ }) })
    public MembersResponse getApiMembers(@BeanParam @Valid PaginationParam paginationParam) {
        var members = membershipService
            .getMembersByReference(GraviteeContext.getExecutionContext(), MembershipReferenceType.API, apiId)
            .stream()
            .filter(memberEntity -> memberEntity.getType() == MembershipMemberType.USER)
            .map(MemberMapper.INSTANCE::convert)
            .sorted(Comparator.comparing(Member::getId))
            .collect(Collectors.toList());
        return new MembersResponse()
            .data(computePaginationData(members, paginationParam))
            .pagination(computePaginationInfo(members.size(), 1, paginationParam))
            .links(computePaginationLinks(members.size(), paginationParam));
    }
}
