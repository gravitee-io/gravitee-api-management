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
package io.gravitee.rest.api.service.plaftform.plugins;

import io.gravitee.plugin.discovery.ServiceDiscoveryPlugin;
import io.gravitee.rest.api.model.platform.plugin.PlatformPluginEntity;
import io.gravitee.rest.api.service.ServiceDiscoveryService;
import io.gravitee.rest.api.service.impl.AbstractPluginService;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class ServiceDiscoveryServiceImpl
    extends AbstractPluginService<ServiceDiscoveryPlugin, PlatformPluginEntity>
    implements ServiceDiscoveryService {

    @Override
    public Set<PlatformPluginEntity> findAll() {
        return super.list().stream().map(this::convert).collect(Collectors.toSet());
    }

    @Override
    public PlatformPluginEntity findById(String resource) {
        ServiceDiscoveryPlugin sdDefinition = super.get(resource);
        return convert(sdDefinition);
    }
}
