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

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Configuration;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class UseCaseRulesTest extends AbstractApimArchitectureTest {

    /**
     * Usecases should only be in {@link AbstractApimArchitectureTest#USECASE_PACKAGE}
     */
    @Test
    public void useCase_should_reside_in_a_usecase_package() {
        classes()
            .that()
            .haveNameMatching(".*UseCase")
            .and()
            .areNotAnnotations()
            .should()
            .resideInAPackage(anyPackageThatContains(USECASE_PACKAGE))
            .as("UseCases should reside in a package '" + anyPackageThatContains(USECASE_PACKAGE) + "'")
            .check(apimClassesWithoutTests());
    }

    /**
     * Usecases should only depend on services (from {@link AbstractApimArchitectureTest#CRUD_SERVICE_PACKAGE} or {@link AbstractApimArchitectureTest#DOMAIN_SERVICE_PACKAGE})
     */
    @Test
    public void usecases_should_only_depend_on_service() {
        classes()
            .that()
            .resideInAnyPackage(anyPackageThatContains(CORE_PACKAGE + ".(*)." + USECASE_PACKAGE))
            .and()
            .areTopLevelClasses()
            // ignore Spring configuration class
            .and()
            .areNotAnnotatedWith(Configuration.class)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                anyPackageThatContains(CRUD_SERVICE_PACKAGE),
                anyPackageThatContains(QUERY_SERVICE_PACKAGE),
                anyPackageThatContains(DOMAIN_SERVICE_PACKAGE),
                anyPackageThatContains(SERVICE_PROVIDER_PACKAGE)
            )
            .check(apimClassesWithoutTests());
    }

    /**
     * Usecases should be independent: it must not depend on another Usecase
     */
    @Test
    public void usecases_should_be_independent() {
        slices()
            .matching(anyPackageThatContains(CORE_PACKAGE + ".(*)." + USECASE_PACKAGE))
            .namingSlices("UseCase $1")
            .as("UseCase")
            .should()
            .notDependOnEachOther()
            .check(apimClasses());
    }
}
