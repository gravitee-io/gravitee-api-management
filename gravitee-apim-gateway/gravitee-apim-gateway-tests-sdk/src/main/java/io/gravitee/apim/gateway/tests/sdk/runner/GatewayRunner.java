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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployOrganization;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayConfigurationBuilder;
import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayConfigurationBuilder.GatewayConfiguration;
import io.gravitee.apim.gateway.tests.sdk.connector.ConnectorBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.container.GatewayTestContainer;
import io.gravitee.apim.gateway.tests.sdk.converters.ApiDeploymentPreparer;
import io.gravitee.apim.gateway.tests.sdk.converters.LegacyApiDeploymentPreparer;
import io.gravitee.apim.gateway.tests.sdk.converters.V4ApiDeploymentPreparer;
import io.gravitee.apim.gateway.tests.sdk.plugin.PluginManifestLoader;
import io.gravitee.apim.gateway.tests.sdk.policy.KeylessPolicy;
import io.gravitee.apim.gateway.tests.sdk.policy.PolicyBuilder;
import io.gravitee.apim.gateway.tests.sdk.protocolhandlers.grpc.Handler;
import io.gravitee.apim.gateway.tests.sdk.reporter.FakeReporter;
import io.gravitee.apim.gateway.tests.sdk.secrets.SecretProviderException;
import io.gravitee.apim.plugin.reactor.ReactorPlugin;
import io.gravitee.apim.plugin.reactor.ReactorPluginManager;
import io.gravitee.common.component.Lifecycle;
import io.gravitee.common.event.impl.SimpleEvent;
import io.gravitee.connector.http.HttpConnectorFactory;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.ExecutionMode;
import io.gravitee.definition.model.v4.sharedpolicygroup.SharedPolicyGroup;
import io.gravitee.gateway.handlers.api.manager.ApiManager;
import io.gravitee.gateway.handlers.sharedpolicygroup.ReactableSharedPolicyGroup;
import io.gravitee.gateway.handlers.sharedpolicygroup.manager.SharedPolicyGroupManager;
import io.gravitee.gateway.platform.organization.ReactableOrganization;
import io.gravitee.gateway.platform.organization.manager.OrganizationManager;
import io.gravitee.gateway.reactive.reactor.v4.reactor.ReactorFactory;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.gateway.standalone.vertx.VertxEmbeddedContainer;
import io.gravitee.node.api.secrets.SecretManagerConfiguration;
import io.gravitee.node.api.secrets.SecretProviderFactory;
import io.gravitee.node.container.spring.env.GraviteeYamlPropertySource;
import io.gravitee.node.reporter.ReporterManager;
import io.gravitee.node.secrets.plugins.SecretProviderPlugin;
import io.gravitee.node.secrets.plugins.SecretProviderPluginManager;
import io.gravitee.plugin.connector.ConnectorPlugin;
import io.gravitee.plugin.connector.ConnectorPluginManager;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.core.api.PluginEvent;
import io.gravitee.plugin.core.api.PluginManifest;
import io.gravitee.plugin.core.internal.PluginEventListener;
import io.gravitee.plugin.core.internal.PluginFactory;
import io.gravitee.plugin.core.internal.PluginImpl;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.endpoint.EndpointConnectorPluginManager;
import io.gravitee.plugin.endpoint.mock.MockEndpointConnectorFactory;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPluginManager;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.plugin.resource.ResourcePlugin;
import io.gravitee.reporter.api.Reporter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import lombok.SneakyThrows;
import org.junit.platform.commons.PreconditionViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

