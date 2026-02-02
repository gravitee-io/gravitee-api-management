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
package io.gravitee.rest.api.management.v2.rest.resource.logs;

import io.gravitee.apim.core.logs_engine.use_case.SearchEnvironmentLogsUseCase;
import io.gravitee.rest.api.management.v2.rest.mapper.LogsEngineMapper;
import io.gravitee.rest.api.management.v2.rest.model.logs.engine.SearchLogsRequest;
import io.gravitee.rest.api.management.v2.rest.model.logs.engine.SearchLogsResponse;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * @author GraviteeSource Team
 */
public class LogsSearchResource extends AbstractResource {

    @Inject
    SearchEnvironmentLogsUseCase searchEnvironmentLogsUseCase;

    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public SearchLogsResponse searchLogs(@BeanParam @Valid PaginationParam paginationParam, @Valid SearchLogsRequest request) {
        var input = new SearchEnvironmentLogsUseCase.Input(
            getAuditInfo(),
            LogsEngineMapper.INSTANCE.fromRequestEntity(request, paginationParam.getPage(), paginationParam.getPerPage())
        );
        var output = searchEnvironmentLogsUseCase.execute(input);
        return LogsEngineMapper.INSTANCE.fromResponseModel(output.response());
    }
}
