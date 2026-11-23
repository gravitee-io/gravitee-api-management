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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        assertThat(portal.get().getActiveThemeId()).isNull();
    }

    @Test
    public void should_read_active_theme_id_from_fixture() throws Exception {
        Optional<Portal> portal = portalRepository.findByIdAndEnvironmentId("portal2", "environment1");

        assertThat(portal).isPresent();
        assertThat(portal.get().getActiveThemeId()).isEqualTo("theme-portal2");
    }

    @Test
    public void should_round_trip_active_theme_id_on_create() throws Exception {
        Portal toCreate = Portal.builder()
            .id("themed-portal")
            .environmentId("environment1")
            .organizationId("organization1")
            .name("Themed Portal")
            .activeThemeId("theme-xyz")
            .build();

        Portal created = portalRepository.create(toCreate);

        assertThat(created.getActiveThemeId()).isEqualTo("theme-xyz");
        assertThat(portalRepository.findById("themed-portal")).hasValueSatisfying(found ->
            assertThat(found.getActiveThemeId()).isEqualTo("theme-xyz")
        );
    }

    @Test
    public void should_round_trip_active_theme_id_on_update() throws Exception {
        Portal toUpdate = Portal.builder()
            .id("portal1")
            .environmentId("environment1")
            .organizationId("organization1")
            .name("Default Portal")
            .activeThemeId("theme-abc")
            .build();

        Portal updated = portalRepository.update(toUpdate);

        assertThat(updated.getActiveThemeId()).isEqualTo("theme-abc");
        assertThat(portalRepository.findById("portal1")).hasValueSatisfying(found ->
            assertThat(found.getActiveThemeId()).isEqualTo("theme-abc")
        );
    }

    @Test
    public void should_read_not_default_and_placeholder_url_when_not_set() throws Exception {
        Optional<Portal> portal = portalRepository.findById("portal1");

        assertThat(portal).hasValueSatisfying(found -> {
            assertThat(found.isDefaultPortal()).isFalse();
            assertThat(found.getUrl()).isEqualTo(Portal.URL_PLACEHOLDER);
            assertThat(found.getAccessPointId()).isNull();
        });
    }

    @Test
    public void should_round_trip_default_portal_and_url_on_create() throws Exception {
        Portal toCreate = Portal.builder()
            .id("default-portal")
            .environmentId("environment3")
            .organizationId("organization2")
            .name("Default Portal")
            .defaultPortal(true)
            .url("https://portal.env3.example")
            .accessPointId("access-point-env3")
            .build();

        portalRepository.create(toCreate);

        assertThat(portalRepository.findById("default-portal")).hasValueSatisfying(found -> {
            assertThat(found.isDefaultPortal()).isTrue();
            assertThat(found.getUrl()).isEqualTo("https://portal.env3.example");
            assertThat(found.getAccessPointId()).isEqualTo("access-point-env3");
        });
    }

    @Test
    public void should_round_trip_default_portal_and_url_on_update() throws Exception {
        Portal toUpdate = Portal.builder()
            .id("portal1")
            .environmentId("environment1")
            .organizationId("organization1")
            .name("Default Portal")
            .defaultPortal(true)
            .url("https://portal.env1.example")
            .accessPointId("access-point-env1")
            .build();

        portalRepository.update(toUpdate);

        assertThat(portalRepository.findById("portal1")).hasValueSatisfying(found -> {
            assertThat(found.isDefaultPortal()).isTrue();
            assertThat(found.getUrl()).isEqualTo("https://portal.env1.example");
            assertThat(found.getAccessPointId()).isEqualTo("access-point-env1");
        });
    }

    @Test
    public void should_update_only_the_name_in_the_target_environment() throws Exception {
        var original = portalRepository.findById("portal3").orElseThrow();

        var updated = portalRepository.updateName("portal3", "environment2", "Renamed").orElseThrow();

        assertThat(updated.getName()).isEqualTo("Renamed");
        assertThat(updated).usingRecursiveComparison().ignoringFields("name").isEqualTo(original);
        assertThat(portalRepository.findById("portal3")).hasValueSatisfying(found ->
            assertThat(found).usingRecursiveComparison().isEqualTo(updated)
        );
    }

    @Test
    public void should_not_update_a_missing_portal_or_a_portal_in_another_environment() throws Exception {
        var original = portalRepository.findById("portal3").orElseThrow();

        assertThat(portalRepository.updateName("missing", "environment1", "Renamed")).isEmpty();
        assertThat(portalRepository.updateName("portal3", "environment1", "Renamed")).isEmpty();
        assertThat(portalRepository.findById("portal3")).hasValueSatisfying(found ->
            assertThat(found).usingRecursiveComparison().isEqualTo(original)
        );
    }

    @Test
    public void should_find_default_by_environment() throws Exception {
        Optional<Portal> portal = portalRepository.findDefaultByEnvironmentId("environment2");

        assertThat(portal).hasValueSatisfying(found -> {
            assertThat(found.getId()).isEqualTo("portal3");
            assertThat(found.isDefaultPortal()).isTrue();
            assertThat(found.getUrl()).isEqualTo("https://portal.env2.example");
        });
    }

    @Test
    public void should_find_every_portal_referencing_an_access_point() throws Exception {
        List<Portal> portals = portalRepository.findByAccessPointId("shared-access-point");

        assertThat(portals).extracting(Portal::getId).containsExactlyInAnyOrder("portal3", "portal4");
    }

    @Test
    public void should_find_no_portal_for_an_unreferenced_access_point() throws Exception {
        assertThat(portalRepository.findByAccessPointId("unknown-access-point")).isEmpty();
    }

    @Test
    public void should_reject_a_lookup_by_null_access_point_id() {
        assertThatThrownBy(() -> portalRepository.findByAccessPointId(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("Access point id must not be null");
    }

    @Test
    public void should_replace_then_clear_the_access_point_id() throws Exception {
        Portal portal = portalRepository.findById("portal3").orElseThrow();

        portal.setAccessPointId("access-point-a");
        portalRepository.update(portal);
        assertThat(portalRepository.findById("portal3")).hasValueSatisfying(found -> {
            assertThat(found.getAccessPointId()).isEqualTo("access-point-a");
            assertThat(found.getUrl()).isEqualTo("https://portal.env2.example");
        });
        assertThat(portalRepository.findByAccessPointId("access-point-a")).extracting(Portal::getId).containsExactly("portal3");
        assertThat(portalRepository.findByAccessPointId("shared-access-point")).extracting(Portal::getId).containsExactly("portal4");

        portal.setAccessPointId("access-point-b");
        portalRepository.update(portal);
        assertThat(portalRepository.findById("portal3")).hasValueSatisfying(found -> {
            assertThat(found.getAccessPointId()).isEqualTo("access-point-b");
            assertThat(found.getUrl()).isEqualTo("https://portal.env2.example");
        });
        assertThat(portalRepository.findByAccessPointId("access-point-a")).isEmpty();
        assertThat(portalRepository.findByAccessPointId("access-point-b")).extracting(Portal::getId).containsExactly("portal3");

        portal.setAccessPointId(null);
        portalRepository.update(portal);
        assertThat(portalRepository.findById("portal3")).hasValueSatisfying(found -> {
            assertThat(found.getAccessPointId()).isNull();
            assertThat(found.getUrl()).isEqualTo("https://portal.env2.example");
        });
        assertThat(portalRepository.findByAccessPointId("access-point-a")).isEmpty();
        assertThat(portalRepository.findByAccessPointId("access-point-b")).isEmpty();
    }

    @Test
    public void should_find_no_default_when_environment_has_none() throws Exception {
        assertThat(portalRepository.findDefaultByEnvironmentId("environment1")).isEmpty();
    }

    @Test
    public void should_not_find_default_of_another_environment() throws Exception {
        assertThat(portalRepository.findDefaultByEnvironmentId("environment3")).isEmpty();
    }

    @Test
    public void should_find_the_default_with_the_lowest_id_when_environment_has_several() throws Exception {
        portalRepository.create(aDefaultPortal("default-b", "environment3"));
        portalRepository.create(aDefaultPortal("default-c", "environment3"));
        portalRepository.create(aDefaultPortal("default-a", "environment3"));

        assertThat(portalRepository.findDefaultByEnvironmentId("environment3")).hasValueSatisfying(found ->
            assertThat(found.getId()).isEqualTo("default-a")
        );
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
    public void should_find_by_active_theme_id_and_environment() throws Exception {
        List<Portal> portals = portalRepository.findByActiveThemeIdAndEnvironmentId("theme-portal2", "environment1");

        assertThat(portals).extracting(Portal::getId).containsExactly("portal2");
    }

    @Test
    public void should_find_nothing_by_active_theme_id_when_environment_differs() throws Exception {
        List<Portal> portals = portalRepository.findByActiveThemeIdAndEnvironmentId("theme-portal2", "environment2");

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

    private static Portal aDefaultPortal(String id, String environmentId) {
        return Portal.builder()
            .id(id)
            .environmentId(environmentId)
            .organizationId("organization2")
            .name(id)
            .defaultPortal(true)
            .url("https://" + id + ".example")
            .build();
    }
}
