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
package io.gravitee.gateway.debug.policy.impl;

import io.gravitee.gateway.policy.PolicyPluginFactory;
import io.gravitee.gateway.reactive.core.condition.ExpressionLanguageConditionFilter;
import io.gravitee.gateway.reactive.policy.ConditionalPolicy;
import io.gravitee.gateway.reactive.policy.DefaultPolicyFactory;

/**
 * {@code DebugPolicyFactory} extends {@link DefaultPolicyFactory} to provide a customizable point
 * for adding debugging functionality for Debug Console. It inherits the behavior of
 * the default policy factory.
 */
public class DebugPolicyFactory extends DefaultPolicyFactory {

    public DebugPolicyFactory(PolicyPluginFactory policyPluginFactory, ExpressionLanguageConditionFilter<ConditionalPolicy> filter) {
        super(policyPluginFactory, filter);
    }
}
