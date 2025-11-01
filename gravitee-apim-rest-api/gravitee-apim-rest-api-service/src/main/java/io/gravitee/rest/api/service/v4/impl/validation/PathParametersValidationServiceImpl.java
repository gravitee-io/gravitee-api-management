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

import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.selector.ChannelSelector;
import io.gravitee.definition.model.v4.flow.selector.HttpSelector;
import io.gravitee.definition.model.v4.flow.selector.SelectorType;
import io.gravitee.rest.api.service.v4.exception.PathParameterOverlapValidationException;
import io.gravitee.rest.api.service.v4.validation.PathParametersValidationService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
        validatePathParamOverlapping(apiType, flowsWithPathParam);
    }

    private Stream<Flow> filterFlowsWithPathParam(ApiType apiType, Stream<Flow> apiFlows, Stream<Flow> planFlows) {
        return Stream.concat(apiFlows, planFlows)
            .filter(Flow::isEnabled)
            .filter(flow -> containsPathParam(apiType, flow));
    }

    private void validatePathParamOverlapping(ApiType apiType, Stream<Flow> flows) {
        // Extract unique, non-empty paths from enabled flows
        List<String> uniquePaths = flows
            .map(flow -> extractPath(apiType, flow))
            .filter(path -> !path.isEmpty())
            .distinct()
            .toList();

        Map<String, List<String>> overlappingPaths = new HashMap<>();
        int pathCount = uniquePaths.size();

        for (int i = 0; i < pathCount; i++) {
            String path1 = uniquePaths.get(i);
            String[] segments1 = SEPARATOR_SPLITTER.split(path1);

            for (int j = i + 1; j < pathCount; j++) {
                String path2 = uniquePaths.get(j);
                String[] segments2 = SEPARATOR_SPLITTER.split(path2);

                if (segments1.length != segments2.length) continue;

                if (arePathsAmbiguous(segments1, segments2)) {
                    String firstParam = findFirstParameter(segments1);

                    if (firstParam == null) firstParam = findFirstParameter(segments2);

                    if (firstParam == null) firstParam = path1;

                    overlappingPaths.computeIfAbsent(firstParam, k -> new ArrayList<>());

                    if (!overlappingPaths.get(firstParam).contains(path1)) overlappingPaths.get(firstParam).add(path1);

                    if (!overlappingPaths.get(firstParam).contains(path2)) overlappingPaths.get(firstParam).add(path2);
                }
            }
        }

        if (!overlappingPaths.isEmpty()) {
            throw new PathParameterOverlapValidationException(
                overlappingPaths.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().toString()))
            );
        }
    }

    /**
     * Returns true if the two paths (split into segments) are ambiguous per OpenAPI 3.0:
     * - Same number of segments
     * - For each segment: both are parameters, or both are static and equal
     */
    private boolean arePathsAmbiguous(String[] segments1, String[] segments2) {
        for (int i = 0; i < segments1.length; i++) {
            boolean isParam1 = segments1[i].startsWith(PATH_PARAM_PREFIX);
            boolean isParam2 = segments2[i].startsWith(PATH_PARAM_PREFIX);

            if (isParam1 && isParam2) continue;

            if (!isParam1 && !isParam2 && segments1[i].equals(segments2[i])) continue;

            return false;
        }

        return true;
    }

    /**
     * Returns the first parameter segment (e.g. ":id") in the given segments, or null if none.
     */
    private String findFirstParameter(String[] segments) {
        return Arrays.stream(segments)
            .filter(segment -> segment.startsWith(PATH_PARAM_PREFIX))
            .findFirst()
            .orElse(null);
    }

    private static Boolean containsPathParam(ApiType apiType, Flow flow) {
        return PARAM_PATTERN.asPredicate().test(extractPath(apiType, flow));
    }

    private static String extractPath(ApiType apiType, Flow flow) {
        return PATH_EXTRACTOR.get(apiType).apply(flow).orElse("");
    }
}
