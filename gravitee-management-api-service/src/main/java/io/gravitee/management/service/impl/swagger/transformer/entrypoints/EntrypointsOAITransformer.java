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

import io.gravitee.management.model.PageEntity;
import io.gravitee.management.model.api.ApiEntrypointEntity;
import io.gravitee.management.service.impl.swagger.SwaggerProperties;
import io.gravitee.management.service.impl.swagger.transformer.page.AbstractPageConfigurationSwaggerTransformer;
import io.gravitee.management.service.swagger.OAIDescriptor;
import io.swagger.v3.oas.models.servers.Server;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EntrypointsOAITransformer extends AbstractPageConfigurationSwaggerTransformer<OAIDescriptor> {

    private final List<ApiEntrypointEntity> entrypoints;

    public EntrypointsOAITransformer(final PageEntity page, final List<ApiEntrypointEntity> entrypoints) {
        super(page);
        this.entrypoints = entrypoints;
    }

    @Override
    public void transform(OAIDescriptor descriptor) {
        if (asBoolean(SwaggerProperties.ENTRYPOINTS_AS_SERVERS) && entrypoints != null && ! entrypoints.isEmpty()) {
            List<Server> servers = new ArrayList<>();

            // Add server according to entrypoints
            entrypoints.forEach(entrypoint -> {
                Server server = new Server();

                if (getProperty(SwaggerProperties.ENTRYPOINT_AS_BASEPATH) == null
                        || getProperty(SwaggerProperties.ENTRYPOINT_AS_BASEPATH).isEmpty()
                        || asBoolean(SwaggerProperties.ENTRYPOINT_AS_BASEPATH)) {
                    server.setUrl(entrypoint.getTarget());
                } else {
                    URI target = URI.create(entrypoint.getTarget());
                    server.setUrl(entrypoint.getTarget().substring(0, entrypoint.getTarget().indexOf(target.getRawPath())));
                }

                servers.add(server);

            });

            descriptor.getSpecification().setServers(servers);
        }
    }
}
