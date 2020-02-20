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
import io.gravitee.management.service.impl.swagger.transformer.OAITransformer;
import io.gravitee.management.service.swagger.OAIDescriptor;
import io.swagger.v3.oas.models.servers.Server;

import java.util.List;
import java.util.function.Consumer;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EntrypointsOAITransformer implements OAITransformer {

    private final List<ApiEntrypointEntity> entrypoints;

    public EntrypointsOAITransformer(List<ApiEntrypointEntity> entrypoints) {
        this.entrypoints = entrypoints;
    }

    @Override
    public void transform(OAIDescriptor descriptor) {
        if (entrypoints != null && ! entrypoints.isEmpty()) {
            // Remove all existing servers
            descriptor.getSpecification().getServers().clear();

            // Add server according to entrypoints
            entrypoints.forEach(new Consumer<ApiEntrypointEntity>() {
                @Override
                public void accept(ApiEntrypointEntity entrypoint) {
                    Server server = new Server();

                    server.setUrl(entrypoint.getTarget());

                    descriptor.getSpecification().getServers().add(server);
                }
            });
        }
    }
}
