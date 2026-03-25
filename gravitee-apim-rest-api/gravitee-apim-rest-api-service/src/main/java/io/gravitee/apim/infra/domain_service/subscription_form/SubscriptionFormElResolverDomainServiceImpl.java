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
package io.gravitee.apim.infra.domain_service.subscription_form;

import io.gravitee.apim.core.api.model.ApiMetadata;
import io.gravitee.apim.core.api.query_service.ApiMetadataQueryService;
import io.gravitee.apim.core.documentation.domain_service.TemplateResolverDomainService;
import io.gravitee.apim.core.documentation.exception.InvalidPageContentException;
import io.gravitee.apim.core.subscription_form.domain_service.SubscriptionFormElResolverDomainService;
import io.gravitee.apim.core.subscription_form.model.Constraint;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormFieldConstraints;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormSchema;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormSchema.DynamicOptionsAttribute;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Resolves EL expressions in subscription form option fields using Freemarker.
 *
 * <p>Expressions follow the format {@code ${expression}} and are evaluated against the target
 * API's metadata. On resolution failure the configured fallback list is used transparently.</p>
 *
 * @author Gravitee.io Team
 */
@Service
@RequiredArgsConstructor
@CustomLog
public class SubscriptionFormElResolverDomainServiceImpl implements SubscriptionFormElResolverDomainService {

    private final TemplateResolverDomainService templateResolver;
    private final ApiMetadataQueryService apiMetadataQueryService;

    @Override
    public Map<String, List<String>> resolveSchemaOptions(SubscriptionFormSchema schema, String environmentId, String apiId) {
        if (schema == null || schema.isEmpty()) {
            return Map.of();
        }

        Map<String, Object> templateParams = buildTemplateParams(environmentId, apiId);

        Map<String, List<String>> result = new LinkedHashMap<>();
        for (SubscriptionFormSchema.Field field : schema.fields()) {
            if (field instanceof DynamicOptionsAttribute dyn && dyn.dynamicOptions() != null) {
                var dynamic = dyn.dynamicOptions();
                result.put(field.fieldKey(), resolve(dynamic.expression(), dynamic.fallback(), templateParams));
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

        Map<String, Object> templateParams = buildTemplateParams(environmentId, apiId);

        Map<String, List<Constraint>> resolved = new LinkedHashMap<>();
        for (Map.Entry<String, List<Constraint>> entry : constraints.byFieldKey().entrySet()) {
            resolved.put(entry.getKey(), resolveConstraintList(entry.getValue(), templateParams));
        }
        return new SubscriptionFormFieldConstraints(resolved);
    }

    private List<Constraint> resolveConstraintList(List<Constraint> constraints, Map<String, Object> templateParams) {
        return constraints
            .stream()
            .map(c -> resolveConstraint(c, templateParams))
            .collect(Collectors.toList());
    }

    private Constraint resolveConstraint(Constraint constraint, Map<String, Object> templateParams) {
        return switch (constraint) {
            case Constraint.DynamicOneOf d -> new Constraint.OneOf(resolve(d.expression(), d.fallback(), templateParams));
            case Constraint.DynamicEachOf d -> new Constraint.EachOf(resolve(d.expression(), d.fallback(), templateParams));
            default -> constraint;
        };
    }

    private List<String> resolve(String expression, List<String> fallback, Map<String, Object> templateParams) {
        if (templateParams.isEmpty()) {
            return fallback;
        }
        try {
            String resolved = templateResolver.resolveTemplate("${" + expression + "}", templateParams);
            if (resolved == null || resolved.isBlank()) {
                return fallback;
            }
            List<String> options = Arrays.stream(resolved.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
            return options.isEmpty() ? fallback : options;
        } catch (InvalidPageContentException e) {
            log.warn("Failed to resolve EL expression '{}' for subscription form options, using fallback: {}", expression, e.getMessage());
            return fallback;
        }
    }

    private Map<String, Object> buildTemplateParams(String environmentId, String apiId) {
        var raw = apiId != null
            ? apiMetadataQueryService.findApiMetadata(environmentId, apiId)
            : apiMetadataQueryService.findEnvironmentMetadata(environmentId);

        var metadata = raw
            .entrySet()
            .stream()
            .filter(e -> effectiveValue(e.getValue()) != null)
            .collect(Collectors.toMap(Map.Entry::getKey, e -> (Object) effectiveValue(e.getValue())));

        Map<String, Object> api = new HashMap<>();
        api.put("metadata", metadata);
        return Map.of("api", api);
    }

    private static String effectiveValue(ApiMetadata m) {
        return m.getValue() != null ? m.getValue() : m.getDefaultValue();
    }
}
