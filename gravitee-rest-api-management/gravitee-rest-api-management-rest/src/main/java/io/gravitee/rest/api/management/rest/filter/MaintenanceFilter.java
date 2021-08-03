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
package io.gravitee.rest.api.management.rest.filter;

import static io.gravitee.rest.api.model.parameters.Key.MAINTENANCE_MODE_ENABLED;

import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.exceptions.MaintenanceModeException;
import java.io.IOException;
import java.util.regex.Pattern;
import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Provider
@PreMatching
@Priority(90)
public class MaintenanceFilter implements ContainerRequestFilter {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Inject
    ParameterService parameterService;

    private Pattern organizationSettings = Pattern.compile("organizations/[^/]+/settings");

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        final boolean maintenanceModeEnabled = parameterService.findAsBoolean(
            MAINTENANCE_MODE_ENABLED,
            ParameterReferenceType.ORGANIZATION
        );
        if (
            maintenanceModeEnabled &&
            organizationSettings.matcher(requestContext.getUriInfo().getPath()).matches() &&
            "POST".equals(requestContext.getRequest().getMethod())
        ) {
            return;
        }

        if (
            maintenanceModeEnabled &&
            !requestContext.getUriInfo().getPath().equals("portal") &&
            !requestContext.getUriInfo().getPath().equals("portal/")
        ) {
            throw new MaintenanceModeException();
        }
    }
}
