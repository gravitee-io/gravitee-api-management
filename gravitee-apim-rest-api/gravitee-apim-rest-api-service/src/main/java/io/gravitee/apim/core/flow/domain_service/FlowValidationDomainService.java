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
package io.gravitee.apim.core.flow.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.api.exception.NativeApiWithMultipleFlowsException;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.flow.exception.InvalidFlowException;
import io.gravitee.apim.core.plugin.model.PlatformPlugin;
import io.gravitee.apim.core.plugin.query_service.EntrypointPluginQueryService;
import io.gravitee.apim.core.policy.domain_service.PolicyValidationDomainService;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.selector.ChannelSelector;
import io.gravitee.definition.model.v4.flow.selector.HttpSelector;
import io.gravitee.definition.model.v4.flow.selector.Selector;
import io.gravitee.definition.model.v4.flow.selector.SelectorType;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.definition.model.v4.nativeapi.NativeFlow;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@DomainService
public class FlowValidationDomainService {

    private static final String PATH_PARAM_PREFIX = ":";
    private static final String PATH_SEPARATOR = "/";
    private static final Pattern SEPARATOR_SPLITTER = Pattern.compile(PATH_SEPARATOR);
    private static final Pattern PARAM_PATTERN = Pattern.compile(":\\w*");
    private final PolicyValidationDomainService policyValidationDomainService;
    private final EntrypointPluginQueryService entrypointConnectorPluginService;

    public FlowValidationDomainService(
        PolicyValidationDomainService policyValidationDomainService,
        EntrypointPluginQueryService entrypointConnectorPluginService
    ) {
        this.policyValidationDomainService = policyValidationDomainService;
        this.entrypointConnectorPluginService = entrypointConnectorPluginService;
    }

    private static final Function<Flow, Optional<String>> HTTP_PATH_EXTRACTOR = flow ->
        flow
            .selectorByType(SelectorType.HTTP)
            .stream()
            .map(selector -> ((HttpSelector) selector).getPath())
            .findFirst();

    private static final Map<ApiType, Function<Flow, Optional<String>>> PATH_EXTRACTOR = Map.of(
        ApiType.PROXY,
        HTTP_PATH_EXTRACTOR,
        ApiType.MESSAGE,
        flow ->
            flow
                .selectorByType(SelectorType.CHANNEL)
                .stream()
                .map(selector -> ((ChannelSelector) selector).getChannel())
                .findFirst(),
        ApiType.MCP_PROXY,
        flow -> Optional.empty(),
        ApiType.LLM_PROXY,
        HTTP_PATH_EXTRACTOR,
        ApiType.A2A_PROXY,
        HTTP_PATH_EXTRACTOR
    );

    public List<Flow> validateAndSanitizeHttpV4(final ApiType apiType, List<Flow> flows) {
        if (flows != null) {
            flows.forEach(flow -> {
                // Check duplicated selectors
                checkDuplicatedSelectors(flow);

                // Check selectors according to api type
                checkSelectorsForType(apiType, flow);

                // Validate policy
                var steps = Stream.of(flow.getRequest(), flow.getResponse(), flow.getPublish(), flow.getSubscribe())
                    .filter(Objects::nonNull)
                    .flatMap(Collection::stream)
                    .toList();

                checkPolicyConfiguration(steps);
            });
        }
        return flows;
    }

    public List<NativeFlow> validateAndSanitizeNativeV4(List<NativeFlow> flows) {
        if (flows != null) {
            if (flows.size() > 1) {
                throw new NativeApiWithMultipleFlowsException();
            }
            flows.forEach(flow -> {
                // Validate policy configuration
                var steps = Stream.of(flow.getEntrypointConnect(), flow.getInteract(), flow.getPublish(), flow.getSubscribe())
                    .filter(Objects::nonNull)
                    .flatMap(Collection::stream)
                    .toList();

                checkPolicyConfiguration(steps);
            });
        }
        return flows;
    }

