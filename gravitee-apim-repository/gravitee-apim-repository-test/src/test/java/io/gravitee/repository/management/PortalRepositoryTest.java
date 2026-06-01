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
package io.gravitee.repository.management;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.repository.management.model.Portal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * @author GraviteeSource Team
 */
public class PortalRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/portal-tests/";
    }

    @Test
    public void should_find_by_id_and_environment() throws Exception {
        Optional<Portal> portal = portalRepository.findByIdAndEnvironmentId("portal1", "environment1");

        assertThat(portal).isPresent();
        assertThat(portal.get().getName()).isEqualTo("Default Portal");
        assertThat(portal.get().getOrganizationId()).isEqualTo("organization1");
    }

    @Test
    public void should_not_find_by_id_when_environment_differs() throws Exception {
        Optional<Portal> portal = portalRepository.findByIdAndEnvironmentId("portal1", "environment2");

        assertThat(portal).isNotPresent();
    }

    @Test
    public void should_find_by_environment() throws Exception {
        List<Portal> portals = portalRepository.findByEnvironmentId("environment1");

        assertThat(portals).extracting(Portal::getId).containsExactlyInAnyOrder("portal1", "portal2");
    }

    @Test
    public void should_find_nothing_when_environment_unknown() throws Exception {
        List<Portal> portals = portalRepository.findByEnvironmentId("unknown-env");

        assertThat(portals).isEmpty();
    }

    @Test
    public void should_create() throws Exception {
        Portal toCreate = Portal.builder()
            .id("new-portal")
            .environmentId("environment1")
            .organizationId("organization1")
            .name("Created Portal")
            .build();

        Portal created = portalRepository.create(toCreate);

        assertThat(created).isEqualTo(toCreate);
        assertThat(portalRepository.findById("new-portal")).hasValue(toCreate);
    }

    @Test
    public void should_update() throws Exception {
        Portal toUpdate = Portal.builder()
            .id("portal1")
            .environmentId("environment1")
            .organizationId("organization1")
            .name("Renamed Portal")
            .build();

        Portal updated = portalRepository.update(toUpdate);

        assertThat(updated.getName()).isEqualTo("Renamed Portal");
        assertThat(portalRepository.findById("portal1")).hasValue(toUpdate);
    }

    @Test
    public void should_delete() throws Exception {
        portalRepository.delete("portal1");

        assertThat(portalRepository.findById("portal1")).isNotPresent();
    }

    @Test
    public void should_delete_by_environment() throws Exception {
        portalRepository.deleteByEnvironmentId("environment1");

        Set<Portal> remaining = portalRepository.findAll();
        assertThat(remaining).extracting(Portal::getId).containsExactlyInAnyOrder("portal3", "portal4");
    }

    @Test
    public void should_delete_by_organization() throws Exception {
        portalRepository.deleteByOrganizationId("organization1");

        Set<Portal> remaining = portalRepository.findAll();
        assertThat(remaining).extracting(Portal::getId).containsExactly("portal4");
    }
}
