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
package io.gravitee.gamma.module.platform.rest.resource.dto.am;

import jakarta.validation.constraints.NotBlank;

/** Request/response DTOs for the Access Management settings endpoints. */
public final class AmDtos {

    private AmDtos() {}

    /**
     * On PUT, {@code serviceAccountAccessToken} {@code null} preserves existing ciphertext, empty
     * clears it, non-blank encrypts and replaces.
     */
    public record AmConnectionRequest(
        @NotBlank String baseUrl,
        String serviceAccountAccessToken,
        String environmentId,
        String defaultDomainId,
        String defaultDomainHrid,
        String gatewayUrl
    ) {}

    // Token value is never returned; hasAccessToken lets the UI render a "set" placeholder.
    public record AmConnectionResponse(
        String baseUrl,
        boolean hasAccessToken,
        String environmentId,
        String defaultDomainId,
        String defaultDomainHrid,
        String gatewayUrl
    ) {}

    public record AmConnectionTestResultResponse(boolean ok, Integer status, String message) {}

    public record DomainResponse(String id, String name, String hrid) {}

    public record EnvironmentResponse(String id, String name) {}

    public record GatewayEntrypointResponse(String id, String name, String url, boolean defaultEntrypoint) {}
}
