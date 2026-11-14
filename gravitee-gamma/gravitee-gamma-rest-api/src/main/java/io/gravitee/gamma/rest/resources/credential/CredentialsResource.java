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
package io.gravitee.gamma.rest.resources.credential;

import io.gravitee.apim.core.credential.model.CredentialQuery;
import io.gravitee.apim.core.credential.model.CredentialStrategy;
import io.gravitee.apim.core.credential.use_case.CreateCredentialUseCase;
import io.gravitee.apim.core.credential.use_case.SearchCredentialsUseCase;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.gamma.rest.resources.credential.dto.CreateCredentialRequestDto;
import io.gravitee.gamma.rest.resources.credential.dto.CredentialDto;
import io.gravitee.gamma.rest.resources.tracing.dto.PaginatedResponseDto;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.common.UuidString;
import jakarta.inject.Inject;
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
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * The credentials of an environment, mounted under
 * {@code /gamma/organizations/{orgId}/environments/{envId}/credentials} by {@code GammaRootResource}. Any Gamma module
 * can use them to call an external service. Responses carry references to the secrets, never their values.
 */
@Produces(MediaType.APPLICATION_JSON)
public class CredentialsResource {

    private static final int MAX_PER_PAGE = 100;

    @Context
    private ResourceContext resourceContext;

    @Context
    private UriInfo uriInfo;

    @Context
    private SecurityContext securityContext;

    @Inject
    private PermissionService permissionService;

    @Inject
    private MembershipService membershipService;

    @Inject
    private CreateCredentialUseCase createCredentialUseCase;

    @Inject
    private SearchCredentialsUseCase searchCredentialsUseCase;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_CREDENTIAL, acls = { RolePermissionAction.CREATE }) })
    public Response createCredential(@NotNull CreateCredentialRequestDto request) {
        var actor = CurrentCredentialActor.of(securityContext, permissionService, membershipService);
        var credential = createCredentialUseCase
            .execute(new CreateCredentialUseCase.Input(UuidString.generateRandom(), actor, request.toCommand()))
            .credential();
        return Response.created(uriInfo.getAbsolutePathBuilder().path(credential.id()).build())
            .entity(CredentialDto.from(credential, actor))
            .build();
    }

    /** Sorted by name; {@code strategy} takes several strategies, comma-separated or repeated. */
    @GET
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_CREDENTIAL, acls = { RolePermissionAction.READ }) })
    public PaginatedResponseDto<CredentialDto> listCredentials(
        @QueryParam("q") String q,
        @QueryParam("strategy") List<String> strategies,
        @QueryParam("serviceId") String serviceId,
        @QueryParam("page") @DefaultValue("1") @Min(1) int page,
        @QueryParam("perPage") @DefaultValue("10") @Min(1) @Max(MAX_PER_PAGE) int perPage
    ) {
        var query = CredentialQuery.builder()
            .name(blankToNull(q))
            .strategies(strategies(strategies))
            .serviceId(blankToNull(serviceId))
            .build();
        var actor = CurrentCredentialActor.of(securityContext, permissionService, membershipService);
        var output = searchCredentialsUseCase.execute(new SearchCredentialsUseCase.Input(actor, query, new PageableImpl(page, perPage)));
        var credentials = output.page();
        return PaginatedResponseDto.of(
            CredentialDto.from(credentials.getContent(), actor, output.ownerNames()),
            credentials.getTotalElements(),
            page,
            perPage
        );
    }

    @Path("{credentialId}")
    public CredentialResource getCredentialResource() {
        return resourceContext.getResource(CredentialResource.class);
    }

    private static Set<CredentialStrategy> strategies(List<String> params) {
        var names = params
            .stream()
            .flatMap(param -> Arrays.stream(param.split(",")))
            .map(String::trim)
            .filter(name -> !name.isEmpty())
            .toList();
        if (names.isEmpty()) {
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
