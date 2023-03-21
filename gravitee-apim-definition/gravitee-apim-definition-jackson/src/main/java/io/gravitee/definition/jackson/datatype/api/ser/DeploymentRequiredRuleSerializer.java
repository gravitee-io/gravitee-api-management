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
package io.gravitee.definition.jackson.datatype.api.ser;

import io.gravitee.definition.model.Rule;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DeploymentRequiredRuleSerializer extends RuleSerializer {

    public DeploymentRequiredRuleSerializer(Class<Rule> t) {
        super(t);
    }

    @Override
    protected boolean hasDescription(Rule rule) {
        // Useful to ignore description when serialize rule to check if need to deploy
        // The deploymentRequiredFilter not work due to HashMap extends in Rule.class
        return false;
    }
}
