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

import static io.gravitee.gateway.reactive.reactor.v4.secrets.GuardrailDefinitionSecretRefsFinder.GUARDRAIL_DEFINITION_KIND;

import io.gravitee.common.event.EventManager;
import io.gravitee.gateway.handlers.guardrail.GuardrailKey;
import io.gravitee.gateway.handlers.guardrail.ReactableGuardrail;
import io.gravitee.gateway.handlers.guardrail.manager.GuardrailManager;
import io.gravitee.gateway.handlers.guardrail.reactor.GuardrailReactor;
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
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.CustomLog;

/**
 * Keeps the deployed Guardrails and hands them to the reactor through {@link ReactorEvent}s, the same way APIs reach
 * their reactor. The Guardrail reactor plugin picks them up from the {@link io.gravitee.gateway.reactor.handler.ReactorHandlerRegistry}.
 *
 * @author GraviteeSource Team
 */
@CustomLog
public class GuardrailManagerImpl implements GuardrailManager {

    private static final String DEPLOYMENT_FAILURES = "guardrail_deployment_failures";

    private final Map<GuardrailKey, ReactableGuardrail> guardrails = new ConcurrentHashMap<>();
    private final Map<String, Counter> deploymentFailures = new ConcurrentHashMap<>();

    private final EventManager eventManager;
    private final LicenseManager licenseManager;
    private final GuardrailRegistry guardrailRegistry;
    private final MeterRegistry meterRegistry;

    public GuardrailManagerImpl(
        EventManager eventManager,
        LicenseManager licenseManager,
        GuardrailRegistry guardrailRegistry,
        MeterRegistry meterRegistry
    ) {
        this.eventManager = eventManager;
        this.licenseManager = licenseManager;
        this.guardrailRegistry = guardrailRegistry;
        this.meterRegistry = meterRegistry;

        subscribeToSecretValueChanges();
    }

    private void subscribeToSecretValueChanges() {
        eventManager.subscribeForEvents(
            event -> {
                if (!(event.content() instanceof SecretDiscoveryEvent secretDiscoveryEvent)) {
                    return;
                }
                if (!(secretDiscoveryEvent.definition() instanceof Definition definition)) {
                    return;
                }
                if (!GUARDRAIL_DEFINITION_KIND.equals(definition.kind())) {
                    return;
                }
                redeployOnSecretValueChange(definition.id(), secretDiscoveryEvent.envId());
            },
            SecretDiscoveryEventType.VALUE_CHANGED
        );
    }

    /**
     * Synchronized with the deployments: read before a concurrent update and published after it, the previous revision
     * would replace the new one, with the secrets the update just revoked.
     */
    private synchronized void redeployOnSecretValueChange(String guardrailId, String environmentId) {
        ReactableGuardrail guardrail = guardrails.get(new GuardrailKey(guardrailId, environmentId));
        if (guardrail == null) {
            log.trace("Received SecretDiscoveryEvent for Guardrail {}, but not found in manager", guardrailId);
            return;
        }
        log.info("Secret value changed for Guardrail {}, updating it", guardrailId);
        eventManager.publishEvent(ReactorEvent.UPDATE, guardrail);
    }

    @Override
    public synchronized boolean register(ReactableGuardrail guardrail) {
        try {
            licenseManager.validatePluginFeatures(
                guardrail.getOrganizationId(),
                guardrail
                    .getDefinition()
                    .getPlugins()
                    .stream()
                    .map(plugin -> new LicenseManager.Plugin(plugin.type(), plugin.id()))
                    .collect(Collectors.toSet())
            );
        } catch (InvalidLicenseException | ForbiddenFeatureException e) {
            log.warn("The Guardrail {} could not be deployed because it is not allowed by the current license", guardrail.getName(), e);
            return false;
        }

        ReactableGuardrail deployedGuardrail = get(guardrail.getId(), guardrail.getEnvironmentId());
        if (deployedGuardrail == null) {
            return deploy(guardrail);
        }
        if (deployedGuardrail.getDeployedAt().before(guardrail.getDeployedAt())) {
            return update(guardrail, deployedGuardrail);
        }
        return false;
    }

