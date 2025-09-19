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
package io.gravitee.gateway.handlers.api.processor.pathparameters;

import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.Plan;
import io.gravitee.definition.model.flow.Flow;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PathParametersExtractor extends AbstractPathParametersExtractor<Api, Flow, Plan> {

    public PathParametersExtractor(Api api) {
        super(api);
    }

    @Override
    protected List<Flow> getApiFlows(Api api) {
        return api.getFlows();
    }

    @Override
    protected List<Plan> getPlans(Api api) {
        return api.getPlans();
    }

    @Override
    protected List<Flow> getPlanFlows(Plan plan) {
        return plan.getFlows();
    }

    @Override
    protected boolean hasPathParam(Flow flow) {
        return PARAM_PATTERN.asPredicate().test(flow.getPath());
    }

    @Override
    protected boolean isEnabled(Flow flow) {
        return flow.isEnabled();
    }

    /**
     * Group pattern by HTTP Method. If flow is configured with an empty list of method, then pattern is assigned to WILDCARD key.
     * @param flows
     * @return
     */
    @Override
    protected Map<PathParameterHttpMethod, Set<PathParameters>> groupPatternsByMethod(final Stream<Flow> flows) {
        final Map<PathParameterHttpMethod, Set<PathParameters>> patternsByMethod = flows
            .flatMap(f -> {
                List<Map.Entry<PathParameterHttpMethod, PathParameters>> flowByMethod;
                if (f.getMethods().isEmpty()) {
                    flowByMethod = List.of(Map.entry(PathParameterHttpMethod.WILDCARD, new PathParameters(f.getPath(), f.getOperator())));
                } else {
                    flowByMethod = f
                        .getMethods()
                        .stream()
                        .map(m -> Map.entry(PathParameterHttpMethod.valueOf(m.name()), new PathParameters(f.getPath(), f.getOperator())))
                        .collect(Collectors.toList());
                }
                return flowByMethod.stream();
            })
            .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.toSet())));

        // Use an empty map for method without path param.
        for (PathParameterHttpMethod method : PathParameterHttpMethod.values()) {
            patternsByMethod.computeIfAbsent(method, param -> Set.of());
        }
        return patternsByMethod;
    }
}
