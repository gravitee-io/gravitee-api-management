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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.PortalRepository;
import io.gravitee.repository.management.model.Environment;
import io.gravitee.repository.management.model.Portal;
import java.util.List;
import java.util.Set;
import lombok.SneakyThrows;
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
class PortalNavigationStructureUpgraderTest {

    @Mock
    EnvironmentRepository environmentRepository;

    @Mock
    PortalRepository portalRepository;

    private PortalNavigationStructureUpgrader upgrader;

    @BeforeEach
    void setUp() {
        upgrader = new PortalNavigationStructureUpgrader(environmentRepository, portalRepository);
    }

    @Test
    @SneakyThrows
    void rewrites_legacy_array_to_top_navbar_map() {
        var portal = portal("[{\"path\":\"/a\"},{\"path\":\"/b\"}]");
        when(environmentRepository.findAll()).thenReturn(Set.of(Environment.DEFAULT));
        when(portalRepository.findByEnvironmentId(Environment.DEFAULT.getId())).thenReturn(List.of(portal));

        assertThat(upgrader.upgrade()).isTrue();

        ArgumentCaptor<Portal> captor = ArgumentCaptor.forClass(Portal.class);
        verify(portalRepository).update(captor.capture());
        assertThat(captor.getValue().getPortalNavigation()).isEqualTo("{\"TOP_NAVBAR\":[{\"path\":\"/a\"},{\"path\":\"/b\"}]}");
    }

    @Test
    @SneakyThrows
    void skips_portals_already_in_structure_shape() {
        var portal = portal("{\"TOP_NAVBAR\":[{\"path\":\"/a\"}]}");
        when(environmentRepository.findAll()).thenReturn(Set.of(Environment.DEFAULT));
        when(portalRepository.findByEnvironmentId(Environment.DEFAULT.getId())).thenReturn(List.of(portal));

        assertThat(upgrader.upgrade()).isTrue();

        verify(portalRepository).findByEnvironmentId(Environment.DEFAULT.getId());
        verifyNoMoreInteractions(portalRepository);
    }

    @Test
    @SneakyThrows
    void skips_portals_with_null_or_blank_navigation() {
        var nullNav = portal(null);
        var blankNav = portal("   ");
        when(environmentRepository.findAll()).thenReturn(Set.of(Environment.DEFAULT));
        when(portalRepository.findByEnvironmentId(Environment.DEFAULT.getId())).thenReturn(List.of(nullNav, blankNav));

        assertThat(upgrader.upgrade()).isTrue();

        verify(portalRepository).findByEnvironmentId(Environment.DEFAULT.getId());
        verifyNoMoreInteractions(portalRepository);
    }

    @Test
    @SneakyThrows
    void skips_portals_with_malformed_json() {
        var portal = portal("{not json");
        when(environmentRepository.findAll()).thenReturn(Set.of(Environment.DEFAULT));
        when(portalRepository.findByEnvironmentId(Environment.DEFAULT.getId())).thenReturn(List.of(portal));

        assertThat(upgrader.upgrade()).isTrue();

        verify(portalRepository).findByEnvironmentId(Environment.DEFAULT.getId());
        verifyNoMoreInteractions(portalRepository);
    }

    private static Portal portal(String portalNavigation) {
        return Portal.builder()
            .id("portal-id")
            .environmentId("env")
            .organizationId("org")
            .name("name")
            .portalNavigation(portalNavigation)
            .build();
    }
}
