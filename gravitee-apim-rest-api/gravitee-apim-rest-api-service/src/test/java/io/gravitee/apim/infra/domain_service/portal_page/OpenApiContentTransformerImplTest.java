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
package io.gravitee.apim.infra.domain_service.portal_page;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.core.portal_page.domain_service.OpenApiContentTransformer;
import io.gravitee.apim.core.portal_page.model.SwaggerUiConfiguration;
import io.gravitee.rest.api.service.impl.swagger.parser.OAIParser;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class OpenApiContentTransformerImplTest {

    private static final String OPENAPI = """
        openapi: 3.0.3
        info:
          title: Test
          version: 1.0.0
        servers:
          - url: https://origin.example.com/base
        paths: {}
        """;

    private final OpenApiContentTransformerImpl transformer = new OpenApiContentTransformerImpl();

    @Test
    void should_keep_content_unchanged_when_server_resolution_flags_are_disabled() {
        var transformed = transformer.transform(
            OPENAPI,
            configuration(false, false, "https://custom.example.com/base"),
            Optional.of(apiContext(List.of("https://apis.gravitee.io/test"), "/test/"))
        );

        assertThat(transformed).isEqualTo(OPENAPI);
    }

    @Test
    void should_replace_existing_server_path_with_api_context_path() {
        var transformed = transformer.transform(OPENAPI, configuration(false, true, ""), Optional.of(apiContext(List.of(), "/test/")));

        assertThat(serverUrls(transformed)).containsExactly("https://origin.example.com/test/");
    }

    @Test
    void should_use_api_entrypoints_without_paths_as_servers() {
        var transformed = transformer.transform(
            OPENAPI,
            configuration(true, false, ""),
            Optional.of(apiContext(List.of("https://apis.gravitee.io/test", "https://backup.gravitee.io/test"), "/test/"))
        );

        assertThat(serverUrls(transformed)).containsExactly("https://apis.gravitee.io", "https://backup.gravitee.io");
    }

    @Test
    void should_replace_api_entrypoint_paths_with_context_path_when_context_path_as_server_path_is_enabled() {
        var transformed = transformer.transform(
            OPENAPI,
            configuration(true, true, ""),
            Optional.of(apiContext(List.of("https://apis.gravitee.io/gateway/echo"), "/test/"))
        );

        assertThat(serverUrls(transformed)).containsExactly("https://apis.gravitee.io/test/");
    }

    @Test
    void should_keep_api_entrypoint_path_when_context_path_as_server_path_is_enabled_without_context_path() {
        var transformed = transformer.transform(
            OPENAPI,
            configuration(true, true, ""),
            Optional.of(new OpenApiContentTransformer.ApiContext(List.of("https://apis.gravitee.io/gateway/echo"), Optional.empty()))
        );

        assertThat(serverUrls(transformed)).containsExactly("https://apis.gravitee.io/gateway/echo");
    }

    @Test
    void should_apply_try_it_url_without_api_context() {
        var transformed = transformer.transform(OPENAPI, configuration(false, true, "https://custom.example.com/base"), Optional.empty());

        assertThat(serverUrls(transformed)).containsExactly("https://custom.example.com/base");
    }

    @Test
    void should_apply_try_it_url_before_replacing_server_path_with_api_context_path() {
        var transformed = transformer.transform(
            OPENAPI,
            configuration(false, true, "https://custom.example.com/tryit?q=test"),
            Optional.of(apiContext(List.of(), "/test/"))
        );

        assertThat(serverUrls(transformed)).containsExactly("https://custom.example.com/test/?q=test");
    }

    private static SwaggerUiConfiguration configuration(boolean entrypointsAsServers, boolean contextPathAsServerPath, String tryItUrl) {
        return new SwaggerUiConfiguration(
            false,
            "none",
            false,
            -1,
            false,
            false,
            false,
            false,
            false,
            false,
            tryItUrl,
            false,
            entrypointsAsServers,
            contextPathAsServerPath
        );
    }

    private static OpenApiContentTransformer.ApiContext apiContext(List<String> entrypoints, String contextPath) {
        return new OpenApiContentTransformer.ApiContext(entrypoints, Optional.of(contextPath));
    }

    private static List<String> serverUrls(String content) {
        return new OAIParser()
            .parse(content)
            .getSpecification()
            .getServers()
            .stream()
            .map(server -> server.getUrl())
            .toList();
    }
}
