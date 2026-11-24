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
package io.gravitee.apim.core.portal.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalHostTest {

    @ParameterizedTest
    @CsvSource(
        {
            "https://Portal.Example.COM/next, portal.example.com",
            "https://portal.example.com:443/next, portal.example.com",
            "http://portal.example.com:80, portal.example.com",
            "https://portal.example.com:8443/a/b, portal.example.com:8443",
            "http://portal.example.com:443, portal.example.com:443",
            "http://partners.localhost:4100/next, partners.localhost:4100",
            "http://localhost:4100, localhost:4100",
            "http://127.0.0.1:4100/next, 127.0.0.1:4100",
        }
    )
    @SneakyThrows
    void should_keep_only_the_lowercase_host_and_a_non_default_port_of_a_url(String url, String expected) {
        assertThat(PortalHost.of(new URI(url).toURL()).value()).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource(
        {
            "Portal.Example.com, portal.example.com",
            "partners.localhost:4100, partners.localhost:4100",
            "127.0.0.1:4100, 127.0.0.1:4100",
            "'[::1]:4100', '[::1]:4100'",
        }
    )
    void should_parse_the_host_a_browser_reports(String raw, String expected) {
        assertThat(PortalHost.parse(raw)).hasValueSatisfying(host -> assertThat(host.value()).isEqualTo(expected));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(
        strings = {
            "",
            "   ",
            "https://portal.example.com",
            "portal.example.com/next",
            "portal.example.com?a=b",
            "user@portal.example.com",
            "portal example.com",
            "portal.example.com:",
            "portal.example.com:99999",
            "portal.example.com:abc",
        }
    )
    void should_refuse_anything_that_is_not_a_host_and_port(String raw) {
        assertThat(PortalHost.parse(raw)).isEmpty();
    }

    @Test
    void should_refuse_a_host_longer_than_the_stored_column() {
        assertThat(PortalHost.parse("a".repeat(257))).isEmpty();
    }
}
