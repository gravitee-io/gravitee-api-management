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

import io.gravitee.apim.core.subscription_form.exception.SubscriptionFormValidationException;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormFieldConstraints;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormSchema;
import java.util.List;
import java.util.Map;

/**
 * Validates a subscription form submission using {@link SubscriptionFormFieldConstraints}.
 *
 * <p>Prefer {@link #SubscriptionFormSubmissionValidator(SubscriptionFormFieldConstraints)} when constraints are
 * loaded from storage; use {@link #SubscriptionFormSubmissionValidator(SubscriptionFormSchema)} to derive constraints
 * from a schema in memory (e.g. tests).</p>
 *
 * <p>All field errors are collected before throwing so that the client receives a complete list of violations in a
 * single response.</p>
 *
 * @author Gravitee.io Team
 */
public class SubscriptionFormSubmissionValidator {

    /** Maximum number of metadata entries accepted in a subscription form submission. */
    public static final int MAX_METADATA_COUNT = 25;

    private final SubscriptionFormFieldConstraints fieldConstraints;

    public SubscriptionFormSubmissionValidator(SubscriptionFormSchema schema) {
        this(SubscriptionFormConstraintsFactory.fromSchema(schema));
    }

    public SubscriptionFormSubmissionValidator(SubscriptionFormFieldConstraints fieldConstraints) {
        this.fieldConstraints = fieldConstraints;
    }

    /**
     * Validates the submitted key-value pairs.
     *
     * @param submittedValues field values keyed by fieldKey; must not be null. When a key is absent the value is
     *                        treated as empty. If a key is present, its value must not be {@code null} (callers that
     *                        build this map in domain code should normalize accordingly).
     * @throws SubscriptionFormValidationException with all collected field errors when the submission is invalid
     */
    public void validate(Map<String, String> submittedValues) {
        if (fieldConstraints.isEmpty()) {
            return;
        }
        if (submittedValues.size() > MAX_METADATA_COUNT) {
            throw new SubscriptionFormValidationException(
                List.of("Subscription metadata must not exceed " + MAX_METADATA_COUNT + " entries")
            );
        }
        List<String> errors = fieldConstraints
            .byFieldKey()
            .entrySet()
            .stream()
            .flatMap(entry -> {
                String fieldKey = entry.getKey();
                String value = submittedValues.getOrDefault(fieldKey, "").trim();
                return entry
                    .getValue()
                    .stream()
                    .filter(c -> !c.check(value))
                    .map(c -> c.formatErrorMessage(fieldKey, value))
                    .filter(msg -> !msg.isEmpty());
            })
            .toList();

        if (!errors.isEmpty()) {
            throw new SubscriptionFormValidationException(errors);
        }
    }
}
