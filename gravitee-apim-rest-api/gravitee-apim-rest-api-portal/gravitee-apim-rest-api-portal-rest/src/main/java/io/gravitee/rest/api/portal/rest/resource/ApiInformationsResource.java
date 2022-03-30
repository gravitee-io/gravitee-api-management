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
package io.gravitee.rest.api.portal.rest.resource;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.api.ApiQuery;
import io.gravitee.rest.api.model.api.header.ApiHeaderEntity;
import io.gravitee.rest.api.portal.rest.model.ApiInformation;
import io.gravitee.rest.api.portal.rest.security.RequirePortalAuth;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiInformationsResource extends AbstractResource {

    @Inject
    private ApiService apiService;

    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @RequirePortalAuth
    public Response getApiInformations(@Context Request request, @PathParam("apiId") String apiId) {
        final ApiQuery apiQuery = new ApiQuery();
        apiQuery.setIds(Collections.singletonList(apiId));
        Collection<ApiEntity> userApis = apiService.findPublishedByUser(
            GraviteeContext.getExecutionContext(),
            getAuthenticatedUserOrNull(),
            apiQuery
        );
        if (userApis.stream().anyMatch(a -> a.getId().equals(apiId))) {
            List<ApiHeaderEntity> all = apiService.getPortalHeaders(GraviteeContext.getExecutionContext(), apiId);
            List<ApiInformation> information = all
                .stream()
                .map(
                    apiHeaderEntity -> {
                        ApiInformation ai = new ApiInformation();
                        ai.setName(apiHeaderEntity.getName());
                        ai.setValue(apiHeaderEntity.getValue());
                        return ai;
                    }
                )
                .collect(Collectors.toList());

            return Response.ok(information).build();
        }
        throw new ApiNotFoundException(apiId);
    }
}
