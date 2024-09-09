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
package io.gravitee.rest.api.service.impl.upgrade.initializer;

import io.gravitee.node.api.initializer.Initializer;
import io.gravitee.repository.management.model.MetadataReferenceType;
import io.gravitee.rest.api.service.MetadataService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class DefaultMetadataInitializer implements Initializer {

    private final Logger logger = LoggerFactory.getLogger(DefaultMetadataInitializer.class);

    @Autowired
    private MetadataService metadataService;

    @Override
    public boolean initialize() {
        ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        // Initialize default metadata
        if (
            metadataService
                .findByReferenceTypeAndReferenceId(MetadataReferenceType.ENVIRONMENT, executionContext.getEnvironmentId())
                .isEmpty()
        ) {
            logger.info("Create default env metadata for {}", executionContext);
            metadataService.initialize(executionContext);
        }
        return true;
    }

    @Override
    public int getOrder() {
        return InitializerOrder.DEFAULT_METADATA_INITIALIZER;
    }
}
