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
import io.gravitee.gateway.policy.AbstractPolicyResolver;
import io.gravitee.gateway.policy.Policy;
import io.gravitee.gateway.policy.StreamType;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class RuleBasedPolicyResolver extends AbstractPolicyResolver {

    protected List<Policy> resolve(StreamType streamType, ExecutionContext context, List<Rule> rules) {
        if (rules != null && ! rules.isEmpty()) {
            return rules.stream()
                    .filter(rule -> rule.isEnabled() && rule.getMethods().contains(context.request().method()))
                    .map(rule -> create(streamType, rule.getPolicy().getName(), rule.getPolicy().getConfiguration()))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}
