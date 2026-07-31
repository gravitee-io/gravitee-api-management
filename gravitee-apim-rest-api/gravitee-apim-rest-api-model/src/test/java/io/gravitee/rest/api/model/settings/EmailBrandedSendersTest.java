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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/**
 * Covers the JSON serialise / deserialise seam between the typed {@code brandedSenders} list and
 * the raw {@code email.branded_senders} parameter value.
 *
 * @author GraviteeSource Team
 */
class EmailBrandedSendersTest {

    @Test
    void should_default_to_empty_list() {
        assertThat(new Email().getBrandedSenders()).isEmpty();
    }

    @Test
    void should_normalize_domains_to_trimmed_lowercase_on_write() {
        var email = new Email();
        var config = BrandedSenderConfig.builder()
            .domains(List.of("  GraviteeCustomer.COM  "))
            .from("noreply@graviteecustomer.com")
            .subject("[Gravitee] %s")
            .build();

        email.setBrandedSenders(List.of(config));

        assertThat(email.getBrandedSenders().get(0).getDomains()).containsExactly("graviteecustomer.com");
    }

    @Test
    void should_handle_configuration_with_null_domains_on_write() {
        var email = new Email();
        var config = BrandedSenderConfig.builder().domains(null).from("noreply@graviteecustomer.com").subject("[Gravitee] %s").build();

        // normalizeDomains must guard a null domains list — otherwise write() NPEs (HTTP 500 on save).
        email.setBrandedSenders(List.of(config));

        assertThat(email.getBrandedSenders()).containsExactly(config);
    }

    @Test
    void should_round_trip_single_configuration() {
        var email = new Email();
        var config = BrandedSenderConfig.builder()
            .domains(List.of("graviteecustomer.com"))
            .from("noreply.integration@graviteecustomer.com")
            .subject("[Gravitee Customer] %s")
            .build();

        email.setBrandedSenders(List.of(config));

        assertThat(email.getBrandedSenders()).containsExactly(config);
    }

    @Test
    void should_round_trip_multiple_configurations() {
        var email = new Email();
        var customer = BrandedSenderConfig.builder()
            .domains(List.of("graviteecustomer.com"))
            .from("noreply@graviteecustomer.com")
            .subject("[Gravitee Customer] %s")
            .build();
        var partner = BrandedSenderConfig.builder()
            .domains(List.of("graviteepartner.com", "graviteepartner.co.uk"))
            .from("noreply@graviteepartner.com")
            .subject("[Gravitee Partner] %s")
            .build();

        email.setBrandedSenders(List.of(customer, partner));

        assertThat(email.getBrandedSenders()).containsExactly(customer, partner);
    }

    @Test
    void should_return_empty_list_when_set_with_null() {
        var email = new Email();
        email.setBrandedSenders(null);
        assertThat(email.getBrandedSenders()).isEmpty();
    }

    @Test
    void should_return_empty_list_when_stored_value_is_json_null() {
        var email = new Email();
        email.setBrandedSendersRaw("null");
        assertThat(email.getBrandedSenders()).isEmpty();
    }

    @Test
    void should_return_empty_list_when_raw_value_is_blank() {
        var email = new Email();
        email.setBrandedSendersRaw("   ");
        assertThat(email.getBrandedSenders()).isEmpty();
    }

    @Test
    void should_return_empty_list_when_stored_json_is_malformed() {
        var email = new Email();
        email.setBrandedSendersRaw("not-json");

        // A corrupt stored value (hand-edited row, partial migration) must not break the settings
        // response — read degrades to an empty list rather than throwing.
        assertThat(email.getBrandedSenders()).isEmpty();
    }

