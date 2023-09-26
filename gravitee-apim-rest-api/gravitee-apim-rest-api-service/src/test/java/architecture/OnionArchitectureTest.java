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
package architecture;

import static com.tngtech.archunit.library.Architectures.onionArchitecture;

import org.junit.jupiter.api.Test;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class OnionArchitectureTest extends AbstractApimArchitectureTest {

    /**
     * Code from {@link AbstractApimArchitectureTest#GRAVITEE_APIM_PACKAGE} should respect
     * <a href="https://www.archunit.org/userguide/html/000_Index.html#_onion_architecture">onion architecture</a>
     * The domain package is the core of the application. It consists of two parts.
     *
     * - The domainModels packages contain the domain entities.
     *
     * - The packages in domainServices contain services that use the entities in the domainModel packages.
     *
     * The applicationServices packages contain services and configuration to run the application and use cases.
     * It can use the items of the domain package but there must not be any dependency from the domain to the application packages.
     *
     * The adapter package contains logic to connect to external systems and/or infrastructure.
     * No adapter may depend on another adapter. Adapters can use both the items of the domain as well as the application packages.
     * Vice versa, neither the domain nor the application packages must contain dependencies on any adapter package.
     */
    @Test
    public void should_respect_onion_architecture() {
        onionArchitecture()
            .domainModels(anyPackageThatContains(MODEL_PACKAGE))
            .domainServices(
                "io.gravitee.apim.core.*." + CRUD_SERVICE_PACKAGE + "..",
                "io.gravitee.apim.core.*." + QUERY_SERVICE_PACKAGE + "..",
                "io.gravitee.apim.core.*." + DOMAIN_SERVICE_PACKAGE + ".."
            )
            .applicationServices(anyPackageThatContains(CORE_PACKAGE + ".*." + USECASE_PACKAGE))
            .adapter("infra", anyPackageThatContains(INFRA_PACKAGE), anyPackageThatContains("io.gravitee.rest.api.service"))
            .check(apimClassesWithoutTests());
    }
}
