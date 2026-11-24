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

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static io.gravitee.gamma.rest.architecture.AbstractGammaArchitectureTest.BASE_PACKAGE;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.gravitee.gamma.rest.core.guardrail.domain_service.GuardrailDeploymentDomainService;
import io.gravitee.gamma.rest.core.guardrail.use_case.DeployGuardrailUseCase;

/**
 * Keeps the one rule a publication cannot be trusted to carry itself: a revision is never published
 * without an enabled guardrail policy.
 *
 * <p>{@link GuardrailDeploymentDomainService#publish} publishes what it is given, and the check lives
 * in {@link DeployGuardrailUseCase} right before it. That holds only while this use case is the single
 * caller, which is what this rule pins: a second publishing path — an import, an automatic redeploy,
 * a rollback that deploys — would otherwise walk past the check without anything failing.
 */
@AnalyzeClasses(packages = BASE_PACKAGE, importOptions = { ImportOption.DoNotIncludeTests.class })
class GuardrailPublicationRulesTest {

    @ArchTest
    static final ArchRule only_the_deploy_use_case_publishes_a_guardrail_revision = methods()
        .that()
        .areDeclaredIn(GuardrailDeploymentDomainService.class)
        .and()
        .haveName("publish")
        .should()
        .onlyBeCalled()
        .byClassesThat()
        .areAssignableTo(DeployGuardrailUseCase.class);
}
