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
package io.gravitee.gateway.handlers.api.spring;

import io.gravitee.common.event.EventManager;
git add \
        gravitee-apim-gateway/gravitee-apim-gateway-handlers/gravitee-apim-gateway-handlers-api/src/main/java/io/gravitee/gateway/handlers/api/ReactableApiProduct.java \
        gravitee-apim-gateway/gravitee-apim-gateway-handlers/gravitee-apim-gateway-handlers-api/src/main/java/io/gravitee/gateway/handlers/api/manager/ApiProductResourceManagerFactory.java \
        gravitee-apim-gateway/gravitee-apim-gateway-handlers/gravitee-apim-gateway-handlers-api/src/main/java/io/gravitee/gateway/handlers/api/manager/impl/DefaultApiProductResourceManagerFactory.java \
        gravitee-apim-gateway/gravitee-apim-gateway-handlers/gravitee-apim-gateway-handlers-api/src/main/java/io/gravitee/gateway/handlers/api/manager/impl/ApiProductManagerImpl.java \
        gravitee-apim-gateway/gravitee-apim-gateway-handlers/gravitee-apim-gateway-handlers-api/src/main/java/io/gravitee/gateway/handlers/api/spring/ApiProductConfiguration.java \
        gravitee-apim-gateway/gravitee-apim-gateway-handlers/gravitee-apim-gateway-handlers-api/src/test/java/io/gravitee/gateway/handlers/api/ReactableApiProductTest.java \
        gravitee-apim-gateway/gravitee-apim-gateway-handlers/gravitee-apim-gateway-handlers-api/src/test/java/io/gravitee/gateway/handlers/api/manager/impl/ApiProductManagerImplTest.java \
        gravitee-apim-gateway/gravitee-apim-gateway-services/gravitee-apim-gateway-services-sync/src/main/java/io/gravitee/gateway/services/sync/process/repository/mapper/ApiProductMapper.java \
        gravitee-apim-gateway/gravitee-apim-gateway-services/gravitee-apim-gateway-services-sync/src/test/java/io/gravitee/gateway/services/sync/process/repository/mapper/ApiProductMapperTest.java

        git commit -m "$(cat <<'EOF'
        feat(gateway): start and stop API Product resources once

        Product deploy used to ignore resources even when the deploy payload
        carried them. Own one DefaultResourceManager per product in
        ApiProductManagerImpl so enabled resources start on deploy, restart
        safely on update, and stop on undeploy or license/sharding revoke.
        EOF
        )"

        git push -u origin HEADimport io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.handlers.api.manager.ApiProductManager;
import io.gravitee.gateway.handlers.api.manager.ApiProductResourceManagerFactory;
import io.gravitee.gateway.handlers.api.manager.impl.ApiProductManagerImpl;
import io.gravitee.gateway.handlers.api.manager.impl.DefaultApiProductResourceManagerFactory;
import io.gravitee.gateway.handlers.api.registry.ApiProductRegistry;
import io.gravitee.gateway.handlers.api.registry.impl.ApiProductRegistryImpl;
import io.gravitee.node.api.license.LicenseManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Arpit Mishra (arpit.mishra at graviteesource.com)
 * @author GraviteeSource Team
 */
@Configuration
public class ApiProductConfiguration {

    @Bean
    public ApiProductRegistry apiProductRegistry(GatewayConfiguration gatewayConfiguration) {
        return new ApiProductRegistryImpl(gatewayConfiguration);
    }

    @Bean
    public ApiProductResourceManagerFactory apiProductResourceManagerFactory(
        ApplicationContext applicationContext,
        ComponentProvider componentProvider
    ) {
        return new DefaultApiProductResourceManagerFactory(applicationContext, componentProvider);
    }

    @Bean
    public ApiProductManager apiProductManager(
        ApiProductRegistry apiProductRegistry,
        EventManager eventManager,
        LicenseManager licenseManager,
        GatewayConfiguration gatewayConfiguration,
        ApiProductResourceManagerFactory apiProductResourceManagerFactory
    ) {
        return new ApiProductManagerImpl(
            apiProductRegistry,
            eventManager,
            licenseManager,
            gatewayConfiguration,
            apiProductResourceManagerFactory
        );
    }
}
