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
package io.gravitee.management.rest.resource;

import io.gravitee.common.component.Lifecycle;
import io.gravitee.management.model.*;
import io.gravitee.management.service.ApiService;
import io.gravitee.management.service.UserService;
import io.gravitee.management.service.exceptions.ApiAlreadyExistsException;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
@Path("/apis")
public class ApisResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private ApiService apiService;

    @Inject
    private UserService userService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Set<ApiListItem> list() {
        Set<ApiEntity> apis;
        if (isAuthenticated()) {
            apis = apiService.findByUser(getAuthenticatedUser());
        } else {
            apis = apiService.findByVisibility(Visibility.PUBLIC);
        }

        return apis.stream()
                .map(this::convert)
                .sorted((o1, o2) -> o1.getName().compareTo(o2.getName()))
                .collect(Collectors.toSet());
    }

    /**
     * Create a new API for the authenticated user.
     * @param newApiEntity
     * @return
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(@Valid final NewApiEntity newApiEntity) throws ApiAlreadyExistsException {
        ApiEntity newApi = apiService.create(newApiEntity, getAuthenticatedUser());
        if (newApi != null) {
            return Response
                    .created(URI.create("/apis/" + newApi.getId()))
                    .entity(newApi)
                    .build();
        }

        return Response.serverError().build();
    }

    @Path("{api}")
    public ApiResource getApiResource() {
        return resourceContext.getResource(ApiResource.class);
    }

    private ApiListItem convert(ApiEntity api) {
        ApiListItem apiItem = new ApiListItem();

        apiItem.setId(api.getId());
        apiItem.setName(api.getName());
        apiItem.setVersion(api.getVersion());
        apiItem.setDescription(api.getDescription());
        apiItem.setCreatedAt(api.getCreatedAt());
        apiItem.setUpdatedAt(api.getUpdatedAt());

        if (api.getVisibility() != null) {
            apiItem.setVisibility(io.gravitee.management.model.Visibility.valueOf(api.getVisibility().toString()));
        }

        if (api.getState() != null) {
            apiItem.setState(Lifecycle.State.valueOf(api.getState().toString()));
        }

        // Add primary owner
        Collection<MemberEntity> members = apiService.getMembers(api.getId(), MembershipType.PRIMARY_OWNER);
        if (! members.isEmpty()) {
            MemberEntity primaryOwner = members.iterator().next();
            UserEntity user = userService.findByName(primaryOwner.getUser());

            PrimaryOwnerEntity owner = new PrimaryOwnerEntity();
            owner.setUsername(user.getUsername());
            owner.setEmail(user.getEmail());
            owner.setFirstname(user.getFirstname());
            owner.setLastname(user.getLastname());
            apiItem.setPrimaryOwner(owner);
        }

        // Add permission for current user (if authenticated)
        if(isAuthenticated()) {
            MemberEntity member = apiService.getMember(apiItem.getId(), getAuthenticatedUser());
            if (member != null) {
                apiItem.setPermission(member.getType());
            } else {
                if (apiItem.getVisibility() == Visibility.PUBLIC) {
                    // If API is public, all users have the user permission
                    apiItem.setPermission(MembershipType.USER);
                }
            }
        }

        return apiItem;
    }
}
