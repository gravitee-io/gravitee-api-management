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
package io.gravitee.rest.api.portal.rest.mapper;

import io.gravitee.apim.core.portal_menu_link.model.PortalMenuLink;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * @author GraviteeSource Team
 */
public class PortalMenuLinkMapper {

    public List<io.gravitee.rest.api.portal.rest.model.PortalMenuLink> map(List<PortalMenuLink> portalMenuLinks) {
        if (portalMenuLinks == null) {
            return null;
        }
        return portalMenuLinks
            .stream()
            .map(menuLink ->
                new io.gravitee.rest.api.portal.rest.model.PortalMenuLink()
                    .id(menuLink.getId())
                    .name(menuLink.getName())
                    .target(menuLink.getTarget())
                    .type(io.gravitee.rest.api.portal.rest.model.PortalMenuLink.TypeEnum.valueOf(menuLink.getType().name()))
                    .order(menuLink.getOrder())
            )
            .toList();
    }
}
