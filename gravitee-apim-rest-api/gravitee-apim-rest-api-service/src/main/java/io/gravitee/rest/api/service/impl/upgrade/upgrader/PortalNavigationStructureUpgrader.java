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

import static io.gravitee.rest.api.service.impl.upgrade.upgrader.UpgraderOrder.PORTAL_NAVIGATION_STRUCTURE_UPGRADER;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.node.api.upgrader.Upgrader;
import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.PortalRepository;
import io.gravitee.repository.management.model.Portal;
import lombok.CustomLog;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Rewrites legacy portal_navigation JSON (bare array) into the map-shaped
 * {@code {"TOP_NAVBAR": [...]}} format. Idempotent: rows already in the new shape are skipped.
 */
@Component
@CustomLog
public class PortalNavigationStructureUpgrader implements Upgrader {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final EnvironmentRepository environmentRepository;
    private final PortalRepository portalRepository;

    public PortalNavigationStructureUpgrader(@Lazy EnvironmentRepository environmentRepository, @Lazy PortalRepository portalRepository) {
        this.environmentRepository = environmentRepository;
        this.portalRepository = portalRepository;
    }

    @Override
    public boolean upgrade() throws UpgraderException {
        return this.wrapException(this::migrateAllEnvironments);
    }

    private boolean migrateAllEnvironments() throws TechnicalException {
        for (final var environment : environmentRepository.findAll()) {
            for (final var portal : portalRepository.findByEnvironmentId(environment.getId())) {
                migratePortal(portal);
            }
        }
        return true;
    }

    private void migratePortal(Portal portal) throws TechnicalException {
        String rewritten = rewriteToStructure(portal.getPortalNavigation());
        if (rewritten == null) {
            return;
        }
        portal.setPortalNavigation(rewritten);
        portalRepository.update(portal);
        log.debug("Rewrote portal_navigation to structure map for portal {}", portal.getId());
    }

    private static String rewriteToStructure(String currentJson) {
        if (currentJson == null || currentJson.isBlank()) {
            return null;
        }
        try {
            JsonNode root = MAPPER.readTree(currentJson);
            if (!root.isArray()) {
                return null;
            }
            var wrapper = MAPPER.createObjectNode();
            wrapper.set(PortalArea.TOP_NAVBAR.name(), (ArrayNode) root);
            return MAPPER.writeValueAsString(wrapper);
        } catch (Exception e) {
            log.warn("Skipping portal_navigation migration for malformed JSON: {}", currentJson, e);
            return null;
        }
    }

    @Override
    public int getOrder() {
        return PORTAL_NAVIGATION_STRUCTURE_UPGRADER;
    }
}
