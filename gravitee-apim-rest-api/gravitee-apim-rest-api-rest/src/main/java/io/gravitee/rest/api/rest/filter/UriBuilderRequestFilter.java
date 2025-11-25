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
package io.gravitee.rest.api.rest.filter;

import io.gravitee.apim.core.installation.query_service.InstallationAccessQueryService;
import io.gravitee.common.http.HttpHeaders;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * @author David BRASSELY (david at gravitee.io)
 * @author GraviteeSource Team
 */
@Slf4j
@Provider
@PreMatching
public class UriBuilderRequestFilter implements ContainerRequestFilter {

    private static final int NO_EXPLICIT_PORT = -1; // this resets explicit port in UriBuilder
    private static final String LOCAL_PORTAL_PREFIX = "/portal";
    private static final String LOCAL_MANAGEMENT_PREFIX = "/management";
    public static final String X_ORIGINAL_FORWARDED_HOST = "X-Original-Forwarded-Host";

    private enum ApiContext {
        PORTAL,
        MANAGEMENT,
        UNKNOWN,
    }

    @Inject
    private InstallationAccessQueryService installationAccessQueryService;

    @Override
    public void filter(ContainerRequestContext ctx) throws IOException {
        UriBuilder baseBuilder = ctx.getUriInfo().getBaseUriBuilder();
        UriBuilder requestBuilder = ctx.getUriInfo().getRequestUriBuilder();

        // Detect API context once for reuse
        ApiContext apiContext = detectApiContext(ctx);

        // order matters as each process method may override values set by previous one(s)
        processProtocolHeader(ctx, baseBuilder, requestBuilder, apiContext);
        boolean usedOriginalHost = processHostHeader(ctx, baseBuilder, requestBuilder, apiContext);
        processPortHeader(ctx, baseBuilder, requestBuilder, usedOriginalHost);
        processPrefixHeader(ctx, baseBuilder, requestBuilder, apiContext);
    }

    private ApiContext detectApiContext(ContainerRequestContext ctx) {
        String absolutePath = ctx.getUriInfo().getAbsolutePath().getPath();
        if (absolutePath != null) {
            if (absolutePath.startsWith(LOCAL_PORTAL_PREFIX)) {
                return ApiContext.PORTAL;
            } else if (absolutePath.startsWith(LOCAL_MANAGEMENT_PREFIX)) {
                return ApiContext.MANAGEMENT;
            }
        }
        return ApiContext.UNKNOWN;
    }

    private void processProtocolHeader(
        ContainerRequestContext ctx,
        UriBuilder baseBuilder,
        UriBuilder requestBuilder,
        ApiContext apiContext
    ) {
        String protoHeaderValue = getFirstHeaderValueOrNull(ctx, HttpHeaders.X_FORWARDED_PROTO);
        if (protoHeaderValue != null) {
            baseBuilder.scheme(protoHeaderValue);
            requestBuilder.scheme(protoHeaderValue);
            ctx.setRequestUri(baseBuilder.build(), requestBuilder.build());
        } else if (installationAccessQueryService != null && apiContext != ApiContext.UNKNOWN) {
            // No header - use scheme from configured API URL
            applyDefaultScheme(ctx, baseBuilder, requestBuilder, apiContext);
        }
    }

    private void applyDefaultScheme(ContainerRequestContext ctx, UriBuilder baseBuilder, UriBuilder requestBuilder, ApiContext apiContext) {
        String apiUrl = switch (apiContext) {
            case PORTAL -> installationAccessQueryService.getPortalAPIUrl(GraviteeContext.getDefaultEnvironment());
            case MANAGEMENT -> installationAccessQueryService.getConsoleAPIUrl(GraviteeContext.getDefaultOrganization());
            default -> null;
        };

        if (apiUrl != null) {
            try {
                URI uri = URI.create(apiUrl);
                String scheme = uri.getScheme();
                if (scheme != null) {
                    baseBuilder.scheme(scheme);
                    requestBuilder.scheme(scheme);
                    ctx.setRequestUri(baseBuilder.build(), requestBuilder.build());
                }
            } catch (Exception e) {
                log.warn("Unable to parse API URL: {}", apiUrl, e);
            }
        }
    }

