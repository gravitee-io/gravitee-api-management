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
package inmemory;

import io.gravitee.apim.core.subscription_form.domain_service.SubscriptionFormElResolverDomainService;
import io.gravitee.apim.core.subscription_form.model.Constraint;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormFieldConstraints;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormSchema;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormSchema.DynamicOptionsAttribute;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * In-memory test double for {@link SubscriptionFormElResolverDomainService}.
 *
 * <p>Pre-configured with a map of {@code expression → resolved options} via {@link #withResolved(Map)}.
 * If an expression is not in the map, the fallback values are returned.</p>
 */
public class SubscriptionFormElResolverInMemory implements SubscriptionFormElResolverDomainService {

    private Map<String, List<String>> resolvedByExpression = Map.of();

    public SubscriptionFormElResolverInMemory withResolved(Map<String, List<String>> resolvedByExpression) {
        this.resolvedByExpression = Map.copyOf(resolvedByExpression);
        return this;
    }

    public void reset() {
        this.resolvedByExpression = Map.of();
    }

    @Override
    public Map<String, List<String>> resolveSchemaOptions(SubscriptionFormSchema schema) {
        return resolveSchemaOptions(schema, null, null);
    }

    @Override
    public SubscriptionFormFieldConstraints resolveConstraints(SubscriptionFormFieldConstraints constraints) {
        return resolveConstraints(constraints, null, null);
    }

    @Override
    public Map<String, List<String>> resolveSchemaOptions(SubscriptionFormSchema schema, String environmentId, String apiId) {
        if (schema == null || schema.isEmpty()) {
            return Map.of();
        }

        Map<String, List<String>> result = new LinkedHashMap<>();
        for (SubscriptionFormSchema.Field field : schema.fields()) {
            if (field instanceof DynamicOptionsAttribute dyn && dyn.dynamicOptions() != null) {
                var dynamic = dyn.dynamicOptions();
                result.put(field.fieldKey(), resolvedByExpression.getOrDefault(dynamic.expression(), dynamic.fallback()));
            }
        }
        return result;
    }

    @Override
    public SubscriptionFormFieldConstraints resolveConstraints(
        SubscriptionFormFieldConstraints constraints,
        String environmentId,
        String apiId
    ) {
        if (constraints == null || constraints.isEmpty()) {
            return constraints;
        }

        Map<String, List<Constraint>> resolved = new LinkedHashMap<>();
        for (Map.Entry<String, List<Constraint>> entry : constraints.byFieldKey().entrySet()) {
            resolved.put(entry.getKey(), resolveConstraintList(entry.getValue()));
        }
        return new SubscriptionFormFieldConstraints(resolved);
    }

    private List<Constraint> resolveConstraintList(List<Constraint> constraints) {
        return constraints.stream().map(this::resolveConstraint).collect(Collectors.toList());
    }

    private Constraint resolveConstraint(Constraint constraint) {
        if (constraint instanceof Constraint.ResolvableOptions resolvableOptions) {
            return resolvableOptions.resolve((expression, fallback) -> resolvedByExpression.getOrDefault(expression, fallback));
        }
        return constraint;
    }
}
