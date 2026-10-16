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
package io.gravitee.apim.core.portal_page.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.apim.core.open_api.OpenApi;
import io.gravitee.apim.core.portal_page.exception.InvalidUrlFormatException;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class OpenApiPageContentTest {

    private static final String SPEC = "openapi: 3.0.3\ninfo:\n  title: Petstore";
    private static final RedocConfiguration ORIGINAL_CONFIGURATION = new RedocConfiguration("https://original.example.com");

    private static OpenApiPageContent anOpenApiPageContent() {
        return new OpenApiPageContent(PortalPageContentId.random(), "org", "env", OpenApi.of(SPEC), ORIGINAL_CONFIGURATION);
    }

    @Nested
    class UpdateViewerSettings {

        @ParameterizedTest
        @ValueSource(strings = { "", "https://api.example.com", "http://localhost:8082/petstore", "HTTPS://API.EXAMPLE.COM" })
        void should_accept_an_empty_or_http_try_it_url(String tryItUrl) {
            var content = anOpenApiPageContent();

            content.updateViewerSettings(new RedocConfiguration(tryItUrl));

            assertThat(content.getViewerSettings()).isEqualTo(new RedocConfiguration(tryItUrl));
        }

        @ParameterizedTest
        @ValueSource(
            strings = { "javascript:alert(1)", "ftp://files.example.com", "not a url", "https://api example.com", "https://", " " }
        )
        void should_reject_a_try_it_url_that_is_not_http(String tryItUrl) {
            var content = anOpenApiPageContent();

            assertThatThrownBy(() -> content.updateViewerSettings(new RedocConfiguration(tryItUrl))).isInstanceOf(
                InvalidUrlFormatException.class
            );
            assertThat(content.getViewerSettings()).isEqualTo(ORIGINAL_CONFIGURATION);
        }
    }

    @Nested
    class Update {

        @ParameterizedTest
        @ValueSource(strings = { "javascript:alert(1)", "ftp://files.example.com" })
        void should_reject_a_configuration_whose_try_it_url_is_not_http(String tryItUrl) {
            var content = anOpenApiPageContent();
            var update = UpdatePortalPageContent.builder().content(SPEC).configuration(new RedocConfiguration(tryItUrl)).build();

            assertThatThrownBy(() -> content.update(update)).isInstanceOf(InvalidUrlFormatException.class);
            assertThat(content.getViewerSettings()).isEqualTo(ORIGINAL_CONFIGURATION);
        }

        @ParameterizedTest
        @ValueSource(strings = { "", "https://api.example.com" })
        void should_accept_a_configuration_whose_try_it_url_is_empty_or_http(String tryItUrl) {
            var content = anOpenApiPageContent();
            var update = UpdatePortalPageContent.builder().content(SPEC).configuration(new RedocConfiguration(tryItUrl)).build();

            content.update(update);

            assertThat(content.getViewerSettings()).isEqualTo(new RedocConfiguration(tryItUrl));
        }
    }
}
