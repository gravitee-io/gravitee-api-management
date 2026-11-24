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
package io.gravitee.gateway.handlers.guardrail.manager.impl;

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
import io.gravitee.definition.model.v4.guardrail.Guardrail;
import io.gravitee.gateway.handlers.guardrail.FakeGuardrailReactorPlugin;
import io.gravitee.gateway.handlers.guardrail.ReactableGuardrail;
import io.gravitee.gateway.handlers.guardrail.registry.GuardrailRegistry;
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
class GuardrailManagerImplTest {

    private static final String GUARDRAIL_ID = "guardrail-id";
    private static final Date NOW = new Date();

    @Mock
    private EventManager eventManager;

    @Mock
    private LicenseManager licenseManager;

    private final GuardrailRegistry guardrailRegistry = new GuardrailRegistry();
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

    private GuardrailManagerImpl cut;

    @BeforeEach
    void setUp() {
        cut = new GuardrailManagerImpl(eventManager, licenseManager, guardrailRegistry, meterRegistry);
        FakeGuardrailReactorPlugin.install(eventManager, guardrailRegistry);
    }

    @Test
    void should_deploy_guardrail_and_discover_its_secrets() {
        ReactableGuardrail guardrail = guardrail("env-a", NOW);

        assertThat(cut.register(guardrail)).isTrue();

        ArgumentCaptor<SecretDiscoveryEvent> discoverCaptor = ArgumentCaptor.forClass(SecretDiscoveryEvent.class);
        verify(eventManager).publishEvent(eq(SecretDiscoveryEventType.DISCOVER), discoverCaptor.capture());
        assertThat(discoverCaptor.getValue().envId()).isEqualTo("env-a");
        assertThat(discoverCaptor.getValue().definition()).isSameAs(guardrail.getDefinition());
        assertThat(discoverCaptor.getValue().metadata().revision()).isEqualTo(String.valueOf(NOW.getTime()));
        verify(eventManager).publishEvent(ReactorEvent.DEPLOY, guardrail);
        assertThat(cut.guardrails()).containsExactly(guardrail);
        assertThat(cut.get(GUARDRAIL_ID, "env-a")).isSameAs(guardrail);
    }

