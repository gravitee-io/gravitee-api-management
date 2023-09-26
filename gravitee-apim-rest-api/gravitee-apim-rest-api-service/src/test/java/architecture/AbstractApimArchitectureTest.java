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

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class AbstractApimArchitectureTest {

    public final String IO_GRAVITEE_PACKAGE = "io.gravitee";
    public final String APIM_PACKAGE = "apim";
    public final String CORE_PACKAGE = "core";
    public final String INFRA_PACKAGE = "infra";
    public final String REST_API_SERVICE_PACKAGE = "rest.api.service";
    public final String GRAVITEE_APIM_PACKAGE = IO_GRAVITEE_PACKAGE + "." + APIM_PACKAGE;
    public final String GRAVITEE_REST_API_SERVICE_PACKAGE = IO_GRAVITEE_PACKAGE + "." + REST_API_SERVICE_PACKAGE;
    public final String USECASE_PACKAGE = "usecase";
    public final String CRUD_SERVICE_PACKAGE = "crud_service";
    public final String QUERY_SERVICE_PACKAGE = "query_service";
    public final String DOMAIN_SERVICE_PACKAGE = "domain_service";
    public final String NOTIFICATION_PACKAGE = "notification";
    public final String ADAPTER_PACKAGE = "adapter";
    public final String MODEL_PACKAGE = "model";

    public JavaClasses apimClassesWithoutTests() {
        return apimClasses(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS);
    }

    public JavaClasses apimClasses() {
        return apimClasses(null);
    }

    public JavaClasses apimClasses(ImportOption importOption) {
        ClassFileImporter classFileImporter = new ClassFileImporter();
        if (importOption != null) {
            classFileImporter = classFileImporter.withImportOption(importOption);
        }
        return classFileImporter.importPackages(GRAVITEE_APIM_PACKAGE);
    }

    public JavaClasses classesFrom(String packageName) {
        ClassFileImporter classFileImporter = new ClassFileImporter();
        return classFileImporter.importPackages(packageName);
    }

    @NotNull
    protected String anyPackageThatContains(String subPackage) {
        return ".." + subPackage + "..";
    }
}
