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
package io.gravitee.rest.api.service.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.settings.BrandedSenderConfig;
import io.gravitee.rest.api.model.settings.Email;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.function.Predicate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * @author GraviteeSource Team
 */
class SenderSettingsValidatorTest {

    private static final String INVALID_FROM_MESSAGE =
        "must be a valid email address, optionally with a display name (e.g. \"Name <user@example.com>\")";
    private static final Predicate<Key> NOTHING_SKIPPED = key -> false;

    private final SenderSettingsValidator cut = new SenderSettingsValidator();

    @AfterEach
    void tearDown() {
        cut.destroy();
    }

    @Test
    void should_accept_an_unchanged_invalid_from() {
        assertThatCode(() ->
            cut.validateChanges(email("Example, Inc <noreply@example.com>"), email("Example, Inc <noreply@example.com>"), NOTHING_SKIPPED)
        ).doesNotThrowAnyException();
    }

    @Test
    void should_reject_a_changed_invalid_from() {
        var e = catchThrowableOfType(ConstraintViolationException.class, () ->
            cut.validateChanges(email("not-an-email"), email("noreply@example.com"), NOTHING_SKIPPED)
        );

        assertThat(e.getConstraintViolations()).extracting(ConstraintViolation::getMessage).containsExactly(INVALID_FROM_MESSAGE);
    }

    @Test
    void should_reject_a_changed_from_with_two_addresses_with_the_dedicated_message() {
        var e = catchThrowableOfType(ConstraintViolationException.class, () ->
            cut.validateChanges(email("a@example.com, b@example.com"), email("noreply@example.com"), NOTHING_SKIPPED)
        );

        assertThat(e.getConstraintViolations())
            .extracting(ConstraintViolation::getMessage)
            .containsExactly("must be a single sender address");
    }

    @Test
    void should_accept_a_changed_valid_from() {
        assertThatCode(() ->
            cut.validateChanges(email("Example <noreply@example.com>"), email("noreply@example.com"), NOTHING_SKIPPED)
        ).doesNotThrowAnyException();
    }

    @Test
    void should_not_check_unchanged_branded_senders_when_only_the_from_changed() {
        List<BrandedSenderConfig> legacy = List.of(brandedSender("legacy.example.com", "Legacy, Inc <noreply@example.com>"));
        Email submitted = email(legacy);
        submitted.setFrom("Example <noreply@example.com>");

        assertThatCode(() -> cut.validateChanges(submitted, email(legacy), NOTHING_SKIPPED)).doesNotThrowAnyException();
    }

    @Test
    void should_not_check_a_skipped_from() {
        assertThatCode(() ->
            cut.validateChanges(email("\"user@my.domain\""), email("noreply@example.com"), key -> key == Key.EMAIL_FROM)
        ).doesNotThrowAnyException();
    }

    @Test
    void should_accept_an_unchanged_invalid_branded_sender_next_to_a_new_valid_one() {
        BrandedSenderConfig legacy = brandedSender("legacy.example.com", "Legacy, Inc <noreply@example.com>");

        assertThatCode(() ->
            cut.validateChanges(
                email(List.of(legacy, brandedSender("example.org", "noreply@example.org"))),
                email(List.of(legacy)),
                NOTHING_SKIPPED
            )
        ).doesNotThrowAnyException();
    }

    @Test
    void should_check_a_changed_branded_sender_as_a_whole_entry() {
        BrandedSenderConfig legacy = brandedSender("legacy.example.com", "Legacy, Inc <noreply@example.com>");
        BrandedSenderConfig legacyWithExtraDomain = brandedSender(List.of("legacy.example.com", "other.example.com"), legacy.getFrom());

        var e = catchThrowableOfType(ConstraintViolationException.class, () ->
            cut.validateChanges(email(List.of(legacyWithExtraDomain)), email(List.of(legacy)), NOTHING_SKIPPED)
        );

        assertThat(e.getConstraintViolations()).extracting(ConstraintViolation::getMessage).containsExactly(INVALID_FROM_MESSAGE);
    }

    @Test
    void should_reject_a_new_branded_sender_with_an_invalid_domain() {
        var e = catchThrowableOfType(ConstraintViolationException.class, () ->
            cut.validateChanges(email(List.of(brandedSender("not a domain", "noreply@example.org"))), email(List.of()), NOTHING_SKIPPED)
        );

        assertThat(e.getConstraintViolations()).extracting(ConstraintViolation::getMessage).contains("must be a valid domain name");
    }

    @Test
    void should_not_check_skipped_branded_senders() {
        assertThatCode(() ->
            cut.validateChanges(
                email(List.of(brandedSender("example.com", "Example, Inc <noreply@example.com>"))),
                email(List.of()),
                key -> key == Key.EMAIL_BRANDED_SENDERS
            )
        ).doesNotThrowAnyException();
    }

    private static Email email(String from) {
        Email email = new Email();
        email.setFrom(from);
        return email;
    }

    private static Email email(List<BrandedSenderConfig> brandedSenders) {
        Email email = new Email();
        email.setBrandedSenders(brandedSenders);
        return email;
    }

    private static BrandedSenderConfig brandedSender(String domain, String from) {
        return brandedSender(List.of(domain), from);
    }

    private static BrandedSenderConfig brandedSender(List<String> domains, String from) {
        return BrandedSenderConfig.builder().domains(domains).from(from).build();
    }
}