    private boolean processHostHeader(
        ContainerRequestContext ctx,
        UriBuilder baseBuilder,
        UriBuilder requestBuilder,
        ApiContext apiContext
    ) {
        // Check for X-Original-Forwarded-Host first (used in multi-proxy scenarios to preserve the original client host)
        String originalHostHeaderValue = getFirstHeaderValueOrNull(ctx, X_ORIGINAL_FORWARDED_HOST);
        boolean usedOriginalHost = originalHostHeaderValue != null;

        // Fall back to X-Forwarded-Host if X-Original-Forwarded-Host is not present
        String hostHeaderValue = usedOriginalHost ? originalHostHeaderValue : getFirstHeaderValueOrNull(ctx, HttpHeaders.X_FORWARDED_HOST);

        if (hostHeaderValue != null) {
            // Split the header value in case of multiple entries (e.g., localhost,localhost)
            String[] hosts = hostHeaderValue.split(",");
            String effectiveHost = hosts[hosts.length - 1].trim(); // Use the last host in the chain

            if (effectiveHost.contains(":")) {
                int lastColonIdx = effectiveHost.lastIndexOf(':');
                String host = effectiveHost.substring(0, lastColonIdx);
                int port = Integer.parseInt(effectiveHost.substring(lastColonIdx + 1));
                baseBuilder.host(host).port(port);
                requestBuilder.host(host).port(port);
            } else {
                baseBuilder.host(effectiveHost).port(NO_EXPLICIT_PORT);
                requestBuilder.host(effectiveHost).port(NO_EXPLICIT_PORT);
            }

            ctx.setRequestUri(baseBuilder.build(), requestBuilder.build());
        } else if (installationAccessQueryService != null && apiContext != ApiContext.UNKNOWN) {
            // No header - use configured API URL host
            applyDefaultHost(ctx, baseBuilder, requestBuilder, apiContext);
        }

        return usedOriginalHost;
    }

    private void applyDefaultHost(ContainerRequestContext ctx, UriBuilder baseBuilder, UriBuilder requestBuilder, ApiContext apiContext) {
        String apiUrl = switch (apiContext) {
            case PORTAL -> installationAccessQueryService.getPortalAPIUrl(GraviteeContext.getDefaultEnvironment());
            case MANAGEMENT -> installationAccessQueryService.getConsoleAPIUrl(GraviteeContext.getDefaultOrganization());
            default -> null;
        };

        if (apiUrl != null) {
            try {
                URI uri = URI.create(apiUrl);
                String host = uri.getHost();
                int port = uri.getPort();

                if (host != null) {
                    baseBuilder.host(host);
                    requestBuilder.host(host);
                    if (port > 0) {
                        baseBuilder.port(port);
                        requestBuilder.port(port);
                    } else {
                        baseBuilder.port(NO_EXPLICIT_PORT);
                        requestBuilder.port(NO_EXPLICIT_PORT);
                    }
                    ctx.setRequestUri(baseBuilder.build(), requestBuilder.build());
                }
            } catch (Exception e) {
                log.warn("Unable to parse API URL: {}", apiUrl, e);
            }
        }
    }

    private void processPortHeader(
        ContainerRequestContext ctx,
        UriBuilder baseBuilder,
        UriBuilder requestBuilder,
        boolean usedOriginalHost
    ) {
        // If we used X-Original-Forwarded-Host, we should NOT use X-Forwarded-Port because they're from different proxy hops
        // Only use X-Forwarded-Port if we used X-Forwarded-Host (same proxy level)
        if (!usedOriginalHost) {
            String portHeaderValue = getFirstHeaderValueOrNull(ctx, HttpHeaders.X_FORWARDED_PORT);
            if (portHeaderValue != null) {
                try {
                    int port = Integer.parseInt(portHeaderValue);
                    baseBuilder.port(port);
                    requestBuilder.port(port);
                    ctx.setRequestUri(baseBuilder.build(), requestBuilder.build());
                } catch (Exception e) {
                    log.warn("Unable to parse port header value: {}", portHeaderValue, e);
                }
            }
        }
        // If usedOriginalHost is true, we rely on the port in X-Original-Forwarded-Host itself
    }

