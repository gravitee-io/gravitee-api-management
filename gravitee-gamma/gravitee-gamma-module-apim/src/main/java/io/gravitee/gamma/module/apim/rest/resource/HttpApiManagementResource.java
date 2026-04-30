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
package io.gravitee.gamma.module.apim.rest.resource;

import io.gravitee.gamma.module.apim.rest.resource.tracing.TracingResource;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;

/**
 * Root resource for the APIM gamma module. Mounted by the gamma plugin loader at
 * {@code /organizations/{orgId}/modules/{pluginId}}. Sub-resources hang off this class.
 */
public class HttpApiManagementResource {

    @Context
    private ResourceContext resourceContext;

    @Path("tracing")
    public TracingResource getTracingResource() {
        return resourceContext.getResource(TracingResource.class);
    }
}