    private void checkSelectorsForType(final ApiType apiType, final Flow flow) {
        if (flow.getSelectors() != null) {
            if (ApiType.PROXY == apiType || ApiType.LLM_PROXY == apiType || ApiType.A2A_PROXY == apiType) {
                Set<String> invalidSelectors = flow
                    .getSelectors()
                    .stream()
                    .filter(selector -> !(selector.getType() == SelectorType.HTTP || selector.getType() == SelectorType.CONDITION))
                    .map(selector -> selector.getType().getLabel())
                    .collect(Collectors.toSet());
                if (!invalidSelectors.isEmpty()) {
                    throw InvalidFlowException.invalidSelector(flow.getName(), apiType, invalidSelectors);
                }
            } else if (ApiType.MESSAGE == apiType) {
                Set<String> invalidSelectors = flow
                    .getSelectors()
                    .stream()
                    .filter(selector -> !(selector.getType() == SelectorType.CHANNEL || selector.getType() == SelectorType.CONDITION))
                    .map(selector -> selector.getType().getLabel())
                    .collect(Collectors.toSet());
                if (!invalidSelectors.isEmpty()) {
                    throw InvalidFlowException.invalidSelector(flow.getName(), apiType, invalidSelectors);
                }

                checkChannelAsyncEntrypoint(flow);
            } else if (ApiType.MCP_PROXY == apiType) {
                Set<String> invalidSelectors = flow
                    .getSelectors()
                    .stream()
                    .filter(selector -> !(selector.getType() == SelectorType.MCP || selector.getType() == SelectorType.CONDITION))
                    .map(selector -> selector.getType().getLabel())
                    .collect(Collectors.toSet());
                if (!invalidSelectors.isEmpty()) {
                    throw InvalidFlowException.invalidSelector(flow.getName(), apiType, invalidSelectors);
                }
            }
        }
    }

    private void checkChannelAsyncEntrypoint(final Flow flow) {
        Optional<ChannelSelector> channelSelectorOpt = flow
            .getSelectors()
            .stream()
            .filter(selector -> selector.getType() == SelectorType.CHANNEL)
            .map(ChannelSelector.class::cast)
            .findFirst();
        if (channelSelectorOpt.isPresent()) {
            ChannelSelector channelSelector = channelSelectorOpt.get();
            if (channelSelector.getEntrypoints() != null) {
                Set<String> asyncEntrypoints = entrypointConnectorPluginService
                    .findBySupportedApi(ApiType.MESSAGE)
                    .stream()
                    .map(PlatformPlugin::getId)
                    .collect(Collectors.toSet());

                Set<String> invalidEntrypoints = channelSelector
                    .getEntrypoints()
                    .stream()
                    .filter(entrypointId -> !asyncEntrypoints.contains(entrypointId))
                    .collect(Collectors.toSet());

                if (!invalidEntrypoints.isEmpty()) {
                    throw InvalidFlowException.invalidEntrypoint(flow.getName(), invalidEntrypoints);
                }
            }
        }
    }

    private void checkPolicyConfiguration(final List<Step> steps) {
        steps
            .stream()
            .filter(step -> step != null && step.getPolicy() != null && step.getConfiguration() != null)
            .forEach(step ->
                step.setConfiguration(
                    policyValidationDomainService.validateAndSanitizeConfiguration(step.getPolicy(), step.getConfiguration())
                )
            );
    }

    private void checkDuplicatedSelectors(final Flow flow) {
        if (flow.getSelectors() != null) {
            Set<Selector> seenSelectors = new HashSet<>();
            Set<String> duplicatedSelectors = flow
                .getSelectors()
                .stream()
                .filter(e -> !seenSelectors.add(e))
                .map(selector -> selector.getType().getLabel())
                .collect(Collectors.toSet());
            if (!duplicatedSelectors.isEmpty()) {
                throw InvalidFlowException.duplicatedSelector(flow.getName(), duplicatedSelectors);
            }
        }
    }

    public void validatePathParameters(ApiType apiType, Stream<Flow> apiFlows, Stream<Flow> planFlows) {
        apiFlows = apiFlows == null ? Stream.empty() : apiFlows;
        planFlows = planFlows == null ? Stream.empty() : planFlows;
        // group all flows in one stream
        final Stream<Flow> flowsWithPathParam = filterFlowsWithPathParam(apiType, apiFlows, planFlows);
        checkOverlappingPaths(apiType, flowsWithPathParam);
    }

