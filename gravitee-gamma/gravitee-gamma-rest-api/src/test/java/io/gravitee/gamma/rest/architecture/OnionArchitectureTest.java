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
package io.gravitee.gamma.rest.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static io.gravitee.gamma.rest.architecture.AbstractGammaArchitectureTest.BASE_PACKAGE;
import static io.gravitee.gamma.rest.architecture.AbstractGammaArchitectureTest.CORE_PACKAGE;
import static io.gravitee.gamma.rest.architecture.AbstractGammaArchitectureTest.INFRA_PACKAGE;
import static io.gravitee.gamma.rest.architecture.AbstractGammaArchitectureTest.RESOURCES_PACKAGE;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Enforces the REST → core → infra dependency direction prescribed by the gamma-rest-api AGENTS.md §1.
 * Catches regressions where, e.g., a use case starts importing an adapter or a JAX-RS resource starts
 * touching the platform repository SPI directly instead of going through a use case.
 */
@AnalyzeClasses(packages = BASE_PACKAGE, importOptions = { ImportOption.DoNotIncludeTests.class })
class OnionArchitectureTest {

    @ArchTest
    static final ArchRule core_must_not_depend_on_infra = noClasses()
        .that()
        .resideInAPackage(CORE_PACKAGE)
        .should()
        .dependOnClassesThat()
        .resideInAPackage(INFRA_PACKAGE);

    @ArchTest
    static final ArchRule core_must_not_depend_on_rest_resources = noClasses()
        .that()
        .resideInAPackage(CORE_PACKAGE)
        .should()
        .dependOnClassesThat()
        .resideInAPackage(RESOURCES_PACKAGE);

    @ArchTest
    static final ArchRule rest_resources_must_not_depend_on_infra = noClasses()
        .that()
        .resideInAPackage(RESOURCES_PACKAGE)
        .should()
        .dependOnClassesThat()
        .resideInAPackage(INFRA_PACKAGE);

    @ArchTest
    static final ArchRule infra_must_not_depend_on_rest_resources = noClasses()
        .that()
        .resideInAPackage(INFRA_PACKAGE)
        .should()
        .dependOnClassesThat()
        .resideInAPackage(RESOURCES_PACKAGE);
}
