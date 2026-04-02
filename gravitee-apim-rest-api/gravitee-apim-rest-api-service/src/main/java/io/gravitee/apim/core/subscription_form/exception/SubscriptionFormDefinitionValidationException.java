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
package io.gravitee.apim.core.subscription_form.exception;

import io.gravitee.apim.core.exception.ValidationDomainException;
import java.util.List;
import lombok.Getter;

/**
 * Thrown when a subscription form definition (GMD template itself) is invalid.
 *
 * <p>This is distinct from {@link SubscriptionFormValidationException}, which is used for validating
 * submitted subscription metadata values against an already valid schema.</p>
 *
 * @author Gravitee.io Team
 */
public class SubscriptionFormDefinitionValidationException extends ValidationDomainException {

    @Getter
    private final List<String> errors;

    public SubscriptionFormDefinitionValidationException(String error) {
        this(List.of(error));
    }

    public SubscriptionFormDefinitionValidationException(List<String> errors) {
        this(errors, null);
    }

    public SubscriptionFormDefinitionValidationException(List<String> errors, Throwable cause) {
        super("Subscription form definition is invalid: " + String.join(", ", errors), cause);
        this.getParameters().put("errors", String.join(", ", errors));
        this.errors = List.copyOf(errors);
    }
}
