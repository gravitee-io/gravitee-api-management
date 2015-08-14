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
package io.gravitee.management.api.resource;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.management.api.exceptions.ApplicationNotFoundException;
import io.gravitee.management.api.exceptions.UserNotFoundException;
import io.gravitee.management.api.model.ApplicationEntity;
import io.gravitee.management.api.model.Owner;
import io.gravitee.management.api.model.UpdateApplicationEntity;
import io.gravitee.management.api.service.ApplicationService;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.PUT;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.Optional;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class ApplicationResource extends AbstractResource {

    @Autowired
    private ApplicationService applicationService;

    @Context
    private SecurityContext securityContext;

    private String applicationName;

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response update(final UpdateApplicationEntity application) {
        ApplicationEntity applicationEntity = getCurrentApplication();

        String authenticatedUser = getAuthenticatedUser();
        Owner owner = applicationEntity.getOwner();
        String ownerLogin = owner.getLogin();

        switch (owner.getType()) {
            case User:
                break;
            case Team:
                break;
        }

        ApplicationEntity updatedApplicationEntity = applicationService.update(applicationName, application);
        if (updatedApplicationEntity != null) {
            return Response.ok().entity(updatedApplicationEntity).build();
        } else {
            return Response.status(HttpStatusCode.BAD_REQUEST_400).build();
        }
    }

    @DELETE
    public Response delete() {
        ApplicationEntity applicationEntity = getCurrentApplication();
        applicationService.delete(applicationEntity.getName());

        return Response.noContent().build();
    }

    private ApplicationEntity getCurrentApplication() throws UserNotFoundException {
        Optional<ApplicationEntity> application = applicationService.findByName(applicationName);
        if (! application.isPresent()) {
            throw new ApplicationNotFoundException();
        }

        return application.get();
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }
}
