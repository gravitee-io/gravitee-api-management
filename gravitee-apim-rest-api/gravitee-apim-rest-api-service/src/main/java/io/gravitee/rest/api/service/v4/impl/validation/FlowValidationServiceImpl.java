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
import io.gravitee.rest.api.service.PolicyService;
import io.gravitee.rest.api.service.impl.TransactionalService;
import io.gravitee.rest.api.service.v4.FlowValidationService;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
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
                flow ->
                    Stream
                        .of(flow.getRequest(), flow.getResponse(), flow.getSubscribe(), flow.getPublish())
                        .filter(Objects::nonNull)
                        .flatMap(Collection::stream)
                        .filter(step -> step != null && step.getPolicy() != null && step.getConfiguration() != null)
                        .forEach(step -> policyService.validatePolicyConfiguration(step.getPolicy(), step.getConfiguration()))
            );
        }
        return flows;
    }
}