    @Test
    void should_survive_parameter_service_semicolon_split_when_value_contains_semicolon() {
        var email = new Email();
        var config = BrandedSenderConfig.builder()
            .domains(List.of("graviteecustomer.com"))
            .from("noreply@graviteecustomer.com")
            .subject("[Gravitee]; verify %s") // free-text semicolon
            .build();

        email.setBrandedSenders(List.of(config));

        // ParameterService persists the value as one string but splits on ';' on read, keeping only the
        // first fragment. The stored form must therefore contain no literal ';'.
        var stored = email.getBrandedSendersRaw();
        assertThat(stored).doesNotContain(";");

        // Simulate ParameterServiceImpl.splitValue + ConfigServiceImpl.getFirstValueOrDefault, then reload.
        var afterParameterServiceRoundTrip = stored.split(";", -1)[0];
        assertThat(afterParameterServiceRoundTrip).isEqualTo(stored);

        var reloaded = new Email();
        reloaded.setBrandedSendersRaw(afterParameterServiceRoundTrip);
        assertThat(reloaded.getBrandedSenders()).containsExactly(config);
    }

    @Test
    void should_throw_when_configuration_exceeds_max_length() {
        var email = new Email();
        var oversized = IntStream.range(0, 200)
            .mapToObj(i ->
                BrandedSenderConfig.builder()
                    .domains(List.of("domain" + i + ".example.com"))
                    .from("noreply@domain" + i + ".example.com")
                    .subject("[Brand" + i + "] %s")
                    .build()
            )
            .toList();

        assertThatThrownBy(() -> email.setBrandedSenders(oversized))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("maximum supported size");
    }

