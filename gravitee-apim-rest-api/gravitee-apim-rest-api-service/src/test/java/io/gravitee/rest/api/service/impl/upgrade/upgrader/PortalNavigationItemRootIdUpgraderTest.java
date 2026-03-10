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
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.PortalNavigationItemRepository;
import io.gravitee.repository.management.model.Environment;
import io.gravitee.repository.management.model.PortalNavigationItem;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import lombok.SneakyThrows;
import org.junit.function.ThrowingRunnable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class PortalNavigationItemRootIdUpgraderTest {

    private static final Environment ANOTHER_ENVIRONMENT = Environment.builder()
        .id("ANOTHER_ENVIRONMENT")
        .hrids(List.of("another environment"))
        .name("another environment")
        .organizationId("ANOTHER_ORG")
        .build();

    @Mock
    EnvironmentRepository environmentRepository;

    @Mock
    PortalNavigationItemRepository portalNavigationItemRepository;

    private PortalNavigationItemRootIdUpgrader upgrader;

    @BeforeEach
    public void setUp() {
        upgrader = new PortalNavigationItemRootIdUpgrader(environmentRepository, portalNavigationItemRepository);
    }

    @Test
    @SneakyThrows
    void should_return_true_when_no_environments() {
        when(environmentRepository.findAll()).thenReturn(Collections.emptySet());

        assertThat(upgrader.upgrade()).isTrue();

        verifyNoInteractions(portalNavigationItemRepository);
    }

    @Test
    @SneakyThrows
    void should_return_true_when_no_nav_items_in_environment() {
        when(environmentRepository.findAll()).thenReturn(Set.of(Environment.DEFAULT));
        when(portalNavigationItemRepository.findAllByOrganizationIdAndEnvironmentId("DEFAULT", "DEFAULT")).thenReturn(
            Collections.emptyList()
        );

        assertThat(upgrader.upgrade()).isTrue();

        verify(portalNavigationItemRepository, never()).update(any());
    }

    @Test
    @SneakyThrows
    void should_throw_upgrader_exception_on_repository_error() {
        when(environmentRepository.findAll()).thenThrow(new TechnicalException("connection failed"));

        final ThrowingRunnable throwing = () -> upgrader.upgrade();

        Exception exception = assertThrows(UpgraderException.class, throwing);
        assertThat(exception.getMessage()).contains("connection failed");
    }

    @Test
    @SneakyThrows
    void should_set_root_id_to_self_for_root_items() {
        when(environmentRepository.findAll()).thenReturn(Set.of(Environment.DEFAULT));

        PortalNavigationItem rootFolder = navItem("folder-1", null, null);
        PortalNavigationItem rootPage = navItem("page-1", null, null);

        when(portalNavigationItemRepository.findAllByOrganizationIdAndEnvironmentId("DEFAULT", "DEFAULT")).thenReturn(
            List.of(rootFolder, rootPage)
        );

        assertThat(upgrader.upgrade()).isTrue();

        ArgumentCaptor<PortalNavigationItem> captor = ArgumentCaptor.forClass(PortalNavigationItem.class);
        verify(portalNavigationItemRepository, times(2)).update(captor.capture());

        List<PortalNavigationItem> updated = captor.getAllValues();
        assertThat(updated).extracting(PortalNavigationItem::getId).containsExactlyInAnyOrder("folder-1", "page-1");
        assertThat(updated).allSatisfy(item -> assertThat(item.getRootId()).isEqualTo(item.getId()));
    }

    @Test
    @SneakyThrows
    void should_resolve_root_id_for_nested_items() {
        when(environmentRepository.findAll()).thenReturn(Set.of(Environment.DEFAULT));

        PortalNavigationItem root = navItem("root", null, null);
        PortalNavigationItem child = navItem("child", "root", null);

        when(portalNavigationItemRepository.findAllByOrganizationIdAndEnvironmentId("DEFAULT", "DEFAULT")).thenReturn(List.of(root, child));

        assertThat(upgrader.upgrade()).isTrue();

        ArgumentCaptor<PortalNavigationItem> captor = ArgumentCaptor.forClass(PortalNavigationItem.class);
        verify(portalNavigationItemRepository, times(2)).update(captor.capture());

        PortalNavigationItem updatedChild = captor
            .getAllValues()
            .stream()
            .filter(i -> "child".equals(i.getId()))
            .findFirst()
            .orElseThrow();
        assertThat(updatedChild.getRootId()).isEqualTo("root");
    }

    @Test
    @SneakyThrows
    void should_resolve_root_id_for_deeply_nested_hierarchy() {
        when(environmentRepository.findAll()).thenReturn(Set.of(Environment.DEFAULT));

        PortalNavigationItem root = navItem("root", null, null);
        PortalNavigationItem level1 = navItem("level1", "root", null);
        PortalNavigationItem level2 = navItem("level2", "level1", null);
        PortalNavigationItem level3 = navItem("level3", "level2", null);

        when(portalNavigationItemRepository.findAllByOrganizationIdAndEnvironmentId("DEFAULT", "DEFAULT")).thenReturn(
            List.of(root, level1, level2, level3)
        );

        assertThat(upgrader.upgrade()).isTrue();

        ArgumentCaptor<PortalNavigationItem> captor = ArgumentCaptor.forClass(PortalNavigationItem.class);
        verify(portalNavigationItemRepository, times(4)).update(captor.capture());

        for (PortalNavigationItem updated : captor.getAllValues()) {
            assertThat(updated.getRootId()).isEqualTo("root");
        }
    }

    @Test
    @SneakyThrows
    void should_skip_items_that_already_have_valid_root_id() {
        when(environmentRepository.findAll()).thenReturn(Set.of(Environment.DEFAULT));

        PortalNavigationItem alreadyMigrated = navItem("item-1", null, "item-1");
        PortalNavigationItem needsMigration = navItem("item-2", null, null);

        when(portalNavigationItemRepository.findAllByOrganizationIdAndEnvironmentId("DEFAULT", "DEFAULT")).thenReturn(
            List.of(alreadyMigrated, needsMigration)
        );

        assertThat(upgrader.upgrade()).isTrue();

        ArgumentCaptor<PortalNavigationItem> captor = ArgumentCaptor.forClass(PortalNavigationItem.class);
        verify(portalNavigationItemRepository, times(1)).update(captor.capture());

        assertThat(captor.getValue().getId()).isEqualTo("item-2");
    }

    @Test
    @SneakyThrows
    void should_migrate_items_with_zero_root_id_sentinel() {
        when(environmentRepository.findAll()).thenReturn(Set.of(Environment.DEFAULT));

        String zeroRootId = "00000000-0000-0000-0000-000000000000";
        PortalNavigationItem itemWithZeroRoot = navItem("item-zero", null, zeroRootId);

        when(portalNavigationItemRepository.findAllByOrganizationIdAndEnvironmentId("DEFAULT", "DEFAULT")).thenReturn(
            List.of(itemWithZeroRoot)
        );

        assertThat(upgrader.upgrade()).isTrue();

        ArgumentCaptor<PortalNavigationItem> captor = ArgumentCaptor.forClass(PortalNavigationItem.class);
        verify(portalNavigationItemRepository, times(1)).update(captor.capture());

        assertThat(captor.getValue().getId()).isEqualTo("item-zero");
        assertThat(captor.getValue().getRootId()).isEqualTo("item-zero");
    }

    @Test
    @SneakyThrows
    void should_process_multiple_environments() {
        when(environmentRepository.findAll()).thenReturn(Set.of(Environment.DEFAULT, ANOTHER_ENVIRONMENT));

        PortalNavigationItem itemEnv1 = navItem("item-env1", null, null);
        PortalNavigationItem itemEnv2 = navItem("item-env2", null, null);

        when(portalNavigationItemRepository.findAllByOrganizationIdAndEnvironmentId("DEFAULT", "DEFAULT")).thenReturn(List.of(itemEnv1));
        when(portalNavigationItemRepository.findAllByOrganizationIdAndEnvironmentId("ANOTHER_ORG", "ANOTHER_ENVIRONMENT")).thenReturn(
            List.of(itemEnv2)
        );

        assertThat(upgrader.upgrade()).isTrue();

        verify(portalNavigationItemRepository, times(2)).update(any());
    }

    @Test
    @SneakyThrows
    void should_throw_when_orphaned_items_cannot_be_resolved() {
        when(environmentRepository.findAll()).thenReturn(Set.of(Environment.DEFAULT));

        PortalNavigationItem orphan = navItem("orphan", "missing-parent", null);

        when(portalNavigationItemRepository.findAllByOrganizationIdAndEnvironmentId("DEFAULT", "DEFAULT")).thenReturn(List.of(orphan));

        final ThrowingRunnable throwing = () -> upgrader.upgrade();

        Exception exception = assertThrows(UpgraderException.class, throwing);
        assertThat(exception.getMessage()).contains("Unable to resolve rootId");
        verify(portalNavigationItemRepository, never()).update(any());
    }

    @Test
    @SneakyThrows
    void should_throw_when_cycle_prevents_root_resolution() {
        when(environmentRepository.findAll()).thenReturn(Set.of(Environment.DEFAULT));

        PortalNavigationItem nodeA = navItem("A", "B", null);
        PortalNavigationItem nodeB = navItem("B", "A", null);

        when(portalNavigationItemRepository.findAllByOrganizationIdAndEnvironmentId("DEFAULT", "DEFAULT")).thenReturn(
            List.of(nodeA, nodeB)
        );

        final ThrowingRunnable throwing = () -> upgrader.upgrade();

        Exception exception = assertThrows(UpgraderException.class, throwing);
        assertThat(exception.getMessage()).contains("Unable to resolve rootId");
        verify(portalNavigationItemRepository, never()).update(any());
    }

    @Test
    void should_have_correct_order() {
        assertThat(upgrader.getOrder()).isEqualTo(UpgraderOrder.PORTAL_NAVIGATION_ITEM_ROOT_ID_UPGRADER);
    }

    private static PortalNavigationItem navItem(String id, String parentId, String rootId) {
        return PortalNavigationItem.builder()
            .id(id)
            .organizationId("DEFAULT")
            .environmentId("DEFAULT")
            .title("Nav Item " + id)
            .type(PortalNavigationItem.Type.FOLDER)
            .area(PortalNavigationItem.Area.TOP_NAVBAR)
            .parentId(parentId)
            .rootId(rootId)
            .order(0)
            .published(true)
            .visibility(PortalNavigationItem.Visibility.PUBLIC)
            .build();
    }
}
