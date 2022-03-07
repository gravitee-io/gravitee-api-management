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
package io.gravitee.gateway.handlers.api.policy;

import io.gravitee.definition.model.Rule;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.flow.policy.PolicyMetadata;
import io.gravitee.gateway.flow.policy.PolicyResolver;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A {@link PolicyResolver} based on the rules from API v1 definitions.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class RuleBasedPolicyResolver implements PolicyResolver {

    protected List<PolicyMetadata> resolve(ExecutionContext context, List<Rule> rules) {
        if (rules != null && !rules.isEmpty()) {
            return rules
                .stream()
                .filter(rule -> rule.isEnabled() && rule.getMethods().contains(context.request().method()))
                .map(rule -> new PolicyMetadata(rule.getPolicy().getName(), rule.getPolicy().getConfiguration()))
                .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}
