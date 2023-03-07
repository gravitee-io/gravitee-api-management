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
package io.gravitee.gateway.reactor.handler.context.provider;

import io.gravitee.common.util.Version;
import io.gravitee.el.TemplateContext;
import io.gravitee.el.TemplateVariableProvider;
import io.gravitee.el.TemplateVariableScope;
import io.gravitee.el.annotations.TemplateVariable;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.node.api.Node;
import java.util.Collections;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@TemplateVariable(scopes = { TemplateVariableScope.API })
public class NodeTemplateVariableProvider implements TemplateVariableProvider {

    private static final String TEMPLATE_VARIABLE_KEY = "node";

    private final NodeProperties nodeProperties;

    public NodeTemplateVariableProvider(Node node, GatewayConfiguration gatewayConfiguration) {
        nodeProperties = new NodeProperties();
        nodeProperties.setId(node.id());
        nodeProperties.setVersion(Version.RUNTIME_VERSION.MAJOR_VERSION);
        nodeProperties.setTenant(gatewayConfiguration.tenant().orElse(null));
        nodeProperties.setShardingTags(gatewayConfiguration.shardingTags().orElse(Collections.emptyList()));
        nodeProperties.setZone(gatewayConfiguration.zone().orElse(null));
    }

    @Override
    public void provide(TemplateContext templateContext) {
        templateContext.setVariable(TEMPLATE_VARIABLE_KEY, nodeProperties);
    }
}
