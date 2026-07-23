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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SenderAddressValidatorTest {

    private SenderAddressValidator cut;

    @Mock
    private ConstraintValidatorContext constraintValidatorContext;

    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder constraintViolationBuilder;

    @BeforeEach
    void setUp() {
        cut = new SenderAddressValidator();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "   " })
    void should_accept_null_or_blank_and_defer_to_notblank(String value) {
        // A required value is enforced by a separate @NotBlank — this constraint only checks shape.
        assertThat(cut.isValid(value, constraintValidatorContext)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "noreply@localhost", // single-label host — old regex wrongly rejected
            "noreply@mail.example.com", // dotted domain
            "o'brien@acme.co", // apostrophe in local part — old regex wrongly rejected
            "Acme Inc <noreply@acme.com>", // RFC 822 personal-name form
            "\"Acme, Inc\" <noreply@acme.com>", // comma in a *quoted* display name is a single sendable address
            "\"a@b\"@x.com", // quoted local part containing '@' is one valid address (send-path accepts it)
        }
    )
    void should_accept_addresses_the_send_path_can_send(String value) {
        assertThat(cut.isValid(value, constraintValidatorContext)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "not-an-email", // no domain
            "a..b@x.com", // consecutive dots — old regex wrongly accepted
            ".a@x.com", // leading dot in local part — old regex wrongly accepted
            "Acme, Inc <noreply@acme.com>", // unquoted comma in display name — send path parses it as a list and fails
            "Acme; Inc <a@localhost>", // unquoted semicolon in display name — likewise unsendable
            "jane@work <Jane>", // the single '@' sits outside the brackets, leaving no address to send
        }
    )
    void should_reject_an_unsendable_value_with_the_default_message(String value) {
        assertThat(cut.isValid(value, constraintValidatorContext)).isFalse();
        // A malformed single address keeps the annotation's default message; only the multi-address case
        // overrides it (see below).
        verify(constraintValidatorContext, never()).buildConstraintViolationWithTemplate(any());
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "alice@example.com, bob@example.com", // a plain address list — both valid, but there are two
            "Alice <alice@example.com>, Bob <bob@example.com>", // comma-joined personal-name list
            "noreply@localhost\nBcc: attacker@evil.com", // header-injection attempt reads as multiple addresses
        }
    )
    void should_reject_a_multi_address_value_with_a_distinct_message(String value) {
        var messageCaptor = ArgumentCaptor.forClass(String.class);
        when(constraintValidatorContext.buildConstraintViolationWithTemplate(messageCaptor.capture())).thenReturn(
            constraintViolationBuilder
        );

        assertThat(cut.isValid(value, constraintValidatorContext)).isFalse();

        verify(constraintValidatorContext).disableDefaultConstraintViolation();
        verify(constraintViolationBuilder).addConstraintViolation();
        assertThat(messageCaptor.getValue()).isEqualTo("must be a single sender address");
    }

    // --- annotation wiring: the messages actually resolve through a real Validator, not just the mock ---

    @Test
    void should_surface_the_default_message_end_to_end_for_a_malformed_address() {
        var violations = beanValidator().validate(new Holder("not-an-email"));
        assertThat(violations)
            .singleElement()
            .extracting(ConstraintViolation::getMessage)
            .isEqualTo("must be a valid email address, optionally with a display name (e.g. \"Name <user@example.com>\")");
    }

    @Test
    void should_surface_the_multi_address_message_end_to_end() {
        var violations = beanValidator().validate(new Holder("alice@example.com, bob@example.com"));
        assertThat(violations).singleElement().extracting(ConstraintViolation::getMessage).isEqualTo("must be a single sender address");
    }

    private static Validator beanValidator() {
        return Validation.buildDefaultValidatorFactory().getValidator();
    }

    private static final class Holder {

        @ValidSenderAddress
        final String from;

        Holder(String from) {
            this.from = from;
        }
    }
}
