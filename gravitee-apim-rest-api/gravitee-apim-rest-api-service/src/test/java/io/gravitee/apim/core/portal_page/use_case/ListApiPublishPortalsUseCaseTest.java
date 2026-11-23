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
package io.gravitee.apim.core.portal_page.use_case;

import static fixtures.core.model.PortalNavigationItemFixtures.ENV_ID;
import static fixtures.core.model.PortalNavigationItemFixtures.ORG_ID;
import static org.assertj.core.api.Assertions.assertThat;

import fixtures.core.model.PortalFixtures;
import inmemory.NavigationPortalDomainServiceInMemory;
import inmemory.PortalCrudServiceInMemory;
import io.gravitee.apim.core.portal.model.Portal;
import io.gravitee.apim.core.portal_page.use_case.ListApiPublishPortalsUseCase.PublishPortal;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ListApiPublishPortalsUseCaseTest {

    private final NavigationPortalDomainServiceInMemory navigationPortals = new NavigationPortalDomainServiceInMemory();
    private final PortalCrudServiceInMemory portalCrudService = navigationPortals.portalCrudService();
    private final ListApiPublishPortalsUseCase useCase = new ListApiPublishPortalsUseCase(navigationPortals);

    @Test
    void should_return_the_default_portal_first_and_then_the_others_by_name() {
        var partners = navigationPortals.givenConsolePortal(ORG_ID, ENV_ID, "partners");
        var developer = portalCrudService.create(Portal.createDefault(ENV_ID, ORG_ID, "Developer Portal", PortalFixtures.PORTAL_URL));
        var beta = navigationPortals.givenConsolePortal(ORG_ID, ENV_ID, "Beta");

        assertThat(portals()).containsExactly(
            new PublishPortal(developer.getId(), "Developer Portal", true),
            new PublishPortal(beta, "Beta", false),
            new PublishPortal(partners, "partners", false)
        );
    }

    @Test
    void should_not_return_the_portals_managed_through_the_automation_api() {
        var developer = portalCrudService.create(Portal.createDefault(ENV_ID, ORG_ID, "Developer Portal", PortalFixtures.PORTAL_URL));
        portalCrudService.create(Portal.create(ENV_ID, ORG_ID, "Automation Portal", PortalFixtures.PORTAL_URL));

        assertThat(portals()).containsExactly(new PublishPortal(developer.getId(), "Developer Portal", true));
    }

    @Test
    void should_not_return_the_portals_of_another_environment() {
        portalCrudService.create(Portal.createDefault("other-env", ORG_ID, "Elsewhere", PortalFixtures.PORTAL_URL));

        assertThat(portals()).isEmpty();
    }

    private List<PublishPortal> portals() {
        return useCase.execute(new ListApiPublishPortalsUseCase.Input(ENV_ID)).portals();
    }
}
