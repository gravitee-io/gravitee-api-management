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

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static io.gravitee.gamma.rest.architecture.AbstractGammaArchitectureTest.BASE_PACKAGE;
import static io.gravitee.gamma.rest.architecture.AbstractGammaArchitectureTest.CORE_PACKAGE;
import static io.gravitee.gamma.rest.architecture.AbstractGammaArchitectureTest.INFRA_PACKAGE;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Enforces the naming conventions from gamma-rest-api AGENTS.md §2 — each suffix lives in its
 * prescribed package so a reader can navigate by name alone.
 */
@AnalyzeClasses(packages = BASE_PACKAGE, importOptions = { ImportOption.DoNotIncludeTests.class })
class NamingConventionTest {

    @ArchTest
    static final ArchRule repository_interfaces_should_reside_in_core_port_repository = classes()
        .that()
        .areInterfaces()
        .and()
        .haveSimpleNameEndingWith("Repository")
        .and()
        .resideInAPackage(BASE_PACKAGE + "..")
        .should()
        .resideInAPackage(CORE_PACKAGE + "port.repository..")
        .allowEmptyShould(true);

    @ArchTest
    static final ArchRule repository_implementations_should_reside_in_infra_repository = classes()
        .that()
        .areNotInterfaces()
        .and()
        .haveSimpleNameEndingWith("Repository")
        .and()
        .resideInAPackage(BASE_PACKAGE + "..")
        .should()
        .resideInAPackage(INFRA_PACKAGE + "repository..")
        .allowEmptyShould(true);

    @ArchTest
    static final ArchRule domain_services_should_reside_in_core_domain_service = classes()
        .that()
        .haveSimpleNameEndingWith("DomainService")
        .and()
        .resideInAPackage(BASE_PACKAGE + "..")
        .should()
        .resideInAPackage(CORE_PACKAGE + "domain_service..")
        .allowEmptyShould(true);

    @ArchTest
    static final ArchRule adapters_should_reside_in_infra_adapter = classes()
        .that()
        .haveSimpleNameEndingWith("Adapter")
        .and()
        .resideInAPackage(BASE_PACKAGE + "..")
        .should()
        .resideInAPackage(INFRA_PACKAGE + "adapter..");
}
