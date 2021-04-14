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

import io.gravitee.rest.api.model.api.header.ApiHeaderEntity;
import io.gravitee.rest.api.model.api.header.UpdateApiHeaderEntity;
import io.gravitee.rest.api.service.ApiHeaderService;
import io.gravitee.rest.api.service.Upgrader;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class DefaultApiHeaderUpgrader implements Upgrader, Ordered {

    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(DefaultApiHeaderUpgrader.class);

    @Autowired
    private ApiHeaderService apiHeaderService;

    @Override
    public boolean upgrade() {
        // Initialize default headers
        if (apiHeaderService.findAll().size() == 0) {
            logger.info("Create default API Headers configuration for default environment");
            apiHeaderService.initialize(GraviteeContext.getDefaultEnvironment());
        }
        return true;
    }

    @Override
    public int getOrder() {
        return 300;
    }
}
