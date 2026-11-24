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
package io.gravitee.apim.gateway.tests.sdk.runner;

import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.definition.model.v4.guardrail.Guardrail;
import io.gravitee.gateway.handlers.guardrail.GuardrailKey;
import io.gravitee.gateway.handlers.guardrail.ReactableGuardrail;
import io.gravitee.gateway.handlers.guardrail.manager.GuardrailManager;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import org.junit.platform.commons.PreconditionViolationException;

/**
 * The Guardrails a gateway test deploys: at class level, or at test level, from an annotation or from the test itself.
 *
 * @author GraviteeSource Team
 */
class GuardrailDeployments {

    static final String GUARDRAIL_ALREADY_DEPLOYED_MESSAGE = "A Guardrail has already been deployed with id {%s}";
    static final String CANNOT_UNDEPLOY_CLASS_GUARDRAIL_MESSAGE = "A Guardrail deployed at class level cannot be undeployed (id {%s})";
    private static final String AT_CLASS_LEVEL = " at class level";

    private final AbstractGatewayTest testInstance;
    private final Supplier<GuardrailManager> guardrailManager;
    private final Map<GuardrailKey, ReactableGuardrail> deployedForTestClass = new HashMap<>();
    private final Map<GuardrailKey, ReactableGuardrail> deployedForTest = new HashMap<>();

    /**
     * @param guardrailManager supplies the manager of the running gateway, which only exists once the gateway is started.
     */
    GuardrailDeployments(AbstractGatewayTest testInstance, Supplier<GuardrailManager> guardrailManager) {
        this.testInstance = testInstance;
        this.guardrailManager = guardrailManager;
    }

    void deployForClass(Guardrail definition) {
        ReactableGuardrail reactableGuardrail = prepare(definition);
        if (deployedForTestClass.containsKey(reactableGuardrail.key())) {
            throw new PreconditionViolationException(String.format(GUARDRAIL_ALREADY_DEPLOYED_MESSAGE, reactableGuardrail.getId()));
        }
        deploy(reactableGuardrail, deployedForTestClass);
    }

    void deployForTest(Guardrail definition) {
        ReactableGuardrail reactableGuardrail = prepare(definition);
        GuardrailKey key = reactableGuardrail.key();
        if (deployedForTestClass.containsKey(key) || deployedForTest.containsKey(key)) {
            throw new PreconditionViolationException(String.format(GUARDRAIL_ALREADY_DEPLOYED_MESSAGE, reactableGuardrail.getId()));
        }
        deploy(reactableGuardrail, deployedForTest);
    }

    /**
     * Deploys a Guardrail from a test, replacing the one the test already deployed. Throws if the Guardrail is deployed at class level.
     */
    void deployFromTest(ReactableGuardrail reactableGuardrail) {
        testInstance.ensureMinimalRequirementForGuardrail(reactableGuardrail);
        GuardrailKey key = reactableGuardrail.key();
        if (deployedForTestClass.containsKey(key)) {
            throw new PreconditionViolationException(
                String.format(GUARDRAIL_ALREADY_DEPLOYED_MESSAGE + AT_CLASS_LEVEL, reactableGuardrail.getId())
            );
        }
        if (deployedForTest.remove(key) != null) {
            undeploy(reactableGuardrail);
        }
        deploy(reactableGuardrail, deployedForTest);
    }

    /**
     * Undeploys a Guardrail from a test. Throws if the Guardrail is deployed at class level.
     */
    void undeployFromTest(String guardrailId, String environmentId) {
        GuardrailKey key = new GuardrailKey(guardrailId, environmentId);
        if (deployedForTestClass.containsKey(key)) {
            throw new PreconditionViolationException(String.format(CANNOT_UNDEPLOY_CLASS_GUARDRAIL_MESSAGE, guardrailId));
        }
        ReactableGuardrail deployed = deployedForTest.remove(key);
        if (deployed != null) {
            undeploy(deployed);
        }
    }

    void undeployForClass() {
        deployedForTestClass.values().forEach(this::undeploy);
        deployedForTestClass.clear();
    }

    void undeployForTest() {
        deployedForTest.values().forEach(this::undeploy);
        deployedForTest.clear();
    }

    private ReactableGuardrail prepare(Guardrail definition) {
        ReactableGuardrail reactableGuardrail = new ReactableGuardrail(definition);
        testInstance.ensureMinimalRequirementForGuardrail(reactableGuardrail);
        return reactableGuardrail;
    }

    private void deploy(ReactableGuardrail reactableGuardrail, Map<GuardrailKey, ReactableGuardrail> deployedGuardrails) {
        Date deployedAt = new Date();
        reactableGuardrail.setDeployedAt(deployedAt);
        reactableGuardrail.setRevision(String.valueOf(deployedAt.getTime()));
        boolean registered;
        try {
            registered = guardrailManager.get().register(reactableGuardrail);
        } catch (Exception e) {
            throw new IllegalStateException("An error occurred deploying the guardrail %s".formatted(reactableGuardrail.getId()), e);
        }
        if (!registered) {
            throw new IllegalStateException(
                "The guardrail %s was not deployed: refused by the license, or older than the one deployed".formatted(
                    reactableGuardrail.getId()
                )
            );
        }
        deployedGuardrails.put(reactableGuardrail.key(), reactableGuardrail);
    }

    private void undeploy(ReactableGuardrail reactableGuardrail) {
        guardrailManager.get().unregister(reactableGuardrail.getId(), reactableGuardrail.getEnvironmentId());
    }
}