    @Override
    public synchronized void unregister(String guardrailId, String environmentId) {
        ReactableGuardrail currentGuardrail = guardrails.remove(new GuardrailKey(guardrailId, environmentId));
        if (currentGuardrail == null) {
            log.warn("Guardrail [{}] of environment [{}] is not deployed", guardrailId, environmentId);
            return;
        }
        log.debug("Undeployment of Guardrail [{}] of environment [{}]", guardrailId, environmentId);
        eventManager.publishEvent(ReactorEvent.UNDEPLOY, currentGuardrail);
        revokeSecrets(currentGuardrail);
        log.info("Guardrail [{}] of environment [{}] has been undeployed", guardrailId, environmentId);
    }

    @Override
    public Collection<ReactableGuardrail> guardrails() {
        return guardrails.values();
    }

    @Override
    public ReactableGuardrail get(String guardrailId, String environmentId) {
        return guardrails.get(new GuardrailKey(guardrailId, environmentId));
    }

    private boolean deploy(ReactableGuardrail guardrail) {
        log.debug("Deployment of {}", guardrail);
        discoverSecrets(guardrail);
        guardrails.put(guardrail.key(), guardrail);
        var startup = startReactor(ReactorEvent.DEPLOY, guardrail);
        if (!startup.running()) {
            guardrails.remove(guardrail.key());
            revokeSecrets(guardrail);
            countFailure(guardrail);
            log.error(
                "Guardrail [{}] of environment [{}] could not start revision [{}]",
                guardrail.getId(),
                guardrail.getEnvironmentId(),
                guardrail.getRevision(),
                startup.failure()
            );
            return false;
        }
        log.info("Guardrail [{}] of environment [{}] has been deployed", guardrail.getId(), guardrail.getEnvironmentId());
        return true;
    }

    private boolean update(ReactableGuardrail guardrail, ReactableGuardrail previousGuardrail) {
        log.debug("Updating {}", guardrail);
        discoverSecrets(guardrail);
        guardrails.put(guardrail.key(), guardrail);
        var startup = startReactor(ReactorEvent.UPDATE, guardrail);
        if (!startup.running()) {
            // The reactor handler registry keeps the previous handler when the new one fails to start: revoking the
            // previous secrets would leave the running revision without them.
            guardrails.put(previousGuardrail.key(), previousGuardrail);
            revokeSecrets(guardrail);
            countFailure(guardrail);
            log.error(
                "Guardrail [{}] of environment [{}] could not start revision [{}]: revision [{}] keeps running",
                guardrail.getId(),
                guardrail.getEnvironmentId(),
                guardrail.getRevision(),
                previousGuardrail.getRevision(),
                startup.failure()
            );
            return false;
        }
        revokeSecrets(previousGuardrail);
        log.info("Guardrail [{}] of environment [{}] has been updated", guardrail.getId(), guardrail.getEnvironmentId());
        return true;
    }

    private ReactorStartup startReactor(ReactorEvent event, ReactableGuardrail guardrail) {
        try {
            eventManager.publishEvent(event, guardrail);
            return new ReactorStartup(isRunning(guardrail), null);
        } catch (RuntimeException e) {
            return new ReactorStartup(false, e);
        }
    }

    private void countFailure(ReactableGuardrail guardrail) {
        deploymentFailures
            .computeIfAbsent(guardrail.getEnvironmentId(), environmentId ->
                Counter.builder(DEPLOYMENT_FAILURES)
                    .description("Guardrail revisions the gateway refused to start")
                    .tag("environment", environmentId)
                    .register(meterRegistry)
            )
            .increment();
    }

    private record ReactorStartup(boolean running, RuntimeException failure) {}

    private boolean isRunning(ReactableGuardrail guardrail) {
        GuardrailReactor reactor = guardrailRegistry.get(guardrail.getId(), guardrail.getEnvironmentId());
        return reactor != null && reactor.reactableGuardrail() == guardrail;
    }

    private void discoverSecrets(ReactableGuardrail guardrail) {
        eventManager.publishEvent(SecretDiscoveryEventType.DISCOVER, secretDiscoveryEventOf(guardrail));
    }

    private void revokeSecrets(ReactableGuardrail guardrail) {
        eventManager.publishEvent(SecretDiscoveryEventType.REVOKE, secretDiscoveryEventOf(guardrail));
    }

    private static SecretDiscoveryEvent secretDiscoveryEventOf(ReactableGuardrail guardrail) {
        return new SecretDiscoveryEvent(
            guardrail.getEnvironmentId(),
            guardrail.getDefinition(),
            new DefinitionMetadata(guardrail.getRevision())
        );
    }
}
