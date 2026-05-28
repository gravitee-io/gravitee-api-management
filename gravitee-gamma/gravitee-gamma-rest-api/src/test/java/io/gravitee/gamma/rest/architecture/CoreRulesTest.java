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

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Enforces gamma-rest-api AGENTS.md §5 (Core layer independence) — the {@code core} package must not
 * leak into any framework or infrastructure dependency. Spring, JAX-RS, Jackson databind, the
 * platform repository SPIs, etc. are all banned from {@code core}; they belong in
 * {@code infra} (adapters / Spring config) or in {@code resources} (JAX-RS).
 */
@AnalyzeClasses(packages = BASE_PACKAGE, importOptions = { ImportOption.DoNotIncludeTests.class })
class CoreRulesTest {

    @ArchTest
    static final ArchRule core_should_only_depend_on_allowed_packages = classes()
        .that()
        .resideInAPackage(CORE_PACKAGE)
        .should()
        .onlyDependOnClassesThat()
        .resideInAnyPackage(
            "java..",
            "lombok..",
            "com.fasterxml..",
            "org.slf4j..",
            "io.gravitee.common..",
            "io.gravitee.apim.core",
            "io.gravitee.apim.core.exception..",
            // meta-annotation pulled transitively by {@code @UseCase} (accepted trade-off, AGENTS.md §5).
            "org.springframework.transaction.annotation..",
            CORE_PACKAGE
        );
}
