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
package io.gravitee.apim.gateway.tests.sdk;

import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployOrganization;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployOrganizations;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeploySharedPolicyGroups;
import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayConfigurationBuilder;
import io.gravitee.apim.gateway.tests.sdk.parameters.GatewayTestParameterResolver;
import io.gravitee.apim.gateway.tests.sdk.runner.GatewayRunner;
import io.gravitee.apim.gateway.tests.sdk.secrets.SecretProviderException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
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
public class GatewayTestingExtension
    implements BeforeAllCallback, BeforeEachCallback, AfterEachCallback, AfterAllCallback, ParameterResolver {

    public static final Logger LOGGER = LoggerFactory.getLogger(GatewayTestingExtension.class);

    private GatewayRunner gatewayRunner;
    private AbstractGatewayTest gatewayTest;
    private Exception exception;

    private final Set<GatewayTestParameterResolver> parameterResolvers = Set.of(
        new HttpClientParameterResolver(),
        new ApiParameterResolver(),
        new AllApisParameterResolver()
    );

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
            deployOrganizationForClass(context);
            deploySharedPolicyGroupsForClass(context);
            deployApisForClass(context);
        } catch (Exception e) {
            LOGGER.error("Before all error: ", e);
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
        deployOrganizationForMethod(context);
        deploySharedPolicyGroupsForMethod(context);
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
    private void deployOrganizationForMethod(ExtensionContext context) throws Exception {
        List<DeployOrganization> deployOrganizations = new ArrayList<>();
        if (context.getRequiredTestMethod().isAnnotationPresent(DeployOrganizations.class)) {
            final DeployOrganizations annotation = context.getRequiredTestMethod().getAnnotation(DeployOrganizations.class);
            deployOrganizations.addAll(Arrays.asList(annotation.value()));
        }

        if (context.getRequiredTestMethod().isAnnotationPresent(DeployOrganization.class)) {
            final DeployOrganization annotation = context.getRequiredTestMethod().getAnnotation(DeployOrganization.class);
            deployOrganizations.add(annotation);
        }
        if (!deployOrganizations.isEmpty()) {
            LOGGER.debug("Deploying organizations for test {}", context.getRequiredTestMethod().getName());
            for (DeployOrganization deployOrganization : deployOrganizations) {
                try {
                    gatewayRunner.deployOrganizationForTest(deployOrganization.organization(), deployOrganization.apis());
                } catch (Exception e) {
                    exception = e;
                    throw e;
                }
            }
        }
    }

    /**
     * A Shared Policy Group can only be deployed once. If {@link io.gravitee.apim.gateway.tests.sdk.annotations.DeploySharedPolicyGroups} annotation is used both at method level and class level, method level one will take precedence over the class level one.
     * @param context
     * @throws IOException
     */
    private void deploySharedPolicyGroupsForMethod(ExtensionContext context) throws Exception {
        List<String> deploySharedPolicyGroups = new ArrayList<>();
        if (context.getRequiredTestMethod().isAnnotationPresent(DeploySharedPolicyGroups.class)) {
            final DeploySharedPolicyGroups annotation = context.getRequiredTestMethod().getAnnotation(DeploySharedPolicyGroups.class);
            deploySharedPolicyGroups.addAll(Arrays.asList(annotation.value()));
        }

        if (!deploySharedPolicyGroups.isEmpty()) {
            LOGGER.debug("Deploying shared policy groups for test {}", context.getRequiredTestMethod().getName());
            for (String deploySharedPolicyGroup : deploySharedPolicyGroups) {
                try {
                    gatewayRunner.deploySharedPolicyGroupForTest(deploySharedPolicyGroup);
                } catch (Exception e) {
                    exception = e;
                    throw e;
                }
            }
        }
    }

    /**
     * Undeploy all the apis for the current test method
     * @param context the current extension context; never {@code null}
     */
    @Override
    public void afterEach(ExtensionContext context) throws IOException {
        if (context.getRequiredTestMethod().isAnnotationPresent(DeployApi.class)) {
            LOGGER.debug("Clear test method's apis");
            gatewayRunner.undeployForTest();
        }
        if (context.getRequiredTestMethod().isAnnotationPresent(DeploySharedPolicyGroups.class)) {
            LOGGER.debug("Clear test method's shared policy groups");
            gatewayRunner.undeploySharedPolicyGroupForTest();
        }
        if (context.getRequiredTestMethod().isAnnotationPresent(DeployOrganization.class)) {
            LOGGER.debug("Clear organization");
            gatewayRunner.undeployOrganizationForTest();
        }
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
        if (requiredTestClass.isAnnotationPresent(DeploySharedPolicyGroups.class)) {
            gatewayRunner.undeploySharedPolicyGroupForClass();
        }
        if (requiredTestClass.isAnnotationPresent(DeployOrganization.class)) {
            gatewayRunner.undeployOrganizationForClass();
        }
        if (gatewayRunner != null && gatewayRunner.isRunning()) {
            gatewayRunner.stop();
        }

        if (exception != null) {
            LOGGER.error("Exception occurred during testing. {}", exception.getMessage());
        }
    }

    private void deployOrganizationForClass(final ExtensionContext context) throws IOException {
        List<DeployOrganization> deployOrganizations = new ArrayList<>();
        final Class<?> requiredTestClass = context.getRequiredTestClass();
        if (requiredTestClass.isAnnotationPresent(DeployOrganizations.class)) {
            final DeployOrganizations annotation = requiredTestClass.getAnnotation(DeployOrganizations.class);
            deployOrganizations.addAll(Arrays.asList(annotation.value()));
        }

        if (requiredTestClass.isAnnotationPresent(DeployOrganization.class)) {
            final DeployOrganization annotation = requiredTestClass.getAnnotation(DeployOrganization.class);
            deployOrganizations.add(annotation);
        }
        if (!deployOrganizations.isEmpty()) {
            LOGGER.debug("Deploying organizations for class {}", requiredTestClass.getName());
            for (DeployOrganization deployOrganization : deployOrganizations) {
                gatewayRunner.deployOrganizationForClass(deployOrganization.organization(), deployOrganization.apis());
            }
        }
    }

    private void deployApisForClass(ExtensionContext context) throws IOException {
        final Class<?> requiredTestClass = context.getRequiredTestClass();
        if (requiredTestClass.isAnnotationPresent(DeployApi.class)) {
            for (String apiDefinition : requiredTestClass.getAnnotation(DeployApi.class).value()) {
                gatewayRunner.deployForClass(apiDefinition);
            }
        }
    }

    private void deploySharedPolicyGroupsForClass(final ExtensionContext context) throws IOException {
        final Class<?> requiredTestClass = context.getRequiredTestClass();
        if (requiredTestClass.isAnnotationPresent(DeploySharedPolicyGroups.class)) {
            for (String sharedPolicyGroupDefinition : requiredTestClass.getAnnotation(DeploySharedPolicyGroups.class).value()) {
                gatewayRunner.deploySharedPolicyGroupForClass(sharedPolicyGroupDefinition);
            }
        }
    }

    private void startGateway(ExtensionContext context) throws SecretProviderException, IOException, InterruptedException {
        final Object testInstance = context.getTestInstance().orElseThrow(() -> new IllegalStateException("Cannot find a test instance"));
        if (testInstance instanceof AbstractGatewayTest gtwTest) {
            this.gatewayTest = gtwTest;
            final GatewayConfigurationBuilder gatewayConfigurationBuilder = GatewayConfigurationBuilder.emptyConfiguration();
            gatewayConfigurationBuilder.set("http.instances", 1);
            gatewayConfigurationBuilder.set("tcp.instances", 1);
            gatewayTest.configureGateway(gatewayConfigurationBuilder);
            gatewayRunner = new GatewayRunner(gatewayConfigurationBuilder, gatewayTest);

            gatewayRunner.configureAndStart(gatewayTest.gatewayPort(), gatewayTest.technicalApiPort());
        } else {
            throw new PreconditionViolationException("Test class must extend AbstractGatewayTest");
        }
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
        throws ParameterResolutionException {
        return parameterResolvers.stream().anyMatch(resolver -> resolver.supports(parameterContext));
    }

    /**
     * Vertx Webclient is not good when testing async APIs. So we inject the Vertx HttpClient which provides more freedom.
     * @param parameterContext the context for the parameter for which an argument should
     * be resolved; never {@code null}
     * @param extensionContext the extension context for the {@code Executable}
     * about to be invoked; never {@code null}
     * @return the instance of HttpClient
     * @throws ParameterResolutionException
     */
    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
        throws ParameterResolutionException {
        final AbstractGatewayTest test = parameterContext
            .getTarget()
            .map(AbstractGatewayTest.class::cast)
            .orElseThrow(() -> new PreconditionViolationException("You need to inject this in a child of AbstractGatewayTest"));

        for (GatewayTestParameterResolver parameterResolver : parameterResolvers) {
            if (parameterResolver.supports(parameterContext)) {
                return parameterResolver.resolve(extensionContext, parameterContext, test);
            }
        }

        throw new PreconditionViolationException("Parameter not injectable");
    }
}
