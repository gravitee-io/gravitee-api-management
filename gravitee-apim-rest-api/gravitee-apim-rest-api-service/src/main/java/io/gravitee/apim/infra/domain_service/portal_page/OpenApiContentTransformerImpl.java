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

import io.gravitee.apim.core.portal_page.domain_service.OpenApiContentTransformer;
import io.gravitee.apim.core.portal_page.model.SwaggerUiConfiguration;
import io.gravitee.rest.api.service.impl.swagger.parser.OAIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import lombok.CustomLog;
import org.springframework.stereotype.Service;

@Service
@CustomLog
public class OpenApiContentTransformerImpl implements OpenApiContentTransformer {

    @Override
    public String transform(String content, SwaggerUiConfiguration configuration, Optional<ApiContext> apiContext) {
        if (content == null || content.isBlank() || configuration == null) {
            return content;
        }

        if (!configuration.entrypointsAsServers() && !configuration.contextPathAsServerPath()) {
            return content;
        }

        try {
            var descriptor = new OAIParser().parse(content);
            if (descriptor == null || descriptor.getSpecification() == null) {
                return content;
            }

            var openApi = descriptor.getSpecification();
            var transformed = applyTryItUrl(openApi, configuration.tryItUrl());
            transformed = apiContext.map(context -> applyApiContext(openApi, configuration, context)).orElse(false) || transformed;
            return transformed ? descriptor.toYaml() : content;
        } catch (Exception e) {
            log.warn("Unable to transform OpenAPI content for portal navigation item", e);
            return content;
        }
    }

    private static boolean applyTryItUrl(OpenAPI openApi, String tryItUrl) {
        if (tryItUrl == null || tryItUrl.isBlank()) {
            return false;
        }

        var target = URI.create(tryItUrl);
        var servers = openApi.getServers();
        if (servers == null || servers.isEmpty()) {
            openApi.setServers(List.of(server(target.toString())));
            return true;
        }

        servers.forEach(server -> server.setUrl(target.toString()));
        openApi.setServers(deduplicateByUrl(servers));
        return true;
    }

    private static boolean applyApiContext(OpenAPI openApi, SwaggerUiConfiguration configuration, ApiContext apiContext) {
        var transformed = false;
        if (configuration.entrypointsAsServers()) {
            transformed = applyEntrypointsAsServers(openApi, apiContext.entrypoints(), configuration.contextPathAsServerPath());
        }
        if (configuration.contextPathAsServerPath()) {
            transformed = applyContextPathAsServerPath(openApi, apiContext.contextPath()) || transformed;
        }
        return transformed;
    }

    private static boolean applyEntrypointsAsServers(OpenAPI openApi, List<String> entrypoints, boolean keepEntrypointPath) {
        if (entrypoints == null || entrypoints.isEmpty()) {
            return false;
        }

        var servers = entrypoints
            .stream()
            .filter(entrypoint -> entrypoint != null && !entrypoint.isBlank())
            .map(entrypoint -> keepEntrypointPath ? Optional.of(entrypoint) : removePath(entrypoint))
            .flatMap(Optional::stream)
            .map(OpenApiContentTransformerImpl::server)
            .toList();

        if (!servers.isEmpty()) {
            openApi.setServers(deduplicateByUrl(servers));
            return true;
        }
        return false;
    }

    private static boolean applyContextPathAsServerPath(OpenAPI openApi, Optional<String> contextPath) {
        if (contextPath.isEmpty() || contextPath.get().isBlank() || openApi.getServers() == null || openApi.getServers().isEmpty()) {
            return false;
        }

        var replaced = false;
        for (var server : openApi.getServers()) {
            var replacement = replacePath(server.getUrl(), contextPath.get());
            if (replacement.isPresent()) {
                server.setUrl(replacement.get());
                replaced = true;
            }
        }

        if (replaced) {
            openApi.setServers(deduplicateByUrl(openApi.getServers()));
        }
        return replaced;
    }

    private static Optional<String> removePath(String url) {
        try {
            var uri = URI.create(url);
            return Optional.of(new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(), null, null, null).toString());
        } catch (IllegalArgumentException | URISyntaxException e) {
            log.warn("Unable to remove path from OpenAPI server URL [{}]", url, e);
            return Optional.empty();
        }
    }

    private static Optional<String> replacePath(String url, String path) {
        try {
            var uri = URI.create(url);
            return Optional.of(
                new URI(
                    uri.getScheme(),
                    uri.getUserInfo(),
                    uri.getHost(),
                    uri.getPort(),
                    normalizePath(path),
                    uri.getQuery(),
                    uri.getFragment()
                ).toString()
            );
        } catch (IllegalArgumentException | URISyntaxException e) {
            log.warn("Unable to replace path in OpenAPI server URL [{}]", url, e);
            return Optional.empty();
        }
    }

    private static String normalizePath(String path) {
        return path.startsWith("/") ? path : "/" + path;
    }

    private static Server server(String url) {
        return new Server().url(url);
    }

    private static List<Server> deduplicateByUrl(List<Server> servers) {
        var urls = new LinkedHashSet<String>();
        return servers
            .stream()
            .filter(server -> urls.add(server.getUrl()))
            .toList();
    }
}
