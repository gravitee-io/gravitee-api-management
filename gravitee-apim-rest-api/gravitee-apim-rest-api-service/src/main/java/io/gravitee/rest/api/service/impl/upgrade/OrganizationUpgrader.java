/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.service.impl.upgrade;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.OrganizationRepository;
import io.gravitee.rest.api.service.Upgrader;
import io.gravitee.rest.api.service.common.ExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.Ordered;

/**
 * An upgrader that run for each organization.
 *
 * @author GraviteeSource Team
 */
public abstract class OrganizationUpgrader implements Upgrader, Ordered {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrganizationUpgrader.class);

    @Lazy
    @Autowired
    private OrganizationRepository organizationRepository;

    // upgradeOrganization is called once for each organization
    protected abstract void upgradeOrganization(ExecutionContext executionContext);

    @Override
    public final boolean upgrade() {
        try {
            organizationRepository
                .findAll()
                .forEach(
                    organization -> {
                        ExecutionContext executionContext = new ExecutionContext(organization);
                        LOGGER.info("Starting {} for {}", this.getClass().getSimpleName(), executionContext);
                        upgradeOrganization(executionContext);
                    }
                );
        } catch (TechnicalException e) {
            LOGGER.error("{} execution failed", this.getClass().getSimpleName(), e);
            return false;
        }
        return true;
    }
}
