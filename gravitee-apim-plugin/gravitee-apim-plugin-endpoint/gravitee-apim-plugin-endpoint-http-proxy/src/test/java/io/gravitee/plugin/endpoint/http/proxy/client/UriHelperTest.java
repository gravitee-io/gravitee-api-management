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
package io.gravitee.plugin.endpoint.http.proxy.client;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.common.util.LinkedMultiValueMap;
import io.gravitee.gateway.reactive.http.vertx.client.VertxHttpClient;
import io.vertx.core.http.RequestOptions;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class UriHelperTest {

    @Test
    void should_configure_absolute_uri() {
        final RequestOptions requestOptions = new RequestOptions();
        final LinkedMultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();

        parameters.put("foo", List.of("bar"));
        UriHelper.configureAbsoluteUri(requestOptions, "http://api.gravitee.io/echo?foo=bar1&hello=gravitee", parameters);

        assertThat(requestOptions.getURI()).isEqualTo("/echo?foo=bar1&hello=gravitee&foo=bar");
        assertThat(requestOptions.getHost()).isEqualTo("api.gravitee.io");
        assertThat(requestOptions.getPort()).isEqualTo(80);
        assertThat(requestOptions.isSsl()).isFalse();
    }

    @Test
    void should_configure_absolute_uri_with_ssl_enabled() {
        final RequestOptions requestOptions = new RequestOptions();
        final LinkedMultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();

        parameters.put("foo", List.of("bar"));
        UriHelper.configureAbsoluteUri(requestOptions, "https://api.gravitee.io/echo?foo=bar1&hello=gravitee", parameters);

        assertThat(requestOptions.getURI()).isEqualTo("/echo?foo=bar1&hello=gravitee&foo=bar");
        assertThat(requestOptions.getHost()).isEqualTo("api.gravitee.io");
        assertThat(requestOptions.getPort()).isEqualTo(443);
        assertThat(requestOptions.isSsl()).isTrue();
    }
}
