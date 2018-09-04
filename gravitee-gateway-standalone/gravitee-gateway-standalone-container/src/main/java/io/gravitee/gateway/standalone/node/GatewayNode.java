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
package io.gravitee.gateway.standalone.node;

import io.gravitee.common.component.LifecycleComponent;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.reactor.Reactor;
import io.gravitee.gateway.standalone.vertx.VertxEmbeddedContainer;
import io.gravitee.node.container.AbstractNode;
import io.gravitee.plugin.alert.AlertEngineService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GatewayNode extends AbstractNode {

    @Autowired
    private GatewayConfiguration configuration;

    @Override
    public String name() {
        return "Gravitee.io - API Gateway";
    }

    @Override
    public String application() {
        return "gio-apim-gateway";
    }

    @Override
    public Map<String, Object> metadata() {
        Map<String, Object> metadata = new HashMap<>();

        if (configuration.tenant().isPresent()) {
            metadata.put("tenant", configuration.tenant().get());
        }
        if (configuration.shardingTags().isPresent()) {
            metadata.put("tags", configuration.shardingTags().get());
        }

        return metadata;
    }

    @Override
    public List<Class<? extends LifecycleComponent>> components() {
        List<Class<? extends LifecycleComponent>> components = super.components();

        components.add(Reactor.class);
        components.add(VertxEmbeddedContainer.class);
        components.add(AlertEngineService.class);

        return components;
    }
}
