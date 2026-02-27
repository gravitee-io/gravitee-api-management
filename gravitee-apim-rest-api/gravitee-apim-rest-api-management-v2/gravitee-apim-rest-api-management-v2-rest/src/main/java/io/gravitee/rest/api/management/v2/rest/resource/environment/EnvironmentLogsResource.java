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
package io.gravitee.rest.api.management.v2.rest.resource.environment;

import io.gravitee.rest.api.management.v2.rest.resource.logs.LogsDefinitionResource;
import io.gravitee.rest.api.management.v2.rest.resource.logs.LogsSearchResource;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;

public class EnvironmentLogsResource {

    @Context
    private ResourceContext resourceContext;

    @Path("/search")
    public LogsSearchResource getLogsSearchResource() {
        return resourceContext.getResource(LogsSearchResource.class);
    }

    @Path("/definition")
    public LogsDefinitionResource getLogsDefinitionResource() {
        return resourceContext.getResource(LogsDefinitionResource.class);
    }
}
