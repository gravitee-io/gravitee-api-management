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
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.UseCase;
import org.junit.jupiter.api.Test;

class GammaArchitectureTest {

    private static final String CORE = "io.gravitee.gamma.core..";
    private static final String INFRA = "io.gravitee.gamma.infra..";
    private static final JavaClasses CLASSES = new ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages("io.gravitee.gamma");

    @Test
    void use_cases_must_live_in_use_case_package_and_end_with_UseCase() {
        ArchRule rule = classes()
            .that()
            .areAnnotatedWith(UseCase.class)
            .should()
            .resideInAPackage("..use_case..")
            .andShould()
            .haveSimpleNameEndingWith("UseCase");
        rule.check(CLASSES);
    }

    @Test
    void domain_services_must_live_in_domain_service_package_and_end_with_DomainService() {
        ArchRule rule = classes()
            .that()
            .areAnnotatedWith(DomainService.class)
            .should()
            .resideInAPackage("..domain_service..")
            .andShould()
            .haveSimpleNameEndingWith("DomainService");
        rule.check(CLASSES);
    }

    @Test
    void use_cases_must_not_depend_on_other_use_cases() {
        ArchRule rule = noClasses().that().areAnnotatedWith(UseCase.class).should().dependOnClassesThat().areAnnotatedWith(UseCase.class);
        rule.check(CLASSES);
    }

    @Test
    void core_must_not_depend_on_spring_framework_except_transactional() {
        ArchRule rule = noClasses()
            .that()
            .resideInAPackage(CORE)
            .and()
            .areNotAnnotations()
            .should()
            .dependOnClassesThat()
            .resideInAPackage("org.springframework..");
        rule.check(CLASSES);
    }

    @Test
    void core_must_not_depend_on_infra() {
        ArchRule rule = noClasses().that().resideInAPackage(CORE).should().dependOnClassesThat().resideInAPackage(INFRA);
        rule.check(CLASSES);
    }
}
