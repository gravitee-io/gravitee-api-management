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

import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * @author GraviteeSource Team
 */
public class SenderAddressValidator implements ConstraintValidator<ValidSenderAddress, String> {

    static final String MULTI_ADDRESS_MESSAGE = "must be a single sender address";

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        // Validate the *whole* value with the same jakarta.mail parser the SMTP send-path uses
        // (EmailServiceImpl -> new InternetAddress(from)), so a From accepted at save time is one delivery
        // can actually send. Checking only an unwrapped inner address would let a broken display name
        // through — an unquoted comma in "Acme, Inc <noreply@acme.com>" parses as two addresses at send
        // time and fails, even though its inner address is valid on its own.
        if (isSendable(value)) {
            return true;
        }
        // A value carrying more than one '@' is an address list, not a single sender. Say so distinctly
        // rather than with the generic message: each address may be valid on its own — the problem is that
        // there is more than one.
        if (hasMultipleAddresses(value)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(MULTI_ADDRESS_MESSAGE).addConstraintViolation();
        }
        return false;
    }

    /**
     * Strict-parses the whole value with {@link InternetAddress#validate()}, mirroring the SMTP send-path so
     * a save-accepted {@code From} is one delivery can send. It is intentionally stricter than the send-path's
     * lenient {@code new InternetAddress(value)} in a single respect: it also rejects a value with no real
     * domain ({@code not-an-email}), which parses but could never be delivered. It therefore never rejects an
     * address the send-path could actually send, so it cannot reintroduce a false-reject lockout.
     */
    private static boolean isSendable(String value) {
        try {
            new InternetAddress(value, true).validate();
            return true;
        } catch (AddressException e) {
            return false;
        }
    }

    private static boolean hasMultipleAddresses(String value) {
        var trimmed = value.trim();
        return trimmed.indexOf('@') != trimmed.lastIndexOf('@');
    }
}
