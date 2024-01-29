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

package io.gravitee.apim.core.api.use_case;

import io.gravitee.apim.core.api.domain_service.UpdateFederatedApiDomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.audit.model.AuditInfo;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

/**
 * @author Remi Baptiste (remi.baptiste at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
public class UpdateFederatedApiUseCase {

    private final UpdateFederatedApiDomainService updateFederatedApiDomainService;

    public UpdateFederatedApiUseCase.Output execute(UpdateFederatedApiUseCase.Input input) {
        return new Output(updateFederatedApiDomainService.update(input.api(), input.auditInfo()));
    }

    public record Output(Api api) {}

    @Builder
    public record Input(AuditInfo auditInfo, Api api) {}
}
