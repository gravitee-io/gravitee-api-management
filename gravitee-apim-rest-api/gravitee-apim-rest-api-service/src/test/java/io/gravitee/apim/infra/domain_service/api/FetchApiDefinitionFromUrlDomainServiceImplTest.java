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
package io.gravitee.apim.infra.domain_service.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.rest.api.service.HttpClientService;
import io.gravitee.rest.api.service.exceptions.ApiDefinitionFetchException;
import io.vertx.core.buffer.Buffer;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class FetchApiDefinitionFromUrlDomainServiceImplTest {

    // A public, allow-listable URL so UrlSanitizerUtils.checkAllowed passes and we exercise the fetch itself.
    private static final String URL = "https://example.com/api-definition.json";

    private HttpClientService httpClientService;
    private FetchApiDefinitionFromUrlDomainServiceImpl service;

    @BeforeEach
    void setUp() {
        httpClientService = mock(HttpClientService.class);
        service = new FetchApiDefinitionFromUrlDomainServiceImpl(httpClientService);
    }

    @Test
    void should_return_body_when_remote_returns_content() {
        when(httpClientService.request(eq(HttpMethod.GET), eq(URL), any(), any(), any())).thenReturn(Buffer.buffer("{\"api\":{}}"));

        assertThat(service.fetch(URL, List.of(), false)).isEqualTo("{\"api\":{}}");
    }

    @Test
    void should_return_empty_string_when_remote_returns_null_body() {
        // HttpClientService returns null (e.g. an empty response). The null guard substitutes an empty string so the
        // downstream parser fails cleanly with a 400 rather than NPE-ing into a 500.
        when(httpClientService.request(eq(HttpMethod.GET), eq(URL), any(), any(), any())).thenReturn(null);

        assertThat(service.fetch(URL, List.of(), false)).isEmpty();
    }

    @Test
    void should_translate_fetch_failure_to_clean_exception() {
        // Any failure raised while fetching must surface as a clean 4xx without leaking the underlying cause.
        when(httpClientService.request(eq(HttpMethod.GET), eq(URL), any(), any(), any())).thenThrow(
            new RuntimeException("Status code: 404. Connection refused")
        );

        assertThatThrownBy(() -> service.fetch(URL, List.of(), false))
            .isInstanceOf(ApiDefinitionFetchException.class)
            .hasMessageNotContainingAny("404", "Connection refused");
    }
}
