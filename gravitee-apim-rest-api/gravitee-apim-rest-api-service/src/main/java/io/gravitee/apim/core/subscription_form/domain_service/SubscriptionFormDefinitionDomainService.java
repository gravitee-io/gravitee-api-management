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
package io.gravitee.apim.core.subscription_form.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdown;
import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdownValidator;
import io.gravitee.apim.core.subscription_form.exception.SubscriptionFormApiAlreadyMappedException;
import io.gravitee.apim.core.subscription_form.exception.SubscriptionFormDefinitionValidationException;
import io.gravitee.apim.core.subscription_form.exception.SubscriptionFormNameAlreadyExistsException;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormFieldConstraints;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormId;
import io.gravitee.apim.core.subscription_form.query_service.SubscriptionFormQueryService;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

/**
 * Validates what a subscription form is made of before it is created or updated: a non-empty
 * GMD definition with at most {@link SubscriptionFormSubmissionValidator#MAX_METADATA_COUNT} fields,
 * a name of at most {@value #MAX_NAME_LENGTH} characters no other form of the environment uses, and APIs of the
 * environment no other form claims.
 *
 * @author Gravitee.io Team
 */
@DomainService
@RequiredArgsConstructor
public class SubscriptionFormDefinitionDomainService {

    /** Width of the {@code name} column. */
    public static final int MAX_NAME_LENGTH = 255;

    private final GraviteeMarkdownValidator graviteeMarkdownValidator;
    private final SubscriptionFormSchemaGenerator schemaGenerator;
    private final SubscriptionFormQueryService subscriptionFormQueryService;
    private final ApiCrudService apiCrudService;

    /**
     * Parses and validates a GMD definition, deriving the constraints submissions are checked against.
     */
    public Definition compile(String gmdContent) {
        var gmd = GraviteeMarkdown.of(gmdContent);
        graviteeMarkdownValidator.validateNotEmpty(gmd);
        var schema = schemaGenerator.generate(gmd);
        if (schema != null && schema.fields().size() > SubscriptionFormSubmissionValidator.MAX_METADATA_COUNT) {
            throw new SubscriptionFormDefinitionValidationException(
                "Subscription form must not exceed " + SubscriptionFormSubmissionValidator.MAX_METADATA_COUNT + " fields"
            );
        }
        return new Definition(gmd, SubscriptionFormConstraintsFactory.fromSchema(schema));
    }

    /**
     * Rejects a name already used by another form of the environment. Blank names are rejected too.
     *
     * @param subscriptionFormId the form being renamed, or {@code null} when creating one
     */
    public void validateName(String environmentId, String name, @Nullable SubscriptionFormId subscriptionFormId) {
        if (name == null || name.isBlank()) {
            throw new SubscriptionFormDefinitionValidationException("Subscription form name must not be empty");
        }
        var trimmed = name.trim();
        if (trimmed.length() > MAX_NAME_LENGTH) {
            throw new SubscriptionFormDefinitionValidationException(
                "Subscription form name must not exceed " + MAX_NAME_LENGTH + " characters"
            );
        }
        var taken = subscriptionFormQueryService
            .findAllByEnvironmentId(environmentId)
            .stream()
            .anyMatch(form -> form.getName().equalsIgnoreCase(trimmed) && !form.getId().equals(subscriptionFormId));
        if (taken) {
            throw new SubscriptionFormNameAlreadyExistsException(trimmed);
        }
    }

    /**
     * Checks the APIs a form is dedicated to: each one exists in the environment and is not mapped to
     * another form. Returns the distinct, non-blank ids to store.
     *
     * @param subscriptionFormId the form being updated, or {@code null} when creating one
     */
    public List<String> validateApiIds(
        String environmentId,
        @Nullable List<String> apiIds,
        @Nullable SubscriptionFormId subscriptionFormId
    ) {
        if (apiIds == null || apiIds.isEmpty()) {
            return List.of();
        }
        var distinctIds = apiIds
            .stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(id -> !id.isEmpty())
            .distinct()
            .toList();
        if (distinctIds.isEmpty()) {
            return List.of();
        }

        Set<String> known = apiCrudService
            .findByIds(distinctIds)
            .stream()
            .filter(api -> environmentId.equals(api.getEnvironmentId()))
            .map(Api::getId)
            .collect(Collectors.toSet());
        var unknown = distinctIds
            .stream()
            .filter(id -> !known.contains(id))
            .toList();
        if (!unknown.isEmpty()) {
            throw new SubscriptionFormDefinitionValidationException("Unknown APIs in this environment: " + String.join(", ", unknown));
        }

        subscriptionFormQueryService
            .findAllByEnvironmentId(environmentId)
            .stream()
            .filter(form -> !form.getId().equals(subscriptionFormId))
            .forEach(form ->
                form
                    .getApiIds()
                    .stream()
                    .filter(distinctIds::contains)
                    .findFirst()
                    .ifPresent(apiId -> {
                        throw new SubscriptionFormApiAlreadyMappedException(apiId, form.getName());
                    })
            );
        return distinctIds;
    }

    public record Definition(GraviteeMarkdown gmdContent, SubscriptionFormFieldConstraints constraints) {}
}
