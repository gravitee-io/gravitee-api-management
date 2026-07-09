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
package io.gravitee.rest.api.validator;

import io.gravitee.rest.api.model.email.EmailAddresses;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

/**
 * @author GraviteeSource Team
 */
public class SenderAddressValidator implements ConstraintValidator<ValidSenderAddress, String> {

    /**
     * Pragmatic {@code local@domain.tld} check for a branded sender address. The domain is matched as
     * RFC 1035 labels (each starts and ends alphanumeric, no empty or leading-dot labels, no consecutive
     * dots) with a bounded label length. The label group is matched with a possessive quantifier
     * ({@code ++}) so the engine runs iteratively — it can't backtrack super-linearly or recurse into a
     * stack overflow on a large input — and it rejects shapes like {@code a..b.com} / {@code .a.com}.
     */
    private static final Pattern EMAIL = Pattern.compile(
        "^[A-Za-z0-9._%+-]+@(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\\.)++[A-Za-z]{2,}$"
    );

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        // Resolve the single address (unwrapping a "Name <addr>" personal name and rejecting multi-address
        // lists) with the same helper the send-time branded-sender matching uses, so save never accepts a
        // From that delivery cannot send. The format check then rejects a malformed single address.
        return EmailAddresses.singleAddress(value)
            .filter(address -> EMAIL.matcher(address).matches())
            .isPresent();
    }
}
