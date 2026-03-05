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
package io.gravitee.rest.api.portal.rest.resource;

import io.gravitee.apim.core.application_member.use_case.GetApplicationRolesUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.portal.rest.model.ApplicationRole;
import io.gravitee.rest.api.portal.rest.model.ConfigurationApplicationRolesResponse;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

public class ApplicationRolesResourceV2 extends AbstractResource {

    @Inject
    private GetApplicationRolesUseCase getApplicationRolesUseCase;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getApplicationRolesV2() {
        var result = getApplicationRolesUseCase.execute(new GetApplicationRolesUseCase.Input(GraviteeContext.getCurrentOrganization()));

        return Response.ok(
            new ConfigurationApplicationRolesResponse().data(
                result
                    .roles()
                    .stream()
                    .map(role ->
                        new ApplicationRole()._default(role.isDefaultRole()).id(role.getName()).name(role.getName()).system(role.isSystem())
                    )
                    .toList()
            )
        ).build();
    }
}
