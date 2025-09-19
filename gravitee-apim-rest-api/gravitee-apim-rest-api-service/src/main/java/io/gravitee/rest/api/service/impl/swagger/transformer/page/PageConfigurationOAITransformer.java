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
package io.gravitee.rest.api.service.impl.swagger.transformer.page;

import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.service.impl.swagger.SwaggerProperties;
import io.gravitee.rest.api.service.impl.swagger.transformer.OAITransformer;
import io.gravitee.rest.api.service.swagger.OAIDescriptor;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class PageConfigurationOAITransformer extends AbstractPageConfigurationSwaggerTransformer<OAIDescriptor> implements OAITransformer {

    public PageConfigurationOAITransformer(PageEntity page) {
        super(page);
    }

    @Override
    public void transform(OAIDescriptor descriptor) {
        String tryItUrl = asString(SwaggerProperties.TRY_IT);
        if (tryItUrl != null && !tryItUrl.isEmpty()) {
            URI newURI = URI.create(tryItUrl);
            descriptor
                .getSpecification()
                .getServers()
                .forEach(server -> {
                    try {
                        server.setUrl(
                            new URI(
                                newURI.getScheme(),
                                newURI.getUserInfo(),
                                newURI.getHost(),
                                newURI.getPort(),
                                newURI.getPath(),
                                newURI.getQuery(),
                                newURI.getFragment()
                            ).toString()
                        );
                    } catch (URISyntaxException e) {
                        log.error(e.getMessage(), e);
                    }
                });

            // Remove possible server duplicates.
            descriptor
                .getSpecification()
                .servers(descriptor.getSpecification().getServers().stream().distinct().collect(Collectors.toList()));
        }
    }
}
