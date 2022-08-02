/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.apim.gateway.tests.sdk;

import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployOrganization;
import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayConfigurationBuilder;
import io.gravitee.apim.gateway.tests.sdk.runner.GatewayRunner;
import java.io.IOException;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.PreconditionViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <pre>
 * This extension is in charge of:
 * - Start/Stop a gateway in a `BeforeAll/AfterAll` style, meaning there is only one instance of gateway running per test class
 * - Deploy/Undeploy apis in a `BeforeAll/AfterAll` style: they are deployed and undeployed only once per test class annotated with {@link DeployApi}
 * - Deploy/Undeploy apis in a `BeforeEach/AfterEach` style: they are deployed and undeployed for each test annotated with {@link DeployApi}
 * </pre>
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GatewayTestingExtension implements BeforeAllCallback, BeforeEachCallback, AfterEachCallback, AfterAllCallback {

    public static final Logger LOGGER = LoggerFactory.getLogger(GatewayTestingExtension.class);

    private GatewayRunner gatewayRunner;
    private AbstractGatewayTest gatewayTest;
    private Exception exception;

    /**
     * Starts the gateway and deploy apis declared at class level.
     * If trying to deploy an api which has the same id as an already deployed one, tests will be skipped explaining which api's id is in error.
     * @param context the current extension context; never {@code null}
     * @throws Exception
     */
    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        LOGGER.debug("Before all for {}", context.getRequiredTestClass().getSimpleName());

        try {
            startGateway(context);
            deployApisForClass(context);
        } catch (Exception e) {
            LOGGER.error("Before all error: {}", e.getMessage());
            exception = e;
            if (gatewayRunner != null && gatewayRunner.isRunning()) {
                gatewayRunner.stop();
            }
            throw e;
        }
    }

    /**
     * If trying to deploy an api which has the same id as an already deployed one, tests will be failed explaining which api's id is in error.
     * Set the deployed apis {@link java.util.Map} on the test instance inherited from {@link AbstractGatewayTest}
     * @param context the current extension context; never {@code null}
     * @throws Exception
     */
    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        beforeDeployingOrganization(context);
        if (context.getRequiredTestMethod().isAnnotationPresent(DeployApi.class)) {
            LOGGER.debug("Deploying apis for test {}", context.getRequiredTestMethod().getName());
            final DeployApi annotation = context.getRequiredTestMethod().getAnnotation(DeployApi.class);
            for (String apiDefinition : annotation.value()) {
                try {
                    gatewayRunner.deployForTest(apiDefinition);
                } catch (Exception e) {
                    exception = e;
                    throw e;
                }
            }
        }

        gatewayTest.deployedApis = gatewayRunner.deployedApis();
    }

    /**
     * An organization can only be deployed once. If {@link DeployOrganization} annotation is used both at method level and class level, method level one will take precedence over the class level one.
     * @param context
     * @throws IOException
     */
    private void beforeDeployingOrganization(ExtensionContext context) throws IOException {
        if (context.getRequiredTestMethod().isAnnotationPresent(DeployOrganization.class)) {
            LOGGER.debug("Deploying organization for test {}", context.getRequiredTestMethod().getName());
            final DeployOrganization annotation = context.getRequiredTestMethod().getAnnotation(DeployOrganization.class);
            try {
                gatewayRunner.deployOrganization(annotation.value());
            } catch (Exception e) {
                exception = e;
                throw e;
            }
        } else if (context.getRequiredTestClass().isAnnotationPresent(DeployOrganization.class)) {
            final DeployOrganization annotation = context.getRequiredTestClass().getAnnotation(DeployOrganization.class);
            gatewayRunner.deployOrganization(annotation.value());
        }
    }

    /**
     * Undeploy all the apis for the current test method
     * @param context the current extension context; never {@code null}
     */
    @Override
    public void afterEach(ExtensionContext context) {
        if (context.getRequiredTestMethod().isAnnotationPresent(DeployApi.class)) {
            LOGGER.debug("Clear test method's apis");
            gatewayRunner.undeployForTest();
        }
        LOGGER.debug("Clear organization");
        gatewayRunner.undeployOrganization();
    }

    /**
     * Undeploy all the apis declared at class level and stop the gateway.
     * Add an error log at the end if there was an error attempting to deploy two apis with the same id.
     * @param context the current extension context; never {@code null}
     * @throws Exception
     */
    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        final Class<?> requiredTestClass = context.getRequiredTestClass();
        if (requiredTestClass.isAnnotationPresent(DeployApi.class)) {
            gatewayRunner.undeployForClass();
        }
        if (gatewayRunner != null && gatewayRunner.isRunning()) {
            gatewayRunner.stop();
        }

        if (exception != null) {
            LOGGER.error("Exception occurred during testing. {}", exception.getMessage());
        }
    }

    private void deployApisForClass(ExtensionContext context) throws Exception {
        final Class<?> requiredTestClass = context.getRequiredTestClass();
        if (requiredTestClass.isAnnotationPresent(DeployApi.class)) {
            for (String apiDefinition : requiredTestClass.getAnnotation(DeployApi.class).value()) {
                gatewayRunner.deployForClass(apiDefinition);
            }
        }
    }

    private void startGateway(ExtensionContext context) throws Exception {
        final Object testInstance = context.getTestInstance().orElseThrow(() -> new IllegalStateException("Cannot find a test instance"));
        if (testInstance instanceof AbstractGatewayTest) {
            gatewayTest = (AbstractGatewayTest) testInstance;
            final GatewayConfigurationBuilder gatewayConfigurationBuilder = GatewayConfigurationBuilder.emptyConfiguration();
            gatewayConfigurationBuilder.set("http.instances", 1);
            gatewayTest.configureGateway(gatewayConfigurationBuilder);
            gatewayRunner = new GatewayRunner(gatewayConfigurationBuilder, gatewayTest);

            gatewayRunner.configureAndStart(gatewayTest.gatewayPort(), gatewayTest.technicalApiPort());
        } else {
            throw new PreconditionViolationException("Test class must extend AbstractGatewayTest");
        }
    }
}
