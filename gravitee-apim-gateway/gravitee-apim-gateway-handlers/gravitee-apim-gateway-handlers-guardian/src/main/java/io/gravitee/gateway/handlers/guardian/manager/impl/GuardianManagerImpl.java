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

import static io.gravitee.gateway.reactive.reactor.v4.secrets.GuardianDefinitionSecretRefsFinder.GUARDIAN_DEFINITION_KIND;

import io.gravitee.common.event.EventManager;
import io.gravitee.gateway.handlers.guardian.ReactableGuardian;
import io.gravitee.gateway.handlers.guardian.manager.GuardianManager;
import io.gravitee.gateway.reactor.ReactorEvent;
import io.gravitee.node.api.license.ForbiddenFeatureException;
import io.gravitee.node.api.license.InvalidLicenseException;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.secrets.api.discovery.Definition;
import io.gravitee.secrets.api.discovery.DefinitionMetadata;
import io.gravitee.secrets.api.event.SecretDiscoveryEvent;
import io.gravitee.secrets.api.event.SecretDiscoveryEventType;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.CustomLog;

/**
 * Keeps the deployed Guardians and hands them to the reactor through {@link ReactorEvent}s, the same way APIs reach
 * their reactor. The Guardian reactor plugin picks them up from the {@link io.gravitee.gateway.reactor.handler.ReactorHandlerRegistry}.
 *
 * @author GraviteeSource Team
 */
@CustomLog
public class GuardianManagerImpl implements GuardianManager {

    private final Map<GuardianKey, ReactableGuardian> guardians = new ConcurrentHashMap<>();

    private final EventManager eventManager;
    private final LicenseManager licenseManager;

    public GuardianManagerImpl(EventManager eventManager, LicenseManager licenseManager) {
        this.eventManager = eventManager;
        this.licenseManager = licenseManager;

        eventManager.subscribeForEvents(
            event -> {
                if (!(event.content() instanceof SecretDiscoveryEvent secretDiscoveryEvent)) {
                    return;
                }
                if (!(secretDiscoveryEvent.definition() instanceof Definition definition)) {
                    return;
                }
                if (!GUARDIAN_DEFINITION_KIND.equals(definition.kind())) {
                    return;
                }
                ReactableGuardian guardian = guardians.get(new GuardianKey(definition.id(), secretDiscoveryEvent.envId()));
                if (guardian == null) {
                    log.trace("Received SecretDiscoveryEvent for Guardian {}, but not found in manager", definition.id());
                    return;
                }
                log.info("Secret value changed for Guardian {}, updating it", definition.id());
                eventManager.publishEvent(ReactorEvent.UPDATE, guardian);
            },
            SecretDiscoveryEventType.VALUE_CHANGED
        );
    }

    @Override
    public boolean register(ReactableGuardian guardian) {
        try {
            licenseManager.validatePluginFeatures(
                guardian.getOrganizationId(),
                guardian
                    .getDefinition()
                    .getPlugins()
                    .stream()
                    .map(plugin -> new LicenseManager.Plugin(plugin.type(), plugin.id()))
                    .collect(Collectors.toSet())
            );
        } catch (InvalidLicenseException | ForbiddenFeatureException e) {
            log.warn("The Guardian {} could not be deployed because it is not allowed by the current license", guardian.getName(), e);
            return false;
        }

        ReactableGuardian deployedGuardian = get(guardian.getId(), guardian.getEnvironmentId());
        if (deployedGuardian == null) {
            deploy(guardian);
            return true;
        }
        if (deployedGuardian.getDeployedAt().before(guardian.getDeployedAt())) {
            update(guardian);
            return true;
        }
        return false;
    }

    @Override
    public void unregister(String guardianId, String environmentId) {
        ReactableGuardian currentGuardian = guardians.remove(new GuardianKey(guardianId, environmentId));
        if (currentGuardian == null) {
            log.warn("Guardian [{}] of environment [{}] is not deployed", guardianId, environmentId);
            return;
        }
        log.debug("Undeployment of Guardian [{}] of environment [{}]", guardianId, environmentId);
        eventManager.publishEvent(ReactorEvent.UNDEPLOY, currentGuardian);
        revokeSecrets(currentGuardian);
        log.info("Guardian [{}] of environment [{}] has been undeployed", guardianId, environmentId);
    }

    @Override
    public void unregisterAll(String guardianId) {
        List<GuardianKey> keys = guardians.keySet().stream().filter(key -> Objects.equals(key.guardianId(), guardianId)).toList();
        if (keys.isEmpty()) {
            log.warn("Guardian [{}] is not deployed in any environment", guardianId);
            return;
        }
        keys.forEach(key -> unregister(key.guardianId(), key.environmentId()));
    }

    @Override
    public Collection<ReactableGuardian> guardians() {
        return guardians.values();
    }

    @Override
    public ReactableGuardian get(String guardianId, String environmentId) {
        return guardians.get(new GuardianKey(guardianId, environmentId));
    }

    private void deploy(ReactableGuardian guardian) {
        log.debug("Deployment of {}", guardian);
        discoverSecrets(guardian);
        guardians.put(keyOf(guardian), guardian);
        eventManager.publishEvent(ReactorEvent.DEPLOY, guardian);
        log.info("Guardian [{}] of environment [{}] has been deployed", guardian.getId(), guardian.getEnvironmentId());
    }

    private void update(ReactableGuardian guardian) {
        log.debug("Updating {}", guardian);
        discoverSecrets(guardian);
        ReactableGuardian previousGuardian = guardians.put(keyOf(guardian), guardian);
        eventManager.publishEvent(ReactorEvent.UPDATE, guardian);
        if (previousGuardian != null) {
            revokeSecrets(previousGuardian);
        }
        log.info("Guardian [{}] of environment [{}] has been updated", guardian.getId(), guardian.getEnvironmentId());
    }

    private void discoverSecrets(ReactableGuardian guardian) {
        eventManager.publishEvent(SecretDiscoveryEventType.DISCOVER, secretDiscoveryEventOf(guardian));
    }

    private void revokeSecrets(ReactableGuardian guardian) {
        eventManager.publishEvent(SecretDiscoveryEventType.REVOKE, secretDiscoveryEventOf(guardian));
    }

    private static SecretDiscoveryEvent secretDiscoveryEventOf(ReactableGuardian guardian) {
        return new SecretDiscoveryEvent(
            guardian.getEnvironmentId(),
            guardian.getDefinition(),
            new DefinitionMetadata(guardian.getDefinition().getVersion())
        );
    }

    private static GuardianKey keyOf(ReactableGuardian guardian) {
        return new GuardianKey(guardian.getId(), guardian.getEnvironmentId());
    }

    private record GuardianKey(String guardianId, String environmentId) {}
}
