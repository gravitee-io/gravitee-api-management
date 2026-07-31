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
package io.gravitee.rest.api.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.gravitee.rest.api.model.settings.BrandedSenderConfig;
import io.gravitee.rest.api.model.settings.BrandedSenders;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mock.env.MockEnvironment;

/**
 * Covers the reconstruction of the {@code email.branded_senders} JSON parameter value from the native
 * yaml list of objects an on-prem operator writes in {@code gravitee.yml}.
 *
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BrandedSendersEnvironmentReaderTest {

    @Mock
    private ConfigurableEnvironment environment;

    private BrandedSendersEnvironmentReader reader;

    private void stub(String key, String value) {
        when(environment.getProperty(key)).thenReturn(value);
    }

    private BrandedSendersEnvironmentReader reader() {
        return new BrandedSendersEnvironmentReader(environment);
    }

    @Test
    void should_read_a_single_configuration_from_indexed_yaml_properties() {
        reader = reader();
        stub("email.branded_senders[0].domains[0]", "example.com");
        stub("email.branded_senders[0].from", "noreply@example.com");
        stub("email.branded_senders[0].subject", "[Example] %s");

        var result = reader.read();

        assertThat(result).isPresent();
        assertThat(BrandedSenders.parse(result.get())).containsExactly(
            BrandedSenderConfig.builder().domains(List.of("example.com")).from("noreply@example.com").subject("[Example] %s").build()
        );
    }

    @Test
    void should_read_multiple_configurations_each_with_multiple_domains() {
        reader = reader();
        stub("email.branded_senders[0].domains[0]", "example.com");
        stub("email.branded_senders[0].domains[1]", "example.org");
        stub("email.branded_senders[0].from", "noreply@example.com");
        stub("email.branded_senders[0].subject", "[Example] %s");
        stub("email.branded_senders[1].domains[0]", "sample.net");
        stub("email.branded_senders[1].from", "noreply@sample.net");

        var result = reader.read();

        assertThat(result).isPresent();
        assertThat(BrandedSenders.parse(result.get())).containsExactly(
            BrandedSenderConfig.builder()
                .domains(List.of("example.com", "example.org"))
                .from("noreply@example.com")
                .subject("[Example] %s")
                .build(),
            BrandedSenderConfig.builder().domains(List.of("sample.net")).from("noreply@sample.net").build()
        );
    }

    @Test
    void should_return_empty_when_no_configuration_is_declared() {
        reader = reader();

        assertThat(reader.read()).isEmpty();
    }

    @Test
    void should_escape_semicolons_so_the_parameter_read_path_does_not_truncate() {
        reader = reader();
        stub("email.branded_senders[0].domains[0]", "example.com");
        stub("email.branded_senders[0].from", "noreply@example.com");
        stub("email.branded_senders[0].subject", "[a; b] %s");

        var result = reader.read();

        assertThat(result).isPresent();
        // The value is stored as a single parameter whose read path splits on ';'; the serialiser must leave no
        // literal ';' in the value, and a round-trip must still recover the original subject.
        assertThat(result.get()).doesNotContain(";");
        assertThat(BrandedSenders.parse(result.get()).get(0).getSubject()).isEqualTo("[a; b] %s");
    }

    @Test
    void should_ignore_an_invalid_configuration_rather_than_fail() {
        reader = reader();
        stub("email.branded_senders[0].domains[0]", "example.com");
        // A CR/LF in a header-bound field is rejected by the serialiser (header-injection defence); the reader
        // logs and returns empty so a broken on-prem config cannot break the whole settings response.
        stub("email.branded_senders[0].from", "noreply@example.com\r\nBcc: evil@example.org");

        assertThat(reader.read()).isEmpty();
        // ...and it must report "not configured" so the console field stays editable rather than locked on a
        // value that is not actually applied.
        assertThat(reader.isConfigured()).isFalse();
    }

    @Test
    void should_read_the_flat_json_string_form() {
        reader = reader();
        stub("email.branded_senders", "[{\"domains\":[\"example.com\"],\"from\":\"noreply@example.com\",\"subject\":\"[Example] %s\"}]");

        var result = reader.read();

        assertThat(result).isPresent();
        assertThat(BrandedSenders.parse(result.get())).containsExactly(
            BrandedSenderConfig.builder().domains(List.of("example.com")).from("noreply@example.com").subject("[Example] %s").build()
        );
        assertThat(reader.isConfigured()).isTrue();
    }

    @Test
    void should_escape_semicolons_in_the_flat_form_too() {
        reader = reader();
        // AC2 promises the flat / env-var form works too; it must get the same ';'-escaping as the native path so a
        // ';' in a subject does not truncate the value at the parameter read path.
        stub("email.branded_senders", "[{\"domains\":[\"example.com\"],\"from\":\"noreply@example.com\",\"subject\":\"[a; b] %s\"}]");

        var result = reader.read();

        assertThat(result).isPresent();
        assertThat(result.get()).doesNotContain(";");
        assertThat(BrandedSenders.parse(result.get()).get(0).getSubject()).isEqualTo("[a; b] %s");
    }

    @Test
    void should_ignore_an_invalid_flat_form() {
        reader = reader();
        // CR/LF smuggled into the flat form is caught by the same serialiser validation and left editable + logged.
        stub(
            "email.branded_senders",
            "[{\"domains\":[\"example.com\"],\"from\":\"a@example.com\\r\\nBcc: evil@example.org\",\"subject\":\"[X] %s\"}]"
        );

        assertThat(reader.read()).isEmpty();
        assertThat(reader.isConfigured()).isFalse();
    }

    @Test
    void should_read_a_scalar_domains_value() {
        reader = reader();
        // A common yaml habit: `domains: example.com` (scalar) rather than a list; it binds to the bare key, not
        // domains[0], and must not be silently dropped.
        stub("email.branded_senders[0].domains", "example.com");
        stub("email.branded_senders[0].from", "noreply@example.com");

        var result = reader.read();

        assertThat(result).isPresent();
        assertThat(BrandedSenders.parse(result.get())).containsExactly(
            BrandedSenderConfig.builder().domains(List.of("example.com")).from("noreply@example.com").build()
        );
    }

    @Test
    void should_keep_later_entries_when_a_middle_entry_is_incomplete() {
        reader = reader();
        stub("email.branded_senders[0].domains[0]", "first.com");
        stub("email.branded_senders[0].from", "noreply@first.com");
        // An incomplete middle entry (only a subject) must not terminate the loop and drop the valid entry after it.
        stub("email.branded_senders[1].subject", "[Incomplete] %s");
        stub("email.branded_senders[2].domains[0]", "third.com");
        stub("email.branded_senders[2].from", "noreply@third.com");

        var result = reader.read();

        assertThat(result).isPresent();
        var configurations = BrandedSenders.parse(result.get());
        assertThat(configurations).hasSize(3);
        assertThat(configurations).anySatisfy(config -> assertThat(config.getFrom()).isEqualTo("noreply@third.com"));
    }

    @Test
    void should_report_configured_when_a_native_list_is_present() {
        reader = reader();
        stub("email.branded_senders[0].from", "noreply@example.com");

        assertThat(reader.isConfigured()).isTrue();
    }

    @Test
    void should_report_configured_from_domains_only() {
        reader = reader();
        stub("email.branded_senders[0].domains[0]", "example.com");

        assertThat(reader.isConfigured()).isTrue();
    }

    @Test
    void should_not_report_configured_when_absent() {
        reader = reader();

        assertThat(reader.isConfigured()).isFalse();
    }

    @Test
    void should_parse_real_yaml_the_way_spring_flattens_it() {
        // Guards against the hand-stubbed indexed-key shape drifting from Spring's real yaml-list flattening: load
        // actual yaml through the same SnakeYAML mechanism gravitee-node uses, then read it back end to end.
        String yaml = """
            email:
              branded_senders:
                - domains:
                    - example.com
                    - example.org
                  from: noreply@example.com
                  subject: "[Example] %s"
                - domains: [sample.net]
                  from: noreply@sample.net
            """;
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(new ByteArrayResource(yaml.getBytes(StandardCharsets.UTF_8)));
        MockEnvironment realEnvironment = new MockEnvironment();
        factory.getObject().forEach((key, value) -> realEnvironment.setProperty(key.toString(), value.toString()));

        // The reader's assumption made explicit: Spring flattens the list to indexed properties.
        assertThat(realEnvironment.getProperty("email.branded_senders[0].from")).isEqualTo("noreply@example.com");
        assertThat(realEnvironment.getProperty("email.branded_senders[0].domains[1]")).isEqualTo("example.org");

        var result = new BrandedSendersEnvironmentReader(realEnvironment).read();

        assertThat(result).isPresent();
        assertThat(BrandedSenders.parse(result.get())).containsExactly(
            BrandedSenderConfig.builder()
                .domains(List.of("example.com", "example.org"))
                .from("noreply@example.com")
                .subject("[Example] %s")
                .build(),
            BrandedSenderConfig.builder().domains(List.of("sample.net")).from("noreply@sample.net").build()
        );
    }
}
