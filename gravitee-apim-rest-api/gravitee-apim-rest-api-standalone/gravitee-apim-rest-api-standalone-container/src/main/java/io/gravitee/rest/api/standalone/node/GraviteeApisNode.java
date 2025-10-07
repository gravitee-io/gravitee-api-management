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
package io.gravitee.rest.api.standalone.node;

import io.gravitee.apim.infra.plugin.ManagementApiServicesManager;
import io.gravitee.common.component.LifecycleComponent;
import io.gravitee.node.api.NodeMetadataResolver;
import io.gravitee.node.container.AbstractNode;
import io.gravitee.node.services.initializer.spring.InitializerConfiguration;
import io.gravitee.node.services.upgrader.spring.UpgraderConfiguration;
import io.gravitee.plugin.alert.AlertEventProducerManager;
import io.gravitee.plugin.alert.AlertTriggerProviderManager;
import io.gravitee.rest.api.service.ScheduledCommandService;
import io.gravitee.rest.api.standalone.jetty.JettyEmbeddedContainer;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GraviteeApisNode extends AbstractNode {

    @Autowired
    private NodeMetadataResolver nodeMetadataResolver;

    private Map<String, Object> metadata = null;

    @Override
    public String name() {
        return "Gravitee.io - Rest APIs";
    }

    @Override
    public String application() {
        return "gio-apim-apis";
    }

    @Override
    public Map<String, Object> metadata() {
        if (metadata == null) {
            metadata = nodeMetadataResolver.resolve();
        }

        return metadata;
    }

    @Override
    public List<Class<? extends LifecycleComponent>> components() {
        final List<Class<? extends LifecycleComponent>> components = super.components();
        components.add(JettyEmbeddedContainer.class);
        components.add(AlertTriggerProviderManager.class);
        components.add(AlertEventProducerManager.class);
        components.add(ScheduledCommandService.class);
        components.add(ManagementApiServicesManager.class);

        // Keep it at the end
        components.addAll(UpgraderConfiguration.getComponents());
        components.addAll(InitializerConfiguration.getComponents());
        return components;
    }
}
