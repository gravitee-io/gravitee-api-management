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
import java.util.Map;
import lombok.Getter;

/**
 * Thrown when submitted subscription form values fail schema validation.
 *
 * <p>The REST layer maps this exception to an HTTP 400 response and includes
 * the individual field errors in the response body.</p>
 *
 * @author Gravitee.io Team
 */
@Getter
public class SubscriptionFormValidationException extends ValidationDomainException {

    private final List<String> errors;

    public SubscriptionFormValidationException(List<String> errors) {
        this(errors, String.join(", ", errors));
    }

    private SubscriptionFormValidationException(List<String> errors, String joinedErrors) {
        super("Subscription form submission is invalid: " + joinedErrors, Map.of("errors", joinedErrors));
        this.errors = List.copyOf(errors);
    }
}
