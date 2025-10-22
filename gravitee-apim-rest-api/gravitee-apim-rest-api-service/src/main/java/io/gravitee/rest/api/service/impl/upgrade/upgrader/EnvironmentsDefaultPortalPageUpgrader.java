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

import static io.gravitee.rest.api.service.impl.upgrade.upgrader.UpgraderOrder.ENVIRONMENTS_DEFAULT_PORTAL_PAGE_UPGRADER;

import io.gravitee.node.api.upgrader.Upgrader;
import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.rest.api.service.PortalPageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class EnvironmentsDefaultPortalPageUpgrader implements Upgrader {

    private final EnvironmentRepository environmentRepository;
    private final PortalPageService portalPageService;

    public EnvironmentsDefaultPortalPageUpgrader(@Lazy EnvironmentRepository environmentRepository, PortalPageService portalPageService) {
        this.environmentRepository = environmentRepository;
        this.portalPageService = portalPageService;
    }

    @Override
    public int getOrder() {
        return ENVIRONMENTS_DEFAULT_PORTAL_PAGE_UPGRADER;
    }

    @Override
    public boolean upgrade() throws UpgraderException {
        try {
            for (var environment : environmentRepository.findAll()) {
                portalPageService.createDefaultPortalHomePage(environment.getId());
            }
            return true;
        } catch (TechnicalException e) {
            log.error("An error occurred while applying EnvironmentsDefaultPortalPageUpgrader upgrader", e);
            return false;
        }
    }
}
