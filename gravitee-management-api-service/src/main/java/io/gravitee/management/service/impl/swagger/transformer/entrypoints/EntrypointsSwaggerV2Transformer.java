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
package io.gravitee.management.service.impl.swagger.transformer.entrypoints;

import io.gravitee.management.model.api.ApiEntrypointEntity;
import io.gravitee.management.service.impl.swagger.transformer.SwaggerV2Transformer;
import io.gravitee.management.service.swagger.SwaggerV2Descriptor;
import io.swagger.models.Scheme;
import io.swagger.models.Swagger;

import java.net.URI;
import java.util.Collections;
import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EntrypointsSwaggerV2Transformer implements SwaggerV2Transformer {

    private final List<ApiEntrypointEntity> entrypoints;

    public EntrypointsSwaggerV2Transformer(List<ApiEntrypointEntity> entrypoints) {
        this.entrypoints = entrypoints;
    }

    @Override
    public void transform(SwaggerV2Descriptor descriptor) {
        if (entrypoints != null && ! entrypoints.isEmpty()) {
            Swagger swagger = descriptor.getSpecification();
            if (swagger.getSchemes() != null) {
                swagger.getSchemes().clear();
            }

            // Swagger v2 supports only a single server
            ApiEntrypointEntity first = entrypoints.iterator().next();
            URI target = URI.create(first.getTarget());
            swagger.setSchemes(Collections.singletonList(Scheme.forValue(target.getScheme())));
            swagger.setHost(target.getHost());
            swagger.setBasePath(target.getPath());
        }
    }
}
