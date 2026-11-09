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
package io.gravitee.rest.api.management.v2.rest.resource.secret;

import io.gravitee.apim.core.secret.model.SecretValue;
import io.gravitee.apim.core.secret.use_case.CreateSecretUseCase;
import io.gravitee.apim.core.secret.use_case.SearchSecretsUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.mapper.SecretMapper;
import io.gravitee.rest.api.management.v2.rest.model.CreateSecret;
import io.gravitee.rest.api.management.v2.rest.model.SecretsResponse;
import io.gravitee.rest.api.management.v2.rest.pagination.PaginationInfo;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.v2.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.GraviteeContext;
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

public class SecretsResource extends AbstractResource {

    private static final int MAX_PER_PAGE = 100;

    @Context
    private ResourceContext resourceContext;

    @Inject
    private CreateSecretUseCase createSecretUseCase;

    @Inject
    private SearchSecretsUseCase searchSecretsUseCase;

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_SECRET, acls = { RolePermissionAction.CREATE }) })
    public Response createSecret(@Valid @NotNull final CreateSecret createSecret) {
        var output = createSecretUseCase.execute(
            new CreateSecretUseCase.Input(createSecret.getKey(), new SecretValue(createSecret.getValue()), getAuditInfo())
        );
        return Response.created(getLocationHeader(output.secret().id())).entity(SecretMapper.INSTANCE.map(output.secret())).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_SECRET, acls = { RolePermissionAction.READ }) })
    public SecretsResponse listSecrets(
        @QueryParam(PaginationParam.PAGE_QUERY_PARAM_NAME) @DefaultValue("1") @Min(1) int page,
        @QueryParam(PaginationParam.PER_PAGE_QUERY_PARAM_NAME) @DefaultValue("10") @Min(1) @Max(MAX_PER_PAGE) int perPage
    ) {
        var executionContext = GraviteeContext.getExecutionContext();
        var paginationParam = new PaginationParam(page, perPage);
        var secrets = searchSecretsUseCase
            .execute(
                new SearchSecretsUseCase.Input(
                    executionContext.getOrganizationId(),
                    executionContext.getEnvironmentId(),
                    paginationParam.toPageable()
                )
            )
            .secrets();

        return new SecretsResponse()
            .data(SecretMapper.INSTANCE.map(secrets.getContent()))
            .pagination(PaginationInfo.computePaginationInfo(secrets.getTotalElements(), secrets.getContent().size(), paginationParam))
            .links(computePaginationLinks(secrets.getTotalElements(), paginationParam));
    }

    @Path("{secretId}")
    public SecretResource getSecretResource() {
        return resourceContext.getResource(SecretResource.class);
    }
}
