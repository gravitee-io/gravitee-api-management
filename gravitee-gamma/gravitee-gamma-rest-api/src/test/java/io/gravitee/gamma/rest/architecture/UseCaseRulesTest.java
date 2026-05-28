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
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static io.gravitee.gamma.rest.architecture.AbstractGammaArchitectureTest.BASE_PACKAGE;
import static io.gravitee.gamma.rest.architecture.AbstractGammaArchitectureTest.CORE_PACKAGE;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.gravitee.apim.core.UseCase;

/**
 * Enforces the use-case rules from gamma-rest-api AGENTS.md §3: location, annotation, no-cross-use-case
 * dependencies. Catches the most common Hexagonal regression — a use case starts calling another use
 * case, which collapses the "one action per class" contract.
 */
@AnalyzeClasses(packages = BASE_PACKAGE, importOptions = { ImportOption.DoNotIncludeTests.class })
class UseCaseRulesTest {

    @ArchTest
    static final ArchRule use_cases_should_reside_in_use_case_packages = classes()
        .that()
        .haveSimpleNameEndingWith("UseCase")
        .and()
        .resideInAPackage(BASE_PACKAGE + "..")
        .should()
        .resideInAPackage(CORE_PACKAGE + "use_case..");

    @ArchTest
    static final ArchRule use_cases_should_be_annotated_with_use_case = classes()
        .that()
        .resideInAPackage(CORE_PACKAGE + "use_case..")
        .and()
        .haveSimpleNameEndingWith("UseCase")
        .should()
        .beAnnotatedWith(UseCase.class);

    @ArchTest
    static final ArchRule use_cases_should_not_depend_on_other_use_cases = noClasses()
        .that()
        .areAnnotatedWith(UseCase.class)
        .should()
        .dependOnClassesThat()
        .areAnnotatedWith(UseCase.class);
}
