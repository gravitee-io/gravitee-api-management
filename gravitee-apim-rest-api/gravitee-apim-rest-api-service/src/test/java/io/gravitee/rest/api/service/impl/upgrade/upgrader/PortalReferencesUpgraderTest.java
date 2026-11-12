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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import inmemory.PortalCrudServiceInMemory;
import inmemory.PortalNavigationItemsCrudServiceInMemory;
import io.gravitee.apim.core.portal.domain_service.PortalContentOwnershipDomainService;
import io.gravitee.apim.core.portal.model.Portal;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal.model.PortalNavigationStructure;
import io.gravitee.apim.core.portal.model.PortalVisibility;
import io.gravitee.apim.core.portal_page.crud_service.PortalNavigationItemCrudService;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.model.Environment;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.net.URI;
import java.net.URL;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalReferencesUpgraderTest {

    private static final Environment ENV_A = Environment.builder().id("env-a").organizationId("org-1").name("A").build();
    private static final Environment ENV_B = Environment.builder().id("env-b").organizationId("org-1").name("B").build();
    private static final Environment ENV_C = Environment.builder().id("env-c").organizationId("org-2").name("C").build();
    private static final PortalId PORTAL_A = PortalId.of("11111111-1111-1111-1111-111111111111");
    private static final PortalId PORTAL_B = PortalId.of("22222222-2222-2222-2222-222222222222");

    @Mock
    EnvironmentRepository environmentRepository;

    private final PortalCrudServiceInMemory portalCrudService = new PortalCrudServiceInMemory();
    private final PortalNavigationItemsCrudServiceInMemory navigationItemCrudService = new PortalNavigationItemsCrudServiceInMemory();
    private final Logger logger = (Logger) LoggerFactory.getLogger(PortalReferencesUpgrader.class);
    private final Level originalLevel = logger.getLevel();
    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();
    private PortalReferencesUpgrader upgrader;

    @BeforeEach
    void setUp() {
        upgrader = new PortalReferencesUpgrader(
            environmentRepository,
            new PortalContentOwnershipDomainService(portalCrudService, navigationItemCrudService),
            portalCrudService
        );
        logs.start();
        logger.addAppender(logs);
        logger.setLevel(Level.INFO);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(logs);
        logger.setLevel(originalLevel);
    }

    @SneakyThrows
    private void givenEnvironments(Environment... environments) {
        when(environmentRepository.findAll()).thenReturn(new LinkedHashSet<>(List.of(environments)));
    }

    @SneakyThrows
    private static URL portalUrl() {
        return URI.create("https://portal.acme.io/next").toURL();
    }

    private void givenPortal(PortalId id, Environment environment, boolean defaultPortal) {
        portalCrudService.create(
            Portal.of(
                id,
                environment.getId(),
                environment.getOrganizationId(),
                "Portal of " + environment.getName(),
                PortalNavigationStructure.empty(),
                null,
                defaultPortal,
                portalUrl()
            )
        );
    }

    private PortalNavigationFolder itemOnTheZeroUuid(Environment environment) {
        var item = PortalNavigationFolder.builder()
            .id(PortalNavigationItemId.random())
            .organizationId(environment.getOrganizationId())
            .environmentId(environment.getId())
            .reference(NavigationItemReference.defaultReference())
            .title("Docs")
            .segment("docs")
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .published(true)
            .visibility(PortalVisibility.PUBLIC)
            .build();
        navigationItemCrudService.create(item);
        return item;
    }

    private List<String> logLines(Level level) {
        return logs.list
            .stream()
            .filter(event -> level.equals(event.getLevel()))
            .map(ILoggingEvent::getFormattedMessage)
            .toList();
    }

    private static NavigationItemReference onPortal(PortalId portalId) {
        return new NavigationItemReference.PortalReference(portalId);
    }

    @Test
    @SneakyThrows
    void should_move_each_environment_onto_its_own_portal() {
        givenEnvironments(ENV_A, ENV_B);
        givenPortal(PORTAL_A, ENV_A, true);
        givenPortal(PORTAL_B, ENV_B, true);
        var itemA = itemOnTheZeroUuid(ENV_A);
        var itemB = itemOnTheZeroUuid(ENV_B);

        assertThat(upgrader.upgrade()).isTrue();

        assertThat(itemA.getReference()).isEqualTo(onPortal(PORTAL_A));
        assertThat(itemB.getReference()).isEqualTo(onPortal(PORTAL_B));
    }

    @Test
    @SneakyThrows
    void should_skip_an_environment_without_portal_and_still_succeed() {
        givenEnvironments(ENV_A, ENV_B);
        givenPortal(PORTAL_B, ENV_B, true);
        var postponedItem = itemOnTheZeroUuid(ENV_A);
        var movedItem = itemOnTheZeroUuid(ENV_B);

        assertThat(upgrader.upgrade()).isTrue();

        assertThat(postponedItem.getReference()).isEqualTo(NavigationItemReference.defaultReference());
        assertThat(movedItem.getReference()).isEqualTo(onPortal(PORTAL_B));
        assertThat(logLines(Level.WARN))
            .singleElement()
            .asString()
            .contains(ENV_A.getId(), "no portal", "first portal")
            .doesNotContain("several");
    }

    @Test
    @SneakyThrows
    void should_skip_an_environment_with_several_portals_and_no_default_and_still_succeed() {
        givenEnvironments(ENV_A, ENV_B);
        givenPortal(PORTAL_A, ENV_A, false);
        givenPortal(PortalId.of("33333333-3333-3333-3333-333333333333"), ENV_A, false);
        givenPortal(PORTAL_B, ENV_B, true);
        var keptItem = itemOnTheZeroUuid(ENV_A);
        var movedItem = itemOnTheZeroUuid(ENV_B);

        assertThat(upgrader.upgrade()).isTrue();

        assertThat(keptItem.getReference()).isEqualTo(NavigationItemReference.defaultReference());
        assertThat(movedItem.getReference()).isEqualTo(onPortal(PORTAL_B));
        assertThat(logLines(Level.WARN))
            .singleElement()
            .asString()
            .contains(ENV_A.getId(), "several portals", "no default", "manual")
            .doesNotContain("first portal");
    }

    @Test
    @SneakyThrows
    void should_use_the_only_portal_of_an_automation_environment() {
        givenEnvironments(ENV_A);
        givenPortal(PORTAL_A, ENV_A, false);
        var item = itemOnTheZeroUuid(ENV_A);

        assertThat(upgrader.upgrade()).isTrue();

        assertThat(item.getReference()).isEqualTo(onPortal(PORTAL_A));
        assertThat(portalCrudService.storage())
            .singleElement()
            .satisfies(portal -> assertThat(portal.isDefault()).isFalse());
    }

    @Test
    @SneakyThrows
    void should_succeed_with_nothing_to_move() {
        givenEnvironments(ENV_A);
        givenPortal(PORTAL_A, ENV_A, true);

        assertThat(upgrader.upgrade()).isTrue();

        assertThat(logLines(Level.INFO)).isEmpty();
        assertThat(logLines(Level.WARN)).isEmpty();
    }

    @Test
    @SneakyThrows
    void should_change_nothing_on_a_second_run() {
        givenEnvironments(ENV_A);
        givenPortal(PORTAL_A, ENV_A, true);
        itemOnTheZeroUuid(ENV_A);
        upgrader.upgrade();
        var afterFirstRun = navigationItemCrudService.storage().stream().map(PortalNavigationItem::getReference).toList();
        logs.list.clear();

        assertThat(upgrader.upgrade()).isTrue();

        assertThat(navigationItemCrudService.storage()).extracting(PortalNavigationItem::getReference).isEqualTo(afterFirstRun);
        assertThat(logLines(Level.INFO)).isEmpty();
    }

    @Test
    @SneakyThrows
    void should_log_one_line_per_changed_environment() {
        givenEnvironments(ENV_A, ENV_B, ENV_C);
        givenPortal(PORTAL_A, ENV_A, true);
        givenPortal(PORTAL_B, ENV_B, true);
        givenPortal(PortalId.of("33333333-3333-3333-3333-333333333333"), ENV_C, true);
        itemOnTheZeroUuid(ENV_A);
        itemOnTheZeroUuid(ENV_A);
        itemOnTheZeroUuid(ENV_B);

        upgrader.upgrade();

        assertThat(logLines(Level.INFO))
            .hasSize(2)
            .anySatisfy(line -> assertThat(line).contains("env-a").contains("2 navigation item"))
            .anySatisfy(line -> assertThat(line).contains("env-b").contains("1 navigation item"))
            .noneMatch(line -> line.contains("env-c"));
    }

    @Test
    @SneakyThrows
    void should_fail_so_the_run_is_not_recorded_when_a_bulk_update_fails() {
        givenEnvironments(ENV_A);
        givenPortal(PORTAL_A, ENV_A, true);
        var failingNavigation = mock(PortalNavigationItemCrudService.class);
        when(failingNavigation.findReferencedPortalIds(any())).thenReturn(Set.of(PortalId.ZERO));
        when(failingNavigation.updatePortalReferences(any(), any(), any())).thenThrow(new TechnicalManagementException("boom"));
        var failing = new PortalReferencesUpgrader(
            environmentRepository,
            new PortalContentOwnershipDomainService(portalCrudService, failingNavigation),
            portalCrudService
        );

        assertThatThrownBy(failing::upgrade).isInstanceOf(UpgraderException.class);
    }

    @Test
    @SneakyThrows
    void should_fail_when_the_environments_cannot_be_read() {
        when(environmentRepository.findAll()).thenThrow(new TechnicalException("boom"));

        assertThatThrownBy(() -> upgrader.upgrade()).isInstanceOf(UpgraderException.class);
    }

    @Test
    void should_run_after_every_portal_navigation_upgrader() {
        assertThat(upgrader.getOrder())
            .isEqualTo(728)
            .isGreaterThan(UpgraderOrder.ENVIRONMENTS_DEFAULT_APIS_FOLDER_UPGRADER)
            .isGreaterThan(UpgraderOrder.PORTAL_NAVIGATION_ITEM_API_OWNED_CONSOLE_MIGRATION_UPGRADER)
            .isGreaterThan(UpgraderOrder.PORTAL_NAVIGATION_ITEM_API_OWNED_REKEY_UPGRADER)
            .isGreaterThan(UpgraderOrder.PORTAL_NAVIGATION_STRUCTURE_UPGRADER)
            .isGreaterThan(UpgraderOrder.PORTAL_NAVIGATION_ITEM_DEFAULT_SEGMENT_UPGRADER)
            .isGreaterThan(UpgraderOrder.PORTAL_NAVIGATION_ITEM_ROOT_ID_UPGRADER)
            .isGreaterThan(UpgraderOrder.ENVIRONMENTS_DEFAULT_PORTAL_NAVIGATION_ITEMS_UPGRADER);
    }
}
