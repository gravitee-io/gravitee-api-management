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
package io.gravitee.rest.api.service.v4.impl.validation;

import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.selector.ChannelSelector;
import io.gravitee.definition.model.v4.flow.selector.Selector;
import io.gravitee.definition.model.v4.flow.selector.SelectorType;
import io.gravitee.rest.api.model.platform.plugin.PlatformPluginEntity;
import io.gravitee.rest.api.service.PolicyService;
import io.gravitee.rest.api.service.impl.TransactionalService;
import io.gravitee.rest.api.service.v4.EntrypointConnectorPluginService;
import io.gravitee.rest.api.service.v4.exception.FlowSelectorsDuplicatedException;
import io.gravitee.rest.api.service.v4.exception.FlowSelectorsEntrypointInvalidException;
import io.gravitee.rest.api.service.v4.exception.FlowSelectorsInvalidException;
import io.gravitee.rest.api.service.v4.validation.FlowValidationService;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
@AllArgsConstructor
public class FlowValidationServiceImpl extends TransactionalService implements FlowValidationService {

    private final PolicyService policyService;
    private final EntrypointConnectorPluginService entrypointConnectorPluginService;

    @Override
    public List<Flow> validateAndSanitize(final ApiType apiType, List<Flow> flows) {
        if (flows != null) {
            flows.forEach(
                flow -> {
                    // Check duplicated selectors
                    checkDuplicatedSelectors(flow);

                    // Check selectors according to api type
                    checkSelectorsForType(apiType, flow);

                    // Validate policy
                    checkPolicyConfiguration(flow);
                }
            );
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
                    throw new FlowSelectorsInvalidException(flow.getName(), apiType, invalidSelectors);
                }
            } else if (ApiType.MESSAGE == apiType) {
                Set<String> invalidSelectors = flow
                    .getSelectors()
                    .stream()
                    .filter(selector -> !(selector.getType() == SelectorType.CHANNEL || selector.getType() == SelectorType.CONDITION))
                    .map(selector -> selector.getType().getLabel())
                    .collect(Collectors.toSet());
                if (!invalidSelectors.isEmpty()) {
                    throw new FlowSelectorsInvalidException(flow.getName(), apiType, invalidSelectors);
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
                    .map(PlatformPluginEntity::getId)
                    .collect(Collectors.toSet());

                Set<String> invalidEntrypoints = channelSelector
                    .getEntrypoints()
                    .stream()
                    .filter(entrypointId -> !asyncEntrypoints.contains(entrypointId))
                    .collect(Collectors.toSet());
                if (!invalidEntrypoints.isEmpty()) {
                    throw new FlowSelectorsEntrypointInvalidException(flow.getName(), invalidEntrypoints);
                }
            }
        }
    }

    private void checkPolicyConfiguration(final Flow flow) {
        Stream
            .of(flow.getRequest(), flow.getResponse(), flow.getSubscribe(), flow.getPublish())
            .filter(Objects::nonNull)
            .flatMap(Collection::stream)
            .filter(step -> step != null && step.getPolicy() != null && step.getConfiguration() != null)
            .forEach(step -> step.setConfiguration(policyService.validatePolicyConfiguration(step.getPolicy(), step.getConfiguration())));
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
                throw new FlowSelectorsDuplicatedException(flow.getName(), duplicatedSelectors);
            }
        }
    }
}
