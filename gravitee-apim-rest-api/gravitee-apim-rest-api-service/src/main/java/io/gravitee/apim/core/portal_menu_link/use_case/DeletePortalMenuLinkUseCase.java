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
package io.gravitee.apim.core.portal_menu_link.use_case;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.portal_menu_link.crud_service.PortalMenuLinkCrudService;
import io.gravitee.apim.core.portal_menu_link.model.PortalMenuLink;
import io.gravitee.apim.core.portal_menu_link.query_service.PortalMenuLinkQueryService;
import java.util.Objects;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@UseCase
public class DeletePortalMenuLinkUseCase {

    private final PortalMenuLinkCrudService portalMenuLinkCrudService;
    private final PortalMenuLinkQueryService portalMenuLinkQueryService;

    public Output execute(Input input) {
        PortalMenuLink existingPortalMenuLink =
            this.portalMenuLinkCrudService.getByIdAndEnvironmentId(input.portalMenuLinkId(), input.environmentId());

        this.portalMenuLinkCrudService.delete(existingPortalMenuLink.getId());
        this.updatePortalMenuLinkOrder(existingPortalMenuLink.getOrder(), existingPortalMenuLink.getEnvironmentId());

        return new Output();
    }

    @Builder
    public record Input(String portalMenuLinkId, String environmentId) {}

    public record Output() {}

    private void updatePortalMenuLinkOrder(int oldOrder, String environmentId) {
        var orderIncrement = -1;

        this.portalMenuLinkQueryService.findByEnvironmentIdSortByOrder(environmentId)
            .stream()
            .filter(menuLink -> this.toBeMovedUp(oldOrder, menuLink.getOrder()))
            .forEach(menuLink -> {
                var updatedOrder = menuLink.getOrder() + orderIncrement;
                this.portalMenuLinkCrudService.update(menuLink.toBuilder().order(updatedOrder).build());
            });
    }

    private boolean toBeMovedUp(int oldOrder, int portalMenuLinkOrder) {
        return oldOrder < portalMenuLinkOrder;
    }
}
