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
package io.gravitee.rest.api.management.rest.resource.organization;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.rest.resource.AbstractResource;
import io.gravitee.rest.api.model.PasswordPolicyEntity;
import io.gravitee.rest.api.service.PasswordPolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;
import org.springframework.beans.factory.annotation.Autowired;

@Tag(name = "Configuration")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class PasswordPolicyResource extends AbstractResource {

    @Autowired
    private PasswordPolicyService passwordPolicyService;

    @GET
    @Operation(
        summary = "Get the password policy",
        description = "Returns the configured password policy for user registration and reset flows"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Password policy",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PasswordPolicyEntity.class))
    )
    public PasswordPolicyEntity getPasswordPolicy() {
        return passwordPolicyService.getPasswordPolicy();
    }
}
