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

import io.swagger.annotations.SwaggerDefinition;
import io.swagger.jaxrs.Reader;
import io.swagger.jaxrs.config.ReaderListener;
import io.swagger.models.Swagger;
import io.swagger.models.Tag;
import io.swagger.models.auth.BasicAuthDefinition;
import java.util.Comparator;
import java.util.TreeMap;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@SwaggerDefinition
public class GraviteeApiDefinition implements ReaderListener {

    public static final String TOKEN_AUTH_SCHEME = "gravitee-auth";

    @Override
    public void beforeScan(Reader reader, Swagger swagger) {}

    @Override
    public void afterScan(Reader reader, Swagger swagger) {
        // sort tags for better comparisons
        swagger.getTags().sort(Comparator.comparing(Tag::getName));
        // sort paths for better comparisons
        swagger.setPaths(new TreeMap<>(swagger.getPaths()));
        // sort definitions for better comparisons
        swagger.setDefinitions(new TreeMap<>(swagger.getDefinitions()));
        swagger.addSecurityDefinition(TOKEN_AUTH_SCHEME, new BasicAuthDefinition());

        swagger
            .getPaths()
            .values()
            .forEach(
                path -> path.getOperations().forEach(operation -> operation.addSecurity(GraviteeApiDefinition.TOKEN_AUTH_SCHEME, null))
            );
    }
}
