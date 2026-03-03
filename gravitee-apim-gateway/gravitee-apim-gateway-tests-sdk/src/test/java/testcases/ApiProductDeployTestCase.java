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
package testcases;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApiProducts;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.gateway.handlers.api.ReactableApiProduct;
import io.gravitee.gateway.handlers.api.manager.ApiProductManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Verifies API Product deploy/undeploy lifecycle in the gateway test SDK.
 *
 * @author GraviteeSource Team
 */
@GatewayTest
@EnableForGatewayTestingExtensionTesting
class ApiProductDeployTestCase extends AbstractGatewayTest {

    @Nested
    @DeployApiProducts("/api-products/product-1.json")
    @DisplayName("Class-level API Product deployment")
    class ClassLevelDeployment {

        @Test
        @DisplayName("Should deploy API Product at class level and find it in registry")
        void should_deploy_api_product_at_class_level() {
            ApiProductManager apiProductManager = getBean(ApiProductManager.class);
            ReactableApiProduct product = apiProductManager.get("api-product-1");

            assertThat(product).isNotNull();
            assertThat(product.getId()).isEqualTo("api-product-1");
            assertThat(product.getName()).isEqualTo("Test API Product");
            assertThat(product.getEnvironmentId()).isEqualTo("DEFAULT");
            assertThat(product.getApiIds()).containsExactlyInAnyOrderElementsOf(java.util.Set.of("api-1", "api-2"));
        }
    }

    @Nested
    @DisplayName("Method-level API Product deployment")
    class MethodLevelDeployment {

        @Test
        @DeployApiProducts("/api-products/product-1.json")
        @DisplayName("Should deploy API Product at method level and find it in registry")
        void should_deploy_api_product_at_method_level() {
            ApiProductManager apiProductManager = getBean(ApiProductManager.class);
            ReactableApiProduct product = apiProductManager.get("api-product-1");

            assertThat(product).isNotNull();
            assertThat(product.getId()).isEqualTo("api-product-1");
        }
    }
}
