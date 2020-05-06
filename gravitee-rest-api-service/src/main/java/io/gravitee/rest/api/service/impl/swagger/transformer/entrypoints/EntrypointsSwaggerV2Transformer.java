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
package io.gravitee.rest.api.service.impl.swagger.transformer.entrypoints;

import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.api.ApiEntrypointEntity;
import io.gravitee.rest.api.service.impl.swagger.SwaggerProperties;
import io.gravitee.rest.api.service.impl.swagger.transformer.SwaggerV2Transformer;
import io.gravitee.rest.api.service.impl.swagger.transformer.page.AbstractPageConfigurationSwaggerTransformer;
import io.gravitee.rest.api.service.swagger.SwaggerDescriptor;
import io.gravitee.rest.api.service.swagger.SwaggerV2Descriptor;
import io.swagger.models.Scheme;
import io.swagger.models.Swagger;

import java.net.URI;
import java.util.Collections;
import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EntrypointsSwaggerV2Transformer extends AbstractPageConfigurationSwaggerTransformer<SwaggerV2Descriptor> {

    private final List<ApiEntrypointEntity> entrypoints;

    public EntrypointsSwaggerV2Transformer(final PageEntity page, final List<ApiEntrypointEntity> entrypoints) {
        super(page);
        this.entrypoints = entrypoints;
    }

    @Override
    public void transform(SwaggerV2Descriptor descriptor) {
        if (asBoolean(SwaggerProperties.ENTRYPOINTS_AS_SERVERS) && entrypoints != null && ! entrypoints.isEmpty()) {
            Swagger swagger = descriptor.getSpecification();

            // Swagger vs2 supports only a single server
            ApiEntrypointEntity first = entrypoints.iterator().next();
            URI target = URI.create(first.getTarget());
            swagger.setSchemes(Collections.singletonList(Scheme.forValue(target.getScheme())));
            swagger.setHost(target.getHost());

            if (getProperty(SwaggerProperties.ENTRYPOINT_AS_BASEPATH) == null
                    || getProperty(SwaggerProperties.ENTRYPOINT_AS_BASEPATH).isEmpty()
                    || asBoolean(SwaggerProperties.ENTRYPOINT_AS_BASEPATH)) {
                swagger.setBasePath(target.getPath());
            }
        }
    }
}
