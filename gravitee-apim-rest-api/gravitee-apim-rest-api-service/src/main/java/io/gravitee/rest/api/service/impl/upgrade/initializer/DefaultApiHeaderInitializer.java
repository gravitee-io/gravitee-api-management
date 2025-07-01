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

import io.gravitee.rest.api.service.ApiHeaderService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
@Slf4j
public class DefaultApiHeaderInitializer extends EnvironmentInitializer {

    @Autowired
    private ApiHeaderService apiHeaderService;

    @Override
    public void initializeEnvironment(ExecutionContext executionContext) {
        // Initialize default headers
        if (apiHeaderService.findAll(executionContext.getEnvironmentId()).isEmpty()) {
            log.info("Create default API Headers configuration for {}", executionContext);
            apiHeaderService.initialize(executionContext);
        }
    }

    @Override
    public int getOrder() {
        return InitializerOrder.DEFAULT_API_HEADER_INITIALIZER;
    }
}
