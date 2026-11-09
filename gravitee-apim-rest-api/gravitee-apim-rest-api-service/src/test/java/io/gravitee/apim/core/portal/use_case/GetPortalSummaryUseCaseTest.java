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
package io.gravitee.apim.core.portal.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import inmemory.PortalCrudServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.ThemeQueryServiceInMemory;
import io.gravitee.apim.core.portal.domain_service.PortalSummaryDomainService;
import io.gravitee.apim.core.portal.exception.PortalNotFoundException;
import io.gravitee.apim.core.portal.model.Portal;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal.model.PortalNavigationStructure;
import java.net.URI;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GetPortalSummaryUseCaseTest {

    private static final String ENV_ID = "env-1";
    private static final String OTHER_ENV_ID = "env-2";

    private final PortalCrudServiceInMemory portalCrudService = new PortalCrudServiceInMemory();
    private GetPortalSummaryUseCase cut;

    @BeforeEach
    void setUp() {
        portalCrudService.reset();
        cut = new GetPortalSummaryUseCase(
            portalCrudService,
            new PortalSummaryDomainService(new ThemeQueryServiceInMemory(), new PortalNavigationItemsQueryServiceInMemory())
        );
    }

    @Test
    void should_return_the_summary_of_the_environment_portal() {
        var portal = portal(ENV_ID);
        portalCrudService.initWith(List.of(portal));

        var result = cut.execute(new GetPortalSummaryUseCase.Input(ENV_ID, portal.getId()));

        assertThat(result.portal().portal()).isEqualTo(portal);
    }

    @Test
    void should_throw_when_the_portal_does_not_exist() {
        var unknownId = PortalId.random();

        assertThatThrownBy(() -> cut.execute(new GetPortalSummaryUseCase.Input(ENV_ID, unknownId))).isInstanceOf(
            PortalNotFoundException.class
        );
    }

    @Test
    void should_throw_when_the_portal_belongs_to_another_environment() {
        var portal = portal(OTHER_ENV_ID);
        portalCrudService.initWith(List.of(portal));

        assertThatThrownBy(() -> cut.execute(new GetPortalSummaryUseCase.Input(ENV_ID, portal.getId()))).isInstanceOf(
            PortalNotFoundException.class
        );
    }

    @SneakyThrows
    private static Portal portal(String environmentId) {
        return Portal.of(
            PortalId.random(),
            environmentId,
            "org-1",
            "Partners",
            PortalNavigationStructure.empty(),
            null,
            false,
            URI.create("https://partners.acme.io").toURL()
        );
    }
}
