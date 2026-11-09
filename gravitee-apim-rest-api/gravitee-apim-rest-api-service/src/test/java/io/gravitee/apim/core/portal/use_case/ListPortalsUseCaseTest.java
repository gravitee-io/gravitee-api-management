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

import inmemory.InstallationAccessQueryServiceInMemory;
import inmemory.PortalCrudServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.ThemeQueryServiceInMemory;
import io.gravitee.apim.core.portal.domain_service.PortalSummaryDomainService;
import io.gravitee.apim.core.portal.domain_service.PortalUrlDomainService;
import io.gravitee.apim.core.portal.model.Portal;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal.model.PortalNavigationStructure;
import io.gravitee.apim.core.portal.model.PortalSummary;
import io.gravitee.rest.api.model.common.PageableImpl;
import java.net.URI;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ListPortalsUseCaseTest {

    private static final String ENV_ID = "env-1";
    private static final String OTHER_ENV_ID = "env-2";
    private static final String ORG_ID = "org-1";

    private final PortalCrudServiceInMemory portalCrudService = new PortalCrudServiceInMemory();
    private ListPortalsUseCase cut;

    @BeforeEach
    void setUp() {
        portalCrudService.reset();
        cut = new ListPortalsUseCase(
            portalCrudService,
            new PortalSummaryDomainService(
                new PortalUrlDomainService(new InstallationAccessQueryServiceInMemory()),
                new ThemeQueryServiceInMemory(),
                new PortalNavigationItemsQueryServiceInMemory()
            )
        );
    }

    @Test
    void should_list_the_default_portal_first_then_the_others_by_name() {
        portalCrudService.initWith(
            List.of(portal("zeta", false), portal("Beta", false), portal("Zulu default", true), portal("alpha", false))
        );

        var result = cut.execute(new ListPortalsUseCase.Input(ENV_ID, null, new PageableImpl(1, 10)));

        assertThat(names(result)).containsExactly("Zulu default", "alpha", "Beta", "zeta");
        assertThat(result.portals().getTotalElements()).isEqualTo(4);
    }

    @Test
    void should_list_the_default_portal_of_a_new_environment() {
        portalCrudService.initWith(List.of(portal("Default Portal", true)));

        var result = cut.execute(new ListPortalsUseCase.Input(ENV_ID, null, new PageableImpl(1, 10)));

        assertThat(result.portals().getContent())
            .singleElement()
            .satisfies(summary -> assertThat(summary.portal().isDefault()).isTrue());
    }

    @Test
    void should_never_list_the_portals_of_another_environment() {
        portalCrudService.initWith(List.of(portal("Mine", true), portal(OTHER_ENV_ID, "Theirs", true)));

        var result = cut.execute(new ListPortalsUseCase.Input(ENV_ID, null, new PageableImpl(1, 10)));

        assertThat(names(result)).containsExactly("Mine");
    }

    @Test
    void should_keep_only_the_portals_whose_name_contains_the_query_ignoring_case() {
        portalCrudService.initWith(List.of(portal("Developer Portal", true), portal("Partners", false), portal("Partner extranet", false)));

        var result = cut.execute(new ListPortalsUseCase.Input(ENV_ID, "PARTNER", new PageableImpl(1, 10)));

        assertThat(names(result)).containsExactly("Partner extranet", "Partners");
        assertThat(result.portals().getTotalElements()).isEqualTo(2);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = { "", "  " })
    void should_keep_every_portal_without_a_query(String query) {
        portalCrudService.initWith(List.of(portal("Developer Portal", true), portal("Partners", false)));

        var result = cut.execute(new ListPortalsUseCase.Input(ENV_ID, query, new PageableImpl(1, 10)));

        assertThat(result.portals().getTotalElements()).isEqualTo(2);
    }

    @Test
    void should_return_the_requested_page_and_count_every_matching_portal() {
        portalCrudService.initWith(List.of(portal("Default", true), portal("a", false), portal("b", false), portal("c", false)));

        var result = cut.execute(new ListPortalsUseCase.Input(ENV_ID, null, new PageableImpl(2, 3)));

        assertThat(names(result)).containsExactly("c");
        assertThat(result.portals().getPageNumber()).isEqualTo(2);
        assertThat(result.portals().getPageElements()).isEqualTo(1);
        assertThat(result.portals().getTotalElements()).isEqualTo(4);
    }

    @Test
    void should_return_an_empty_page_past_the_last_one() {
        portalCrudService.initWith(List.of(portal("Default", true)));

        var result = cut.execute(new ListPortalsUseCase.Input(ENV_ID, null, new PageableImpl(3, 10)));

        assertThat(result.portals().getContent()).isEmpty();
        assertThat(result.portals().getTotalElements()).isEqualTo(1);
    }

    private static List<String> names(ListPortalsUseCase.Output result) {
        return result.portals().getContent().stream().map(PortalSummary::portal).map(Portal::getName).toList();
    }

    private static Portal portal(String name, boolean defaultPortal) {
        return portal(ENV_ID, name, defaultPortal);
    }

    @SneakyThrows
    private static Portal portal(String environmentId, String name, boolean defaultPortal) {
        return Portal.of(
            PortalId.random(),
            environmentId,
            ORG_ID,
            name,
            PortalNavigationStructure.empty(),
            null,
            defaultPortal,
            URI.create("https://portal.acme.io").toURL()
        );
    }
}
