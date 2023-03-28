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
package io.gravitee.rest.api.service.impl.upgrade.initializer;

import io.gravitee.node.api.initializer.Initializer;
import io.gravitee.rest.api.service.OrganizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class DefaultOrganizationInitializer implements Initializer {

    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(DefaultOrganizationInitializer.class);

    @Autowired
    private OrganizationService organizationService;

    @Override
    public boolean initialize() {
        // initialize default organization.
        if (organizationService.count().equals(0L)) {
            logger.info("    No organization found. Add default one.");
            organizationService.initialize();
        }
        return true;
    }

    @Override
    public int getOrder() {
        return 100;
    }
}