/**
 * <pre>
 * Manages the lifecycle of the testing Gateway Instance.
 * Before tests, it will configure (properties and plugin registration) and start the gateway, deploy the apis declared at class level then at method level with {@link DeployApi}.
 * After tests, it will gracefully undeploy all the apis and then stop the Gateway.
 * </pre>
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GatewayRunner {

    public static final String DEFAULT_CONFIGURATION_FOLDER = "/gravitee-default";

    public static final Logger LOGGER = LoggerFactory.getLogger(GatewayRunner.class);
    public static final String API_ALREADY_DEPLOYED_MESSAGE = "An API has already been deployed with id {%s}";
    public static final String SPG_ALREADY_DEPLOYED_MESSAGE = "A Shared Policy Group has already been deployed with id {%s}";
    public static final String ORGANIZATION_ALREADY_DEPLOYED_MESSAGE = "An organization has already been deployed with id {%s}";
    public static final String SHARED_POLICY_GROUP_ALREADY_DEPLOYED_MESSAGE =
        "A shared policy group has already been deployed with id {%s}";
    public static final String CANNOT_UNDEPPLOY_CLASS_API_MESSAGE = "An API deployed at class level cannot be undeployed (id {%s})";
    public static final String CANNOT_UNDEPPLOY_CLASS_SPG_MESSAGE =
        "A Shared Policy Group deployed at class level cannot be undeployed (id {%s})";

    private final GatewayConfigurationBuilder gatewayConfigurationBuilder;
    private final AbstractGatewayTest testInstance;
    private final ExecutionMode v2ExecutionMode;
    private final ObjectMapper graviteeMapper;
    private final Map<String, ReactableApi<?>> deployedForTestClass;
    private final Map<String, ReactableApi<?>> deployedForTest;
    private final Map<DefinitionVersion, ApiDeploymentPreparer> apiDeploymentPreparers;
    private final Properties configuredSystemProperties;
    private final Map<String, ReactableOrganization> deployedOrganizationsForTestClass;
    private final Map<String, ReactableOrganization> deployedOrganizationsForTest;
    private final Map<SharedPolicyGroupKey, ReactableSharedPolicyGroup> deployedSharedPolicyGroupsForTestClass;
    private final Map<SharedPolicyGroupKey, ReactableSharedPolicyGroup> deployedSharedPolicyGroupsForTest;
    private GatewayTestContainer gatewayContainer;
    private VertxEmbeddedContainer vertxContainer;
    private Path tempDir;
    private boolean isRunning = false;

    record SharedPolicyGroupKey(String sharedPolicyGroupId, String environmentId) {}

    public GatewayRunner(GatewayConfigurationBuilder gatewayConfigurationBuilder, AbstractGatewayTest testInstance) {
        this.gatewayConfigurationBuilder = gatewayConfigurationBuilder;
        this.testInstance = testInstance;
        this.v2ExecutionMode = testInstance.getClass().getAnnotation(GatewayTest.class).v2ExecutionMode();
        this.graviteeMapper = new GraviteeMapper();
        this.deployedForTestClass = new HashMap<>();
        this.deployedForTest = new HashMap<>();
        this.configuredSystemProperties = new Properties();
        this.deployedOrganizationsForTestClass = new HashMap<>();
        this.deployedOrganizationsForTest = new HashMap<>();
        this.deployedSharedPolicyGroupsForTestClass = new HashMap<>();
        this.deployedSharedPolicyGroupsForTest = new HashMap<>();

        // Allow test instance to access api deployed at class level
        testInstance.setDeployedClassApis(this.deployedForTestClass);

        this.apiDeploymentPreparers =
            Map.of(
                DefinitionVersion.V1,
                new LegacyApiDeploymentPreparer(),
                DefinitionVersion.V2,
                new LegacyApiDeploymentPreparer(),
                DefinitionVersion.V4,
                new V4ApiDeploymentPreparer()
            );
    }

    /**
     * Configures the testing gateway:
     *  - uses the right `gravite.home` and `gravitee.conf`
     *  - overrides variables thanks to {@link AbstractGatewayTest#configureGateway(GatewayConfigurationBuilder)}
     *  - registers the reporters, connectors, policies and resources
     *  And then, starts the gateway
     * @param gatewayPort is the port used to reach the apis deployed on the gateway
     * @param technicalApiPort is the port used to reach the technical api.
     * @throws Exception
     */
    public void configureAndStart(int gatewayPort, int technicalApiPort) throws IOException, InterruptedException, SecretProviderException {
        try {
            // gather extra config from the test class
            GatewayConfiguration gatewayConfiguration = gatewayConfigurationBuilder.build();

            // prepare Gateway
            configure(gatewayPort, technicalApiPort, gatewayConfiguration);

            // create Gateway
            gatewayContainer =
                new GatewayTestContainer(context -> {
                    // add extra configuration to gravitee.yaml
                    applyOnGraviteeYaml(context, gatewayConfiguration.yamlProperties());

                    // secret provider plugins need to be set up during boot to ensure a proper resolution of secrets during startup.
                    registerSecretProvider(context);
                });

            // initialize the container (e.g.: prepare the spring context)
            gatewayContainer.initialize();

            // inject requirements to tests
            testInstance.setApplicationContext(gatewayContainer.applicationContext());
            testInstance.setDeployCallback(this::deployFromTest);
            testInstance.setUndeployCallback(this::undeployFromTest);
            testInstance.setDeploySharedPolicyGroupCallback(this::deploySharedPolicyGroupFromTest);
            testInstance.setUndeploySharedPolicyGroupCallback(this::undeploySharedPolicyGroupFromTest);

            // register plugins
            registerReactors(gatewayContainer);

            registerReporters(gatewayContainer);

            registerEntrypoints(gatewayContainer);

            registerConnectors(gatewayContainer);

            registerEndpoints(gatewayContainer);

            registerPolicies(gatewayContainer);

            registerResources(gatewayContainer);

            // start Gateway
            vertxContainer = startServer(gatewayContainer);
            isRunning = true;

            testInstance.init();
        } finally {
            removeTemporaryFolderIfNeeded();
        }
    }

    private void configure(int gatewayPort, int technicalApiPort, GatewayConfiguration gatewayConfiguration) throws IOException {
        String graviteeHome;
        final String homeFolder = testInstance.getClass().getAnnotation(GatewayTest.class).configFolder();

        // Try to get graviteeHome from jar. Useful for policies having a maven dependency to this sdk.
        graviteeHome = loadConfigurationFromJar(homeFolder);

        // If no graviteeHome, get the resource in the module.
        if (graviteeHome == null) {
            final URL home = GatewayRunner.class.getResource(homeFolder);
            if (home == null) {
                throw new IllegalStateException("Configuration folder for gateway does not exists");
            }
            graviteeHome = URLDecoder.decode(home.getPath(), StandardCharsets.UTF_8.name());
        }

        System.setProperty("gravitee.home", graviteeHome);
        System.setProperty("gravitee.conf", graviteeHome + File.separator + "config" + File.separator + "gravitee.yml");

        registerCustomProtocolHandlers(Handler.class);

        if (v2ExecutionMode == ExecutionMode.V3) {
            System.setProperty("api.v2.emulateV4Engine.default", "no");
        } else {
            System.setProperty("api.v2.emulateV4Engine.default", "yes");
        }
        gatewayConfiguration
            .systemProperties()
            .forEach((k, v) -> {
                configuredSystemProperties.put(k, v);
                System.setProperty(String.valueOf(k), String.valueOf(v));
            });

        System.setProperty("http.port", String.valueOf(gatewayPort));
        System.setProperty("services.core.http.port", String.valueOf(technicalApiPort));
        System.setProperty("services.core.http.enabled", String.valueOf(false));
    }

    private void applyOnGraviteeYaml(ApplicationContext context, Properties extraProperties) {
        ConfigurableEnvironment configurableEnvironment = (ConfigurableEnvironment) context.getBean(Environment.class);
        GraviteeYamlPropertySource graviteeProperties = (GraviteeYamlPropertySource) configurableEnvironment
            .getPropertySources()
            .get("graviteeYamlConfiguration");
        if (graviteeProperties != null) {
            extraProperties.forEach((k, v) -> graviteeProperties.getSource().put(String.valueOf(k), v));
        }
    }

    /**
     * Allow users of the SDK to use it without creating a dedicated /resources/gravitee-default folder containing the configuration.
     *
     * The Gateway is loading configuration for "graviteeProperties" with a FileSystemResource. If the resource is inside a jar, it throws an error.
     *
     * This method create a temporary folder to copy sdk default configuration in.
     * This temporary folder will be removed when gateway stops or an exception occurs.
     *
     * @param homeFolder is the home folder configured by {@link GatewayTest#configFolder()}. We load from jar only if value is equal to {@link GatewayRunner#DEFAULT_CONFIGURATION_FOLDER}
     * @return
     * @throws IOException
     */
    private String loadConfigurationFromJar(String homeFolder) throws IOException {
        if (DEFAULT_CONFIGURATION_FOLDER.equals(homeFolder)) {
            final URL configFolderURL = getClass().getResource(DEFAULT_CONFIGURATION_FOLDER);
            assert configFolderURL != null;
            final URLConnection urlConnection = configFolderURL.openConnection();
            if (urlConnection instanceof JarURLConnection jarUrlConnection && Files.notExists(Path.of(DEFAULT_CONFIGURATION_FOLDER))) {
                final Path targetDir = Files.createTempDirectory(String.format("%s-config", testInstance.getClass().getSimpleName()));
                tempDir = targetDir;

                copyJarResourcesRecursively(targetDir, jarUrlConnection);
                return targetDir + DEFAULT_CONFIGURATION_FOLDER;
            }
        }
        return null;
    }

    /**
     * Stops the gateway and remove temporary configuration folder if needed.
     * @throws InterruptedException
     */
    public void stop() throws InterruptedException {
        // unset system properties set by user to avoid conflict in tests
        configuredSystemProperties.forEach((k, v) -> System.clearProperty(k.toString()));
        stopServer(gatewayContainer, vertxContainer);
        testInstance.cleanUp();
    }

    /**
     * Deploys an Organization with its apis  declared at class level
     *
     * @param organizationDefinitionPath is the definition file of the organization to deploy
     * @param apisDefPath array of api definition path to deploy
     * @throws Exception
     */
    public void deployOrganizationForClass(String organizationDefinitionPath, final String[] apisDefPath) throws IOException {
        final ReactableOrganization reactableOrganization = loadOrganizationDefinition(organizationDefinitionPath);
        if (deployedOrganizationsForTestClass.containsKey(reactableOrganization.getId())) {
            throw new PreconditionViolationException(String.format(ORGANIZATION_ALREADY_DEPLOYED_MESSAGE, reactableOrganization.getId()));
        }
        deployOrganization(reactableOrganization, deployedOrganizationsForTestClass);
        for (String apiDef : apisDefPath) {
            deployForClass(apiDef, reactableOrganization.getId());
        }
    }

    /**
     * Deploys an Organization, declared at method level, thanks to {@link DeployOrganization}
     * @param organizationDefinitionPath is the definition of the organization to deploy
     * @throws Exception
     */
    public void deployOrganizationForTest(String organizationDefinitionPath, final String[] apisDefinitionPath) throws IOException {
        final ReactableOrganization reactableOrganization = loadOrganizationDefinition(organizationDefinitionPath);
        if (
            deployedOrganizationsForTestClass.containsKey(reactableOrganization.getId()) ||
            deployedOrganizationsForTest.containsKey(reactableOrganization.getId())
        ) {
            throw new PreconditionViolationException(String.format(ORGANIZATION_ALREADY_DEPLOYED_MESSAGE, reactableOrganization.getId()));
        }
        deployOrganization(reactableOrganization, deployedOrganizationsForTest);
        for (String apiDef : apisDefinitionPath) {
            deployForTest(apiDef, reactableOrganization.getId());
        }
    }

    /**
     * Deploy an organization and add it to the map belonging to its context (Class or Method).
     * Before deploying the organization, we can enrich configuration if user has overridden {@link AbstractGatewayTest#configureApi(Api)}
     * Then, we ensure the api met the minimal requirement before been deployed.
     * @param reactableOrganization the ReactableOrganization to deploy
     * @param deployedOrganizations the map containing deployed organizations.
     */
    private void deployOrganization(ReactableOrganization reactableOrganization, Map<String, ReactableOrganization> deployedOrganizations) {
        OrganizationManager organizationManager = gatewayContainer.applicationContext().getBean(OrganizationManager.class);

        testInstance.ensureMinimalRequirementForOrganization(reactableOrganization);
        reactableOrganization.setDeployedAt(new Date());

        try {
            organizationManager.register(reactableOrganization);
        } catch (Exception e) {
            throw new IllegalStateException("An error occurred deploying the organization %s".formatted(reactableOrganization.getId()), e);
        }
        deployedOrganizations.put(reactableOrganization.getId(), reactableOrganization);
    }

    public void deploySharedPolicyGroupForClass(String sharedPolicyGroupDefinitionPath) throws IOException {
        final ReactableSharedPolicyGroup reactableSharedPolicyGroup = loadSharedPolicyGroupDefinition(sharedPolicyGroupDefinitionPath);
        if (
            deployedSharedPolicyGroupsForTestClass.containsKey(
                new SharedPolicyGroupKey(reactableSharedPolicyGroup.getId(), reactableSharedPolicyGroup.getEnvironmentId())
            )
        ) {
            throw new PreconditionViolationException(
                String.format(ORGANIZATION_ALREADY_DEPLOYED_MESSAGE, reactableSharedPolicyGroup.getId())
            );
        }
        deploySharedPolicyGroup(reactableSharedPolicyGroup, deployedSharedPolicyGroupsForTestClass);
    }

    public void deploySharedPolicyGroupForTest(String sharedPolicyGroupDefinitionPath) throws IOException {
        final ReactableSharedPolicyGroup reactableSharedPolicyGroup = loadSharedPolicyGroupDefinition(sharedPolicyGroupDefinitionPath);
        if (
            deployedSharedPolicyGroupsForTestClass.containsKey(
                new SharedPolicyGroupKey(reactableSharedPolicyGroup.getId(), reactableSharedPolicyGroup.getEnvironmentId())
            ) ||
            deployedSharedPolicyGroupsForTest.containsKey(
                new SharedPolicyGroupKey(reactableSharedPolicyGroup.getId(), reactableSharedPolicyGroup.getEnvironmentId())
            )
        ) {
            throw new PreconditionViolationException(
                String.format(SHARED_POLICY_GROUP_ALREADY_DEPLOYED_MESSAGE, reactableSharedPolicyGroup.getId())
            );
        }
        deploySharedPolicyGroup(reactableSharedPolicyGroup, deployedSharedPolicyGroupsForTest);
    }

    /**
     * Deploy a Shared Policy Group and add it to the map belonging to its context (Class or Method).
     * @param reactableSharedPolicyGroup the Shared Policy Group to deploy
     * @param deployedSharedPolicyGroups the map containing deployed shared policy groups.
     */
    private void deploySharedPolicyGroup(
        ReactableSharedPolicyGroup reactableSharedPolicyGroup,
        Map<SharedPolicyGroupKey, ReactableSharedPolicyGroup> deployedSharedPolicyGroups
    ) {
        SharedPolicyGroupManager sharedPolicyGroupManager = gatewayContainer.applicationContext().getBean(SharedPolicyGroupManager.class);

        testInstance.ensureMinimalRequirementForOrganization(reactableSharedPolicyGroup);
        reactableSharedPolicyGroup.setDeployedAt(new Date());

        try {
            sharedPolicyGroupManager.register(reactableSharedPolicyGroup);
        } catch (Exception e) {
            throw new IllegalStateException(
                "An error occurred deploying the shared policy group %s".formatted(reactableSharedPolicyGroup.getId()),
                e
            );
        }
        deployedSharedPolicyGroups.put(
            new SharedPolicyGroupKey(reactableSharedPolicyGroup.getId(), reactableSharedPolicyGroup.getEnvironmentId()),
            reactableSharedPolicyGroup
        );
    }

    public void deployForClass(String apiDefinitionPath) throws IOException {
        deployForClass(apiDefinitionPath, null);
    }

    /**
     * Deploys an API, declared at class level, thanks to it definition
     * @param apiDefinitionPath is the definition file of the api to deploy
     * @throws Exception
     */
    public void deployForClass(String apiDefinitionPath, final String organizationId) throws IOException {
        final ReactableApi<?> reactableApi = toReactableApi(apiDefinitionPath);
        if (deployedForTestClass.containsKey(reactableApi.getId())) {
            throw new PreconditionViolationException(String.format(API_ALREADY_DEPLOYED_MESSAGE, reactableApi.getId()));
        }
        deploy(reactableApi, deployedForTestClass, organizationId);
    }

    /**
     * Deploys an API, declared at method level, thanks to it definition
     * @param apiDefinitionPath is the definition of the api to deploy
     * @throws Exception
     */
    public void deployForTest(String apiDefinitionPath) throws IOException {
        deployForTest(apiDefinitionPath, null);
    }

    /**
     * Deploys an API, declared at method level, thanks to it definition
     * @param apiDefinitionPath is the path of the api definition to deploy
     * @param organizationId the target organization, could be <code>null</code>
     * @throws Exception
     */
    public void deployForTest(String apiDefinitionPath, final String organizationId) throws IOException {
        final ReactableApi<?> reactableApi = toReactableApi(apiDefinitionPath);
        if (deployedForTestClass.containsKey(reactableApi.getId()) || deployedForTest.containsKey(reactableApi.getId())) {
            throw new PreconditionViolationException(String.format(API_ALREADY_DEPLOYED_MESSAGE, reactableApi.getId()));
        }
        deploy(reactableApi, deployedForTest, organizationId);
    }

    /**
     * Deploys an API from a test. Throws if trying to deploy an api deployed at class level
     * @param reactableApi is the api to deploy
     * @throws Exception
     */
    private void deployFromTest(ReactableApi<?> reactableApi) {
        if (deployedForTestClass.containsKey(reactableApi.getId())) {
            throw new PreconditionViolationException(String.format(API_ALREADY_DEPLOYED_MESSAGE + " at class level", reactableApi.getId()));
        }
        if (deployedForTest.containsKey(reactableApi.getId())) {
            undeploy(reactableApi);
            deployedForTest.remove(reactableApi.getId());
        }
        deploy(reactableApi, deployedForTest, reactableApi.getOrganizationId());
    }

    /**
     * Undeploys an API from a test. Throws if the api is deployed at class level
     * @param api
     */
    private void undeployFromTest(String api) {
        if (deployedForTestClass.containsKey(api)) {
            throw new PreconditionViolationException(String.format(CANNOT_UNDEPPLOY_CLASS_API_MESSAGE, api));
        }
        if (deployedForTest.containsKey(api)) {
            ApiManager apiManager = gatewayContainer.applicationContext().getBean(ApiManager.class);
            apiManager.unregister(api);
            deployedForTest.remove(api);
        }
    }

    /**
     * Deploys a Shared Policy Group from a test. Throws if trying to deploy a shared policy group deployed at class level
     * @param reactableSharedPolicyGroup is the shared policy group to deploy
     * @throws Exception
     */
    private void deploySharedPolicyGroupFromTest(ReactableSharedPolicyGroup reactableSharedPolicyGroup) {
        if (deployedSharedPolicyGroupsForTestClass.containsKey(reactableSharedPolicyGroup.getId())) {
            throw new PreconditionViolationException(
                String.format(SPG_ALREADY_DEPLOYED_MESSAGE + " at class level", reactableSharedPolicyGroup.getId())
            );
        }
        if (deployedSharedPolicyGroupsForTest.containsKey(reactableSharedPolicyGroup.getId())) {
            undeploySharedPolicyGroup(reactableSharedPolicyGroup);
            deployedSharedPolicyGroupsForTest.remove(reactableSharedPolicyGroup.getId());
        }
        deploySharedPolicyGroup(reactableSharedPolicyGroup, deployedSharedPolicyGroupsForTest);
    }

    /**
     * Undeploys a Shared Policy Group from a test. Throws if the shared policy group is deployed at class level
     * @param sharedPolicyGroup
     */
    private void undeploySharedPolicyGroupFromTest(String sharedPolicyGroup, String environmentId) {
        if (deployedSharedPolicyGroupsForTestClass.containsKey(new SharedPolicyGroupKey(sharedPolicyGroup, environmentId))) {
            throw new PreconditionViolationException(String.format(CANNOT_UNDEPPLOY_CLASS_SPG_MESSAGE, sharedPolicyGroup));
        }
        if (deployedSharedPolicyGroupsForTest.containsKey(new SharedPolicyGroupKey(sharedPolicyGroup, environmentId))) {
            SharedPolicyGroupManager sharedPolicyGroupManager = gatewayContainer
                .applicationContext()
                .getBean(SharedPolicyGroupManager.class);
            sharedPolicyGroupManager.unregister(sharedPolicyGroup);
            deployedSharedPolicyGroupsForTest.remove(new SharedPolicyGroupKey(sharedPolicyGroup, environmentId));
        }
    }

    /**
     * Access the list of deployed API for the current test.
     * It allows to update an API directly for testing purpose, for example, the {@link io.gravitee.definition.model.Endpoint.Status}
     * @return the list of deployed API for the current test.
     */
    public Map<String, ReactableApi<?>> deployedApis() {
        return deployedForTest;
    }

    private ReactableApi<?> toReactableApi(String apiDefinitionPath) throws IOException {
        final JsonNode apiAsJson = loadResource(apiDefinitionPath, JsonNode.class);
        final DefinitionVersion definitionVersion = extractApiDefinitionVersion(apiAsJson);
        // ⚠️ Workaround to be able to configure an environmentId for a given test. EnvironmentId field is normally not part of the Api Definition
        final String environmentId = extractApiEnvironmentId(apiAsJson).orElse("DEFAULT");
        final ReactableApi<?> reactableApi;
        if (DefinitionVersion.V4.equals(definitionVersion)) {
            final io.gravitee.definition.model.v4.Api api = graviteeMapper.treeToValue(
                apiAsJson,
                io.gravitee.definition.model.v4.Api.class
            );
            reactableApi = apiDeploymentPreparers.get(definitionVersion).toReactable(api, environmentId);
        } else {
            final Api api = graviteeMapper.treeToValue(apiAsJson, Api.class);
            reactableApi = apiDeploymentPreparers.get(definitionVersion).toReactable(api, environmentId);
        }
        return reactableApi;
    }

    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Deploy an api and add it to the map belonging to its context (Class or Method).
     * Before deploying the api, we can enrich configuration if user has overridden {@link AbstractGatewayTest#configureApi(Api)}
     * Then, we ensure the api met the minimal requirement before been deployed.
     * @param reactableApi the ReactableApi to deploy
     * @param deployedApis the map containing deployed apis.
     */
    private void deploy(ReactableApi<?> reactableApi, Map<String, ReactableApi<?>> deployedApis, final String organizationId) {
        ApiManager apiManager = gatewayContainer.applicationContext().getBean(ApiManager.class);

        if (!DefinitionVersion.V4.equals(reactableApi.getDefinitionVersion())) {
            final Api api = (Api) reactableApi.getDefinition();
            api.setExecutionMode(v2ExecutionMode);
            testInstance.configureApi(api);
        }

        testInstance.configureApi(reactableApi, reactableApi.getDefinition().getClass());
        reactableApi.setOrganizationId(organizationId);

        ensureMinimalRequirementForApi(reactableApi);

        try {
            reactableApi.setDeployedAt(new Date());
            apiManager.register(reactableApi);
        } catch (Exception e) {
            throw new IllegalStateException("An error occurred deploying the api %s".formatted(reactableApi.getId()), e);
        }
        deployedApis.put(reactableApi.getId(), reactableApi);
    }

    /**
     * Undeploys organizations declared at class level.
     */
    public void undeployOrganizationForClass() {
        deployedOrganizationsForTestClass.forEach((key, value) -> undeployOrganization(value));
        deployedForTestClass.clear();
    }

    /**
     * Undeploys organizations declared at method level.
     */
    public void undeployOrganizationForTest() {
        deployedOrganizationsForTest.forEach((key, value) -> undeployOrganization(value));
        deployedOrganizationsForTest.clear();
    }

    public void undeployOrganization(ReactableOrganization reactableOrganization) {
        OrganizationManager organizationManager = gatewayContainer.applicationContext().getBean(OrganizationManager.class);
        organizationManager.unregister(reactableOrganization.getId());
    }

    public void undeploySharedPolicyGroupForClass() {
        deployedSharedPolicyGroupsForTestClass.forEach((key, value) -> undeploySharedPolicyGroup(value));
    }

    public void undeploySharedPolicyGroupForTest() {
        deployedSharedPolicyGroupsForTest.forEach((key, value) -> undeploySharedPolicyGroup(value));
        deployedSharedPolicyGroupsForTest.clear();
    }

    public void undeploySharedPolicyGroup(ReactableSharedPolicyGroup reactableSharedPolicyGroup) {
        SharedPolicyGroupManager sharedPolicyGroupManager = gatewayContainer.applicationContext().getBean(SharedPolicyGroupManager.class);
        sharedPolicyGroupManager.unregister(reactableSharedPolicyGroup.getId());
    }

    /**
     * Undeploys APIs declared at class level.
     */
    public void undeployForClass() {
        deployedForTestClass.forEach((key, value) -> undeploy(value));
        deployedForTestClass.clear();
    }

    /**
     * Undeploys APIs declared at method level.
     */
    public void undeployForTest() {
        deployedForTest.forEach((key, value) -> undeploy(value));
        deployedForTest.clear();
    }

    private void undeploy(ReactableApi<?> api) {
        ApiManager apiManager = gatewayContainer.applicationContext().getBean(ApiManager.class);
        apiManager.unregister(api.getId());
    }

    private VertxEmbeddedContainer startServer(GatewayTestContainer container) throws InterruptedException {
        final Thread starterThread = new Thread(() -> {
            try {
                container.start();
            } catch (Exception e) {
                System.exit(-1);
            }
        });

        starterThread.start();

        final VertxEmbeddedContainer vertxEmbeddedContainer = container.applicationContext().getBean(VertxEmbeddedContainer.class);

        // First, wait for the vertxEmbeddedContainer to be started
        while (vertxEmbeddedContainer.lifecycleState() != Lifecycle.State.STARTED) {
            Thread.sleep(5);
        }
        // Then, wait for the GatewayTestContainer to be started
        while (container.lifecycleState() != Lifecycle.State.STARTED) {
            Thread.sleep(5);
        }
        return vertxEmbeddedContainer;
    }

    private void stopServer(GatewayTestContainer container, VertxEmbeddedContainer vertxContainer) throws InterruptedException {
        if (container != null) {
            final Thread stopThread = new Thread(() -> {
                try {
                    container.stop();
                } catch (Exception e) {
                    System.exit(-1);
                }
            });

            stopThread.start();

            while (vertxContainer.lifecycleState() != Lifecycle.State.STOPPED) {
                Thread.sleep(5);
            }
        }
    }

    private void ensureMinimalRequirementForApi(ReactableApi<?> reactableApi) {
        apiDeploymentPreparers.get(reactableApi.getDefinitionVersion()).ensureMinimalRequirementForApi(reactableApi.getDefinition());
        if (reactableApi.getOrganizationId() == null) {
            reactableApi.setOrganizationId("DEFAULT");
        }
    }

    @SneakyThrows
    private void registerSecretProvider(ApplicationContext applicationContext) {
        SecretProviderPluginManager pluginManager = applicationContext.getBean(SecretProviderPluginManager.class);
        Set<SecretProviderPlugin<? extends SecretProviderFactory<?>, ? extends SecretManagerConfiguration>> secretProviderFactories =
            new HashSet<>();
        try {
            testInstance.configureSecretProviders(secretProviderFactories);
        } catch (Exception e) {
            throw new SecretProviderException(e);
        }
        secretProviderFactories.forEach(pluginManager::register);
    }

    private void registerReactors(GatewayTestContainer container) {
        final ReactorPluginManager reactorPluginManager = container.applicationContext().getBean(ReactorPluginManager.class);
        Set<ReactorPlugin<? extends ReactorFactory<?>>> reactorFactoriesMap = new HashSet<>();
        testInstance.configureReactors(reactorFactoriesMap);
        reactorFactoriesMap.forEach(reactorPluginManager::register);
    }

    private void registerReporters(GatewayTestContainer container) {
        ReporterManager reporterManager = container.applicationContext().getBean(ReporterManager.class);

        Map<String, Reporter> reportersMap = new HashMap<>();
        testInstance.configureReporters(reportersMap);
        ensureMinimalRequirementForReporters(container, reportersMap);
        reportersMap.forEach((key, value) -> reporterManager.register(value));
    }

    private void ensureMinimalRequirementForReporters(GatewayTestContainer container, Map<String, Reporter> reporters) {
        reporters.putIfAbsent("fakeReporter", (FakeReporter) container.applicationContext().getBean("fakeReporter"));
    }

    private void registerResources(GatewayTestContainer container) {
        String[] resourceBeanNamesForType = container
            .applicationContext()
            .getBeanNamesForType(ResolvableType.forClassWithGenerics(ConfigurablePluginManager.class, ResourcePlugin.class));

        final ConfigurablePluginManager<ResourcePlugin> resourceManager = (ConfigurablePluginManager<ResourcePlugin>) container
            .applicationContext()
            .getBean(resourceBeanNamesForType[0]);

        Map<String, ResourcePlugin> resourcesMap = new HashMap<>();
        testInstance.configureResources(resourcesMap);
        resourcesMap.forEach((key, value) -> resourceManager.register(value));
    }

    private void registerPolicies(GatewayTestContainer container) {
        String[] policyBeanNamesForType = container
            .applicationContext()
            .getBeanNamesForType(ResolvableType.forClassWithGenerics(ConfigurablePluginManager.class, PolicyPlugin.class));
        final ConfigurablePluginManager<PolicyPlugin> policyManager = (ConfigurablePluginManager<PolicyPlugin>) container
            .applicationContext()
            .getBean(policyBeanNamesForType[0]);

        Map<String, PolicyPlugin> policyMap = new HashMap<>();
        loadPolicyAsAPlugin(container.applicationContext(), policyMap);

        testInstance.configurePolicies(policyMap);
        ensureMinimalRequirementForPolicies(policyMap);
        policyMap.forEach((key, value) -> policyManager.register(value));
    }

    /**
     * This method will load the PluginManifest generated from "plugin.properties" file, load the plugin needed by the user and then fake the {@link PluginEvent#DEPLOYED} and {@link PluginEvent#ENDED} to apply the initialization phase of a policy (see {@link io.gravitee.policy.api.PolicyContext})
     * @param applicationContext is the application context of the container
     * @param policies is the Map of policies to initialize
     */
    private void loadPolicyAsAPlugin(ApplicationContext applicationContext, Map<String, PolicyPlugin> policies) {
        final PluginManifest manifest = PluginManifestLoader.readManifest();
        testInstance.loadPolicy(manifest, policies);
        if (manifest != null && !policies.isEmpty()) {
            final PluginEventListener pluginEventListener = applicationContext.getBean(PluginEventListener.class);
            final PluginImpl plugin = (PluginImpl) PluginFactory.from(manifest);
            plugin.setDependencies(new URL[0]);
            pluginEventListener.onEvent(new SimpleEvent<>(PluginEvent.DEPLOYED, plugin));
            pluginEventListener.onEvent(new SimpleEvent<>(PluginEvent.ENDED, null));
        }
    }

    private void ensureMinimalRequirementForPolicies(Map<String, PolicyPlugin> policies) {
        policies.putIfAbsent("key-less", PolicyBuilder.build("key-less", KeylessPolicy.class));
    }

    private void registerConnectors(GatewayTestContainer container) {
        ConnectorPluginManager connectorPluginManager = container.applicationContext().getBean(ConnectorPluginManager.class);

        Map<String, ConnectorPlugin> connectorsMap = new HashMap<>();
        testInstance.configureConnectors(connectorsMap);
        ensureMinimalRequirementForConnectors(connectorsMap);
        connectorsMap.forEach((key, value) -> connectorPluginManager.register(value));
    }

    private void ensureMinimalRequirementForConnectors(Map<String, ConnectorPlugin> connectors) {
        connectors.putIfAbsent("connector-http", ConnectorBuilder.build("connector-http", HttpConnectorFactory.class));
    }

    private void registerEntrypoints(GatewayTestContainer container) {
        final EntrypointConnectorPluginManager entrypointPluginManager = container
            .applicationContext()
            .getBean(EntrypointConnectorPluginManager.class);

        Map<String, EntrypointConnectorPlugin<?, ?>> entrypointsMap = new HashMap<>();
        testInstance.configureEntrypoints(entrypointsMap);
        entrypointsMap.forEach((key, value) -> entrypointPluginManager.register(value));
    }

    private void registerEndpoints(GatewayTestContainer container) {
        final EndpointConnectorPluginManager endpointPluginManager = container
            .applicationContext()
            .getBean(EndpointConnectorPluginManager.class);

        Map<String, EndpointConnectorPlugin<?, ?>> endpointsMap = new HashMap<>();
        testInstance.configureEndpoints(endpointsMap);
        ensureMinimalRequirementForEndpoints(endpointsMap);
        endpointsMap.forEach((key, value) -> endpointPluginManager.register(value));
    }

    private void ensureMinimalRequirementForEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
        endpoints.putIfAbsent("mock", EndpointBuilder.build("mock", MockEndpointConnectorFactory.class));
    }

    private ReactableOrganization loadOrganizationDefinition(String orgDefinitionPath) throws IOException {
        final io.gravitee.definition.model.Organization organization = loadResource(
            orgDefinitionPath,
            io.gravitee.definition.model.Organization.class
        );
        return new ReactableOrganization(organization);
    }

    private ReactableSharedPolicyGroup loadSharedPolicyGroupDefinition(String sharedPolicyGroupDefinitionPath) throws IOException {
        final SharedPolicyGroup sharedPolicyGroup = loadResource(sharedPolicyGroupDefinitionPath, SharedPolicyGroup.class);
        return new ReactableSharedPolicyGroup(sharedPolicyGroup);
    }

    private <T> T loadResource(String resourcePath, Class<T> toClass) throws IOException {
        try {
            final URL jsonFile = loadURL(resourcePath);
            String definition = Files.readString(Paths.get(jsonFile.toURI()));

            final AbstractGatewayTest.PlaceholderSymbols placeHolderSymbols = testInstance.configurePlaceHolder();

            final HashMap<String, String> variables = new HashMap<>();
            testInstance.configurePlaceHolderVariables(variables);

            for (Map.Entry<String, String> entry : variables.entrySet()) {
                definition =
                    definition.replaceAll(
                        Pattern.quote(placeHolderSymbols.prefix() + entry.getKey() + placeHolderSymbols.suffix()),
                        entry.getValue()
                    );
            }

            definition =
                definition
                    .replaceAll("http://localhost:8080", "http://localhost:" + testInstance.getWiremockPort())
                    .replaceAll("https://localhost:8080", "https://localhost:" + testInstance.getWiremockHttpsPort());

            return graviteeMapper.readValue(definition, toClass);
        } catch (URISyntaxException e) {
            throw new IOException("Invalid resourcePath [" + resourcePath + "].", e);
        }
    }

    private URL loadURL(String resourcePath) {
        return GatewayRunner.class.getResource(resourcePath);
    }

    /**
     * Utility method to copy a folder from a jar.
     * @param destination is the destination path.
     * @param jarConnection is the connection get from source URL.
     * @throws IOException
     */
    private void copyJarResourcesRecursively(Path destination, JarURLConnection jarConnection) throws IOException {
        JarFile jarFile = jarConnection.getJarFile();
        for (Iterator<JarEntry> it = jarFile.entries().asIterator(); it.hasNext();) {
            JarEntry entry = it.next();
            if (entry.getName().startsWith(jarConnection.getEntryName())) {
                if (entry.getName().contains("./") || entry.getName().contains("../")) {
                    throw new SecurityException("JarEntry trying to access FileSystem with relative path: " + entry.getName());
                }
                if (!entry.isDirectory()) {
                    try (InputStream entryInputStream = jarFile.getInputStream(entry)) {
                        Files.copy(entryInputStream, Paths.get(destination.toString(), entry.getName()));
                    }
                } else {
                    Files.createDirectories(Paths.get(destination.toString(), entry.getName()));
                }
            }
        }
    }

    /**
     * Remove the temporary folder from variable "tempDir". This folder has been created to copy the resources from jar.
     * @throws IOException
     */
    private void removeTemporaryFolderIfNeeded() throws IOException {
        if (tempDir != null) {
            LOGGER.debug("Removing temp folder: {}", tempDir);
            Files.walkFileTree(
                tempDir,
                new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        if (exc != null) {
                            throw exc;
                        }
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                }
            );
        }
    }

    /**
     * Read the definition JsonNode and try to find the definition version.
     * First, check if `definitionVersion` field is present and use its value
     * Then, check if `gravitee` field is present and use its value
     * Default to V2.
     * @param apiAsJson the api definition as {@link JsonNode}
     * @return the definitionVersion found in definition
     */
    private DefinitionVersion extractApiDefinitionVersion(JsonNode apiAsJson) {
        if (apiAsJson.has("definitionVersion")) {
            return DefinitionVersion.valueOfLabel(apiAsJson.get("definitionVersion").asText());
        } else if (apiAsJson.has("gravitee")) {
            return DefinitionVersion.valueOfLabel(apiAsJson.get("gravitee").asText());
        } else {
            return DefinitionVersion.V2;
        }
    }

    /**
     * Read the environment id from json.
     * ⚠️ this attribute is not an official one from the definition. It is a workaround for the Test SDK as we do not go through Api Synchronizer process (and mapping which is filling environment information)
     * @param apiAsJson
     * @return the environmentId wrapped in an optional
     */
    private Optional<String> extractApiEnvironmentId(JsonNode apiAsJson) {
        if (apiAsJson.has("environmentId")) {
            return Optional.ofNullable(apiAsJson.get("environmentId").asText());
        }
        return Optional.empty();
    }

    /**
     * {@link URL} class looks in system property "java.protocol.handler.pkgs" for {@link URLStreamHandler} to register to be able to support other protocols.
     * New {@link URLStreamHandler} have to be named <code>Handler</code> in a package of the name of the protocol.
     * The separator of the system property to register multiple handlers is "|"
     * @param handlerClass to register, must extends {@link URLStreamHandler}
     */
    @SuppressWarnings("java:S1872")
    private static void registerCustomProtocolHandlers(Class<? extends URLStreamHandler> handlerClass) {
        String originalProperty = System.getProperty("java.protocol.handler.pkgs", "");
        String pkg = handlerClass.getPackage().getName();
        int lastDot = pkg.lastIndexOf('.');
        assert lastDot != -1 : "You can't add url handlers in the base package";
        // Ignore java:S1872 which explains that class names are not unique, they are only when they are within a package.
        // Here, we just enforce the class name is "Handler" because system property "java.protocol.handler.pkgs" will only look for class of this name and inherting from URLStreamHandler
        assert handlerClass.getSimpleName().equals("Handler") : "A URLStreamHandler must be in a class named 'Handler'; not " +
        handlerClass.getSimpleName();

        final String protocolHandlersPackage =
            handlerClass.getPackage().getName().substring(0, lastDot) + (originalProperty.isEmpty() ? "" : "|" + originalProperty);
        // As we will put our custom protocols in the same package, add it only once.
        if (!originalProperty.contains(protocolHandlersPackage)) {
            System.setProperty("java.protocol.handler.pkgs", protocolHandlersPackage);
        }
    }
}
