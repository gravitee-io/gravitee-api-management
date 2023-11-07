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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NamingConventionTest extends AbstractApimArchitectureTest {

    @Nested
    class Usecases {

        /**
         * CrudServices implementation should be suffixed with "CrudServiceImpl"
         */
        @Test
        public void usecases_should_be_suffixed_with_Usecase() {
            classes()
                .that()
                .resideInAPackage(anyPackageThatContains(USECASE_PACKAGE))
                .and()
                .areNotAnnotatedWith(Configuration.class)
                .and()
                .areTopLevelClasses()
                .should()
                .haveSimpleNameEndingWith("UseCase")
                .check(apimClassesWithoutTests());
        }
    }

    @Nested
    class CrudServices {

        /**
         * CrudServices implementation should be suffixed with "CrudServiceImpl"
         */
        @Test
        public void crud_services_implementation_should_be_suffixed_with_CrudServiceImpl() {
            classes()
                .that()
                .resideInAPackage(anyPackageThatContains(INFRA_PACKAGE + "." + CRUD_SERVICE_PACKAGE))
                .and()
                .areNotAnnotatedWith(Configuration.class)
                .and()
                .areAnnotatedWith(Component.class)
                .and()
                .resideOutsideOfPackage(".." + INFRA_PACKAGE + "." + CRUD_SERVICE_PACKAGE + ".*.adapter..")
                .should()
                .haveSimpleNameEndingWith("CrudServiceImpl")
                .check(apimClassesWithoutTests());
        }

        /**
         * CrudServices interfaces should be suffixed with "CrudService"
         */
        @Test
        public void crud_services_interfaces_should_be_suffixed_with_CrudService() {
            classes()
                .that()
                .resideInAPackage(anyPackageThatContains(CORE_PACKAGE + ".(*)." + CRUD_SERVICE_PACKAGE))
                .and()
                .areInterfaces()
                .should()
                .haveSimpleNameEndingWith("CrudService")
                .check(apimClassesWithoutTests());
        }
    }

    @Nested
    class DomainServices {

        /**
         * DomainServices implementation should be suffixed with "DomainServiceImpl"
         */
        @Test
        public void domain_services_implementation_should_be_suffixed_with_DomainServiceImpl() {
            classes()
                .that()
                .resideInAPackage(anyPackageThatContains(INFRA_PACKAGE + "." + DOMAIN_SERVICE_PACKAGE))
                .and()
                .areNotAnnotatedWith(Configuration.class)
                .and()
                .areAnnotatedWith(Component.class)
                .should()
                .haveSimpleNameEndingWith("DomainServiceImpl")
                // TODO: here we allow no class to be passed to the rule because package does not exist yet
                .allowEmptyShould(true)
                .check(apimClassesWithoutTests());
        }

        /**
         * DomainServices interfaces should be suffixed with "DomainService"
         */
        @Test
        public void domain_services_interfaces_should_be_suffixed_with_DomainService() {
            classes()
                .that()
                .resideInAPackage(anyPackageThatContains(CORE_PACKAGE + "." + DOMAIN_SERVICE_PACKAGE))
                .and()
                .areInterfaces()
                .should()
                .haveSimpleNameEndingWith("DomainService")
                // TODO: here we allow no class to be passed to the rule because package does not exist yet
                .allowEmptyShould(true)
                .check(apimClassesWithoutTests());
        }
    }
}
