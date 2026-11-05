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
package io.gravitee.apim.core.portal.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.installation.query_service.InstallationAccessQueryService;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class PortalUrlDomainService {

    private static final String NEXT_PORTAL_PATH = "/next";
    private static final Set<String> HTTP_SCHEMES = Set.of("http", "https");

    private final InstallationAccessQueryService installationAccessQueryService;

    /**
     * The URL of the environment's Next Gen portal: its portal URL followed by {@code /next}.
     *
     * @return empty when the environment has no portal URL, or one that is not an absolute http(s) URL without credentials
     */
    public Optional<URL> resolveDefaultPortalUrl(String environmentId) {
        var portalUrl = installationAccessQueryService.getPortalUrl(environmentId);
        if (portalUrl == null || portalUrl.isBlank()) {
            return Optional.empty();
        }
        return toHttpUrl(withoutTrailingSlashes(portalUrl.strip()) + NEXT_PORTAL_PATH);
    }

    private static String withoutTrailingSlashes(String url) {
        var end = url.length();
        while (end > 0 && url.charAt(end - 1) == '/') {
            end--;
        }
        return url.substring(0, end);
    }

    private static Optional<URL> toHttpUrl(String value) {
        try {
            var uri = new URI(value);
            // A user-info part would put credentials in the stored URL and in the logs.
            if (
                uri.getScheme() == null ||
                !HTTP_SCHEMES.contains(uri.getScheme().toLowerCase()) ||
                uri.getHost() == null ||
                uri.getRawUserInfo() != null
            ) {
                return Optional.empty();
            }
            return Optional.of(uri.toURL());
        } catch (URISyntaxException | MalformedURLException e) {
            return Optional.empty();
        }
    }
}
