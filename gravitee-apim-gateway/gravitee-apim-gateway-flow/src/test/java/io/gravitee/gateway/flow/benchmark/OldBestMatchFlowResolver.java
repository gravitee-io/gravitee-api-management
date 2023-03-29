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
package io.gravitee.gateway.flow.benchmark;

import io.gravitee.definition.model.flow.Flow;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.flow.BestMatchFlowResolver;
import io.gravitee.gateway.flow.FlowResolver;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Implementation of BestMatchFlowResolver before improvement
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class OldBestMatchFlowResolver extends BestMatchFlowResolver {

    private final Map<String, Pattern> cache = new ConcurrentHashMap<>();

    private static final char OPTIONAL_TRAILING_SEPARATOR = '?';
    private static final String PATH_SEPARATOR = "/";
    private static final String PATH_PARAM_PREFIX = ":";
    private static final String PATH_PARAM_REGEX = "[a-zA-Z0-9\\-._~%!$&'()* +,;=:@/]+";

    private final FlowResolver flowResolver;

    public OldBestMatchFlowResolver(FlowResolver flowResolver) {
        super(flowResolver);
        this.flowResolver = flowResolver;
    }

    @Override
    public List<Flow> resolve(ExecutionContext context) {
        return filter(flowResolver.resolve(context), context);
    }

    private List<Flow> filter(List<Flow> flows, ExecutionContext context) {
        // Do not process empty flows
        if (flows == null || flows.isEmpty()) {
            return null;
        }

        // Compare against the incoming request path
        final String path = context.request().pathInfo();

        int pieces = -1;

        List<Flow> filteredFlows = new ArrayList<>();

        for (Flow flow : flows) {
            Pattern pattern = cache.computeIfAbsent(flow.getPath(), this::transform);
            if (pattern.matcher(path).lookingAt()) {
                int split = flow.getPath().split(PATH_SEPARATOR).length;
                if (split >= pieces) {
                    // If we found more matching, forget the previous one
                    if (split > pieces) {
                        filteredFlows.clear();
                    }

                    pieces = split;
                    filteredFlows.add(flow);
                }
            }
        }

        return filteredFlows;
    }

    private Pattern transform(String path) {
        String[] branches = path.split(PATH_SEPARATOR);
        StringBuilder buffer = new StringBuilder(PATH_SEPARATOR);

        for (final String branch : branches) {
            if (!branch.isEmpty()) {
                if (branch.startsWith(PATH_PARAM_PREFIX)) {
                    buffer.append(PATH_PARAM_REGEX);
                } else {
                    buffer.append(branch);
                }

                buffer.append(PATH_SEPARATOR);
            }
        }

        // Last path separator is not required to match
        buffer.append(OPTIONAL_TRAILING_SEPARATOR);

        return Pattern.compile(buffer.toString());
    }
}
