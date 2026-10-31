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
package io.gravitee.gateway.handlers.guardian.manager.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.gravitee.common.event.EventListener;
import io.gravitee.common.event.EventManager;
import io.gravitee.common.event.impl.SimpleEvent;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.definition.model.v4.guardian.Guardian;
import io.gravitee.gateway.handlers.guardian.ReactableGuardian;
import io.gravitee.gateway.reactor.ReactorEvent;
import io.gravitee.node.api.license.ForbiddenFeatureException;
import io.gravitee.node.api.license.InvalidLicenseException;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.secrets.api.discovery.Definition;
import io.gravitee.secrets.api.discovery.DefinitionMetadata;
import io.gravitee.secrets.api.event.SecretDiscoveryEvent;
import io.gravitee.secrets.api.event.SecretDiscoveryEventType;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
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
class GuardianManagerImplTest {

    private static final String GUARDIAN_ID = "guardian-id";
    private static final Date NOW = new Date();

    @Mock
    private EventManager eventManager;

    @Mock
    private LicenseManager licenseManager;

    private GuardianManagerImpl cut;

    @BeforeEach
    void setUp() {
        cut = new GuardianManagerImpl(eventManager, licenseManager);
    }

    @Test
    void should_deploy_guardian_and_discover_its_secrets() {
        ReactableGuardian guardian = guardian("env-a", NOW, "1");

        assertThat(cut.register(guardian)).isTrue();

        ArgumentCaptor<SecretDiscoveryEvent> discoverCaptor = ArgumentCaptor.forClass(SecretDiscoveryEvent.class);
        verify(eventManager).publishEvent(eq(SecretDiscoveryEventType.DISCOVER), discoverCaptor.capture());
        assertThat(discoverCaptor.getValue().envId()).isEqualTo("env-a");
        assertThat(discoverCaptor.getValue().definition()).isSameAs(guardian.getDefinition());
        assertThat(discoverCaptor.getValue().metadata().revision()).isEqualTo("1");
        verify(eventManager).publishEvent(ReactorEvent.DEPLOY, guardian);
        assertThat(cut.guardians()).containsExactly(guardian);
        assertThat(cut.get(GUARDIAN_ID, "env-a")).isSameAs(guardian);
    }

    @Test
    void should_not_deploy_guardian_refused_by_the_license() throws InvalidLicenseException, ForbiddenFeatureException {
        ReactableGuardian guardian = guardian("env-a", NOW, "1");
        doThrow(new ForbiddenFeatureException(List.of())).when(licenseManager).validatePluginFeatures(any(), any());

        assertThat(cut.register(guardian)).isFalse();

        verify(eventManager, never()).publishEvent(any(ReactorEvent.class), any());
        assertThat(cut.guardians()).isEmpty();
    }

    @Test
    void should_update_guardian_when_a_more_recent_copy_is_registered() {
        ReactableGuardian guardian = guardian("env-a", NOW, "1");
        ReactableGuardian newerGuardian = guardian("env-a", Date.from(NOW.toInstant().plus(Duration.ofHours(1))), "2");
        cut.register(guardian);

        assertThat(cut.register(newerGuardian)).isTrue();

        verify(eventManager).publishEvent(ReactorEvent.UPDATE, newerGuardian);
        ArgumentCaptor<SecretDiscoveryEvent> revokeCaptor = ArgumentCaptor.forClass(SecretDiscoveryEvent.class);
        verify(eventManager).publishEvent(eq(SecretDiscoveryEventType.REVOKE), revokeCaptor.capture());
        assertThat(revokeCaptor.getValue().metadata().revision()).isEqualTo("1");
        assertThat(cut.guardians()).containsExactly(newerGuardian);
    }

    @Test
    void should_ignore_an_older_copy_of_a_deployed_guardian() {
        ReactableGuardian guardian = guardian("env-a", NOW, "2");
        ReactableGuardian olderGuardian = guardian("env-a", Date.from(NOW.toInstant().minus(Duration.ofHours(1))), "1");
        cut.register(guardian);

        assertThat(cut.register(olderGuardian)).isFalse();

        verify(eventManager, never()).publishEvent(eq(ReactorEvent.UPDATE), any());
        assertThat(cut.get(GUARDIAN_ID, "env-a")).isSameAs(guardian);
    }

    @Test
    void should_undeploy_guardian_and_revoke_its_secrets() {
        ReactableGuardian guardian = guardian("env-a", NOW, "1");
        cut.register(guardian);

        cut.unregister(GUARDIAN_ID, "env-a");

        verify(eventManager).publishEvent(ReactorEvent.UNDEPLOY, guardian);
        ArgumentCaptor<SecretDiscoveryEvent> revokeCaptor = ArgumentCaptor.forClass(SecretDiscoveryEvent.class);
        verify(eventManager).publishEvent(eq(SecretDiscoveryEventType.REVOKE), revokeCaptor.capture());
        assertThat(revokeCaptor.getValue().envId()).isEqualTo("env-a");
        assertThat(cut.guardians()).isEmpty();
    }

