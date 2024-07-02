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
package io.gravitee.gateway.handlers.environmentflow.spring;

import io.gravitee.common.event.EventManager;
import io.gravitee.gateway.handlers.environmentflow.manager.EnvironmentFlowManager;
import io.gravitee.gateway.handlers.environmentflow.manager.impl.EnvironmentFlowManagerImpl;
import io.gravitee.node.api.license.LicenseManager;
import org.springframework.context.annotation.Bean;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EnvironmentFlowConfiguration {

    @Bean
    public EnvironmentFlowManager environmentFlowManager(EventManager eventManager, LicenseManager licenseManager) {
        return new EnvironmentFlowManagerImpl(eventManager, licenseManager);
    }
}
