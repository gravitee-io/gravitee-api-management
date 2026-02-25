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
package io.gravitee.apim.core.api_product.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.license.domain_service.LicenseDomainService;
import lombok.RequiredArgsConstructor;

/**
 * Verifies whether an API Product can be deployed based on the current organization license.
 * API Product deployment requires the "universe" license tier.
 */
@UseCase
@RequiredArgsConstructor
public class VerifyApiProductDeployUseCase {

    private final LicenseDomainService licenseDomainService;

    public Output execute(Input input) {
        boolean ok = licenseDomainService.isApiProductDeploymentAllowed(input.organizationId());
        String reason = ok ? null : "API Product deployment requires a universe license.";
        return new Output(ok, reason);
    }

    public record Input(String organizationId) {}

    public record Output(boolean ok, String reason) {}
}
