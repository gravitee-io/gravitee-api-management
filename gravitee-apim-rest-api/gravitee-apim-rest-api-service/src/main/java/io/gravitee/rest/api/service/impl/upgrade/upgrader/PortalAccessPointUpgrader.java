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

import static io.gravitee.rest.api.service.impl.upgrade.upgrader.UpgraderOrder.PORTAL_ACCESS_POINT_UPGRADER;

import io.gravitee.apim.core.portal.domain_service.DefaultPortalDomainService;
import io.gravitee.node.api.upgrader.Upgrader;
import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.rest.api.service.common.GraviteeContext;
import lombok.CustomLog;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Links the portals stored with a URL, before portals referenced an access point, to the access point their environment's
 * portals share: each of them stored its environment's portal URL, so none is lost.
 */
@Component
@CustomLog
public class PortalAccessPointUpgrader implements Upgrader {

    private final EnvironmentRepository environmentRepository;
    private final DefaultPortalDomainService defaultPortalDomainService;

    public PortalAccessPointUpgrader(
        @Lazy EnvironmentRepository environmentRepository,
        DefaultPortalDomainService defaultPortalDomainService
    ) {
        this.environmentRepository = environmentRepository;
        this.defaultPortalDomainService = defaultPortalDomainService;
    }

    @Override
    public int getOrder() {
        return PORTAL_ACCESS_POINT_UPGRADER;
    }

    @Override
    public boolean upgrade() throws UpgraderException {
        return this.wrapException(this::applyUpgrade);
    }

    private boolean applyUpgrade() throws TechnicalException {
        for (var environment : environmentRepository.findAll()) {
            // ParameterService memoizes parameters per thread by key only: clear the context around each environment so
            // none gets a portal URL read for another one.
            GraviteeContext.cleanContext();
            try {
                var linked = defaultPortalDomainService.linkUnlinkedPortals(environment.getId());
                if (linked > 0) {
                    log.info("Linked {} portal(s) of environment [{}] to an access point", linked, environment.getId());
                }
            } finally {
                GraviteeContext.cleanContext();
            }
        }
        return true;
    }
}
