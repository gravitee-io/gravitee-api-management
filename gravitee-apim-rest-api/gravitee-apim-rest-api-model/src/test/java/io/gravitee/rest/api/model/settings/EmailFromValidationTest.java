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
package io.gravitee.rest.api.model.settings;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Bean-validation coverage for the default sender address ({@code email.from}), which previously accepted any
 * string and only failed at send time with an HTTP 500. Mirrors {@link BrandedSenderValidationTest}, which
 * covers the same constraint on a branded sender's {@code from}.
 *
 * @author GraviteeSource Team
 */
class EmailFromValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private static Email emailWithFrom(String from) {
        var email = new Email();
        email.setFrom(from);
        return email;
    }

    @Test
    void should_accept_a_valid_from() {
        assertThat(validator.validate(emailWithFrom("noreply@gravitee.io"))).isEmpty();
    }

    @Test
    void should_accept_from_with_display_name() {
        assertThat(validator.validate(emailWithFrom("Gravitee <noreply@gravitee.io>"))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = { "not-an-email", "foo@", "user@a..b.com", "user@.example.com" })
    void should_reject_malformed_from(String from) {
        assertThat(validator.validate(emailWithFrom(from))).anyMatch(v -> v.getPropertyPath().toString().equals("from"));
    }

    @Test
    void should_reject_multi_address_from() {
        // The send path resolves a single address, so a comma-separated list must not survive save — accepting
        // it is what let a From through that delivery then rejected.
        assertThat(validator.validate(emailWithFrom("noreply@gravitee.io, other@gravitee.io"))).anyMatch(v ->
            v.getPropertyPath().toString().equals("from")
        );
    }

    @Test
    void should_report_a_readable_message_for_a_malformed_from() {
        assertThat(validator.validate(emailWithFrom("not-an-email"))).anyMatch(v ->
            v.getMessage().equals("must be a valid email address, optionally with a display name (e.g. \"Name <user@example.com>\")")
        );
    }

    // --- an absent sender stays legal ---

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = { "", "   " })
    void should_normalize_an_absent_from_to_null(String from) {
        // The default sender is optional: leaving it unset falls back to the Key.EMAIL_FROM default, so this
        // must stay valid — it is what a @NotBlank here would break. Blank is collapsed to null rather than
        // merely accepted, because an empty value that reaches storage does NOT fall back: the send path reads
        // "" and hands it to new InternetAddress(""), which throws — the same 500 this ticket is about.
        var email = emailWithFrom(from);

        assertThat(email.getFrom()).isNull();
        assertThat(validator.validate(email)).isEmpty();
    }

    @Test
    void should_trim_surrounding_whitespace_on_write() {
        assertThat(emailWithFrom("  noreply@gravitee.io  ").getFrom()).isEqualTo("noreply@gravitee.io");
    }

    // --- @Valid cascade through the real save graph (settings entity -> email -> from) ---

    @Test
    void should_cascade_from_settings_entity_into_from() {
        var settings = new ConsoleSettingsEntity();
        settings.getEmail().setFrom("not-an-email");

        assertThat(validator.validate(settings)).anyMatch(v -> v.getPropertyPath().toString().equals("email.from"));
    }
}
