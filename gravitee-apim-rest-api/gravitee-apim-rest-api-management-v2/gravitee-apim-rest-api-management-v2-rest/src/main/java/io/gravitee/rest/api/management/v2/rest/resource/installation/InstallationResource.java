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
package io.gravitee.rest.api.management.v2.rest.resource.installation;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.model.ConsoleApplication;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import java.util.List;

/**
 * Defines the REST resource for installation-level endpoints, including the list of
 * console applications available in the Gamma environment dropdown.
 *
 * @author GraviteeSource Team
 */
@Tag(name = "Installation")
@Path("/installation")
public class InstallationResource extends AbstractResource {

    @GET
    @Path("/applications")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "List console applications",
        description = "Returns the list of applications available in the Gamma console dropdown for the current user."
    )
    @ApiResponse(
        responseCode = "200",
        description = "List of console applications",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            array = @ArraySchema(schema = @Schema(implementation = ConsoleApplication.class))
        )
    )
    public List<ConsoleApplication> getApplications() {
        return List.of(
            new ConsoleApplication("app-alpha", "App Alpha From Backend", "Box"),
            new ConsoleApplication("app-beta", "App Beta From Backend", "Settings"),
            new ConsoleApplication("developer-portal", "Developer Portal From Backend", "Globe")
        );
    }
}
