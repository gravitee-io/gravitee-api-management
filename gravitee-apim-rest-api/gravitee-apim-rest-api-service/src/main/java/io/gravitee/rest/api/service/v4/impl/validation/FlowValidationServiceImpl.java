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

import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.selector.Selector;
import io.gravitee.rest.api.service.PolicyService;
import io.gravitee.rest.api.service.impl.TransactionalService;
import io.gravitee.rest.api.service.v4.exception.FlowSelectorsDuplicatedException;
import io.gravitee.rest.api.service.v4.validation.FlowValidationService;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class FlowValidationServiceImpl extends TransactionalService implements FlowValidationService {

    private final PolicyService policyService;

    public FlowValidationServiceImpl(final PolicyService policyService) {
        this.policyService = policyService;
    }

    @Override
    public List<Flow> validateAndSanitize(List<Flow> flows) {
        if (flows != null) {
            flows.forEach(
                flow -> {
                    // Check duplicated selectors
                    checkDuplicatedSelectors(flow);

                    // Validate policy
                    checkPolicyConfiguration(flow);
                }
            );
        }
        return flows;
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
