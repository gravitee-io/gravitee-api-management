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
package io.gravitee.apim.rest.api.automation.resource;

import io.gravitee.apim.plugin.gamma.api.automation.GammaAutomationPort;
import io.gravitee.apim.rest.api.automation.openapi.OpenApiFragmentMerger;
import io.gravitee.apim.rest.api.automation.spring.GammaAutomationPorts;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Path("/")
public class OpenAPIResource {

    private static final String OPEN_API_DOCUMENT = "open-api.yaml";

    @Inject
    private GammaAutomationPorts gammaAutomationPorts;

    @GET
    @Path("/" + OPEN_API_DOCUMENT)
    @Produces("application/yaml")
    public Response getOpenApi() {
        Map<String, String> fragments = new LinkedHashMap<>();
        for (GammaAutomationPort port : gammaAutomationPorts.all()) {
            port.openApiFragment().ifPresent(fragment -> fragments.put(port.module(), fragment));
        }
        if (fragments.isEmpty()) {
            return Response.ok(document()).build();
        }
        try (var base = document()) {
            return Response.ok(OpenApiFragmentMerger.merge(base, fragments)).build();
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot merge Gamma module fragments into the automation OpenAPI document", e);
        }
    }

    private InputStream document() {
        InputStream document = this.getClass().getClassLoader().getResourceAsStream(OPEN_API_DOCUMENT);
        if (document == null) {
            throw new IllegalStateException("[" + OPEN_API_DOCUMENT + "] is missing from the automation-rest jar");
        }
        return document;
    }
}
