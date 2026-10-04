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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.repository.management.api.EventLatestRepository;
import io.gravitee.repository.management.api.OrganizationRepository;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import io.gravitee.repository.management.model.Organization;
import io.gravitee.rest.api.service.OrganizationService;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrganizationEventBootstrapUpgraderTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private EventLatestRepository eventLatestRepository;

    @Mock
    private OrganizationService organizationService;

    private OrganizationEventBootstrapUpgrader upgrader;

    @BeforeEach
    void setUp() {
        upgrader = new OrganizationEventBootstrapUpgrader(organizationRepository, eventLatestRepository, organizationService);
    }

    @Test
    void should_publish_a_snapshot_when_latest_event_is_missing() throws Exception {
        Organization organization = Organization.builder().id("org-1").build();
        when(organizationRepository.findAll()).thenReturn(Set.of(organization));
        when(eventLatestRepository.findByOrganizationId("org-1")).thenReturn(List.of());

        assertThat(upgrader.upgrade()).isTrue();

        verify(organizationService).publishOrganization("org-1");
    }

    @Test
    void should_preserve_an_existing_latest_event() throws Exception {
        Organization organization = Organization.builder().id("org-1").build();
        when(organizationRepository.findAll()).thenReturn(Set.of(organization));
        Event organizationEvent = new Event();
        organizationEvent.setType(EventType.PUBLISH_ORGANIZATION);
        when(eventLatestRepository.findByOrganizationId("org-1")).thenReturn(List.of(organizationEvent));

        assertThat(upgrader.upgrade()).isTrue();

        verify(organizationService, never()).publishOrganization("org-1");
    }

    @Test
    void should_publish_a_snapshot_when_only_an_api_event_exists() throws Exception {
        Organization organization = Organization.builder().id("org-1").build();
        when(organizationRepository.findAll()).thenReturn(Set.of(organization));
        Event apiEvent = new Event();
        apiEvent.setType(EventType.PUBLISH_API);
        when(eventLatestRepository.findByOrganizationId("org-1")).thenReturn(List.of(apiEvent));

        assertThat(upgrader.upgrade()).isTrue();

        verify(organizationService).publishOrganization("org-1");
    }

    @Test
    void should_run_after_latest_event_migration() {
        assertThat(upgrader.getOrder()).isEqualTo(UpgraderOrder.EVENTS_LATEST_UPGRADER + 1);
    }
}
