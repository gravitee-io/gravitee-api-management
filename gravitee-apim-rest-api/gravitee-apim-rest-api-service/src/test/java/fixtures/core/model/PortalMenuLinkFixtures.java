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
package fixtures.core.model;

import io.gravitee.apim.core.portal_menu_link.model.PortalMenuLink;
import io.gravitee.apim.core.portal_menu_link.model.PortalMenuLinkType;
import java.util.function.Supplier;

public class PortalMenuLinkFixtures {

    private PortalMenuLinkFixtures() {}

    private static final Supplier<PortalMenuLink.PortalMenuLinkBuilder> BASE = () ->
        PortalMenuLink
            .builder()
            .id("portalMenuLinkId")
            .environmentId("environmentId")
            .type(PortalMenuLinkType.EXTERNAL)
            .name("portalMenuLinkName")
            .target("portalMenuLinkTarget")
            .order(1);

    public static PortalMenuLink aPortalMenuLink() {
        return BASE.get().build();
    }
}
