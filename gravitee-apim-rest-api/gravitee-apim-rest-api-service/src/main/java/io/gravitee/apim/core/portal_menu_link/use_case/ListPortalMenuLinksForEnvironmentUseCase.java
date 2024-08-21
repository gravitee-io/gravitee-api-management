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
import io.gravitee.apim.core.portal_menu_link.model.PortalMenuLink;
import io.gravitee.apim.core.portal_menu_link.query_service.PortalMenuLinkQueryService;
import java.util.List;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@UseCase
public class ListPortalMenuLinksForEnvironmentUseCase {

    private final PortalMenuLinkQueryService portalMenuLinkQueryService;

    public Output execute(Input input) {
        return new Output(this.portalMenuLinkQueryService.findByEnvironmentIdSortByOrder(input.environmentId()));
    }

    @Builder
    public record Input(String environmentId) {}

    public record Output(List<PortalMenuLink> portalMenuLinkList) {}
}
