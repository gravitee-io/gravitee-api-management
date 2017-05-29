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
package io.gravitee.management.standalone.node;

import io.gravitee.common.component.LifecycleComponent;
import io.gravitee.common.node.AbstractNode;
import io.gravitee.management.service.InitializerService;
import io.gravitee.management.services.ServiceManager;
import io.gravitee.management.standalone.jetty.JettyEmbeddedContainer;
import io.gravitee.plugin.core.api.PluginRegistry;
import io.gravitee.plugin.core.internal.PluginEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ManagementNode extends AbstractNode {

    @Override
    public String name() {
        return "Gravitee.io - Management API";
    }

    @Override
    protected List<Class<? extends LifecycleComponent>> getLifecycleComponents() {
        List<Class<? extends LifecycleComponent>> components = new ArrayList<>();

        components.add(PluginEventListener.class);
        components.add(PluginRegistry.class);
        components.add(ServiceManager.class);
        components.add(JettyEmbeddedContainer.class);
        components.add(InitializerService.class);

        return components;
    }
}