    @Test
    void should_reject_line_breaks_in_from() {
        var email = new Email();
        var config = BrandedSenderConfig.builder()
            .domains(List.of("graviteecustomer.com"))
            .from("noreply@graviteecustomer.com\r\nBcc: evil@graviteepartner.com")
            .subject("[Gravitee] %s")
            .build();

        assertThatThrownBy(() -> email.setBrandedSenders(List.of(config)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("line breaks");
    }

    @Test
    void should_reject_line_breaks_in_subject() {
        var email = new Email();
        var config = BrandedSenderConfig.builder()
            .domains(List.of("graviteecustomer.com"))
            .from("noreply@graviteecustomer.com")
            .subject("[Gravitee] %s\nInjected-Header: x")
            .build();

        assertThatThrownBy(() -> email.setBrandedSenders(List.of(config)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("line breaks");
    }

    @Test
    void should_reject_line_breaks_in_domain() {
        var email = new Email();
        var config = BrandedSenderConfig.builder()
            .domains(List.of("graviteecustomer.com\r\nevil.com"))
            .from("noreply@graviteecustomer.com")
            .subject("[Gravitee] %s")
            .build();

        assertThatThrownBy(() -> email.setBrandedSenders(List.of(config)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("line breaks");
    }

    @Test
    void should_reject_null_entry_in_list() {
        var email = new Email();
        var configs = new ArrayList<BrandedSenderConfig>();
        configs.add(
            BrandedSenderConfig.builder()
                .domains(List.of("graviteecustomer.com"))
                .from("noreply@graviteecustomer.com")
                .subject("[Gravitee] %s")
                .build()
        );
        configs.add(null);

        assertThatThrownBy(() -> email.setBrandedSenders(configs))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("null entries");
    }

    @Test
    void should_resolve_configuration_matching_recipient_domain() {
        var config = BrandedSenderConfig.builder()
            .domains(List.of("example.com"))
            .from("noreply@example.com")
            .subject("[Example] %s")
            .build();
        var raw = BrandedSenders.write(List.of(config));

        assertThat(BrandedSenders.resolve(raw, "developer@example.com")).hasValueSatisfying(matched -> {
            assertThat(matched.getFrom()).isEqualTo("noreply@example.com");
            assertThat(matched.getSubject()).isEqualTo("[Example] %s");
        });
    }

    @Test
    void should_match_recipient_domain_case_insensitively() {
        var raw = BrandedSenders.write(
            List.of(BrandedSenderConfig.builder().domains(List.of("example.com")).from("noreply@example.com").build())
        );

        assertThat(BrandedSenders.resolve(raw, "Developer@EXAMPLE.COM")).hasValueSatisfying(matched ->
            assertThat(matched.getFrom()).isEqualTo("noreply@example.com")
        );
    }

    @Test
    void should_return_empty_for_a_multi_address_recipient_string() {
        var raw = BrandedSenders.write(
            List.of(BrandedSenderConfig.builder().domains(List.of("example.org")).from("noreply@example.org").build())
        );

        // "a@example.com, b@example.org" must not brand both recipients for example.org's tenant.
        assertThat(BrandedSenders.resolve(raw, "alice@example.com, bob@example.org")).isEmpty();
    }

    @Test
    void should_return_empty_for_a_multi_address_personal_name_recipient_string() {
        var raw = BrandedSenders.write(
            List.of(BrandedSenderConfig.builder().domains(List.of("example.org")).from("noreply@example.org").build())
        );

        // Comma-joined personal-name list where only the LAST address's domain (example.org) matches the
        // config; it must not brand the whole recipient group by that trailing recipient's domain.
        assertThat(BrandedSenders.resolve(raw, "Jane Developer <jane@example.com>, John Support <john@example.org>")).isEmpty();
    }

    @Test
    void should_return_empty_when_a_parsed_configuration_has_null_domains() {
        // A hand-edited / legacy stored value can omit the "domains" key entirely; parse() does not run
        // write()'s validation, so such a config reaches the match path with a null domains list. It must be
        // skipped (never NPE) rather than matched.
        assertThat(BrandedSenders.resolve("[{\"from\":\"noreply@example.com\"}]", "developer@example.com")).isEmpty();
    }

    @Test
    void should_resolve_from_a_personal_name_recipient() {
        var raw = BrandedSenders.write(
            List.of(BrandedSenderConfig.builder().domains(List.of("example.com")).from("noreply@example.com").build())
        );

        assertThat(BrandedSenders.resolve(raw, "Jane Developer <jane@example.com>")).hasValueSatisfying(matched ->
            assertThat(matched.getFrom()).isEqualTo("noreply@example.com")
        );
    }

    @Test
    void should_return_empty_when_no_configuration_matches_the_recipient_domain() {
        var raw = BrandedSenders.write(
            List.of(BrandedSenderConfig.builder().domains(List.of("example.com")).from("noreply@example.com").build())
        );

        assertThat(BrandedSenders.resolve(raw, "developer@example.org")).isEmpty();
    }

    @Test
    void should_return_the_first_configuration_when_several_list_the_same_domain() {
        var raw = BrandedSenders.write(
            List.of(
                BrandedSenderConfig.builder().domains(List.of("example.com")).from("first@example.com").build(),
                BrandedSenderConfig.builder().domains(List.of("example.com")).from("second@example.com").build()
            )
        );

        assertThat(BrandedSenders.resolve(raw, "developer@example.com")).hasValueSatisfying(matched ->
            assertThat(matched.getFrom()).isEqualTo("first@example.com")
        );
    }

    @Test
    void should_return_empty_for_a_blank_or_malformed_value() {
        assertThat(BrandedSenders.resolve(null, "developer@example.com")).isEmpty();
        assertThat(BrandedSenders.resolve("   ", "developer@example.com")).isEmpty();
        assertThat(BrandedSenders.resolve("not-json", "developer@example.com")).isEmpty();
    }

    @Test
    void should_return_empty_when_the_recipient_has_no_domain() {
        var raw = BrandedSenders.write(
            List.of(BrandedSenderConfig.builder().domains(List.of("example.com")).from("noreply@example.com").build())
        );

        assertThat(BrandedSenders.resolve(raw, null)).isEmpty();
        assertThat(BrandedSenders.resolve(raw, "not-an-email")).isEmpty();
    }

    @Test
    void should_accept_a_subject_prefix_with_a_literal_percent_on_write() {
        var email = new Email();
        // The subject is substituted literally at send time (not String.format), so a stray '%' is safe to store.
        var config = BrandedSenderConfig.builder()
            .domains(List.of("example.com"))
            .from("noreply@example.com")
            .subject("100% off %s")
            .build();

        email.setBrandedSenders(List.of(config));

        assertThat(email.getBrandedSenders().get(0).getSubject()).isEqualTo("100% off %s");
    }

    @Test
    void should_accept_a_subject_prefix_without_a_placeholder_on_write() {
        var email = new Email();
        var config = BrandedSenderConfig.builder().domains(List.of("example.com")).from("noreply@example.com").subject("[Example]").build();

        email.setBrandedSenders(List.of(config));

        assertThat(email.getBrandedSenders().get(0).getSubject()).isEqualTo("[Example]");
    }
}
