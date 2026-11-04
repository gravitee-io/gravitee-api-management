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
package io.gravitee.gateway.handlers.guardian.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.event.EventManager;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.v4.guardian.Guardian;
import io.gravitee.gateway.api.genericevent.GenericEvent;
import io.gravitee.gateway.handlers.guardian.FakeGuardianReactorPlugin;
import io.gravitee.gateway.handlers.guardian.ReactableGuardian;
import io.gravitee.gateway.handlers.guardian.manager.impl.GuardianManagerImpl;
import io.gravitee.gateway.handlers.guardian.registry.GuardianRegistry;
import io.gravitee.gateway.reactor.ReactorEvent;
import io.gravitee.node.api.license.LicenseManager;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GuardianGenericEventSubscriberTest {

    private static final String GUARDIAN_ID = "guardian-id";
    private static final String ENVIRONMENT_ID = "env-a";
    private static final Instant UPDATED_AT = Instant.parse("2026-11-05T10:00:00Z");
    private static final String PAYLOAD = """
        {"id":"guardian-id","environmentId":"env-a","name":"My guardian","phase":"REQUEST",
         "policies":[{"name":"a step","policy":"a-policy","enabled":true}]}""";

    @Mock
    private EventManager eventManager;

    @Mock
    private LicenseManager licenseManager;

    private final ObjectMapper objectMapper = new GraviteeMapper(false);

    private GuardianManagerImpl guardianManager;
    private GuardianGenericEventSubscriber cut;

    @BeforeEach
    void setUp() {
        GuardianRegistry guardianRegistry = new GuardianRegistry();
        FakeGuardianReactorPlugin.install(eventManager, guardianRegistry);
        guardianManager = new GuardianManagerImpl(eventManager, licenseManager, guardianRegistry);
        cut = new GuardianGenericEventSubscriber(guardianManager, objectMapper);
    }

    @Test
    void should_deploy_the_guardian_of_a_published_event() {
        cut.onUpsert(event(GUARDIAN_ID, ENVIRONMENT_ID, UPDATED_AT, PAYLOAD));

        ReactableGuardian deployed = guardianManager.get(GUARDIAN_ID, ENVIRONMENT_ID);
        assertThat(deployed).isNotNull();
        assertThat(deployed.getName()).isEqualTo("My guardian");
        assertThat(deployed.getOrganizationId()).isEqualTo("org-id");
        assertThat(deployed.getDefinition().getPhase()).isEqualTo(Guardian.Phase.REQUEST);
        assertThat(deployed.getDefinition().getPolicies()).hasSize(1);
        verify(eventManager).publishEvent(ReactorEvent.DEPLOY, deployed);
    }

    @Test
    void should_derive_the_deployment_date_and_revision_from_the_event_update_date() {
        cut.onUpsert(event(GUARDIAN_ID, ENVIRONMENT_ID, UPDATED_AT, PAYLOAD));

        ReactableGuardian deployed = guardianManager.get(GUARDIAN_ID, ENVIRONMENT_ID);
        assertThat(deployed.getDeployedAt()).isEqualTo(Date.from(UPDATED_AT));
        assertThat(deployed.getRevision()).isEqualTo(String.valueOf(UPDATED_AT.toEpochMilli()));
    }

    @Test
    void should_identify_the_guardian_by_the_event_rather_than_by_its_definition() {
        cut.onUpsert(event("event-id", "env-b", UPDATED_AT, PAYLOAD));

        ReactableGuardian deployed = guardianManager.get("event-id", "env-b");
        assertThat(deployed).isNotNull();
        assertThat(deployed.getDefinition().getId()).isEqualTo("event-id");
        assertThat(deployed.getDefinition().getEnvironmentId()).isEqualTo("env-b");
        assertThat(guardianManager.get(GUARDIAN_ID, ENVIRONMENT_ID)).isNull();
    }

    @Test
    void should_update_the_guardian_when_a_newer_event_is_published() {
        cut.onUpsert(event(GUARDIAN_ID, ENVIRONMENT_ID, UPDATED_AT, PAYLOAD));

        cut.onUpsert(event(GUARDIAN_ID, ENVIRONMENT_ID, UPDATED_AT.plus(Duration.ofMinutes(1)), PAYLOAD.replace("My guardian", "Renamed")));

        ReactableGuardian deployed = guardianManager.get(GUARDIAN_ID, ENVIRONMENT_ID);
        assertThat(deployed.getName()).isEqualTo("Renamed");
        assertThat(deployed.getDeployedAt()).isEqualTo(Date.from(UPDATED_AT.plus(Duration.ofMinutes(1))));
        assertThat(deployed.getRevision()).isEqualTo(String.valueOf(UPDATED_AT.plus(Duration.ofMinutes(1)).toEpochMilli()));
        verify(eventManager).publishEvent(ReactorEvent.UPDATE, deployed);
    }

    @Test
    void should_ignore_an_event_whose_payload_is_not_a_guardian() {
        cut.onUpsert(event(GUARDIAN_ID, ENVIRONMENT_ID, UPDATED_AT, "{\"phase\":\"NOT_A_PHASE\"}"));

        assertThat(guardianManager.guardians()).isEmpty();
        verify(eventManager, never()).publishEvent(eq(ReactorEvent.DEPLOY), any());
    }

    @Test
    void should_keep_the_deployed_revision_when_an_update_payload_is_not_a_guardian() {
        cut.onUpsert(event(GUARDIAN_ID, ENVIRONMENT_ID, UPDATED_AT, PAYLOAD));
        ReactableGuardian deployed = guardianManager.get(GUARDIAN_ID, ENVIRONMENT_ID);

        cut.onUpsert(event(GUARDIAN_ID, ENVIRONMENT_ID, UPDATED_AT.plus(Duration.ofMinutes(1)), "{\"phase\":\"NOT_A_PHASE\"}"));

        assertThat(guardianManager.get(GUARDIAN_ID, ENVIRONMENT_ID)).isSameAs(deployed);
        assertThat(deployed.getRevision()).isEqualTo(String.valueOf(UPDATED_AT.toEpochMilli()));
        verify(eventManager, never()).publishEvent(eq(ReactorEvent.UPDATE), any());
        verify(eventManager, never()).publishEvent(eq(ReactorEvent.UNDEPLOY), any());
    }

    @Test
    void should_keep_the_deployed_revision_when_an_update_payload_has_no_phase() {
        cut.onUpsert(event(GUARDIAN_ID, ENVIRONMENT_ID, UPDATED_AT, PAYLOAD));
        ReactableGuardian deployed = guardianManager.get(GUARDIAN_ID, ENVIRONMENT_ID);

        cut.onUpsert(
            event(GUARDIAN_ID, ENVIRONMENT_ID, UPDATED_AT.plus(Duration.ofMinutes(1)), PAYLOAD.replace("\"phase\":\"REQUEST\",", ""))
        );

        assertThat(guardianManager.get(GUARDIAN_ID, ENVIRONMENT_ID)).isSameAs(deployed);
        verify(eventManager, never()).publishEvent(eq(ReactorEvent.UPDATE), any());
    }

    @Test
    void should_ignore_an_event_without_update_date() {
        cut.onUpsert(event(GUARDIAN_ID, ENVIRONMENT_ID, null, PAYLOAD));

        assertThat(guardianManager.guardians()).isEmpty();
        verify(eventManager, never()).publishEvent(eq(ReactorEvent.DEPLOY), any());
    }

    @Test
    void should_undeploy_the_guardian_of_a_removed_event() {
        cut.onUpsert(event(GUARDIAN_ID, ENVIRONMENT_ID, UPDATED_AT, PAYLOAD));
        ReactableGuardian deployed = guardianManager.get(GUARDIAN_ID, ENVIRONMENT_ID);

        cut.onRemove(event(GUARDIAN_ID, ENVIRONMENT_ID, null, null));

        assertThat(guardianManager.guardians()).isEmpty();
        verify(eventManager).publishEvent(ReactorEvent.UNDEPLOY, deployed);
    }

    @Test
    void should_ignore_the_removal_of_a_guardian_never_deployed() {
        cut.onRemove(event(GUARDIAN_ID, ENVIRONMENT_ID, null, null));

        verify(eventManager, never()).publishEvent(eq(ReactorEvent.UNDEPLOY), any());
    }

    private static GenericEvent event(String id, String environmentId, Instant updatedAt, String payload) {
        return new GenericEvent(GuardianGenericEventSubscriber.GUARDIAN_KIND, id, environmentId, "org-id", null, updatedAt, payload);
    }
}
