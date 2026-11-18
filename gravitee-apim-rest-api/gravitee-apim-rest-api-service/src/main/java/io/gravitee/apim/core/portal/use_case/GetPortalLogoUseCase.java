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
package io.gravitee.apim.core.portal.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.portal.crud_service.PortalCrudService;
import io.gravitee.apim.core.portal.domain_service.PortalLogoDomainService;
import io.gravitee.apim.core.portal.exception.PortalLogoNotFoundException;
import io.gravitee.apim.core.portal.exception.PortalNotFoundException;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal.model.PortalLogo;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetPortalLogoUseCase {

    private final PortalCrudService portalCrudService;
    private final PortalLogoDomainService portalLogoDomainService;

    public record Input(String environmentId, PortalId portalId) {}

    public record Output(PortalLogo logo) {}

    public Output execute(Input input) {
        var portalId = input.portalId().toString();
        portalCrudService
            .findByIdAndEnvironmentId(input.portalId(), input.environmentId())
            .orElseThrow(() -> new PortalNotFoundException(portalId));
        return portalLogoDomainService
            .findLogo(input.environmentId())
            .map(logo -> new Output(PortalLogo.of(logo)))
            .orElseThrow(() -> new PortalLogoNotFoundException(portalId));
    }
}
