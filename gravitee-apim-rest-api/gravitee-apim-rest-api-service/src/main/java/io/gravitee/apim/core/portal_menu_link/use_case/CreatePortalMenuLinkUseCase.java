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
import io.gravitee.apim.core.portal_menu_link.model.CreatePortalMenuLink;
import io.gravitee.apim.core.portal_menu_link.model.PortalMenuLink;
import io.gravitee.apim.core.portal_menu_link.model.PortalMenuLinkVisibility;
import io.gravitee.apim.core.portal_menu_link.query_service.PortalMenuLinkQueryService;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@UseCase
public class CreatePortalMenuLinkUseCase {

    private final PortalMenuLinkCrudService portalMenuLinkCrudService;
    private final PortalMenuLinkQueryService portalMenuLinkQueryService;

    public Output execute(Input input) {
        var newOrder = this.portalMenuLinkQueryService.findByEnvironmentIdSortByOrder(input.environmentId()).size() + 1;
        PortalMenuLink portalMenuLinkToCreate = PortalMenuLink
            .builder()
            .id(UuidString.generateRandom())
            .environmentId(input.environmentId())
            .type(input.portalMenuLinkToCreate().getType())
            .name(input.portalMenuLinkToCreate().getName())
            .target(input.portalMenuLinkToCreate().getTarget())
            .visibility(input.portalMenuLinkToCreate().getVisibility())
            .order(newOrder)
            .build();

        validatePortalMenuLinkToCreate(portalMenuLinkToCreate);

        return new Output(this.portalMenuLinkCrudService.create(portalMenuLinkToCreate));
    }

    @Builder
    public record Input(String environmentId, CreatePortalMenuLink portalMenuLinkToCreate) {}

    public record Output(PortalMenuLink portalMenuLink) {}

    private void validatePortalMenuLinkToCreate(PortalMenuLink portalMenuLinkToCreate) {
        if (portalMenuLinkToCreate.getName() == null || portalMenuLinkToCreate.getName().isEmpty()) {
            throw new InvalidDataException("Name is required.");
        }
    }
}