    private void processPrefixHeader(
        ContainerRequestContext ctx,
        UriBuilder baseBuilder,
        UriBuilder requestBuilder,
        ApiContext apiContext
    ) {
        String prefixHeaderValue = getFirstHeaderValueOrNull(ctx, HttpHeaders.X_FORWARDED_PREFIX);

        if (prefixHeaderValue != null && !prefixHeaderValue.isEmpty()) {
            // Use header value - prepend the prefix to the existing path
            String currentPath = baseBuilder.build().getPath();
            String currentRequestPath = requestBuilder.build().getPath();
            updatePaths(
                ctx,
                baseBuilder,
                requestBuilder,
                joinPaths(prefixHeaderValue, currentPath),
                joinPaths(prefixHeaderValue, currentRequestPath)
            );
        } else if (installationAccessQueryService != null && apiContext != ApiContext.UNKNOWN) {
            // No header - replace local path prefix with configured proxy path
            applyProxyPathReplacement(ctx, baseBuilder, requestBuilder, apiContext);
        }
    }

    private void applyProxyPathReplacement(
        ContainerRequestContext ctx,
        UriBuilder baseBuilder,
        UriBuilder requestBuilder,
        ApiContext apiContext
    ) {
        String localPrefix = switch (apiContext) {
            case PORTAL -> LOCAL_PORTAL_PREFIX;
            case MANAGEMENT -> LOCAL_MANAGEMENT_PREFIX;
            default -> null;
        };

        String proxyPath = switch (apiContext) {
            case PORTAL -> installationAccessQueryService.getPortalApiPath();
            case MANAGEMENT -> installationAccessQueryService.getConsoleApiPath();
            default -> null;
        };

        if (localPrefix != null && proxyPath != null && !localPrefix.equals(proxyPath)) {
            // Replace local prefix with proxy path
            String currentPath = baseBuilder.build().getPath();
            String currentRequestPath = requestBuilder.build().getPath();

            String newPath = currentPath;
            if (currentPath != null && currentPath.startsWith(localPrefix)) {
                newPath = joinPaths(proxyPath, currentPath.substring(localPrefix.length()));
            }

            String newRequestPath = currentRequestPath;
            if (currentRequestPath != null && currentRequestPath.startsWith(localPrefix)) {
                newRequestPath = joinPaths(proxyPath, currentRequestPath.substring(localPrefix.length()));
            }

            updatePaths(ctx, baseBuilder, requestBuilder, newPath, newRequestPath);
        }
    }

    private String getFirstHeaderValueOrNull(ContainerRequestContext ctx, String headerName) {
        List<String> headerValues = ctx.getHeaders().get(headerName);
        if (headerValues != null && !headerValues.isEmpty()) {
            return headerValues.get(0);
        } else {
            return null;
        }
    }

    /**
     * Joins a prefix path with a suffix path, normalizing to avoid double slashes.
     * Removes trailing slash from prefix and ensures suffix starts with a slash.
     */
    private String joinPaths(String prefix, String suffix) {
        String normalizedPrefix = prefix != null && prefix.endsWith("/") ? prefix.substring(0, prefix.length() - 1) : prefix;
        String normalizedSuffix = suffix != null && !suffix.isEmpty() && !suffix.startsWith("/") ? "/" + suffix : suffix;
        return (normalizedPrefix != null ? normalizedPrefix : "") + (normalizedSuffix != null ? normalizedSuffix : "");
    }

    /**
     * Updates the path in both builders and sets the request URI.
     */
    private void updatePaths(
        ContainerRequestContext ctx,
        UriBuilder baseBuilder,
        UriBuilder requestBuilder,
        String basePath,
        String requestPath
    ) {
        baseBuilder.replacePath(basePath);
        requestBuilder.replacePath(requestPath);
        ctx.setRequestUri(baseBuilder.build(), requestBuilder.build());
    }
}
