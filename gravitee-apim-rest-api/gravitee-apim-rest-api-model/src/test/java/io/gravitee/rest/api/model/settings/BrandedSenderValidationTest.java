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
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Bean-validation coverage for the branded-sender constraints, including the {@code @Valid} cascade
 * that enforces them end-to-end through the settings save graph
 * ({@code settings entity -> email -> branded senders}).
 *
 * @author GraviteeSource Team
 */
class BrandedSenderValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private static BrandedSenderConfig.BrandedSenderConfigBuilder validConfig() {
        return BrandedSenderConfig.builder()
            .domains(List.of("graviteecustomer.com"))
            .from("noreply@graviteecustomer.com")
            .subject("[Gravitee Customer] %s");
    }

    // --- BrandedSenderConfig ---

    @Test
    void should_accept_a_valid_configuration() {
        assertThat(validator.validate(validConfig().build())).isEmpty();
    }

    @Test
    void should_accept_from_with_display_name() {
        assertThat(validator.validate(validConfig().from("Gravitee Customer <noreply@graviteecustomer.com>").build())).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = { "not-an-email", "user@a..b.com", "user@.example.com" })
    void should_reject_malformed_from(String from) {
        assertThat(validator.validate(validConfig().from(from).build())).isNotEmpty();
    }

    @Test
    void should_reject_blank_from() {
        assertThat(validator.validate(validConfig().from("   ").build())).isNotEmpty();
    }

    @Test
    void should_reject_empty_domains() {
        assertThat(validator.validate(validConfig().domains(List.of()).build())).isNotEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = { "not a domain", "-example.com", "example-.com", "localhost", "example.a", "example.123" })
    void should_reject_invalid_domain(String domain) {
        assertThat(validator.validate(validConfig().domains(List.of(domain)).build())).isNotEmpty();
    }

    @Test
    void should_reject_blank_domain_entry() {
        assertThat(validator.validate(validConfig().domains(Arrays.asList("graviteecustomer.com", "   ")).build())).isNotEmpty();
    }

    @Test
    void should_reject_duplicate_domains_case_insensitively() {
        var config = validConfig().domains(List.of("graviteecustomer.com", "GraviteeCustomer.com")).build();
        assertThat(validator.validate(config)).anyMatch(v -> v.getMessage().contains("duplicate"));
    }

    @Test
    void should_reject_null_domain_entry() {
        assertThat(validator.validate(validConfig().domains(Arrays.asList("graviteecustomer.com", null)).build())).isNotEmpty();
    }

    @Test
    void should_reject_display_name_with_invalid_inner_address() {
        assertThat(validator.validate(validConfig().from("Gravitee <not-an-email>").build())).isNotEmpty();
    }

    @Test
    void should_reject_from_with_an_address_before_the_display_name() {
        // A multi-address value whose trailing "Name <addr>" pair is itself a single valid address must still
        // be rejected: the greedy display-name strip used to accept it, but new InternetAddress(from) rejects
        // the whole string at send time — save must not accept a From that delivery cannot send.
        assertThat(validator.validate(validConfig().from("alice@example.com, Team <noreply@graviteecustomer.com>").build())).isNotEmpty();
    }

    @Test
    void should_reject_subject_exceeding_max_length() {
        assertThat(validator.validate(validConfig().subject("x".repeat(256)).build())).isNotEmpty();
    }

    // --- @Valid cascade through the real save graph (settings entity -> email -> branded senders) ---

    @Test
    void should_cascade_from_settings_entity_into_branded_senders() {
        var settings = new PortalSettingsEntity();
        settings.getEmail().setBrandedSenders(List.of(validConfig().from("not-an-email").build()));

        assertThat(validator.validate(settings)).anyMatch(v -> v.getPropertyPath().toString().equals("email.brandedSenders[0].from"));
    }

    @Test
    void should_cascade_into_branded_senders_when_validating_email() {
        var email = new Email();
        email.setBrandedSenders(List.of(validConfig().from("not-an-email").build()));

        assertThat(validator.validate(email)).anyMatch(v -> v.getPropertyPath().toString().equals("brandedSenders[0].from"));
    }

    @Test
    void should_accept_whitespace_domain_normalized_before_cascade() {
        var email = new Email();
        email.setBrandedSenders(List.of(validConfig().domains(List.of("  GraviteeCustomer.COM  ")).build()));

        // setBrandedSenders trims + lower-cases at the write boundary, so the @Pattern cascade sees a
        // clean value: surrounding whitespace is normalised, not rejected.
        assertThat(validator.validate(email)).isEmpty();
    }
}
