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
package io.gravitee.apim.infra.converter.oai;

import static io.gravitee.rest.api.service.validator.JsonHelper.clearNullValues;
import static io.gravitee.rest.api.service.validator.JsonHelper.getScope;

import io.gravitee.apim.core.utils.CollectionUtils;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.model.flow.Operator;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.selector.ConditionSelector;
import io.gravitee.definition.model.v4.flow.selector.HttpSelector;
import io.gravitee.definition.model.v4.flow.selector.SelectorType;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.policy.api.swagger.Policy;
import io.gravitee.rest.api.service.impl.swagger.visitor.v3.OAIOperationVisitor;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import joptsimple.internal.Strings;

public class OAIToFlowsConverter {

    public static OAIToFlowsConverter INSTANCE = new OAIToFlowsConverter();
    private static final Pattern PATH_PARAMS_PATTERN = Pattern.compile("\\{(.[^/\\}]*)\\}");

    List<Flow> convert(OpenAPI specification, Collection<? extends OAIOperationVisitor> visitors) {
        List<Flow> flows = new ArrayList<>();
        specification
            .getPaths()
            .forEach((key, pathItem) -> {
                String path = PATH_PARAMS_PATTERN.matcher(key).replaceAll(":$1");
                Map<PathItem.HttpMethod, Operation> operations = pathItem.readOperationsMap();
                operations.forEach((httpMethod, operation) -> {
                    final Flow flow = mapFlow(path, Collections.singleton(HttpMethod.valueOf(httpMethod.name())));
                    if (!CollectionUtils.isEmpty(visitors)) {
                        visitors.forEach(oaiOperationVisitor -> {
                            Optional<Policy> policy = (Optional<Policy>) oaiOperationVisitor.visit(specification, operation);
                            if (policy.isPresent()) {
                                String configuration = clearNullValues(policy.get().getConfiguration());
                                final Step step = mapStep(policy.get(), configuration, operation);
                                String scope = getScope(configuration);
                                if (scope != null && scope.equalsIgnoreCase("response")) {
                                    flow.getResponse().add(step);
                                } else {
                                    flow.getRequest().add(step);
                                }
                            }
                        });
                    }
                    flows.add(flow);
                });
            });
        return flows;
    }

    private Flow mapFlow(String path, Set<HttpMethod> methods) {
        return Flow.builder()
            .name(Strings.EMPTY)
            .enabled(true)
            .selectors(
                List.of(
                    ConditionSelector.builder().condition(Strings.EMPTY).type(SelectorType.CONDITION).build(),
                    HttpSelector.builder().path(path).pathOperator(Operator.EQUALS).methods(methods).type(SelectorType.HTTP).build()
                )
            )
            .build();
    }

    private Step mapStep(Policy policy, String configuration, Operation operation) {
        return Step.builder()
            .name(policy.getName())
            .enabled(true)
            .description(
                operation.getSummary() == null
                    ? (operation.getOperationId() == null ? operation.getDescription() : operation.getOperationId())
                    : operation.getSummary()
            )
            .policy(policy.getName())
            .configuration(configuration)
            .build();
    }
}