    @Test
    void should_do_nothing_when_undeploying_an_unknown_guardian() {
        cut.unregister(GUARDIAN_ID, "env-a");

        verify(eventManager, never()).publishEvent(any(ReactorEvent.class), any());
        verify(eventManager, never()).publishEvent(any(SecretDiscoveryEventType.class), any());
    }

    @Test
    void should_deploy_the_same_id_independently_per_environment() {
        ReactableGuardian environmentA = guardian("env-a", NOW, "1");
        ReactableGuardian environmentB = guardian("env-b", Date.from(NOW.toInstant().plus(Duration.ofHours(1))), "5");
        cut.register(environmentA);
        cut.register(environmentB);

        cut.unregister(GUARDIAN_ID, "env-a");

        verify(eventManager).publishEvent(ReactorEvent.DEPLOY, environmentA);
        verify(eventManager).publishEvent(ReactorEvent.DEPLOY, environmentB);
        verify(eventManager, never()).publishEvent(eq(ReactorEvent.UPDATE), any());
        verify(eventManager).publishEvent(ReactorEvent.UNDEPLOY, environmentA);
        verify(eventManager, never()).publishEvent(ReactorEvent.UNDEPLOY, environmentB);
        assertThat(cut.guardians()).containsExactly(environmentB);
    }

    @Test
    void should_undeploy_every_environment_of_a_guardian() {
        ReactableGuardian environmentA = guardian("env-a", NOW, "1");
        ReactableGuardian environmentB = guardian("env-b", NOW, "5");
        cut.register(environmentA);
        cut.register(environmentB);

        cut.unregisterAll(GUARDIAN_ID);

        verify(eventManager).publishEvent(ReactorEvent.UNDEPLOY, environmentA);
        verify(eventManager).publishEvent(ReactorEvent.UNDEPLOY, environmentB);
        assertThat(cut.guardians()).isEmpty();
    }

    @Test
    void should_do_nothing_when_undeploying_every_environment_of_an_unknown_guardian() {
        cut.unregisterAll(GUARDIAN_ID);

        verify(eventManager, never()).publishEvent(any(ReactorEvent.class), any());
    }

    @Test
    void should_update_guardian_when_one_of_its_secrets_changes() {
        ReactableGuardian environmentA = guardian("env-a", NOW, "1");
        ReactableGuardian environmentB = guardian("env-b", NOW, "1");
        cut.register(environmentA);
        cut.register(environmentB);

        secretValueChangedListener().onEvent(secretValueChanged("env-a", new Definition("guardian", GUARDIAN_ID)));

        verify(eventManager).publishEvent(ReactorEvent.UPDATE, environmentA);
        verify(eventManager, never()).publishEvent(ReactorEvent.UPDATE, environmentB);
    }

    @Test
    void should_ignore_secret_changes_of_other_definitions_or_unknown_guardians() {
        cut.register(guardian("env-a", NOW, "1"));

        EventListener<SecretDiscoveryEventType, SecretDiscoveryEvent> listener = secretValueChangedListener();
        listener.onEvent(secretValueChanged("env-a", new Definition("shared-policy-group", GUARDIAN_ID)));
        listener.onEvent(secretValueChanged("env-a", new Definition("guardian", "unknown-guardian")));
        listener.onEvent(secretValueChanged("other-env", new Definition("guardian", GUARDIAN_ID)));

        verify(eventManager, never()).publishEvent(eq(ReactorEvent.UPDATE), any());
    }

    @SuppressWarnings("unchecked")
    private EventListener<SecretDiscoveryEventType, SecretDiscoveryEvent> secretValueChangedListener() {
        ArgumentCaptor<EventListener<SecretDiscoveryEventType, SecretDiscoveryEvent>> listenerCaptor = ArgumentCaptor.forClass(
            EventListener.class
        );
        verify(eventManager).subscribeForEvents(listenerCaptor.capture(), eq(SecretDiscoveryEventType.VALUE_CHANGED));
        return listenerCaptor.getValue();
    }

    private static SimpleEvent<SecretDiscoveryEventType, SecretDiscoveryEvent> secretValueChanged(
        String environmentId,
        Definition definition
    ) {
        return new SimpleEvent<>(
            SecretDiscoveryEventType.VALUE_CHANGED,
            new SecretDiscoveryEvent(environmentId, definition, new DefinitionMetadata(null))
        );
    }

    private static ReactableGuardian guardian(String environmentId, Date deployedAt, String version) {
        return ReactableGuardian.builder()
            .id(GUARDIAN_ID)
            .name("guardian-name")
            .environmentId(environmentId)
            .organizationId("org-id")
            .deployedAt(deployedAt)
            .definition(
                Guardian.builder()
                    .id(GUARDIAN_ID)
                    .environmentId(environmentId)
                    .version(version)
                    .phase(Guardian.Phase.REQUEST)
                    .policies(List.of(Step.builder().policy("a-policy").enabled(true).build()))
                    .build()
            )
            .build();
    }
}
