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
import io.gravitee.management.service.InitializerService;
import io.gravitee.management.standalone.jetty.JettyEmbeddedContainer;
import io.gravitee.node.container.AbstractNode;
import io.gravitee.plugin.alert.AlertEngineService;

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
    public String application() {
        return "gio-apim-management";
    }

    @Override
    public List<Class<? extends LifecycleComponent>> components() {
        final List<Class<? extends LifecycleComponent>> components = super.components();
        components.add(JettyEmbeddedContainer.class);
        components.add(InitializerService.class);
        components.add(AlertEngineService.class);
        return components;
    }
}
