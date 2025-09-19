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

import io.gravitee.definition.model.flow.Flow;
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
import java.util.stream.Stream;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractPathParametersExtractor<A, F, P> {

    protected static final Pattern PARAM_PATTERN = Pattern.compile(":\\w*");
    protected final Map<PathParameterHttpMethod, Set<PathParameters>> patternsByHttpMethod;
    protected final A api;

    protected AbstractPathParametersExtractor(A api) {
        Objects.requireNonNull(api, "Api is mandatory");
        this.api = api;
        patternsByHttpMethod = compilePatternsByHttpMethod();
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
            .anyMatch(parameters -> !parameters.getName().isEmpty());
    }

    /**
     * Group flow path containing path parameters by Http Method.
     * If a flow is defined for all methods (empty set), then it will be assigned to WILDCARD key.
     *
     * @return a map of {@link PathParameters}, containing patterns and parameters name grouped by {@link PathParameterHttpMethod}
     */
    private Map<PathParameterHttpMethod, Set<PathParameters>> compilePatternsByHttpMethod() {
        final Stream<F> flowsWithParam = filterFlowsWithPathParam();
        // group pattern by HTTP Method <> List<Pattern>
        return groupPatternsByMethod(flowsWithParam);
    }

    /**
     * Filter flows that contains a path parameter (for example ':productId')
     * @return a stream of flows containing a path parameter
     */
    private Stream<F> filterFlowsWithPathParam() {
        Stream<F> flowsWithParam;
        flowsWithParam = Stream.empty();

        if (getApiFlows(api) != null) {
            flowsWithParam = Stream.concat(flowsWithParam, getApiFlows(api).stream().filter(this::isEnabled).filter(this::hasPathParam));
        }
        if (getPlans(api) != null) {
            flowsWithParam = Stream.concat(
                flowsWithParam,
                getPlans(api)
                    .stream()
                    .flatMap(plan -> getPlanFlows(plan) == null ? Stream.empty() : getPlanFlows(plan).stream())
                    .filter(this::isEnabled)
                    .filter(this::hasPathParam)
            );
        }
        return flowsWithParam;
    }

    protected abstract List<F> getApiFlows(A api);

    protected abstract List<F> getPlanFlows(P plan);

    protected abstract List<P> getPlans(A api);

    /**
     * Checks if a flow contains a path parameter
     * @param flow
     * @return true if path parameter is contained
     */
    protected abstract boolean hasPathParam(F flow);

    protected abstract boolean isEnabled(F flow);

    /**
     * Group pattern by HTTP Method. If flow is configured with an empty list of method, then pattern is assigned to WILDCARD key.
     * @param flows
     * @return
     */
    protected abstract Map<PathParameterHttpMethod, Set<PathParameters>> groupPatternsByMethod(final Stream<F> flows);

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
                    pattern.getParameters().forEach(p -> pathParameters.put(p.getName(), matcher.group(p.getId())));
                }
            });
    }
}
