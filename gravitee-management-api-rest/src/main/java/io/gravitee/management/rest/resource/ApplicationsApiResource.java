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

import io.gravitee.management.model.ApiEntity;
import io.gravitee.management.model.ApplicationEntity;
import io.gravitee.management.rest.annotation.Role;
import io.gravitee.management.rest.annotation.RoleType;
import io.gravitee.management.service.ApiService;
import io.gravitee.management.service.ApplicationService;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Set;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
@Role(RoleType.OWNER)
public class ApplicationsApiResource extends AbstractResource {

    @Inject
    private ApplicationService applicationService;

    @Inject
    private ApiService apiService;

    @PathParam("apiName")
    private String apiName;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Set<ApplicationEntity> associatedApplications() {
        // Check that the API exists
        ApiEntity api = apiService.findByName(apiName);

        // Validate user rights : only the owner of the API

        return applicationService.findByApi(api.getName());
    }

}
