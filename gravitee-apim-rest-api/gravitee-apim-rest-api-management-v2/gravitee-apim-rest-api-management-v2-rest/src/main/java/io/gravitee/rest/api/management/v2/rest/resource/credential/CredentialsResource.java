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
package io.gravitee.rest.api.management.v2.rest.resource.credential;

import io.gravitee.apim.core.credential.model.CredentialQuery;
import io.gravitee.apim.core.credential.model.CredentialStrategy;
import io.gravitee.apim.core.credential.use_case.CreateCredentialUseCase;
import io.gravitee.apim.core.credential.use_case.SearchCredentialsUseCase;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.mapper.CredentialMapper;
import io.gravitee.rest.api.management.v2.rest.model.CreateCredential;
import io.gravitee.rest.api.management.v2.rest.model.CredentialsResponse;
import io.gravitee.rest.api.management.v2.rest.pagination.PaginationInfo;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.v2.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.UuidString;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class CredentialsResource extends AbstractResource {

    private static final int MAX_PER_PAGE = 100;

    @Context
    private ResourceContext resourceContext;

    @Inject
    private CreateCredentialUseCase createCredentialUseCase;

    @Inject
    private SearchCredentialsUseCase searchCredentialsUseCase;

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_CREDENTIAL, acls = { RolePermissionAction.CREATE }) })
    public Response createCredential(@Valid @NotNull final CreateCredential createCredential) {
        var credential = createCredentialUseCase
            .execute(
                new CreateCredentialUseCase.Input(
                    UuidString.generateRandom(),
                    CurrentCredentialActor.of(securityContext, permissionService),
                    CredentialMapper.INSTANCE.map(createCredential)
                )
            )
            .credential();
        return Response.created(getLocationHeader(credential.id())).entity(CredentialMapper.INSTANCE.map(credential)).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_CREDENTIAL, acls = { RolePermissionAction.READ }) })
    public CredentialsResponse listCredentials(
        @QueryParam("q") String q,
        @QueryParam("strategy") List<String> strategies,
        @QueryParam("serviceId") String serviceId,
        @QueryParam(PaginationParam.PAGE_QUERY_PARAM_NAME) @DefaultValue("1") @Min(1) int page,
        @QueryParam(PaginationParam.PER_PAGE_QUERY_PARAM_NAME) @DefaultValue("10") @Min(1) @Max(MAX_PER_PAGE) int perPage
    ) {
        var query = CredentialQuery.builder()
            .name(blankToNull(q))
            .strategies(strategies(strategies))
            .serviceId(blankToNull(serviceId))
            .build();
        var paginationParam = new PaginationParam(page, perPage);
        var credentials = searchCredentialsUseCase
            .execute(
                new SearchCredentialsUseCase.Input(
                    CurrentCredentialActor.of(securityContext, permissionService),
                    query,
                    paginationParam.toPageable()
                )
            )
            .page();

        return new CredentialsResponse()
            .data(CredentialMapper.INSTANCE.map(credentials.getContent()))
            .pagination(
                PaginationInfo.computePaginationInfo(credentials.getTotalElements(), credentials.getContent().size(), paginationParam)
            )
            .links(computePaginationLinks(credentials.getTotalElements(), paginationParam));
    }

    @Path("{credentialId}")
    public CredentialResource getCredentialResource() {
        return resourceContext.getResource(CredentialResource.class);
    }

    private static Set<CredentialStrategy> strategies(List<String> names) {
        if (names == null || names.isEmpty()) {
            return null;
        }
        var strategies = EnumSet.noneOf(CredentialStrategy.class);
        for (var name : names) {
            try {
                strategies.add(CredentialStrategy.valueOf(name));
            } catch (IllegalArgumentException e) {
                throw new ValidationDomainException(
                    "strategy must be one of " + Arrays.toString(CredentialStrategy.values()) + ", not " + name + "."
                );
            }
        }
        return strategies;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
