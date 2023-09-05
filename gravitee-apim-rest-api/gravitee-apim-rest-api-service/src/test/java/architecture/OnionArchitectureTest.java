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
     * Code from {@link AbstractApimArchitectureTest#GRAVITEE_APIM_PACKAGE} should respect onion architecture
     */
    @Test
    public void should_respect_onion_architecture() {
        onionArchitecture()
            // TODO: at this point in time, we don't have the adapters, so we accept layers to be optional
            .domainModels(anyPackageThatContains(MODEL_PACKAGE))
            .domainServices(
                anyPackageThatContains(CORE_PACKAGE + "." + CRUD_SERVICE_PACKAGE),
                anyPackageThatContains(CORE_PACKAGE + "." + DOMAIN_SERVICE_PACKAGE)
            )
            .applicationServices(anyPackageThatContains(CORE_PACKAGE + "." + USECASE_PACKAGE))
            .adapter("persistence#crud_service", anyPackageThatContains(INFRA_PACKAGE + ".(**)." + CRUD_SERVICE_PACKAGE))
            .adapter("persistence#domain_service", anyPackageThatContains(INFRA_PACKAGE + ".(**)." + DOMAIN_SERVICE_PACKAGE))
            // TODO: here we allow no class to be passed to the rule because package does not exist yet (for domain_service)
            .allowEmptyShould(true)
            .check(apimClassesWithoutTests());
    }
}
