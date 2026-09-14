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
import io.gravitee.apim.core.access_point.model.AccessPoint;
import io.gravitee.apim.core.access_point.query_service.AccessPointQueryService;
import io.gravitee.apim.core.agent.exception.PortalAgentNotFoundException;
import io.gravitee.apim.core.agent.model.PortalAgentCard;
import io.gravitee.apim.core.agent.service_provider.AimCatalogQueryService;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.portal_page.domain_service.PortalAgentAccessDomainService;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemViewerContext;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetPortalAgentUseCase {

    private final PortalAgentAccessDomainService portalAgentAccessDomainService;
    private final AimCatalogQueryService aimCatalogQueryService;
    private final ApiCrudService apiCrudService;
    private final AccessPointQueryService accessPointQueryService;

    public Output execute(Input input) {
        var navItem = portalAgentAccessDomainService.findAccessible(input.environmentId(), input.agentId(), input.viewerContext());
        var agent = aimCatalogQueryService
            .findById(input.environmentId(), input.agentId())
            .orElseThrow(() -> new PortalAgentNotFoundException(input.agentId()));
        var proxyUrl = resolveProxyUrl(input.environmentId(), navItem.getApiId());
        return new Output(agent.withDefinitionUrl(proxyUrl));
    }

    private String resolveProxyUrl(String environmentId, String apiId) {
        var gatewayBase = Optional.ofNullable(accessPointQueryService.getGatewayAccessPoints(environmentId))
            .orElse(List.of())
            .stream()
            .findFirst()
            .map(AccessPoint::buildInstallationAccess)
            .orElse("");

        var contextPath = apiCrudService
            .findById(apiId)
            .flatMap(api ->
                api
                    .getApiListeners()
                    .stream()
                    .filter(HttpListener.class::isInstance)
                    .map(HttpListener.class::cast)
                    .findFirst()
                    .flatMap(listener -> listener.getPaths().stream().findFirst())
                    .map(path -> path.getPath())
            )
            .orElse("");

        if (gatewayBase.isEmpty() || contextPath.isEmpty()) {
            return null;
        }
        var base = gatewayBase.endsWith("/") ? gatewayBase.substring(0, gatewayBase.length() - 1) : gatewayBase;
        var path = contextPath.startsWith("/") ? contextPath : "/" + contextPath;
        return base + path;
    }

    public record Input(String environmentId, String agentId, PortalNavigationItemViewerContext viewerContext) {}

    public record Output(PortalAgentCard agent) {}
}
