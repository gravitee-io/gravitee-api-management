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
}
