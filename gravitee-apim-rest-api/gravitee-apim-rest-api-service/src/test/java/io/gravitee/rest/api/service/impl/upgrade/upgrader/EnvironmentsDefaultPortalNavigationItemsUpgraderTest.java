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
package io.gravitee.rest.api.service.impl.upgrade.upgrader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import fixtures.core.model.PortalFixtures;
import fixtures.core.model.PortalNavigationItemFixtures;
import inmemory.ApiCrudServiceInMemory;
import inmemory.NavigationPortalDomainServiceInMemory;
import inmemory.PortalCrudServiceInMemory;
import inmemory.PortalNavigationItemSourceDomainServiceInMemory;
import inmemory.PortalNavigationItemsCrudServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.PortalPageContentCrudServiceInMemory;
import inmemory.PortalPageContentQueryServiceInMemory;
import io.gravitee.apim.core.portal.model.Portal;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal.model.PortalNavigationStructure;
import io.gravitee.apim.core.portal_page.domain_service.ApiOwnedNavigationDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemDomainService;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import io.gravitee.apim.core.portal_page.use_case.CreateDefaultPortalNavigationItemsUseCase;
import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.model.Environment;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class EnvironmentsDefaultPortalNavigationItemsUpgraderTest {

    private static final Environment ANOTHER_ENVIRONMENT = Environment.builder()
        .id("ANOTHER_ENVIRONMENT")
        .hrids(List.of("another environment"))
        .name("another environment")
        .organizationId("ANOTHER_ORG")
        .build();

    @Mock
    EnvironmentRepository environmentRepository;

    @Mock
    CreateDefaultPortalNavigationItemsUseCase createDefaultPortalNavigationItemsUseCase;

    @Mock
    PortalNavigationItemsQueryService portalNavigationItemsQueryService;

    private EnvironmentsDefaultPortalNavigationItemsUpgrader upgrader;

    @BeforeEach
    public void setUp() {
        upgrader = new EnvironmentsDefaultPortalNavigationItemsUpgrader(
            environmentRepository,
            createDefaultPortalNavigationItemsUseCase,
            portalNavigationItemsQueryService
        );
    }

    @AfterEach
    void cleanGraviteeContext() {
        GraviteeContext.cleanContext();
    }

    @Test
    @SneakyThrows
    void should_do_nothing_when_there_is_no_environment() {
        when(environmentRepository.findAll()).thenReturn(Collections.emptySet());
        assertThat(upgrader.upgrade()).isTrue();
        verifyNoInteractions(createDefaultPortalNavigationItemsUseCase);
    }

    @Test
    @SneakyThrows
    void should_return_false_when_something_wrong_happens() {
        when(environmentRepository.findAll()).thenThrow(new TechnicalException("this is a test exception"));

        // When
        final Executable throwing = () -> upgrader.upgrade();

        // Then
        Exception exception = assertThrows(UpgraderException.class, throwing);
        assertThat(exception.getMessage()).contains("this is a test exception");
    }

    @Test
    @SneakyThrows
    void should_create_default_portal_page_for_both_environments_when_neither_has_a_homepage() {
        when(environmentRepository.findAll()).thenReturn(Set.of(Environment.DEFAULT, ANOTHER_ENVIRONMENT));
        when(portalNavigationItemsQueryService.findTopLevelItemsByEnvironmentIdAndPortalArea(any(), eq(PortalArea.HOMEPAGE))).thenReturn(
            List.of()
        );

        assertThat(upgrader.upgrade()).isTrue();

        ArgumentCaptor<String> captorOrgId = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> captorEnvId = ArgumentCaptor.forClass(String.class);

        verify(createDefaultPortalNavigationItemsUseCase, times(2)).execute(captorOrgId.capture(), captorEnvId.capture());
        assertThat(captorOrgId.getAllValues()).containsExactlyInAnyOrder("DEFAULT", "ANOTHER_ORG");
        assertThat(captorEnvId.getAllValues()).containsExactlyInAnyOrder("DEFAULT", "ANOTHER_ENVIRONMENT");
    }

    @Test
    @SneakyThrows
    void should_not_seed_an_environment_with_the_portal_url_memoized_for_the_previous_one() {
        when(environmentRepository.findAll()).thenReturn(new LinkedHashSet<>(List.of(Environment.DEFAULT, ANOTHER_ENVIRONMENT)));
        when(portalNavigationItemsQueryService.findTopLevelItemsByEnvironmentIdAndPortalArea(any(), eq(PortalArea.HOMEPAGE))).thenReturn(
            List.of()
        );
        // ParameterService memoizes portal.url per thread by key only: simulate the value read while seeding DEFAULT
        doAnswer(invocation -> GraviteeContext.getCurrentParameters().put(Key.PORTAL_URL, "https://default.acme.io"))
            .when(createDefaultPortalNavigationItemsUseCase)
            .execute("DEFAULT", "DEFAULT");
        var memoizedWhenSeedingAnother = new AtomicReference<String>("unset");
        doAnswer(invocation -> {
            memoizedWhenSeedingAnother.set(GraviteeContext.getCurrentParameters().get(Key.PORTAL_URL));
            return null;
        })
            .when(createDefaultPortalNavigationItemsUseCase)
            .execute("ANOTHER_ORG", "ANOTHER_ENVIRONMENT");

        assertThat(upgrader.upgrade()).isTrue();

        assertThat(memoizedWhenSeedingAnother.get()).isNull();
    }

    @Test
    @SneakyThrows
    void should_skip_environment_that_already_has_a_homepage() {
        // Given: DEFAULT was already fully seeded (has a homepage) - even if a user later deleted e.g.
        // its "Guides" folder on purpose, this upgrader must not touch it. ANOTHER_ENVIRONMENT has no
        // homepage yet, so it should still be repaired.
        when(environmentRepository.findAll()).thenReturn(Set.of(Environment.DEFAULT, ANOTHER_ENVIRONMENT));
        when(
            portalNavigationItemsQueryService.findTopLevelItemsByEnvironmentIdAndPortalArea(eq("DEFAULT"), eq(PortalArea.HOMEPAGE))
        ).thenReturn(List.of(PortalNavigationItemFixtures.aPage("00000000-0000-0000-0000-000000000999", "Home Page", null)));
        when(
            portalNavigationItemsQueryService.findTopLevelItemsByEnvironmentIdAndPortalArea(
                eq("ANOTHER_ENVIRONMENT"),
                eq(PortalArea.HOMEPAGE)
            )
        ).thenReturn(List.of());

        assertThat(upgrader.upgrade()).isTrue();

        verify(createDefaultPortalNavigationItemsUseCase, never()).execute(eq("DEFAULT"), eq("DEFAULT"));
        verify(createDefaultPortalNavigationItemsUseCase, times(1)).execute("ANOTHER_ORG", "ANOTHER_ENVIRONMENT");
    }

    /** The upgrader runs after the default portal upgrader: the seeding it triggers names that portal. */
    @Nested
    class SeedingOnTheDefaultPortal {

        private static final PortalId DEFAULT_PORTAL_ID = PortalId.of("11111111-1111-1111-1111-111111111111");

        private final PortalCrudServiceInMemory portalCrudService = new PortalCrudServiceInMemory();
        private final PortalNavigationItemsCrudServiceInMemory navigationItemsCrudService = new PortalNavigationItemsCrudServiceInMemory();
        private final PortalNavigationItemsQueryServiceInMemory navigationItemsQueryService = new PortalNavigationItemsQueryServiceInMemory(
            navigationItemsCrudService.storage()
        );

        private EnvironmentsDefaultPortalNavigationItemsUpgrader realSeedingUpgrader;

        @BeforeEach
        void setUp() {
            var pageContentCrudService = new PortalPageContentCrudServiceInMemory();
            var navigationPortals = new NavigationPortalDomainServiceInMemory(portalCrudService);
            var navigationItemDomainService = new PortalNavigationItemDomainService(
                navigationItemsCrudService,
                navigationItemsQueryService,
                pageContentCrudService,
                PortalPageContentQueryServiceInMemory.sharing(pageContentCrudService.storage()),
                new ApiCrudServiceInMemory(),
                new PortalNavigationItemSourceDomainServiceInMemory(),
                new ApiOwnedNavigationDomainService(navigationItemsQueryService, navigationItemsCrudService),
                navigationPortals
            );
            realSeedingUpgrader = new EnvironmentsDefaultPortalNavigationItemsUpgrader(
                environmentRepository,
                new CreateDefaultPortalNavigationItemsUseCase(
                    navigationItemDomainService,
                    pageContentCrudService,
                    navigationItemsQueryService,
                    navigationPortals
                ),
                navigationItemsQueryService
            );
        }

        @Test
        @SneakyThrows
        void should_seed_an_environment_on_its_existing_default_portal() {
            portalCrudService.initWith(
                List.of(
                    Portal.of(
                        DEFAULT_PORTAL_ID,
                        "DEFAULT",
                        "DEFAULT",
                        "Developer Portal",
                        PortalNavigationStructure.empty(),
                        null,
                        true,
                        PortalFixtures.PORTAL_URL
                    )
                )
            );
            when(environmentRepository.findAll()).thenReturn(Set.of(Environment.DEFAULT));

            assertThat(realSeedingUpgrader.upgrade()).isTrue();

            assertThat(navigationItemsCrudService.storage())
                .isNotEmpty()
                .extracting(PortalNavigationItem::getReference)
                .containsOnly(new NavigationItemReference.PortalReference(DEFAULT_PORTAL_ID));
            assertThat(portalCrudService.storage()).hasSize(1);
        }

        @Test
        @SneakyThrows
        void should_skip_an_environment_without_a_default_portal_and_succeed() {
            portalCrudService.initWith(
                List.of(
                    Portal.of(
                        DEFAULT_PORTAL_ID,
                        "DEFAULT",
                        "DEFAULT",
                        "Developer Portal",
                        PortalNavigationStructure.empty(),
                        null,
                        true,
                        PortalFixtures.PORTAL_URL
                    ),
                    PortalFixtures.aPortal(
                        PortalId.of("22222222-2222-2222-2222-222222222222"),
                        ANOTHER_ENVIRONMENT.getId(),
                        ANOTHER_ENVIRONMENT.getOrganizationId(),
                        "Automation Portal"
                    )
                )
            );
            when(environmentRepository.findAll()).thenReturn(Set.of(Environment.DEFAULT, ANOTHER_ENVIRONMENT));

            assertThat(realSeedingUpgrader.upgrade()).isTrue();

            assertThat(navigationItemsCrudService.storage())
                .isNotEmpty()
                .allSatisfy(item -> assertThat(item.getEnvironmentId()).isEqualTo("DEFAULT"));
        }
    }
}
