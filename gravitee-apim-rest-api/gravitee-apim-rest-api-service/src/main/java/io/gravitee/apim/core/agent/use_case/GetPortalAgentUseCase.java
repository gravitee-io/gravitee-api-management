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
package io.gravitee.apim.core.agent.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.agent.exception.PortalAgentNotFoundException;
import io.gravitee.apim.core.agent.model.PortalAgentCard;
import io.gravitee.apim.core.agent.service_provider.AimCatalogQueryService;
import io.gravitee.apim.core.portal_page.domain_service.PortalAgentAccessDomainService;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemViewerContext;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetPortalAgentUseCase {

    private final PortalAgentAccessDomainService portalAgentAccessDomainService;
    private final AimCatalogQueryService aimCatalogQueryService;

    public Output execute(Input input) {
        portalAgentAccessDomainService.findAccessible(input.environmentId(), input.agentId(), input.viewerContext());
        var agent = aimCatalogQueryService
            .findById(input.environmentId(), input.agentId())
            .orElseThrow(() -> new PortalAgentNotFoundException(input.agentId()));
        return new Output(agent);
    }

    public record Input(String environmentId, String agentId, PortalNavigationItemViewerContext viewerContext) {}

    public record Output(PortalAgentCard agent) {}
}
