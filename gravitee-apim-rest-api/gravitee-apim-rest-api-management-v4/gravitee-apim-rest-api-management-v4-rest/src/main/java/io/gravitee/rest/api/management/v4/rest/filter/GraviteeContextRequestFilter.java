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
package io.gravitee.rest.api.management.v4.rest.filter;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import io.gravitee.rest.api.service.v4.ApiService;
import java.io.IOException;
import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Provider
@Priority(10)
public class GraviteeContextRequestFilter implements ContainerRequestFilter {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Inject
    private EnvironmentService environmentService;

    @Inject
    private ApiRepository apiRepository;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        MultivaluedMap<String, String> pathsParams = requestContext.getUriInfo().getPathParameters();
        GraviteeContext.setCurrentOrganization(pathsParams.getFirst("orgId"));
        if (pathsParams.containsKey("envId")) {
            GraviteeContext.setCurrentEnvironment(pathsParams.getFirst("envId"));
        } else if (pathsParams.containsKey("apiId")) {
            String apiId = pathsParams.getFirst("apiId");
            try {
                apiRepository
                    .findById(apiId)
                    .ifPresent(
                        api -> {
                            EnvironmentEntity apiEnv = environmentService.findById(api.getEnvironmentId());
                            if (apiEnv != null) {
                                GraviteeContext.setCurrentEnvironment(apiEnv.getId());
                                GraviteeContext.setCurrentOrganization(apiEnv.getOrganizationId());
                            }
                        }
                    );
            } catch (TechnicalException e) {
                logger.error(String.format("Error while fetching execution context for API {}", apiId), e);
                throw new ApiNotFoundException(apiId);
            }
        }
    }
}
