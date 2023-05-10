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
package io.gravitee.gateway.handlers.api.processor.pathparameters;

import io.gravitee.definition.model.flow.Flow;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.netty.handler.codec.http.QueryStringDecoder;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PathParametersExtractor {

    private static final Pattern PARAM_PATTERN = Pattern.compile(":\\w*");
    private final Map<PathParameterHttpMethod, Set<PathParameter>> patternsByHttpMethod;

    public PathParametersExtractor(Api api) {
        Objects.requireNonNull(api, "Api is mandatory");
        patternsByHttpMethod = compilePatternsByHttpMethod(api);
    }

    /**
     * Check if path parameters can be extracted for the api.
     * @return true if at least one flow is configured with a path parameter.
     */
    public boolean canExtractPathParams() {
        return patternsByHttpMethod
            .values()
            .stream()
            .flatMap(Collection::stream)
            .flatMap(p -> p.getParameters().stream())
            .anyMatch(parameters -> parameters.length() > 0);
    }

    /**
     * Group flow path containing path parameters by Http Method.
     * If a flow is defined for all methods (empty set), then it will be assigned to WILDCARD key.
     *
     * @param api
     * @return a map of {@link PathParameter}, containing patterns and parameters name grouped by {@link PathParameterHttpMethod}
     */
    private static Map<PathParameterHttpMethod, Set<PathParameter>> compilePatternsByHttpMethod(final Api api) {
        final Stream<Flow> flowsWithParam = filterFlowsWithPathParam(api);
        // group pattern by HTTP Method <> List<Pattern>
        return groupPatternsByMethod(flowsWithParam);
    }

    /**
     * Filter flows that contains a path parameter (for example ':productId')
     * @param api
     * @return a stream of flows containing a path parameter
     */
    private static Stream<Flow> filterFlowsWithPathParam(final Api api) {
        Stream<Flow> flowsWithParam;
        flowsWithParam = Stream.empty();

        if (api.getDefinition().getFlows() != null) {
            flowsWithParam =
                Stream.concat(
                    flowsWithParam,
                    api.getDefinition().getFlows().stream().filter(flow -> PARAM_PATTERN.asPredicate().test(flow.getPath()))
                );
        }
        if (api.getDefinition().getPlans() != null) {
            flowsWithParam =
                Stream.concat(
                    flowsWithParam,
                    api
                        .getDefinition()
                        .getPlans()
                        .stream()
                        .flatMap(plan -> plan.getFlows() == null ? Stream.empty() : plan.getFlows().stream())
                        .filter(flow -> PARAM_PATTERN.asPredicate().test(flow.getPath()))
                );
        }
        return flowsWithParam;
    }

    /**
     * Group pattern by HTTP Method. If flow is configured with an empty list of method, then pattern is assigned to WILDCARD key.
     * @param flows
     * @return
     */
    private static Map<PathParameterHttpMethod, Set<PathParameter>> groupPatternsByMethod(final Stream<Flow> flows) {
        final Map<PathParameterHttpMethod, Set<PathParameter>> patternsByMethod = flows
            .flatMap(f -> {
                List<Map.Entry<PathParameterHttpMethod, PathParameter>> flowByMethod;
                if (f.getMethods().isEmpty()) {
                    flowByMethod = List.of(Map.entry(PathParameterHttpMethod.WILDCARD, new PathParameter(f.getPath(), f.getOperator())));
                } else {
                    flowByMethod =
                        f
                            .getMethods()
                            .stream()
                            .map(m -> Map.entry(PathParameterHttpMethod.valueOf(m.name()), new PathParameter(f.getPath(), f.getOperator())))
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

    /**
     * Extracts path parameters value regarding current request method and pathInfo.
     * @param requestMethod is the HTTP Method for the current request
     * @param requestPathInfo is the pathInfo for the current request
     * @return a map of path parameters value by path parameter name
     */
    public Map<String, String> extract(final String requestMethod, final String requestPathInfo) {
        Map<String, String> pathParameters = new HashMap<>();
        computePathParam(PathParameterHttpMethod.WILDCARD, requestPathInfo, pathParameters);
        computePathParam(requestMethod, requestPathInfo, pathParameters);
        return pathParameters;
    }

    private void computePathParam(final String requestMethod, final String requestPathInfo, Map<String, String> pathParameters) {
        computePathParam(PathParameterHttpMethod.valueOf(requestMethod), requestPathInfo, pathParameters);
    }

    private void computePathParam(final PathParameterHttpMethod method, final String requestPathInfo, Map<String, String> pathParameters) {
        patternsByHttpMethod
            .get(method)
            .forEach(pattern -> {
                String path = requestPathInfo;
                try {
                    path = QueryStringDecoder.decodeComponent(path, Charset.defaultCharset());
                } catch (IllegalArgumentException ignored) {
                    // Keep path as it is in case of exception
                }

                final Matcher matcher = pattern.getPathPattern().matcher(path);
                if (matcher.find()) {
                    pattern.getParameters().forEach(p -> pathParameters.put(p, matcher.group(p)));
                }
            });
    }
}
