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
package io.gravitee.apim.infra.domain_service.documentation;

import static io.gravitee.apim.core.validation.Validator.Error.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.documentation.domain_service.ValidatePageSourceDomainService;
import io.gravitee.apim.core.documentation.model.PageSource;
import io.reactivex.rxjava3.core.Single;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientRequest;
import io.vertx.rxjava3.core.http.HttpClientResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class ValidatePageSourceDomainServiceImplTest {

    private static final String PAGE_NAME = "test-page";

    Vertx vertx = mock(Vertx.class);
    private final ValidatePageSourceDomainService cut = new ValidatePageSourceDomainServiceImpl(new ObjectMapper(), vertx);

    @Nested
    class Github {

        @BeforeEach
        void setUp() {
            HttpClient httpClient = mock(HttpClient.class);
            when(vertx.createHttpClient()).thenReturn(httpClient);

            HttpClientRequest request = mock(HttpClientRequest.class);
            when(httpClient.rxRequest(any())).thenReturn(Single.just(request));

            HttpClientResponse response = mock(HttpClientResponse.class);
            when(response.statusCode()).thenReturn(200);
            when(request.rxSend()).thenReturn(Single.just(response));
        }

        @Test
        void should_return_empty_with_null_source() {
            var result = cut.validateAndSanitize(new ValidatePageSourceDomainService.Input("no-source", null));
            assertThat(result.value()).isEmpty();
            assertThat(result.errors()).isEmpty();
            result.peek(value -> fail("should not peek value"), error -> fail("should not peek errors"));
        }

        @Test
        void should_validate_with_required_fields() {
            var source = PageSource
                .builder()
                .type("github-fetcher")
                .configurationMap(
                    Map.of(
                        "githubUrl",
                        "https://api.github.com",
                        "owner",
                        "test",
                        "repository",
                        "doc-samples",
                        "filepath",
                        "/",
                        "username",
                        "test",
                        "personalAccessToken",
                        "test-token"
                    )
                )
                .build();

            var result = cut.validateAndSanitize(new ValidatePageSourceDomainService.Input(PAGE_NAME, source));

            assertThat(result.severe()).isEmpty();
            assertThat(result.warning()).isEmpty();
            assertThat(result.value()).isNotEmpty();
            assertThat(result.value().get())
                .usingRecursiveComparison()
                .ignoringFields("source.configuration")
                .isEqualTo(new ValidatePageSourceDomainService.Input(PAGE_NAME, source));
        }

        @Test
        void should_validate_with_optional_fields() {
            var source = PageSource
                .builder()
                .type("github-fetcher")
                .configurationMap(
                    Map.of(
                        "githubUrl",
                        "https://api.github.com",
                        "owner",
                        "test",
                        "repository",
                        "doc-samples",
                        "filepath",
                        "/",
                        "username",
                        "test",
                        "personalAccessToken",
                        "test-token",
                        "branchOrTag",
                        "main",
                        "autoFetch",
                        true,
                        "useSystemProxy",
                        false,
                        "fetchCron",
                        "5 * * * * *"
                    )
                )
                .build();

            var result = cut.validateAndSanitize(new ValidatePageSourceDomainService.Input(PAGE_NAME, source));

            assertThat(result.severe()).isEmpty();
            assertThat(result.warning()).isEmpty();
            assertThat(result.value()).isNotEmpty();
            assertThat(result.value().get())
                .usingRecursiveComparison()
                .ignoringFields("source.configuration")
                .isEqualTo(new ValidatePageSourceDomainService.Input(PAGE_NAME, source));
        }

        @Test
        void should_default_github_URL() {
            var source = PageSource
                .builder()
                .type("github-fetcher")
                .configurationMap(
                    Map.of(
                        "owner",
                        "test",
                        "repository",
                        "doc-samples",
                        "filepath",
                        "/",
                        "username",
                        "test",
                        "personalAccessToken",
                        "test-token"
                    )
                )
                .build();

            var expectedSource = PageSource
                .builder()
                .type("github-fetcher")
                .configurationMap(
                    Map.of(
                        "githubUrl",
                        "https://api.github.com",
                        "owner",
                        "test",
                        "repository",
                        "doc-samples",
                        "filepath",
                        "/",
                        "username",
                        "test",
                        "personalAccessToken",
                        "test-token"
                    )
                )
                .build();

            var result = cut.validateAndSanitize(new ValidatePageSourceDomainService.Input(PAGE_NAME, source));

            assertThat(result.severe()).isEmpty();
            assertThat(result.warning()).isEmpty();
            assertThat(result.value()).isNotEmpty();
            assertThat(result.value().get())
                .usingRecursiveComparison()
                .ignoringFields("source.configuration")
                .isEqualTo(new ValidatePageSourceDomainService.Input(PAGE_NAME, expectedSource));
        }

        @Test
        void should_return_error_with_missing_required_property() {
            var source = PageSource
                .builder()
                .type("github-fetcher")
                .configurationMap(
                    Map.of("repository", "doc-samples", "filepath", "/", "username", "test", "personalAccessToken", "test-token")
                )
                .build();

            var result = cut.validateAndSanitize(new ValidatePageSourceDomainService.Input(PAGE_NAME, source));

            assertThat(result.warning()).isEmpty();
            assertThat(result.severe())
                .isNotEmpty()
                .hasValue(List.of(severe("property [owner] is required in [github-fetcher] configuration for page [test-page]")));
        }

        @Test
        void should_return_error_with_invalid_github_URL() {
            var source = PageSource
                .builder()
                .type("github-fetcher")
                .configurationMap(
                    Map.of(
                        "githubUrl",
                        "INVALID URL HERE",
                        "owner",
                        "test",
                        "repository",
                        "doc-samples",
                        "filepath",
                        "/",
                        "username",
                        "test",
                        "personalAccessToken",
                        "test-token"
                    )
                )
                .build();

            var result = cut.validateAndSanitize(new ValidatePageSourceDomainService.Input(PAGE_NAME, source));

            assertThat(result.warning()).isEmpty();
            assertThat(result.severe())
                .isNotEmpty()
                .hasValue(List.of(severe("property [githubUrl] of source [github-fetcher] must be a valid URL for page [test-page]")));
        }

        @Test
        void should_sanitize_and_return_warning_with_unknown_property() {
            var source = PageSource
                .builder()
                .type("github-fetcher")
                .configurationMap(
                    Map.of(
                        "githubUrl",
                        "https://api.github.com",
                        "owner",
                        "test",
                        "repository",
                        "doc-samples",
                        "filepath",
                        "/",
                        "username",
                        "test",
                        "personalAccessToken",
                        "test-token",
                        "unknownPropertyKey",
                        "whatever"
                    )
                )
                .build();

            var expectedSource = PageSource
                .builder()
                .type("github-fetcher")
                .configurationMap(
                    Map.of(
                        "githubUrl",
                        "https://api.github.com",
                        "owner",
                        "test",
                        "repository",
                        "doc-samples",
                        "filepath",
                        "/",
                        "username",
                        "test",
                        "personalAccessToken",
                        "test-token"
                    )
                )
                .build();

            var result = cut.validateAndSanitize(new ValidatePageSourceDomainService.Input(PAGE_NAME, source));

            assertThat(result.warning())
                .isNotEmpty()
                .hasValue(
                    List.of(
                        warning("page [test-page] contains unknown configuration property [unknownPropertyKey] for [github-fetcher] source")
                    )
                );
            assertThat(result.value()).isNotEmpty();
            assertThat(result.value().get())
                .usingRecursiveComparison()
                .ignoringFields("source.configuration")
                .isEqualTo(new ValidatePageSourceDomainService.Input(PAGE_NAME, expectedSource));
        }
    }

    @Nested
    class HTTP {

        @Test
        void should_validate_with_required_fields() {
            var source = PageSource
                .builder()
                .type("http-fetcher")
                .configurationMap(Map.of("url", "https://petstore.swagger.io/v2/swagger.json"))
                .build();

            var result = cut.validateAndSanitize(new ValidatePageSourceDomainService.Input(PAGE_NAME, source));

            assertThat(result.severe()).isEmpty();
            assertThat(result.warning()).isEmpty();
            assertThat(result.value()).hasValue(new ValidatePageSourceDomainService.Input(PAGE_NAME, source));
        }

        @Test
        void should_validate_with_optional_fields() {
            var source = PageSource
                .builder()
                .type("http-fetcher")
                .configurationMap(
                    Map.of(
                        "url",
                        "https://petstore.swagger.io/v2/swagger.json",
                        "autoFetch",
                        true,
                        "useSystemProxy",
                        false,
                        "fetchCron",
                        "5 * * * * *"
                    )
                )
                .build();

            var result = cut.validateAndSanitize(new ValidatePageSourceDomainService.Input(PAGE_NAME, source));

            assertThat(result.severe()).isEmpty();
            assertThat(result.warning()).isEmpty();
            assertThat(result.value()).isNotEmpty().hasValue(new ValidatePageSourceDomainService.Input(PAGE_NAME, source));
        }

        @Test
        void should_return_error_with_invalid_URL() {
            var source = PageSource.builder().type("http-fetcher").configurationMap(Map.of("url", "INVALID URL HERE")).build();

            var result = cut.validateAndSanitize(new ValidatePageSourceDomainService.Input(PAGE_NAME, source));

            assertThat(result.warning()).isEmpty();
            assertThat(result.severe())
                .isNotEmpty()
                .hasValue(List.of(severe("property [url] of source [http-fetcher] must be a valid URL for page [test-page]")));
        }

        @Test
        void should_return_error_with_invalid_cron_expression() {
            var source = PageSource
                .builder()
                .type("http-fetcher")
                .configurationMap(Map.of("url", "https://petstore.swagger.io/v2/swagger.json", "fetchCron", "***"))
                .build();

            var result = cut.validateAndSanitize(new ValidatePageSourceDomainService.Input(PAGE_NAME, source));

            assertThat(result.warning()).isEmpty();
            assertThat(result.severe())
                .isNotEmpty()
                .hasValue(
                    List.of(severe("property [fetchCron] of source [http-fetcher] must be a valid cron expression for page [test-page]"))
                );
        }

        @Test
        void should_sanitize_and_return_warning_with_unknown_property() {
            var source = PageSource
                .builder()
                .type("http-fetcher")
                .configurationMap(Map.of("url", "https://petstore.swagger.io/v2/swagger.json", "unknownPropertyKey", "whatever"))
                .build();

            var expectedSource = PageSource
                .builder()
                .type("http-fetcher")
                .configurationMap(Map.of("url", "https://petstore.swagger.io/v2/swagger.json"))
                .build();

            var result = cut.validateAndSanitize(new ValidatePageSourceDomainService.Input(PAGE_NAME, source));

            assertThat(result.warning())
                .isNotEmpty()
                .hasValue(
                    List.of(
                        warning("page [test-page] contains unknown configuration property [unknownPropertyKey] for [http-fetcher] source")
                    )
                );
            assertThat(result.value()).isNotEmpty().hasValue((new ValidatePageSourceDomainService.Input(PAGE_NAME, expectedSource)));
        }
    }
}
