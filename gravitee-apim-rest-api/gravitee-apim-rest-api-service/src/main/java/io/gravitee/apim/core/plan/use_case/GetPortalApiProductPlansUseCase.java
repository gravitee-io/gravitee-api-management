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
package io.gravitee.apim.core.plan.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal_page.domain_service.PortalApiProductPlanDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationAccessDomainService;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemViewerContext;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetPortalApiProductPlansUseCase {

    private final PortalNavigationAccessDomainService accessService;
    private final PortalApiProductPlanDomainService portalApiProductPlanDomainService;

    public Output execute(Input input) {
        var portal = input
            .portalId()
            .map(portalId -> accessService.resolveScope(input.organizationId(), input.environmentId(), Optional.of(portalId)));
        return new Output(
            portalApiProductPlanDomainService.findAccessiblePlans(
                input.environmentId(),
                input.apiProductId(),
                input.viewerContext(),
                portal
            )
        );
    }

    /**
     * @param portalId the portal that must list the API product; empty on the unscoped paths, where a listing on any portal of the environment does
     */
    public record Input(
        String environmentId,
        String organizationId,
        String apiProductId,
        PortalNavigationItemViewerContext viewerContext,
        Optional<PortalId> portalId
    ) {}

    public record Output(List<Plan> plans) {}
}
