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

import io.gravitee.common.http.MediaType;
import io.gravitee.management.model.NewTenantEntity;
import io.gravitee.management.model.TenantEntity;
import io.gravitee.management.model.UpdateTenantEntity;
import io.gravitee.management.service.TenantService;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = {"Configuration", "Tenants"})
public class TenantsResource extends AbstractResource  {

    @Autowired
    private TenantService tenantService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<TenantEntity> list()  {
        return tenantService.findAll()
                .stream()
                .sorted((o1, o2) -> String.CASE_INSENSITIVE_ORDER.compare(o1.getName(), o2.getName()))
                .collect(Collectors.toList());
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<TenantEntity> create(@Valid @NotNull final List<NewTenantEntity> tenant) {
        return tenantService.create(tenant);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<TenantEntity> update(@Valid @NotNull final List<UpdateTenantEntity> tenant) {
        return tenantService.update(tenant);
    }

    @Path("{tenant}")
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    public void delete(@PathParam("tenant") String tenant) {
        tenantService.delete(tenant);
    }
}
