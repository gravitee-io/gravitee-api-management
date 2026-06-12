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
package io.gravitee.apim.plugin.gamma.api.identity;

/**
 * Shared connection details a gamma module uses to reach an AM instance for an organization.
 * Storage and token encryption are owned by APIM rest-api core; see {@link AmConnectionRepository}.
 *
 * <p>Nullable fields ({@code serviceAccountAccessToken}, {@code environmentId},
 * {@code defaultDomainId}, {@code defaultDomainHrid}, {@code gatewayUrl}) may be {@code null} when
 * not yet configured. {@code environmentId} is the AM environment the {@code defaultDomainId} lives in.
 */
public record AmConnection(
    String baseUrl,
    String serviceAccountAccessToken,
    String environmentId,
    String defaultDomainId,
    String defaultDomainHrid,
    String gatewayUrl
) {
    public boolean isConfigured() {
        return baseUrl != null && !baseUrl.isBlank() && serviceAccountAccessToken != null && !serviceAccountAccessToken.isBlank();
    }

    // Redact the token so it never leaks into logs or exception messages via the record's toString().
    @Override
    public String toString() {
        return (
            "AmConnection[baseUrl=" +
            baseUrl +
            ", serviceAccountAccessToken=" +
            (serviceAccountAccessToken == null ? "null" : "***") +
            ", environmentId=" +
            environmentId +
            ", defaultDomainId=" +
            defaultDomainId +
            ", defaultDomainHrid=" +
            defaultDomainHrid +
            ", gatewayUrl=" +
            gatewayUrl +
            "]"
        );
    }
}
