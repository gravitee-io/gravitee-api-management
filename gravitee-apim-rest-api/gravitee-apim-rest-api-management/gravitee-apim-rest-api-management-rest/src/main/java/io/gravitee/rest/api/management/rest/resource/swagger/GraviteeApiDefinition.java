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
package io.gravitee.rest.api.management.rest.resource.swagger;

import io.gravitee.common.util.Version;
import io.swagger.v3.jaxrs2.ReaderListener;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.integration.api.OpenApiReader;
import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.*;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@OpenAPIDefinition
public class GraviteeApiDefinition implements ReaderListener {

    public static final String TOKEN_AUTH_SCHEME = "gravitee-auth";

    @Override
    public void beforeScan(OpenApiReader reader, OpenAPI openAPI) {
        openAPI.info(new Info().version(Version.RUNTIME_VERSION.MAJOR_VERSION).title("Gravitee.io - Management API"));
    }

    @Override
    public void afterScan(OpenApiReader reader, OpenAPI openAPI) {
        Map<String, Tag> tags = new TreeMap<>();
        if (openAPI.getTags() != null) {
            openAPI.getTags().forEach(tag -> tags.put(tag.getName(), tag));
        }
        openAPI
            .getPaths()
            .values()
            .stream()
            .map(PathItem::readOperations)
            .flatMap(Collection::stream)
            .map(Operation::getTags)
            .filter(Objects::nonNull)
            .flatMap(Collection::stream)
            .forEach(tag -> tags.computeIfAbsent(tag, s -> new Tag().name(s)));
        // sort tags for better comparisons
        openAPI.tags(new ArrayList<>(tags.values()));
        // sort paths for better comparisons
        Paths paths = new Paths();
        paths.putAll(new TreeMap<>(openAPI.getPaths()));
        openAPI.setPaths(paths);
        // sort definitions for better comparisons
        Components components = new Components();
        components.schemas(new TreeMap<>(openAPI.getComponents().getSchemas()));
        components.addSecuritySchemes(TOKEN_AUTH_SCHEME, new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("basic"));
        openAPI.components(components);
        openAPI.addSecurityItem(new SecurityRequirement().addList(TOKEN_AUTH_SCHEME));
    }
}
