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
package io.gravitee.rest.api.service.v4.impl;

import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.rest.api.model.platform.plugin.PlatformPluginEntity;
import io.gravitee.rest.api.service.JsonSchemaService;
import io.gravitee.rest.api.service.impl.AbstractPluginService;
import io.gravitee.rest.api.service.v4.EndpointService;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * @author GraviteeSource Team
 */
@Component("EndpointPluginServiceImplV4")
public class EndpointPluginServiceImpl
    extends AbstractPluginService<EndpointConnectorPlugin<?>, PlatformPluginEntity>
    implements EndpointService {

    public EndpointPluginServiceImpl(
        JsonSchemaService jsonSchemaService,
        ConfigurablePluginManager<EndpointConnectorPlugin<?>> pluginManager
    ) {
        super(jsonSchemaService, pluginManager);
    }

    @Override
    public Set<PlatformPluginEntity> findAll() {
        return super.list().stream().map(this::convert).collect(Collectors.toSet());
    }

    @Override
    public PlatformPluginEntity findById(String endpointPluginId) {
        return convert(super.get(endpointPluginId));
    }

    @Override
    public String validateEndpointConfiguration(final String endpointPluginId, final String configuration) {
        PlatformPluginEntity endpointPluginEntity = this.findById(endpointPluginId);
        return validateConfiguration(endpointPluginEntity, configuration);
    }
}
