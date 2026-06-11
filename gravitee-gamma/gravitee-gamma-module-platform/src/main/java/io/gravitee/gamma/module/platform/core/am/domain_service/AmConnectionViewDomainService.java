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
package io.gravitee.gamma.module.platform.core.am.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.plugin.gamma.api.identity.AmConnection;
import io.gravitee.apim.plugin.gamma.api.identity.AmConnectionRepository;
import lombok.RequiredArgsConstructor;

/**
 * Reads the per-org AM connection and exposes the safe, UI-facing view of it.
 * hasAccessToken reflects ciphertext presence so the UI can render a "set" placeholder without
 * ever revealing the value. Shared by the get and save use cases so the read-back stays consistent.
 */
@DomainService
@RequiredArgsConstructor
public class AmConnectionViewDomainService {

    private final AmConnectionRepository amConnectionRepository;

    public record View(
        String baseUrl,
        boolean hasAccessToken,
        String environmentId,
        String defaultDomainId,
        String defaultDomainHrid,
        String gatewayUrl
    ) {}

    public View forOrg(String orgId) {
        var connection = amConnectionRepository.findByOrg(orgId);
        boolean hasToken = amConnectionRepository.hasTokenForOrg(orgId);

        String baseUrl = connection.map(c -> c.baseUrl() == null ? "" : c.baseUrl()).orElse("");
        String environmentId = connection.map(AmConnection::environmentId).orElse(null);
        String defaultDomainId = connection.map(AmConnection::defaultDomainId).orElse(null);
        String defaultDomainHrid = connection.map(AmConnection::defaultDomainHrid).orElse(null);
        String gatewayUrl = connection.map(AmConnection::gatewayUrl).orElse(null);

        return new View(baseUrl, hasToken, environmentId, defaultDomainId, defaultDomainHrid, gatewayUrl);
    }
}