    @Test
    void should_remove_a_first_deployment_that_does_not_start_and_revoke_its_secrets() {
        ReactableGuardrail guardrail = guardrail("env-a", NOW);
        lenient().doNothing().when(eventManager).publishEvent(ReactorEvent.DEPLOY, guardrail);

        boolean deployed = cut.register(guardrail);

        ArgumentCaptor<SecretDiscoveryEvent> revokeCaptor = ArgumentCaptor.forClass(SecretDiscoveryEvent.class);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(deployed).as("a rejected first deployment is not reported as successful").isFalse();
            softly.assertThat(cut.get(GUARDRAIL_ID, "env-a")).as("no rejected Guardrail remains in the manager").isNull();
            softly.assertThat(cut.guardrails()).as("the rejected Guardrail is absent from deployed Guardrails").isEmpty();
            softly.assertThat(guardrailRegistry.get(GUARDRAIL_ID, "env-a")).as("no reactor was installed").isNull();
        });
        verify(eventManager).publishEvent(eq(SecretDiscoveryEventType.REVOKE), revokeCaptor.capture());
        assertThat(revokeCaptor.getValue().metadata().revision())
            .as("only the rejected revision's secrets are revoked")
            .isEqualTo(guardrail.getRevision());
    }

    @Test
    void should_remove_a_first_deployment_when_the_reactor_factory_throws() {
        ReactableGuardrail guardrail = guardrail("env-a", NOW);
        lenient()
            .doThrow(new IllegalArgumentException("Incompatible guardrail policy protocol"))
            .when(eventManager)
            .publishEvent(ReactorEvent.DEPLOY, guardrail);

        boolean deployed = cut.register(guardrail);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(deployed).as("a thrown deployment refusal is reported through the registration result").isFalse();
            softly.assertThat(cut.get(GUARDRAIL_ID, "env-a")).as("a factory refusal leaves no deployed Guardrail").isNull();
            softly.assertThat(guardrailRegistry.get(GUARDRAIL_ID, "env-a")).as("a factory refusal installs no reactor").isNull();
        });
        ArgumentCaptor<SecretDiscoveryEvent> revokeCaptor = ArgumentCaptor.forClass(SecretDiscoveryEvent.class);
        verify(eventManager).publishEvent(eq(SecretDiscoveryEventType.REVOKE), revokeCaptor.capture());
        assertThat(revokeCaptor.getValue().metadata().revision())
            .as("the refused revision's secrets are revoked")
            .isEqualTo(guardrail.getRevision());
    }

    @Test
    void should_preserve_the_running_revision_when_the_updated_reactor_factory_throws() {
        ReactableGuardrail guardrail = guardrail("env-a", NOW);
        ReactableGuardrail newerGuardrail = guardrail("env-a", Date.from(NOW.toInstant().plus(Duration.ofHours(1))));
        cut.register(guardrail);
        var previousReactor = guardrailRegistry.get(GUARDRAIL_ID, "env-a");
        lenient()
            .doThrow(new IllegalArgumentException("Incompatible guardrail policy protocol"))
            .when(eventManager)
            .publishEvent(ReactorEvent.UPDATE, newerGuardrail);

        boolean updated = cut.register(newerGuardrail);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(updated).as("a thrown update refusal is reported through the registration result").isFalse();
            softly.assertThat(cut.get(GUARDRAIL_ID, "env-a")).as("the previous definition remains deployed").isSameAs(guardrail);
            softly
                .assertThat(guardrailRegistry.get(GUARDRAIL_ID, "env-a"))
                .as("the previous reactor remains active")
                .isSameAs(previousReactor);
        });
        ArgumentCaptor<SecretDiscoveryEvent> revokeCaptor = ArgumentCaptor.forClass(SecretDiscoveryEvent.class);
        verify(eventManager).publishEvent(eq(SecretDiscoveryEventType.REVOKE), revokeCaptor.capture());
        assertThat(revokeCaptor.getValue().metadata().revision())
            .as("only the refused update's secrets are revoked")
            .isEqualTo(newerGuardrail.getRevision());
    }

    @ParameterizedTest
    @EnumSource(value = ReactorEvent.class, names = { "DEPLOY", "UPDATE" })
    void should_report_the_startup_cause_once_with_the_failed_revision(ReactorEvent event) {
        if (event == ReactorEvent.UPDATE) {
            cut.register(guardrail("env-a", NOW));
        }
        ReactableGuardrail rejectedGuardrail = guardrail("env-a", Date.from(NOW.toInstant().plusSeconds(1)));
        var failure = new IllegalStateException("Component initialization failed");
        lenient().doThrow(failure).when(eventManager).publishEvent(event, rejectedGuardrail);
        Logger logger = (Logger) LoggerFactory.getLogger(GuardrailManagerImpl.class);
        var appender = new ListAppender<ILoggingEvent>();
        appender.start();
        logger.addAppender(appender);
        try {
            boolean registered = cut.register(rejectedGuardrail);

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
                            .contains(GUARDRAIL_ID, "env-a", rejectedGuardrail.getRevision());
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
            cut.register(guardrail("env-a", NOW));
        }
        ReactableGuardrail rejectedGuardrail = guardrail("env-a", Date.from(NOW.toInstant().plusSeconds(1)));
        lenient()
            .doThrow(new IllegalArgumentException("Incompatible guardrail policy protocol"))
            .when(eventManager)
            .publishEvent(event, rejectedGuardrail);

        cut.register(rejectedGuardrail);

        assertThat(meterRegistry.find("guardrail_deployment_failures").tag("environment", "env-a").counter())
            .as("deployment failures of the environment")
            .isNotNull()
            .extracting(Counter::count)
            .isEqualTo(1.0);
    }

    @Test
    void should_not_count_a_successful_deployment_as_a_failure() {
        cut.register(guardrail("env-a", NOW));

        assertThat(meterRegistry.find("guardrail_deployment_failures").counters())
            .as("no failure recorded")
            .allSatisfy(counter -> assertThat(counter.count()).isZero());
    }

    @Test
    void should_allow_the_same_revision_to_be_retried_after_a_rejected_first_deployment() {
        ReactableGuardrail guardrail = guardrail("env-a", NOW);
        lenient().doNothing().when(eventManager).publishEvent(ReactorEvent.DEPLOY, guardrail);
        cut.register(guardrail);
        FakeGuardrailReactorPlugin.install(eventManager, guardrailRegistry);

        assertThat(cut.register(guardrail)).as("a first failure does not make the revision look already deployed").isTrue();
        assertThat(cut.get(GUARDRAIL_ID, "env-a")).isSameAs(guardrail);
    }

    @Test
    void should_not_deploy_guardrail_refused_by_the_license() throws InvalidLicenseException, ForbiddenFeatureException {
        ReactableGuardrail guardrail = guardrail("env-a", NOW);
        doThrow(new ForbiddenFeatureException(List.of())).when(licenseManager).validatePluginFeatures(any(), any());

        assertThat(cut.register(guardrail)).isFalse();

        verify(eventManager, never()).publishEvent(any(ReactorEvent.class), any());
        assertThat(cut.guardrails()).isEmpty();
    }

    @Test
    void should_update_guardrail_when_a_more_recent_copy_is_registered() {
        FakeGuardrailReactorPlugin.install(eventManager, guardrailRegistry);
        ReactableGuardrail guardrail = guardrail("env-a", NOW);
        ReactableGuardrail newerGuardrail = guardrail("env-a", Date.from(NOW.toInstant().plus(Duration.ofHours(1))));
        cut.register(guardrail);

        assertThat(cut.register(newerGuardrail)).isTrue();

        verify(eventManager).publishEvent(ReactorEvent.UPDATE, newerGuardrail);
        ArgumentCaptor<SecretDiscoveryEvent> revokeCaptor = ArgumentCaptor.forClass(SecretDiscoveryEvent.class);
        verify(eventManager).publishEvent(eq(SecretDiscoveryEventType.REVOKE), revokeCaptor.capture());
        assertThat(revokeCaptor.getValue().metadata().revision()).isEqualTo(String.valueOf(NOW.getTime()));
        assertThat(cut.guardrails()).containsExactly(newerGuardrail);
    }

    @Test
    void should_keep_the_previous_revision_when_the_updated_guardrail_does_not_start() {
        ReactableGuardrail guardrail = guardrail("env-a", NOW);
        ReactableGuardrail newerGuardrail = guardrail("env-a", Date.from(NOW.toInstant().plus(Duration.ofHours(1))));
        FakeGuardrailReactorPlugin.install(eventManager, guardrailRegistry);
        cut.register(guardrail);
        // The reactor handler registry keeps the running handler when the new one fails to start.
        lenient().doNothing().when(eventManager).publishEvent(ReactorEvent.UPDATE, newerGuardrail);

        assertThat(cut.register(newerGuardrail)).isFalse();

        assertThat(cut.get(GUARDRAIL_ID, "env-a")).isSameAs(guardrail);
        ArgumentCaptor<SecretDiscoveryEvent> revokeCaptor = ArgumentCaptor.forClass(SecretDiscoveryEvent.class);
        verify(eventManager).publishEvent(eq(SecretDiscoveryEventType.REVOKE), revokeCaptor.capture());
        assertThat(revokeCaptor.getValue().metadata().revision()).isEqualTo(newerGuardrail.getRevision());
    }

    @Test
    void should_ignore_an_older_copy_of_a_deployed_guardrail() {
        ReactableGuardrail guardrail = guardrail("env-a", NOW);
        ReactableGuardrail olderGuardrail = guardrail("env-a", Date.from(NOW.toInstant().minus(Duration.ofHours(1))));
        cut.register(guardrail);

        assertThat(cut.register(olderGuardrail)).isFalse();

        verify(eventManager, never()).publishEvent(eq(ReactorEvent.UPDATE), any());
        assertThat(cut.get(GUARDRAIL_ID, "env-a")).isSameAs(guardrail);
    }

    @Test
    void should_undeploy_guardrail_and_revoke_its_secrets() {
        ReactableGuardrail guardrail = guardrail("env-a", NOW);
        cut.register(guardrail);

        cut.unregister(GUARDRAIL_ID, "env-a");

        verify(eventManager).publishEvent(ReactorEvent.UNDEPLOY, guardrail);
        ArgumentCaptor<SecretDiscoveryEvent> revokeCaptor = ArgumentCaptor.forClass(SecretDiscoveryEvent.class);
        verify(eventManager).publishEvent(eq(SecretDiscoveryEventType.REVOKE), revokeCaptor.capture());
        assertThat(revokeCaptor.getValue().envId()).isEqualTo("env-a");
        assertThat(cut.guardrails()).isEmpty();
    }

    @Test
    void should_do_nothing_when_undeploying_an_unknown_guardrail() {
        cut.unregister(GUARDRAIL_ID, "env-a");

        verify(eventManager, never()).publishEvent(any(ReactorEvent.class), any());
        verify(eventManager, never()).publishEvent(any(SecretDiscoveryEventType.class), any());
    }

    @Test
    void should_deploy_the_same_id_independently_per_environment() {
        ReactableGuardrail environmentA = guardrail("env-a", NOW);
        ReactableGuardrail environmentB = guardrail("env-b", Date.from(NOW.toInstant().plus(Duration.ofHours(1))));
        cut.register(environmentA);
        cut.register(environmentB);

        cut.unregister(GUARDRAIL_ID, "env-a");

        verify(eventManager).publishEvent(ReactorEvent.DEPLOY, environmentA);
        verify(eventManager).publishEvent(ReactorEvent.DEPLOY, environmentB);
        verify(eventManager, never()).publishEvent(eq(ReactorEvent.UPDATE), any());
        verify(eventManager).publishEvent(ReactorEvent.UNDEPLOY, environmentA);
        verify(eventManager, never()).publishEvent(ReactorEvent.UNDEPLOY, environmentB);
        assertThat(cut.guardrails()).containsExactly(environmentB);
    }

    @Test
    void should_update_guardrail_when_one_of_its_secrets_changes() {
        ReactableGuardrail environmentA = guardrail("env-a", NOW);
        ReactableGuardrail environmentB = guardrail("env-b", NOW);
        cut.register(environmentA);
        cut.register(environmentB);

        secretValueChangedListener().onEvent(secretValueChanged("env-a", new Definition("guardrail", GUARDRAIL_ID)));

        verify(eventManager).publishEvent(ReactorEvent.UPDATE, environmentA);
        verify(eventManager, never()).publishEvent(ReactorEvent.UPDATE, environmentB);
    }

    @Test
    void should_ignore_secret_changes_of_other_definitions_or_unknown_guardrails() {
        cut.register(guardrail("env-a", NOW));

        EventListener<SecretDiscoveryEventType, SecretDiscoveryEvent> listener = secretValueChangedListener();
        listener.onEvent(secretValueChanged("env-a", new Definition("shared-policy-group", GUARDRAIL_ID)));
        listener.onEvent(secretValueChanged("env-a", new Definition("guardrail", "unknown-guardrail")));
        listener.onEvent(secretValueChanged("other-env", new Definition("guardrail", GUARDRAIL_ID)));

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

    private static ReactableGuardrail guardrail(String environmentId, Date deployedAt) {
        return ReactableGuardrail.builder()
            .organizationId("org-id")
            .deployedAt(deployedAt)
            .revision(String.valueOf(deployedAt.getTime()))
            .definition(
                Guardrail.builder()
                    .id(GUARDRAIL_ID)
                    .name("guardrail-name")
                    .environmentId(environmentId)
                    .phase(Guardrail.Phase.REQUEST)
                    .policies(List.of(Step.builder().policy("a-policy").enabled(true).build()))
                    .build()
            )
            .build();
    }
}