    private Stream<Flow> filterFlowsWithPathParam(ApiType apiType, Stream<Flow> apiFlows, Stream<Flow> planFlows) {
        return Stream.concat(apiFlows, planFlows)
            .filter(Flow::isEnabled)
            .filter(flow -> containsPathParam(apiType, flow));
    }

    private void checkOverlappingPaths(ApiType apiType, Stream<Flow> flows) {
        // Extract unique, non-empty paths from enabled flows
        List<String> uniquePaths = flows
            .map(flow -> extractPath(apiType, flow))
            .map(this::normalizePath) // normalize to avoid ambiguity due to slashes/case
            .filter(path -> !path.isEmpty())
            .distinct()
            .toList();

        Map<String, Set<String>> overlappingPaths = new HashMap<>();
        int pathCount = uniquePaths.size();

        for (int i = 0; i < pathCount; i++) {
            String path1 = uniquePaths.get(i);
            String[] segments1 = splitPathSegments(path1);

            for (int j = i + 1; j < pathCount; j++) {
                String path2 = uniquePaths.get(j);
                String[] segments2 = splitPathSegments(path2);

                if (segments1.length != segments2.length) continue;

                if (arePathsAmbiguous(segments1, segments2)) {
                    // Use a deterministic grouping key to avoid merging unrelated conflicts
                    String key = buildAmbiguitySignature(segments1);
                    Set<String> paths = overlappingPaths.computeIfAbsent(key, k -> new HashSet<>());
                    paths.add(path1);
                    paths.add(path2);
                }
            }
        }

        if (!overlappingPaths.isEmpty()) {
            // Sort lists for stable output
            Map<String, String> payload = overlappingPaths
                .entrySet()
                .stream()
                .collect(
                    Collectors.toMap(Map.Entry::getKey, entry -> {
                        List<String> sortedPaths = new ArrayList<>(entry.getValue());
                        sortedPaths.sort(String::compareTo);
                        return sortedPaths.toString();
                    })
                );

            throw new ValidationDomainException("Invalid path parameters", payload);
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
     * Normalize path:
     * - Collapse multiple slashes
     * - Remove trailing slash (except root "/")
     * - Lowercase literals if routing is case-insensitive; keeping case as-is here
     */
    private String normalizePath(String raw) {
        if (raw == null) return "";
        String p = raw.trim();

        if (p.isEmpty()) return "";
        // Collapse multiple slashes
        p = p.replaceAll("/{2,}", PATH_SEPARATOR);
        // Remove trailing slash except root
        if (p.length() > 1 && p.endsWith(PATH_SEPARATOR)) {
            p = p.substring(0, p.length() - 1);
        }
        // Ensure leading slash for consistency
        if (!p.startsWith(PATH_SEPARATOR)) {
            p = PATH_SEPARATOR + p;
        }

        return p;
    }

    /**
     * Split path into non-empty segments after normalization.
     */
    private String[] splitPathSegments(String path) {
        return Arrays.stream(SEPARATOR_SPLITTER.split(path))
            .filter(s -> !s.isEmpty())
            .toArray(String[]::new);
    }

    /**
     * Build a deterministic ambiguity signature by replacing any parameter segment with ":" and keeping literals.
     * Example: /users/:id/orders -> /users/:/orders
     */
    private String buildAmbiguitySignature(String[] segments) {
        return (
            PATH_SEPARATOR +
            Arrays.stream(segments)
                .map(s -> s.startsWith(PATH_PARAM_PREFIX) ? PATH_PARAM_PREFIX : s)
                .collect(Collectors.joining(PATH_SEPARATOR))
        );
    }

    private static Boolean containsPathParam(ApiType apiType, Flow flow) {
        final String path = extractPath(apiType, flow);
        return PARAM_PATTERN.asPredicate().test(path);
    }

    private static String extractPath(ApiType apiType, Flow flow) {
        return PATH_EXTRACTOR.get(apiType).apply(flow).orElse("");
    }
}
