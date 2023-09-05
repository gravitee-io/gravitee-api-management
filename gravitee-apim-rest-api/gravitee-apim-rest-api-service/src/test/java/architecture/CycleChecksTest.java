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

import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import org.junit.jupiter.api.Test;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class CycleChecksTest extends AbstractApimArchitectureTest {

    /**
     * Code from {@link AbstractApimArchitectureTest#GRAVITEE_APIM_PACKAGE} should not have cyclic dependencies
     */
    @Test
    public void should_be_free_of_cycles() {
        slices()
            // checks all subpackages of APIM_PACKAGE for cycles
            .matching(GRAVITEE_APIM_PACKAGE + ".(**)..")
            .should()
            .beFreeOfCycles()
            .check(apimClasses());
    }
}
