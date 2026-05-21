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

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import io.gravitee.common.data.domain.Page;
import org.junit.jupiter.api.Test;

/**
 * Architectural rules for upgraders.
 *
 * <p>Upgraders must complete fully — they cannot degrade gracefully like UI paths can. Since repositories may now return
 * {@link Page#getTotalElements()} = {@code -1} when the underlying count operation timed out (APIM-14093), any upgrader
 * that branches on that value risks silently skipping data. Pass-through to the UI is the only accepted consumer of
 * {@code -1}; backend "must-complete" paths must use {@code PageUtils.forEachPage(...)} or compute page-fullness from
 * {@code page.getContent().size()} instead.
 */
public class UpgraderRulesTest extends AbstractApimArchitectureTest {

    private static final String UPGRADER_PACKAGE = "..upgrade.upgrader..";

    @Test
    public void upgraders_should_not_branch_on_page_total_elements() {
        noClasses()
            .that()
            .resideInAPackage(UPGRADER_PACKAGE)
            .should()
            .callMethod(Page.class, "getTotalElements")
            .because(
                "Upgraders must complete fully even when the repository count times out (Page.totalElements may be -1). " +
                    "Use PageUtils.forEachPage(...) or rely on page-fullness (page.getContent().size() == pageSize) instead."
            )
            .check(upgraderClassesWithoutTests());
    }

    private JavaClasses upgraderClassesWithoutTests() {
        return new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("io.gravitee.rest.api.service.impl.upgrade.upgrader");
    }
}
