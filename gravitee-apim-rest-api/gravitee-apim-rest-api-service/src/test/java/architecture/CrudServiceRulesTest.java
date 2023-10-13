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

import static com.tngtech.archunit.base.DescribedPredicate.alwaysTrue;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.nameMatching;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import org.junit.jupiter.api.Test;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class CrudServiceRulesTest extends AbstractApimArchitectureTest {

    /**
     * CrudServices should only be in {@link AbstractApimArchitectureTest#USECASE_PACKAGE}
     */
    @Test
    public void crud_service_should_reside_in_a_crud_service_package() {
        classes()
            .that()
            .haveNameMatching(".*CrudService*")
            .should()
            .resideInAPackage(anyPackageThatContains(CRUD_SERVICE_PACKAGE))
            .as("CrudServices should reside in a package '" + anyPackageThatContains(CRUD_SERVICE_PACKAGE) + "'")
            .check(apimClassesWithoutTests());
    }

    /**
     * CrudServices should not use Usecases
     */
    @Test
    public void crud_services_should_not_use_usecase() {
        noClasses()
            .that()
            .resideInAnyPackage(anyPackageThatContains(CRUD_SERVICE_PACKAGE))
            .should()
            .dependOnClassesThat()
            .resideInAPackage(anyPackageThatContains(USECASE_PACKAGE))
            .check(apimClassesWithoutTests());
    }

    /**
     * CrudServices should be independent: it must not depend on another CrudService
     */
    @Test
    public void crud_services_should_be_independent() {
        slices()
            .matching(anyPackageThatContains(CRUD_SERVICE_PACKAGE + ".(*)"))
            .namingSlices("CrudService $1")
            .as("CrudService")
            .should()
            .notDependOnEachOther()
            // ignore dependencies from inmemory package are they implements services
            .ignoreDependency(nameMatching(".*crud_service\\.inmemory.*"), alwaysTrue())
            .check(apimClassesWithoutTests());
    }
}
