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
package io.gravitee.rest.api.service.v4.impl.validation;

import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.selector.ChannelSelector;
import io.gravitee.definition.model.v4.flow.selector.HttpSelector;
import io.gravitee.definition.model.v4.flow.selector.SelectorType;
import io.gravitee.rest.api.service.v4.exception.PathParameterOverlapValidationException;
import io.gravitee.rest.api.service.v4.validation.PathParametersValidationService;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 * @deprecated Use {@link io.gravitee.apim.core.flow.domain_service.FlowValidationDomainService} instead
 */
@Component
@Deprecated
public class PathParametersValidationServiceImpl implements PathParametersValidationService {

    private static final String PATH_PARAM_PREFIX = ":";
    private static final String PATH_SEPARATOR = "/";
    private static final Pattern SEPARATOR_SPLITTER = Pattern.compile(PATH_SEPARATOR);
    private static final Pattern PARAM_PATTERN = Pattern.compile(":\\w*");

    private static final Map<ApiType, Function<Flow, Optional<String>>> PATH_EXTRACTOR = Map.of(
        ApiType.PROXY,
        flow ->
            flow
                .selectorByType(SelectorType.HTTP)
                .stream()
                .map(selector -> ((HttpSelector) selector).getPath())
                .findFirst(),
        ApiType.MESSAGE,
        flow ->
            flow
                .selectorByType(SelectorType.CHANNEL)
                .stream()
                .map(selector -> ((ChannelSelector) selector).getChannel())
                .findFirst(),
        ApiType.LLM_PROXY,
        flow ->
            flow
                .selectorByType(SelectorType.HTTP)
                .stream()
                .map(selector -> ((HttpSelector) selector).getPath())
                .findFirst()
    );

    @Override
    public void validate(ApiType apiType, Stream<Flow> apiFlows, Stream<Flow> planFlows) {
        apiFlows = apiFlows == null ? Stream.empty() : apiFlows;
        planFlows = planFlows == null ? Stream.empty() : planFlows;
        // group all flows in one stream
        final Stream<Flow> flowsWithPathParam = filterFlowsWithPathParam(apiType, apiFlows, planFlows);
        // foreach flow, reprendre PathParameters.extractPathParamsAndPAttern
        validatePathParamOverlapping(apiType, flowsWithPathParam);
    }

    private Stream<Flow> filterFlowsWithPathParam(ApiType apiType, Stream<Flow> apiFlows, Stream<Flow> planFlows) {
        return Stream.concat(apiFlows, planFlows)
            .filter(Flow::isEnabled)
            .filter(flow -> containsPathParam(apiType, flow));
    }

    private void validatePathParamOverlapping(ApiType apiType, Stream<Flow> flows) {
        Map<String, Integer> paramWithPosition = new HashMap<>();
        Map<String, List<String>> pathsByParam = new HashMap<>();
        final AtomicBoolean hasOverlap = new AtomicBoolean(false);

        flows.forEach(flow -> {
            final String path = extractPath(apiType, flow);
            String[] branches = SEPARATOR_SPLITTER.split(path);
            for (int i = 0; i < branches.length; i++) {
                final String currentBranch = branches[i];
                if (currentBranch.startsWith(PATH_PARAM_PREFIX)) {
                    // Store every path for a path param in a map
                    prepareOverlapsMap(pathsByParam, path, currentBranch);
                    if (isOverlapping(paramWithPosition, currentBranch, i)) {
                        // Exception is thrown later to be able to provide every overlapping case to the end user
                        hasOverlap.set(true);
                    } else {
                        paramWithPosition.put(currentBranch, i);
                    }
                }
            }
        });

        if (hasOverlap.get()) {
            throw new PathParameterOverlapValidationException(
                pathsByParam
                    .entrySet()
                    .stream()
                    // Only keep params with overlap
                    .filter(entry -> entry.getValue().size() > 1)
                    .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().toString()))
            );
        }
    }

    private static void prepareOverlapsMap(Map<String, List<String>> pathsByParam, String path, String branches) {
        pathsByParam.compute(branches, (key, value) -> {
            if (value == null) {
                value = new ArrayList<>();
            }
            // Add the path only once to the error message
            if (!value.contains(path)) {
                value.add(path);
            }
            return value;
        });
    }

    private static boolean isOverlapping(Map<String, Integer> paramWithPosition, String param, Integer i) {
        return paramWithPosition.containsKey(param) && !paramWithPosition.get(param).equals(i);
    }

    private static Boolean containsPathParam(ApiType apiType, Flow flow) {
        final String path = extractPath(apiType, flow);
        return PARAM_PATTERN.asPredicate().test(path);
    }

    private static String extractPath(ApiType apiType, Flow flow) {
        return PATH_EXTRACTOR.get(apiType).apply(flow).orElse("");
    }
}
