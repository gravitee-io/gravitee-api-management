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
package io.gravitee.rest.api.service.impl.swagger.transformer.page;

import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.service.impl.swagger.SwaggerProperties;
import io.gravitee.rest.api.service.impl.swagger.transformer.SwaggerV2Transformer;
import io.gravitee.rest.api.service.swagger.SwaggerV2Descriptor;
import io.swagger.models.Scheme;

import java.net.URI;
import java.util.Collections;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PageConfigurationSwaggerV2Transformer extends AbstractPageConfigurationSwaggerTransformer<SwaggerV2Descriptor> implements SwaggerV2Transformer {

    public PageConfigurationSwaggerV2Transformer(PageEntity page) {
        super(page);
    }

    @Override
    public void transform(SwaggerV2Descriptor descriptor) {
        String tryItUrl = asString(SwaggerProperties.TRY_IT);
        if (tryItUrl != null && ! tryItUrl.isEmpty()) {
            URI newURI = URI.create(tryItUrl);
            descriptor.getSpecification().setSchemes(Collections.singletonList(Scheme.forValue(newURI.getScheme())));
            descriptor.getSpecification().setHost((newURI.getPort() != -1) ? newURI.getHost() + ':' + newURI.getPort() : newURI.getHost());
            descriptor.getSpecification().setBasePath((newURI.getRawPath().isEmpty()) ? "/" : newURI.getRawPath());
        }
    }
}
