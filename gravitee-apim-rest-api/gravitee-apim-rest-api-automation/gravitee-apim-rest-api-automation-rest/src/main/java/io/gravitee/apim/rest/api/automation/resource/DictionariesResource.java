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
package io.gravitee.apim.rest.api.automation.resource;

import static io.gravitee.rest.api.model.permissions.RolePermissionAction.CREATE;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.UPDATE;

import io.gravitee.apim.core.dictionary.domain_service.ValidateDictionaryDomainService;
import io.gravitee.apim.core.dictionary.use_case.CreateOrUpdateDictionaryUseCase;
import io.gravitee.apim.rest.api.automation.mapper.DictionaryMapper;
import io.gravitee.apim.rest.api.automation.model.DictionarySpec;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.HRIDToUUID;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;

/**
 * @author Benoit Bordigoni (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DictionariesResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private ValidateDictionaryDomainService validateDictionaryDomainService;

    @Inject
    private CreateOrUpdateDictionaryUseCase createOrUpdateDictionaryCRDUseCase;

    @Path("/{hrid}")
    public DictionaryResource getDictionaryResource() {
        return resourceContext.getResource(DictionaryResource.class);
    }

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_DICTIONARY, acls = { CREATE, UPDATE }) })
    public Response createOrUpdate(@Valid @NotNull DictionarySpec spec, @QueryParam("dryRun") boolean dryRun) {
        var dictionary = DictionaryMapper.INSTANCE.toDictionary(spec);
        validateDictionaryDomainService.validate(dictionary);

        if (dryRun) {
            return Response.ok(DictionaryMapper.INSTANCE.toDictionaryState(spec)).build();
        }

        dictionary.setId(HRIDToUUID.dictionary().context(getAuditInfo()).hrid(spec.getHrid()).id());
        var executionContext = GraviteeContext.getExecutionContext();
        var result = createOrUpdateDictionaryCRDUseCase.execute(
            new CreateOrUpdateDictionaryUseCase.Input(executionContext, dictionary, spec.getDeployed())
        );

        return Response.ok(DictionaryMapper.INSTANCE.toDictionaryState(result.dictionary(), executionContext)).build();
    }
}
