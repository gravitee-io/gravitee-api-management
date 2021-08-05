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
package io.gravitee.rest.api.management.rest.resource.configuration.dictionary;

import static java.util.stream.Collectors.toList;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.rest.model.configuration.dictionary.DictionaryListItem;
import io.gravitee.rest.api.management.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.configuration.dictionary.DictionaryEntity;
import io.gravitee.rest.api.model.configuration.dictionary.NewDictionaryEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.configuration.dictionary.DictionaryService;
import io.swagger.annotations.*;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = { "Configuration", "Dictionaries" })
public class DictionariesResource extends AbstractResource {

    @Autowired
    private DictionaryService dictionaryService;

    @Context
    private ResourceContext resourceContext;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions(@Permission(value = RolePermission.ENVIRONMENT_DICTIONARY, acls = RolePermissionAction.READ))
    @ApiOperation(
        value = "Get the list of global dictionaries",
        notes = "User must have the DICTIONARY[READ] permission to use this service"
    )
    @ApiResponses(
        {
            @ApiResponse(code = 200, message = "List global dictionaries", response = DictionaryListItem.class, responseContainer = "List"),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    public List<DictionaryListItem> getDictionaries() {
        return dictionaryService
            .findAll()
            .stream()
            .map(
                dictionaryEntity -> {
                    DictionaryListItem item = new DictionaryListItem();
                    item.setId(dictionaryEntity.getId());
                    item.setName(dictionaryEntity.getName());
                    item.setDescription(dictionaryEntity.getDescription());
                    item.setCreatedAt(dictionaryEntity.getCreatedAt());
                    item.setUpdatedAt(dictionaryEntity.getUpdatedAt());
                    item.setDeployedAt(dictionaryEntity.getDeployedAt());
                    item.setType(dictionaryEntity.getType());
                    item.setState(dictionaryEntity.getState());

                    if (dictionaryEntity.getProperties() != null) {
                        item.setProperties(dictionaryEntity.getProperties().size());
                    }

                    if (dictionaryEntity.getProvider() != null) {
                        item.setProvider(dictionaryEntity.getProvider().getType());
                    }

                    return item;
                }
            )
            .collect(toList());
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_DICTIONARY, acls = RolePermissionAction.CREATE) })
    @ApiOperation(value = "Create a dictionary", notes = "User must have the DICTIONARY[CREATE] permission to use this service")
    @ApiResponses(
        {
            @ApiResponse(code = 201, message = "Dictionary successfully created", response = DictionaryEntity.class),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    public Response createDictionary(
        @ApiParam(name = "dictionary", required = true) @Valid @NotNull NewDictionaryEntity newDictionaryEntity
    ) {
        DictionaryEntity newDictionary = dictionaryService.create(newDictionaryEntity);

        if (newDictionary != null) {
            return Response.created(this.getLocationHeader(newDictionary.getId())).entity(newDictionary).build();
        }

        return Response.serverError().build();
    }

    @Path("{dictionary}")
    public DictionaryResource getDictionaryResource() {
        return resourceContext.getResource(DictionaryResource.class);
    }
}
