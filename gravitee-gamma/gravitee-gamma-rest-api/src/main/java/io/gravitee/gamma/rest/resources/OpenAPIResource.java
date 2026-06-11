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
package io.gravitee.gamma.rest.resources;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.io.InputStream;
import java.net.URI;
import java.util.regex.Pattern;

@Path("/docs")
public class OpenAPIResource {

    private static final Pattern SAFE_SEGMENT = Pattern.compile("[a-z][a-z0-9-]*");

    @GET
    @Produces("text/html")
    public Response getGlobalIndex() {
        return serveClasspath("openapi/index.html", "text/html");
    }

    @GET
    @Path("/{domain}")
    public Response redirectToDomain(@PathParam("domain") String domain, @Context UriInfo uriInfo) {
        validateSegment(domain);
        URI redirectUri = uriInfo.getAbsolutePathBuilder().path("/").build();
        return Response.status(Response.Status.MOVED_PERMANENTLY).location(redirectUri).build();
    }

    @GET
    @Path("/{domain}/")
    @Produces("text/html")
    public Response getDomainIndex(@PathParam("domain") String domain) {
        validateSegment(domain);
        return serveClasspath("openapi/%s/index.html".formatted(domain), "text/html");
    }

    @GET
    @Path("/{domain}/{file}.yaml")
    @Produces("application/yaml")
    public Response getYaml(@PathParam("domain") String domain, @PathParam("file") String file) {
        validateSegment(domain);
        validateSegment(file);
        return serveClasspath("openapi/%s/openapi-%s.yaml".formatted(domain, file), "application/yaml");
    }

    private Response serveClasspath(String path, String mediaType) {
        InputStream stream = getClass().getClassLoader().getResourceAsStream(path);
        if (stream == null) {
            throw new NotFoundException();
        }
        return Response.ok(stream).type(mediaType).build();
    }

    private void validateSegment(String segment) {
        if (segment == null || !SAFE_SEGMENT.matcher(segment).matches()) {
            throw new BadRequestException("Invalid path segment: " + segment);
        }
    }
}
