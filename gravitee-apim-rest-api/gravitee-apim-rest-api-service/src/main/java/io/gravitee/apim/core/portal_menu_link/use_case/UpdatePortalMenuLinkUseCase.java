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

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.portal_menu_link.crud_service.PortalMenuLinkCrudService;
import io.gravitee.apim.core.portal_menu_link.model.PortalMenuLink;
import io.gravitee.apim.core.portal_menu_link.model.UpdatePortalMenuLink;
import io.gravitee.apim.core.portal_menu_link.query_service.PortalMenuLinkQueryService;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import java.util.Objects;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@UseCase
public class UpdatePortalMenuLinkUseCase {

    private final PortalMenuLinkCrudService portalMenuLinkCrudService;
    private final PortalMenuLinkQueryService portalMenuLinkQueryService;

    public Output execute(Input input) {
        PortalMenuLink existingPortalMenuLink =
            this.portalMenuLinkCrudService.getByIdAndEnvironmentId(input.portalMenuLinkId(), input.environmentId());

        var portalMenuLinkToUpdate = existingPortalMenuLink.update(input.portalMenuLinkToUpdate());
        validatePortalMenuLinkToUpdate(portalMenuLinkToUpdate);

        var updatedPortalMenuLink = this.portalMenuLinkCrudService.update(portalMenuLinkToUpdate);

        if (existingPortalMenuLink.getOrder() != updatedPortalMenuLink.getOrder()) {
            this.updatePortalMenuLinkOrder(existingPortalMenuLink.getOrder(), updatedPortalMenuLink);
        }

        return new Output(updatedPortalMenuLink);
    }

    @Builder
    public record Input(String portalMenuLinkId, String environmentId, UpdatePortalMenuLink portalMenuLinkToUpdate) {}

    public record Output(PortalMenuLink portalMenuLinkEntity) {}

    private void updatePortalMenuLinkOrder(int oldOrder, PortalMenuLink updatedPortalMenuLink) {
        var newOrder = updatedPortalMenuLink.getOrder();
        var shouldMoveDown = newOrder < oldOrder;
        var orderIncrement = shouldMoveDown ? 1 : -1;

        this.portalMenuLinkQueryService.findByEnvironmentIdSortByOrder(updatedPortalMenuLink.getEnvironmentId())
            .stream()
            .filter(menuLink -> !Objects.equals(menuLink.getId(), updatedPortalMenuLink.getId()))
            .filter(menuLink ->
                shouldMoveDown
                    ? this.toBeMovedDown(oldOrder, newOrder, menuLink.getOrder())
                    : this.toBeMovedUp(oldOrder, newOrder, menuLink.getOrder())
            )
            .forEach(menuLink -> {
                var updatedOrder = menuLink.getOrder() + orderIncrement;
                this.portalMenuLinkCrudService.update(menuLink.toBuilder().order(updatedOrder).build());
            });
    }

    private boolean toBeMovedUp(int oldOrder, int newOrder, int portalMenuLinkOrder) {
        return oldOrder < portalMenuLinkOrder && portalMenuLinkOrder <= newOrder;
    }

    private boolean toBeMovedDown(int oldOrder, int newOrder, int portalMenuLinkOrder) {
        return newOrder <= portalMenuLinkOrder && portalMenuLinkOrder < oldOrder;
    }

    private void validatePortalMenuLinkToUpdate(PortalMenuLink portalMenuLinkToUpdate) {
        if (portalMenuLinkToUpdate.getName() == null || portalMenuLinkToUpdate.getName().isEmpty()) {
            throw new InvalidDataException("Name is required.");
        }
    }
}
