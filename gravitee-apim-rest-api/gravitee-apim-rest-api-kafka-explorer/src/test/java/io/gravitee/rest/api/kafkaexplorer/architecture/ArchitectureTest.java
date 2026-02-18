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
package io.gravitee.rest.api.kafkaexplorer.architecture;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ArchitectureTest {

    private static final String BASE_PACKAGE = "io.gravitee.rest.api.kafkaexplorer";
    private static final String DOMAIN_PACKAGE = BASE_PACKAGE + ".domain..";
    private static final String DOMAIN_SERVICE_PACKAGE = BASE_PACKAGE + ".domain.domain_service..";
    private static final String RESOURCE_PACKAGE = BASE_PACKAGE + ".resource..";
    private static final String INFRASTRUCTURE_PACKAGE = BASE_PACKAGE + ".infrastructure..";
    private static final String MAPPER_PACKAGE = BASE_PACKAGE + ".mapper..";
    private static final String GENERATED_MODEL_PACKAGE = BASE_PACKAGE + ".rest.model..";

    private final JavaClasses classes = new ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages(BASE_PACKAGE);

    private final JavaClasses allClasses = new ClassFileImporter().importPackages(BASE_PACKAGE);

    @Nested
    class DomainLayer {

        @Test
        void domain_should_not_depend_on_resource() {
            noClasses()
                .that()
                .resideInAPackage(DOMAIN_PACKAGE)
                .should()
                .dependOnClassesThat()
                .resideInAPackage(RESOURCE_PACKAGE)
                .because("Domain layer must not depend on the resource (REST) layer")
                .check(classes);
        }

        @Test
        void domain_should_not_depend_on_mapper() {
            noClasses()
                .that()
                .resideInAPackage(DOMAIN_PACKAGE)
                .should()
                .dependOnClassesThat()
                .resideInAPackage(MAPPER_PACKAGE)
                .because("Domain layer must not depend on mappers")
                .check(classes);
        }

        @Test
        void domain_should_not_depend_on_generated_models() {
            noClasses()
                .that()
                .resideInAPackage(DOMAIN_PACKAGE)
                .should()
                .dependOnClassesThat()
                .resideInAPackage(GENERATED_MODEL_PACKAGE)
                .because("Domain layer must not depend on OpenAPI generated models")
                .check(classes);
        }

        @Test
        void domain_should_not_depend_on_jakarta_ws_rs() {
            noClasses()
                .that()
                .resideInAPackage(DOMAIN_PACKAGE)
                .should()
                .dependOnClassesThat()
                .resideInAPackage("jakarta.ws.rs..")
                .because("Domain layer must not depend on JAX-RS")
                .check(classes);
        }
    }

    @Nested
    class UseCaseRules {

        @Test
        void use_cases_should_reside_in_use_case_package() {
            classes()
                .that()
                .haveNameMatching(".*UseCase")
                .and()
                .areNotAnnotations()
                .should()
                .resideInAPackage("..use_case..")
                .check(classes);
        }

        @Test
        void use_cases_should_be_suffixed_with_UseCase() {
            classes()
                .that()
                .resideInAPackage("..use_case..")
                .and()
                .areTopLevelClasses()
                .should()
                .haveSimpleNameEndingWith("UseCase")
                .check(classes);
        }

        @Test
        void use_cases_should_be_independent() {
            slices()
                .matching("..domain.(*).use_case..")
                .namingSlices("UseCase $1")
                .as("UseCase")
                .should()
                .notDependOnEachOther()
                .allowEmptyShould(true)
                .check(allClasses);
        }
    }

    @Nested
    class MapperLayer {

        @Test
        void mapper_should_not_depend_on_resource() {
            noClasses()
                .that()
                .resideInAPackage(MAPPER_PACKAGE)
                .should()
                .dependOnClassesThat()
                .resideInAPackage(RESOURCE_PACKAGE)
                .because("Mappers must be pure converters and not depend on the resource layer")
                .check(classes);
        }

        @Test
        void mapper_should_not_depend_on_use_cases() {
            noClasses()
                .that()
                .resideInAPackage(MAPPER_PACKAGE)
                .should()
                .dependOnClassesThat()
                .resideInAPackage(BASE_PACKAGE + ".domain.use_case..")
                .because("Mappers must be pure converters and not depend on use cases")
                .check(classes);
        }
    }

    @Nested
    class InfrastructureLayer {

        @Test
        void infrastructure_should_not_depend_on_resource() {
            noClasses()
                .that()
                .resideInAPackage(INFRASTRUCTURE_PACKAGE)
                .should()
                .dependOnClassesThat()
                .resideInAPackage(RESOURCE_PACKAGE)
                .because("Infrastructure layer must not depend on the resource (REST) layer")
                .check(classes);
        }

        @Test
        void infrastructure_should_not_depend_on_use_cases() {
            noClasses()
                .that()
                .resideInAPackage(INFRASTRUCTURE_PACKAGE)
                .should()
                .dependOnClassesThat()
                .resideInAPackage(BASE_PACKAGE + ".domain.use_case..")
                .because("Infrastructure layer must not depend on use cases")
                .check(classes);
        }
    }

    @Nested
    class DomainIndependence {

        @Test
        void domain_should_only_depend_on_allowed_packages() {
            classes()
                .that()
                .resideInAPackage(DOMAIN_PACKAGE)
                .should()
                .onlyDependOnClassesThat(
                    resideInAnyPackage(
                        DOMAIN_PACKAGE,
                        DOMAIN_SERVICE_PACKAGE,
                        "java..",
                        "lombok..",
                        "org.slf4j..",
                        "io.gravitee.apim.core..",
                        "com.fasterxml.jackson.."
                    )
                )
                .because("Domain should be free from framework dependencies")
                .check(classes);
        }
    }

    @Nested
    class CycleChecks {

        @Test
        void should_be_free_of_cycles() {
            slices().matching(BASE_PACKAGE + ".(**)..").should().beFreeOfCycles().check(allClasses);
        }
    }
}
