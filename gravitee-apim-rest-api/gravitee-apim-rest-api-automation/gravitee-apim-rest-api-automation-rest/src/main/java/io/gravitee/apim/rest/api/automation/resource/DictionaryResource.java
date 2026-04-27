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
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.DELETE;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.UPDATE;

import io.gravitee.apim.core.dictionary.domain_service.DictionaryAutomationDomainService;
import io.gravitee.apim.core.dictionary.use_case.DeleteDictionaryUseCase;
import io.gravitee.apim.rest.api.automation.exception.HRIDNotFoundException;
import io.gravitee.apim.rest.api.automation.mapper.DictionaryMapper;
import io.gravitee.apim.rest.api.automation.model.DictionaryState;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.configuration.dictionary.DictionaryEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.HRIDToUUID;
import io.gravitee.rest.api.service.impl.configuration.dictionary.DictionaryNotFoundException;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

/**
 * @author Benoit Bordigoni (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DictionaryResource extends AbstractResource {

    @Inject
    private DeleteDictionaryUseCase deleteDictionaryUseCase;

    @Inject
    private DictionaryAutomationDomainService dictionaryService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_DICTIONARY, acls = { RolePermissionAction.READ }) })
    public Response getDictionaryByHRID(@PathParam("hrid") String hrid) {
        var executionContext = GraviteeContext.getExecutionContext();
        String id = HRIDToUUID.dictionary().context(getAuditInfo()).hrid(hrid).id();

        DictionaryEntity entity = dictionaryService.findById(executionContext, id).orElseThrow(() -> new HRIDNotFoundException(hrid));

        DictionaryState state = DictionaryMapper.INSTANCE.toDictionaryState(entity, executionContext);

        boolean canWrite = hasPermission(
            executionContext,
            RolePermission.ENVIRONMENT_DICTIONARY,
            executionContext.getEnvironmentId(),
            CREATE,
            UPDATE,
            DELETE
        );
        if (!canWrite) {
            state.setDynamic(null);
        }

        return Response.ok(state).build();
    }

    @DELETE
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_DICTIONARY, acls = RolePermissionAction.DELETE) })
    public Response deleteDictionaryByHrid(@PathParam("hrid") String hrid) {
        var executionContext = GraviteeContext.getExecutionContext();
        try {
            String id = HRIDToUUID.dictionary().context(getAuditInfo()).hrid(hrid).id();
            deleteDictionaryUseCase.execute(new DeleteDictionaryUseCase.Input(executionContext, id));
        } catch (DictionaryNotFoundException e) {
            throw new HRIDNotFoundException(hrid);
        }
        return Response.noContent().build();
    }
}
