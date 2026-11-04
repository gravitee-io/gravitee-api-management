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
import io.gravitee.definition.model.v4.guardian.Guardian;
import io.gravitee.gateway.handlers.guardian.GuardianKey;
import io.gravitee.gateway.handlers.guardian.ReactableGuardian;
import io.gravitee.gateway.handlers.guardian.manager.GuardianManager;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import org.junit.platform.commons.PreconditionViolationException;

/**
 * The Guardians a gateway test deploys: at class level, or at test level, from an annotation or from the test itself.
 *
 * @author GraviteeSource Team
 */
class GuardianDeployments {

    static final String GUARDIAN_ALREADY_DEPLOYED_MESSAGE = "A Guardian has already been deployed with id {%s}";
    static final String CANNOT_UNDEPLOY_CLASS_GUARDIAN_MESSAGE = "A Guardian deployed at class level cannot be undeployed (id {%s})";
    private static final String AT_CLASS_LEVEL = " at class level";

    private final AbstractGatewayTest testInstance;
    private final Supplier<GuardianManager> guardianManager;
    private final Map<GuardianKey, ReactableGuardian> deployedForTestClass = new HashMap<>();
    private final Map<GuardianKey, ReactableGuardian> deployedForTest = new HashMap<>();

    /**
     * @param guardianManager supplies the manager of the running gateway, which only exists once the gateway is started.
     */
    GuardianDeployments(AbstractGatewayTest testInstance, Supplier<GuardianManager> guardianManager) {
        this.testInstance = testInstance;
        this.guardianManager = guardianManager;
    }

    void deployForClass(Guardian definition) {
        ReactableGuardian reactableGuardian = prepare(definition);
        if (deployedForTestClass.containsKey(reactableGuardian.key())) {
            throw new PreconditionViolationException(String.format(GUARDIAN_ALREADY_DEPLOYED_MESSAGE, reactableGuardian.getId()));
        }
        deploy(reactableGuardian, deployedForTestClass);
    }

    void deployForTest(Guardian definition) {
        ReactableGuardian reactableGuardian = prepare(definition);
        GuardianKey key = reactableGuardian.key();
        if (deployedForTestClass.containsKey(key) || deployedForTest.containsKey(key)) {
            throw new PreconditionViolationException(String.format(GUARDIAN_ALREADY_DEPLOYED_MESSAGE, reactableGuardian.getId()));
        }
        deploy(reactableGuardian, deployedForTest);
    }

    /**
     * Deploys a Guardian from a test, replacing the one the test already deployed. Throws if the Guardian is deployed at class level.
     */
    void deployFromTest(ReactableGuardian reactableGuardian) {
        testInstance.ensureMinimalRequirementForGuardian(reactableGuardian);
        GuardianKey key = reactableGuardian.key();
        if (deployedForTestClass.containsKey(key)) {
            throw new PreconditionViolationException(
                String.format(GUARDIAN_ALREADY_DEPLOYED_MESSAGE + AT_CLASS_LEVEL, reactableGuardian.getId())
            );
        }
        if (deployedForTest.remove(key) != null) {
            undeploy(reactableGuardian);
        }
        deploy(reactableGuardian, deployedForTest);
    }

    /**
     * Undeploys a Guardian from a test. Throws if the Guardian is deployed at class level.
     */
    void undeployFromTest(String guardianId, String environmentId) {
        GuardianKey key = new GuardianKey(guardianId, environmentId);
        if (deployedForTestClass.containsKey(key)) {
            throw new PreconditionViolationException(String.format(CANNOT_UNDEPLOY_CLASS_GUARDIAN_MESSAGE, guardianId));
        }
        ReactableGuardian deployed = deployedForTest.remove(key);
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

    private ReactableGuardian prepare(Guardian definition) {
        ReactableGuardian reactableGuardian = new ReactableGuardian(definition);
        testInstance.ensureMinimalRequirementForGuardian(reactableGuardian);
        return reactableGuardian;
    }

    private void deploy(ReactableGuardian reactableGuardian, Map<GuardianKey, ReactableGuardian> deployedGuardians) {
        Date deployedAt = new Date();
        reactableGuardian.setDeployedAt(deployedAt);
        reactableGuardian.setRevision(String.valueOf(deployedAt.getTime()));
        boolean registered;
        try {
            registered = guardianManager.get().register(reactableGuardian);
        } catch (Exception e) {
            throw new IllegalStateException("An error occurred deploying the guardian %s".formatted(reactableGuardian.getId()), e);
        }
        if (!registered) {
            throw new IllegalStateException(
                "The guardian %s was not deployed: refused by the license, or older than the one deployed".formatted(
                    reactableGuardian.getId()
                )
            );
        }
        deployedGuardians.put(reactableGuardian.key(), reactableGuardian);
    }

    private void undeploy(ReactableGuardian reactableGuardian) {
        guardianManager.get().unregister(reactableGuardian.getId(), reactableGuardian.getEnvironmentId());
    }
}
