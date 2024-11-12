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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
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

    private static final Map<ApiType, Function<Flow, Optional<String>>> PATH_EXTRACTOR = Map.of(
        ApiType.PROXY,
        flow -> flow.selectorByType(SelectorType.HTTP).stream().map(selector -> ((HttpSelector) selector).getPath()).findFirst(),
        ApiType.MESSAGE,
        flow -> flow.selectorByType(SelectorType.CHANNEL).stream().map(selector -> ((ChannelSelector) selector).getChannel()).findFirst()
    );

    public List<Flow> validateAndSanitizeHttpV4(final ApiType apiType, List<Flow> flows) {
        if (flows != null) {
            flows.forEach(flow -> {
                // Check duplicated selectors
                checkDuplicatedSelectors(flow);

                // Check selectors according to api type
                checkSelectorsForType(apiType, flow);

                // Validate policy
                var steps = Stream
                    .of(flow.getRequest(), flow.getResponse(), flow.getPublish(), flow.getSubscribe())
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
                // Validate policy
                var steps = Stream
                    .of(flow.getInteract(), flow.getConnect(), flow.getPublish(), flow.getSubscribe())
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
            if (ApiType.PROXY == apiType) {
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
        validatePathParamOverlapping(apiType, flowsWithPathParam);
    }

    private Stream<Flow> filterFlowsWithPathParam(ApiType apiType, Stream<Flow> apiFlows, Stream<Flow> planFlows) {
        return Stream.concat(apiFlows, planFlows).filter(Flow::isEnabled).filter(flow -> containsPathParam(apiType, flow));
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
            throw new ValidationDomainException(
                "Some path parameters are used at different position across different flows.",
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
        pathsByParam.compute(
            branches,
            (key, value) -> {
                if (value == null) {
                    value = new ArrayList<>();
                }
                // Add the path only once to the error message
                if (!value.contains(path)) {
                    value.add(path);
                }
                return value;
            }
        );
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
