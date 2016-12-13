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
package io.gravitee.gateway.handlers.api.policy.api;

import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.handlers.api.path.Path;
import io.gravitee.gateway.handlers.api.path.PathResolver;
import io.gravitee.gateway.policy.AbstractPolicyChainResolver;
import io.gravitee.gateway.policy.Policy;
import io.gravitee.gateway.policy.StreamType;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A policy chain resolver based on the policy configuration from the API.
 * This policy configuration is done by path / method.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiPolicyChainResolver extends AbstractPolicyChainResolver {

    @Autowired
    private PathResolver pathResolver;

    @Override
    protected List<Policy> calculate(StreamType streamType, Request request, Response response, ExecutionContext executionContext) {
        // Resolve the "configured" path according to the inbound request
        Path path = pathResolver.resolve(request.path());
        executionContext.setAttribute(ExecutionContext.ATTR_RESOLVED_PATH, path.getResolvedPath());

        return path.getRules()
                .stream()
                .filter(rule -> rule.isEnabled() && rule.getMethods().contains(request.method()))
                .map(rule -> create(streamType, rule.getPolicy().getName(), rule.getPolicy().getConfiguration()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
