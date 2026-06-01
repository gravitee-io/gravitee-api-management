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

import static io.gravitee.rest.api.service.impl.upgrade.upgrader.UpgraderOrder.ENVIRONMENTS_DEFAULT_PORTAL_UPGRADER;

import io.gravitee.apim.core.portal.use_case.CreateDefaultPortalUseCase;
import io.gravitee.node.api.upgrader.Upgrader;
import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EnvironmentRepository;
import lombok.CustomLog;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@CustomLog
public class EnvironmentsDefaultPortalUpgrader implements Upgrader {

    private final EnvironmentRepository environmentRepository;
    private final CreateDefaultPortalUseCase createDefaultPortalUseCase;

    public EnvironmentsDefaultPortalUpgrader(
        @Lazy EnvironmentRepository environmentRepository,
        CreateDefaultPortalUseCase createDefaultPortalUseCase
    ) {
        this.environmentRepository = environmentRepository;
        this.createDefaultPortalUseCase = createDefaultPortalUseCase;
    }

    @Override
    public int getOrder() {
        return ENVIRONMENTS_DEFAULT_PORTAL_UPGRADER;
    }

    @Override
    public boolean upgrade() throws UpgraderException {
        return this.wrapException(this::applyUpgrade);
    }

    private boolean applyUpgrade() throws TechnicalException {
        for (final var environment : environmentRepository.findAll()) {
            createDefaultPortalUseCase.execute(environment.getOrganizationId(), environment.getId());
        }
        return true;
    }
}
