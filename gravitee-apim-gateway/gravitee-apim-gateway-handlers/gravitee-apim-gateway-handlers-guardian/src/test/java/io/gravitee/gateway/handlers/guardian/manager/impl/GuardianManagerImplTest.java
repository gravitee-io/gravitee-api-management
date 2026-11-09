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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.read.ListAppender;
import io.gravitee.common.event.EventListener;
import io.gravitee.common.event.EventManager;
import io.gravitee.common.event.impl.SimpleEvent;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.definition.model.v4.guardian.Guardian;
import io.gravitee.gateway.handlers.guardian.FakeGuardianReactorPlugin;
import io.gravitee.gateway.handlers.guardian.ReactableGuardian;
import io.gravitee.gateway.handlers.guardian.registry.GuardianRegistry;
import io.gravitee.gateway.reactor.ReactorEvent;
import io.gravitee.node.api.license.ForbiddenFeatureException;
import io.gravitee.node.api.license.InvalidLicenseException;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.secrets.api.discovery.Definition;
import io.gravitee.secrets.api.discovery.DefinitionMetadata;
import io.gravitee.secrets.api.event.SecretDiscoveryEvent;
import io.gravitee.secrets.api.event.SecretDiscoveryEventType;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GuardianManagerImplTest {

    private static final String GUARDIAN_ID = "guardian-id";
    private static final Date NOW = new Date();

    @Mock
    private EventManager eventManager;

    @Mock
    private LicenseManager licenseManager;

    private final GuardianRegistry guardianRegistry = new GuardianRegistry();
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

    private GuardianManagerImpl cut;

    @BeforeEach
    void setUp() {
        cut = new GuardianManagerImpl(eventManager, licenseManager, guardianRegistry, meterRegistry);
        FakeGuardianReactorPlugin.install(eventManager, guardianRegistry);
    }

    @Test
    void should_deploy_guardian_and_discover_its_secrets() {
        ReactableGuardian guardian = guardian("env-a", NOW);

        assertThat(cut.register(guardian)).isTrue();

        ArgumentCaptor<SecretDiscoveryEvent> discoverCaptor = ArgumentCaptor.forClass(SecretDiscoveryEvent.class);
        verify(eventManager).publishEvent(eq(SecretDiscoveryEventType.DISCOVER), discoverCaptor.capture());
        assertThat(discoverCaptor.getValue().envId()).isEqualTo("env-a");
        assertThat(discoverCaptor.getValue().definition()).isSameAs(guardian.getDefinition());
        assertThat(discoverCaptor.getValue().metadata().revision()).isEqualTo(String.valueOf(NOW.getTime()));
        verify(eventManager).publishEvent(ReactorEvent.DEPLOY, guardian);
        assertThat(cut.guardians()).containsExactly(guardian);
        assertThat(cut.get(GUARDIAN_ID, "env-a")).isSameAs(guardian);
    }

    @Test
    void should_remove_a_first_deployment_that_does_not_start_and_revoke_its_secrets() {
        ReactableGuardian guardian = guardian("env-a", NOW);
        lenient().doNothing().when(eventManager).publishEvent(ReactorEvent.DEPLOY, guardian);

        boolean deployed = cut.register(guardian);

        ArgumentCaptor<SecretDiscoveryEvent> revokeCaptor = ArgumentCaptor.forClass(SecretDiscoveryEvent.class);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(deployed).as("a rejected first deployment is not reported as successful").isFalse();
            softly.assertThat(cut.get(GUARDIAN_ID, "env-a")).as("no rejected Guardian remains in the manager").isNull();
            softly.assertThat(cut.guardians()).as("the rejected Guardian is absent from deployed Guardians").isEmpty();
            softly.assertThat(guardianRegistry.get(GUARDIAN_ID, "env-a")).as("no reactor was installed").isNull();
        });
        verify(eventManager).publishEvent(eq(SecretDiscoveryEventType.REVOKE), revokeCaptor.capture());
        assertThat(revokeCaptor.getValue().metadata().revision())
            .as("only the rejected revision's secrets are revoked")
            .isEqualTo(guardian.getRevision());
    }

    @Test
    void should_remove_a_first_deployment_when_the_reactor_factory_throws() {
        ReactableGuardian guardian = guardian("env-a", NOW);
        lenient()
            .doThrow(new IllegalArgumentException("Incompatible guardrail protocol"))
            .when(eventManager)
            .publishEvent(ReactorEvent.DEPLOY, guardian);

        boolean deployed = cut.register(guardian);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(deployed).as("a thrown deployment refusal is reported through the registration result").isFalse();
            softly.assertThat(cut.get(GUARDIAN_ID, "env-a")).as("a factory refusal leaves no deployed Guardian").isNull();
            softly.assertThat(guardianRegistry.get(GUARDIAN_ID, "env-a")).as("a factory refusal installs no reactor").isNull();
        });
        ArgumentCaptor<SecretDiscoveryEvent> revokeCaptor = ArgumentCaptor.forClass(SecretDiscoveryEvent.class);
        verify(eventManager).publishEvent(eq(SecretDiscoveryEventType.REVOKE), revokeCaptor.capture());
        assertThat(revokeCaptor.getValue().metadata().revision())
            .as("the refused revision's secrets are revoked")
            .isEqualTo(guardian.getRevision());
    }

    @Test
    void should_preserve_the_running_revision_when_the_updated_reactor_factory_throws() {
        ReactableGuardian guardian = guardian("env-a", NOW);
        ReactableGuardian newerGuardian = guardian("env-a", Date.from(NOW.toInstant().plus(Duration.ofHours(1))));
        cut.register(guardian);
        var previousReactor = guardianRegistry.get(GUARDIAN_ID, "env-a");
        lenient()
            .doThrow(new IllegalArgumentException("Incompatible guardrail protocol"))
            .when(eventManager)
            .publishEvent(ReactorEvent.UPDATE, newerGuardian);

        boolean updated = cut.register(newerGuardian);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(updated).as("a thrown update refusal is reported through the registration result").isFalse();
            softly.assertThat(cut.get(GUARDIAN_ID, "env-a")).as("the previous definition remains deployed").isSameAs(guardian);
            softly
                .assertThat(guardianRegistry.get(GUARDIAN_ID, "env-a"))
                .as("the previous reactor remains active")
                .isSameAs(previousReactor);
        });
        ArgumentCaptor<SecretDiscoveryEvent> revokeCaptor = ArgumentCaptor.forClass(SecretDiscoveryEvent.class);
        verify(eventManager).publishEvent(eq(SecretDiscoveryEventType.REVOKE), revokeCaptor.capture());
        assertThat(revokeCaptor.getValue().metadata().revision())
            .as("only the refused update's secrets are revoked")
            .isEqualTo(newerGuardian.getRevision());
    }

    @ParameterizedTest
    @EnumSource(value = ReactorEvent.class, names = { "DEPLOY", "UPDATE" })
    void should_report_the_startup_cause_once_with_the_failed_revision(ReactorEvent event) {
        if (event == ReactorEvent.UPDATE) {
            cut.register(guardian("env-a", NOW));
        }
        ReactableGuardian rejectedGuardian = guardian("env-a", Date.from(NOW.toInstant().plusSeconds(1)));
        var failure = new IllegalStateException("Component initialization failed");
        lenient().doThrow(failure).when(eventManager).publishEvent(event, rejectedGuardian);
        Logger logger = (Logger) LoggerFactory.getLogger(GuardianManagerImpl.class);
        var appender = new ListAppender<ILoggingEvent>();
        appender.start();
        logger.addAppender(appender);
        try {
            boolean registered = cut.register(rejectedGuardian);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(registered).as("failed startup is rejected").isFalse();
                var errors = appender.list
                    .stream()
                    .filter(entry -> entry.getLevel() == Level.ERROR)
                    .toList();
                softly.assertThat(errors).as("one diagnostic owns the startup failure").hasSize(1);
                softly
                    .assertThat(errors)
                    .singleElement()
                    .satisfies(entry -> {
                        softly
                            .assertThat(entry.getFormattedMessage())
                            .as("failed revision context")
                            .contains(GUARDIAN_ID, "env-a", rejectedGuardian.getRevision());
                        softly
                            .assertThat(entry.getThrowableProxy())
                            .as("original startup cause")
                            .isInstanceOfSatisfying(ThrowableProxy.class, cause ->
                                softly.assertThat(cause.getThrowable()).isSameAs(failure)
                            );
                    });
            });
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @ParameterizedTest
    @EnumSource(value = ReactorEvent.class, names = { "DEPLOY", "UPDATE" })
    void should_count_each_startup_failure_per_environment(ReactorEvent event) {
        if (event == ReactorEvent.UPDATE) {
            cut.register(guardian("env-a", NOW));
        }
        ReactableGuardian rejectedGuardian = guardian("env-a", Date.from(NOW.toInstant().plusSeconds(1)));
        lenient()
            .doThrow(new IllegalArgumentException("Incompatible guardrail protocol"))
            .when(eventManager)
            .publishEvent(event, rejectedGuardian);

        cut.register(rejectedGuardian);

        assertThat(meterRegistry.find("guardian_deployment_failures").tag("environment", "env-a").counter())
            .as("deployment failures of the environment")
            .isNotNull()
            .extracting(Counter::count)
            .isEqualTo(1.0);
    }

    @Test
    void should_not_count_a_successful_deployment_as_a_failure() {
        cut.register(guardian("env-a", NOW));

        assertThat(meterRegistry.find("guardian_deployment_failures").counters())
            .as("no failure recorded")
            .allSatisfy(counter -> assertThat(counter.count()).isZero());
    }

    @Test
    void should_allow_the_same_revision_to_be_retried_after_a_rejected_first_deployment() {
        ReactableGuardian guardian = guardian("env-a", NOW);
        lenient().doNothing().when(eventManager).publishEvent(ReactorEvent.DEPLOY, guardian);
        cut.register(guardian);
        FakeGuardianReactorPlugin.install(eventManager, guardianRegistry);

        assertThat(cut.register(guardian)).as("a first failure does not make the revision look already deployed").isTrue();
        assertThat(cut.get(GUARDIAN_ID, "env-a")).isSameAs(guardian);
    }

    @Test
    void should_not_deploy_guardian_refused_by_the_license() throws InvalidLicenseException, ForbiddenFeatureException {
        ReactableGuardian guardian = guardian("env-a", NOW);
        doThrow(new ForbiddenFeatureException(List.of())).when(licenseManager).validatePluginFeatures(any(), any());

        assertThat(cut.register(guardian)).isFalse();

        verify(eventManager, never()).publishEvent(any(ReactorEvent.class), any());
        assertThat(cut.guardians()).isEmpty();
    }

    @Test
    void should_update_guardian_when_a_more_recent_copy_is_registered() {
        FakeGuardianReactorPlugin.install(eventManager, guardianRegistry);
        ReactableGuardian guardian = guardian("env-a", NOW);
        ReactableGuardian newerGuardian = guardian("env-a", Date.from(NOW.toInstant().plus(Duration.ofHours(1))));
        cut.register(guardian);

        assertThat(cut.register(newerGuardian)).isTrue();

        verify(eventManager).publishEvent(ReactorEvent.UPDATE, newerGuardian);
        ArgumentCaptor<SecretDiscoveryEvent> revokeCaptor = ArgumentCaptor.forClass(SecretDiscoveryEvent.class);
        verify(eventManager).publishEvent(eq(SecretDiscoveryEventType.REVOKE), revokeCaptor.capture());
        assertThat(revokeCaptor.getValue().metadata().revision()).isEqualTo(String.valueOf(NOW.getTime()));
        assertThat(cut.guardians()).containsExactly(newerGuardian);
    }

    @Test
    void should_keep_the_previous_revision_when_the_updated_guardian_does_not_start() {
        ReactableGuardian guardian = guardian("env-a", NOW);
        ReactableGuardian newerGuardian = guardian("env-a", Date.from(NOW.toInstant().plus(Duration.ofHours(1))));
        FakeGuardianReactorPlugin.install(eventManager, guardianRegistry);
        cut.register(guardian);
        // The reactor handler registry keeps the running handler when the new one fails to start.
        lenient().doNothing().when(eventManager).publishEvent(ReactorEvent.UPDATE, newerGuardian);

        assertThat(cut.register(newerGuardian)).isFalse();

        assertThat(cut.get(GUARDIAN_ID, "env-a")).isSameAs(guardian);
        ArgumentCaptor<SecretDiscoveryEvent> revokeCaptor = ArgumentCaptor.forClass(SecretDiscoveryEvent.class);
        verify(eventManager).publishEvent(eq(SecretDiscoveryEventType.REVOKE), revokeCaptor.capture());
        assertThat(revokeCaptor.getValue().metadata().revision()).isEqualTo(newerGuardian.getRevision());
    }

    @Test
    void should_ignore_an_older_copy_of_a_deployed_guardian() {
        ReactableGuardian guardian = guardian("env-a", NOW);
        ReactableGuardian olderGuardian = guardian("env-a", Date.from(NOW.toInstant().minus(Duration.ofHours(1))));
        cut.register(guardian);

        assertThat(cut.register(olderGuardian)).isFalse();

        verify(eventManager, never()).publishEvent(eq(ReactorEvent.UPDATE), any());
        assertThat(cut.get(GUARDIAN_ID, "env-a")).isSameAs(guardian);
    }

    @Test
    void should_undeploy_guardian_and_revoke_its_secrets() {
        ReactableGuardian guardian = guardian("env-a", NOW);
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
        ReactableGuardian environmentA = guardian("env-a", NOW);
        ReactableGuardian environmentB = guardian("env-b", Date.from(NOW.toInstant().plus(Duration.ofHours(1))));
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
    void should_update_guardian_when_one_of_its_secrets_changes() {
        ReactableGuardian environmentA = guardian("env-a", NOW);
        ReactableGuardian environmentB = guardian("env-b", NOW);
        cut.register(environmentA);
        cut.register(environmentB);

        secretValueChangedListener().onEvent(secretValueChanged("env-a", new Definition("guardian", GUARDIAN_ID)));

        verify(eventManager).publishEvent(ReactorEvent.UPDATE, environmentA);
        verify(eventManager, never()).publishEvent(ReactorEvent.UPDATE, environmentB);
    }

    @Test
    void should_ignore_secret_changes_of_other_definitions_or_unknown_guardians() {
        cut.register(guardian("env-a", NOW));

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

    private static ReactableGuardian guardian(String environmentId, Date deployedAt) {
        return ReactableGuardian.builder()
            .organizationId("org-id")
            .deployedAt(deployedAt)
            .revision(String.valueOf(deployedAt.getTime()))
            .definition(
                Guardian.builder()
                    .id(GUARDIAN_ID)
                    .name("guardian-name")
                    .environmentId(environmentId)
                    .phase(Guardian.Phase.REQUEST)
                    .policies(List.of(Step.builder().policy("a-policy").enabled(true).build()))
                    .build()
            )
            .build();
    }
}
