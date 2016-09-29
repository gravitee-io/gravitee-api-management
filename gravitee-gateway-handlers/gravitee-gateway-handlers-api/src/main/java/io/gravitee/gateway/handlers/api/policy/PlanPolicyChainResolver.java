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

import io.gravitee.definition.model.Path;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.policy.Policy;
import io.gravitee.gateway.policy.StreamType;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A policy resolver based on the plan subscribed by the consumer identity.
 * This identity is, for the moment, based on the api-key discovered from HTTP request header
 * or HTTP request query parameter.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PlanPolicyChainResolver extends AbstractPolicyChainResolver {

    @Override
    protected List<Policy> calculate(StreamType streamType, Request request, Response response, ExecutionContext executionContext) {
        if (streamType == StreamType.ON_REQUEST) {
            // The plan identifier has been loaded while validating API Key
            String planId = (String) executionContext.getAttribute(ExecutionContext.ATTR_PLAN);
            if (planId != null) {
                Map<String, Path> paths = api.getPlan(planId).getPaths();

                if (paths != null && ! paths.isEmpty()) {
                    // For 1.0.0, there is only a single root path defined
                    Path rootPath = paths.values().iterator().next();
                    return rootPath.getRules().stream()
                            .filter(rule -> rule.isEnabled() && rule.getMethods().contains(request.method()))
                            .map(rule -> create(rule.getPolicy().getName(), rule.getPolicy().getConfiguration()))
                            .collect(Collectors.toList());
                }
            }
        }

        return Collections.emptyList();
    }
}
