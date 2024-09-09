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

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.gravitee.apim.gateway.tests.sdk.utils.URLUtils.exchangePort;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayConfigurationBuilder;
import io.gravitee.apim.gateway.tests.sdk.plugin.PluginRegister;
import io.gravitee.apim.gateway.tests.sdk.runner.ApiConfigurer;
import io.gravitee.apim.gateway.tests.sdk.runner.ApiDeployer;
import io.gravitee.apim.gateway.tests.sdk.runner.OrganizationConfigurer;
import io.gravitee.apim.gateway.tests.sdk.runner.SharedPolicyGroupDeployer;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.Endpoint;
import io.gravitee.gateway.handlers.sharedpolicygroup.ReactableSharedPolicyGroup;
import io.gravitee.gateway.platform.organization.ReactableOrganization;
import io.gravitee.gateway.platform.organization.manager.OrganizationManager;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.node.container.spring.env.GraviteeYamlPropertySource;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Slf4JLoggerFactory;
import io.reactivex.rxjava3.observers.TestObserver;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.rxjava3.core.Vertx;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.commons.PreconditionViolationException;
import org.mockito.Mockito;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.test.util.TestSocketUtils;
import org.springframework.util.StringUtils;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@ExtendWith(VertxExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public abstract class AbstractGatewayTest
    implements PluginRegister, ApiConfigurer, ApiDeployer, OrganizationConfigurer, ApplicationContextAware, SharedPolicyGroupDeployer {

    private static final ObjectMapper objectMapper = new GraviteeMapper();
    protected static final PlaceholderSymbols DEFAULT_PLACEHOLDER_SYMBOLS = new PlaceholderSymbols("${", "}");

    @Getter
    private int wiremockHttpsPort;

    @Getter
    private int wiremockPort;

    /**
     * The wiremock used by the deployed apis as a backend.
     */
    protected static WireMockServer wiremock;

    protected ApplicationContext applicationContext;

    private int gatewayPort = -1;
    private int technicalApiPort = -1;
    private int tcpPort = -1;

    /**
     * Map of deployed apis for the current test method thanks to {@link io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi}
     */
    protected Map<String, ReactableApi<?>> deployedApis;
    private Map<String, ReactableApi<?>> deployedForTestClass;

    private Consumer<ReactableApi<?>> apiDeployer;
    private Consumer<String> apiUndeployer;
    private Consumer<ReactableSharedPolicyGroup> sharedPolicyGroupDeployer;
    private BiConsumer<String, String> sharedPolicyGroupUndeployer;
    protected Vertx vertx;

    /**
     * Represent the symbol used for placeholder.
     * Default is <code>${}</code>, meaning the prefix is <code>${</code> and the suffix is <code>}</code>.
     * This allows for adding some particular variables such as <code>${MY_VARIABLE}</code> into the JSON API definition
     * that can be dynamically replaced before the deployment.
     *
     * @param prefix the prefix for the placeholder.
     * @param suffix the suffix for the placeholder.
     */
    public record PlaceholderSymbols(String prefix, String suffix) {}

    /**
     * Return the placeholder prefix <code>${</code> and suffix <code>}</code> which allows for defining variables such as <code>${MY_VARIABLE}</code>.
     * Override this method to define a custom one.
     *
     * @return the placeholder prefix and suffix to use for variables.
     */
    public PlaceholderSymbols configurePlaceHolder() {
        return DEFAULT_PLACEHOLDER_SYMBOLS;
    }

    /**
     * Allows providing variables that will be used to resolve the placeholders set in the API definition.
     *
     * @param variables the key/value map that can be enriched with variables.
     */
    public void configurePlaceHolderVariables(Map<String, String> variables) {}

    /**
     * Override this method to modify the configuration of the wiremock server which acts as a backend for the deployed apis.
     * @param configuration the {@link WireMockConfiguration} to modify accordingly to your need.
     */
    protected void configureWireMock(WireMockConfiguration configuration) {}

    /**
     * Override this method if you want to pass some specific configuration to the gateway.
     * WARNING: since the gateway is run only once for the test class, this configuration will impact all your test cases.
     * To know the options, please check the documentation <a href="https://documentation.gravitee.io/apim/getting-started/install-guides">here</a>.
     * @param gatewayConfigurationBuilder is the configuration builder. Just use it with {@link GatewayConfigurationBuilder#set(String, Object)}.
     *                                    For example, to configure tags for the gateway, just use <code>gatewayConfigurationBuilder.set("tags", "my-tag")</code>
     */
    protected void configureGateway(GatewayConfigurationBuilder gatewayConfigurationBuilder) {}

    /**
     * Override this method if you want to pass some specific configuration to the HttpClient.
     * It differs from WebClient which only response with Single, which was not good for SSE api.
     */
    protected void configureHttpClient(HttpClientOptions options) {}

    /**
     * Proxy for {@link TestObserver#await(long, TimeUnit)} with a default of 30 seconds.
     * It awaits 30 seconds or until this TestObserver/TestSubscriber receives an onError or onComplete events, whichever happens first.
     * @param obs is the observer to await
     * @return the observer after wait
     */
    protected <T> TestObserver<T> awaitTerminalEvent(TestObserver<T> obs) throws InterruptedException {
        obs.await(30, TimeUnit.SECONDS);
        return obs;
    }

    /**
     * Get bean from Gateway container's  application context
     * Example: getBean(ApiKeyRepository.class);
     * @param requiredType is the type of the bean to get
     * @return the bean
     * @param <T> is the type of the bean to load.
     */
    protected <T> T getBean(Class<T> requiredType) {
        return applicationContext.getBean(requiredType);
    }

    /**
     * Get the Spring {@link io.gravitee.node.container.spring.env.GraviteeYamlPropertySource} bean.
     * @return the Spring {@link io.gravitee.node.container.spring.env.GraviteeYamlPropertySource} bean.
     */
    protected GraviteeYamlPropertySource getGraviteeYamlProperties() {
        final ConfigurableEnvironment environment = (ConfigurableEnvironment) getBean(Environment.class);
        return (GraviteeYamlPropertySource) environment.getPropertySources().get("graviteeYamlConfiguration");
    }

    protected AbstractGatewayTest() {}

    public void init() {
        InternalLoggerFactory.setDefaultFactory(Slf4JLoggerFactory.INSTANCE);
        prepareWireMock();
    }

    public void cleanUp() {
        if (wiremock != null) {
            wiremock.stop();
        }
    }

    /**
     * Here we inject Vertx parameter even if it's not use:
     * when implementing the test cases, the developer will need a {@link io.vertx.ext.web.client.WebClient}, which is automatically resolved as a parameter if Vertx has already been resolved.
     * Injecting it in the BeforeEach at abstract class level allows to automatically inject it to ease the life of the developer.
     * <p>
     * Ensure the testContext is completed before starting a test, see Vertx documentation"
     * Update endpoints for each apis deployed for the whole test class, see {@link AbstractGatewayTest#updateEndpointsOnDeployedApisForClassIfNeeded()}
     * </p>
     * @param vertx this parameter is only used to let the VertxExtension initialize it. It will allow to use WebClient directly.
     */
    @BeforeEach
    public void setUp(Vertx vertx) throws Exception {
        this.vertx = vertx;
        resetAllMocks();
        // Prepare something on a Vert.x event-loop thread
        // The thread changes with each test instance
    }

    private void resetAllMocks() throws Exception {
        if (wiremock != null) {
            wiremock.resetAll();
        }

        for (String name : applicationContext.getBeanDefinitionNames()) {
            Object bean = applicationContext.getBean(name);
            if (AopUtils.isAopProxy(bean) && bean instanceof Advised advised) {
                bean = advised.getTargetSource().getTarget();
            }
            if (Mockito.mockingDetails(bean).isMock()) {
                Mockito.reset(bean);
            }
        }
    }

    private void prepareWireMock() {
        final WireMockConfiguration wireMockConfiguration = wireMockConfig().dynamicPort().dynamicHttpsPort();
        configureWireMock(wireMockConfiguration);
        // If a port has already been configured in a previous test, reuse the same for the class
        if (wiremockPort > 0) {
            wireMockConfiguration.port(wiremockPort);
        }
        if (wiremockHttpsPort > 0) {
            wireMockConfiguration.httpsPort(wiremockHttpsPort);
        }
        wiremock = new WireMockServer(wireMockConfiguration);
        wiremock.start();
        wiremockPort = wiremock.port();
        wiremockHttpsPort = wiremock.httpsPort();
    }

    @Override
    public void configureApi(Api api) {}

    @Override
    public void configureOrganization(ReactableOrganization reactableOrganization) {}

    @Override
    public void configureApi(ReactableApi<?> api, Class<?> definitionClass) {}

    @Override
    public void setDeployCallback(Consumer<ReactableApi<?>> apiDeployer) {
        this.apiDeployer = apiDeployer;
    }

    @Override
    public void setUndeployCallback(Consumer<String> apiUndeployer) {
        this.apiUndeployer = apiUndeployer;
    }

    @Override
    public void deploy(ReactableApi<?> reactableApi) {
        apiDeployer.accept(reactableApi);
    }

    @Override
    public void undeploy(String apiId) {
        apiUndeployer.accept(apiId);
    }

    @Override
    public void redeploy(ReactableApi<?> reactableApi) {
        undeploy(reactableApi.getId());
        deploy(reactableApi);
    }

    @Override
    public void setDeploySharedPolicyGroupCallback(Consumer<ReactableSharedPolicyGroup> sharedPolicyGroupDeployer) {
        this.sharedPolicyGroupDeployer = sharedPolicyGroupDeployer;
    }

    @Override
    public void setUndeploySharedPolicyGroupCallback(BiConsumer<String, String> sharedPolicyGroupUndeployer) {
        this.sharedPolicyGroupUndeployer = sharedPolicyGroupUndeployer;
    }

    @Override
    public void deploySharedPolicyGroup(ReactableSharedPolicyGroup reactableSharedPolicyGroup) {
        sharedPolicyGroupDeployer.accept(reactableSharedPolicyGroup);
    }

    @Override
    public void undeploySharedPolicyGroup(String sharedPolicyGroupId, String environmentId) {
        sharedPolicyGroupUndeployer.accept(sharedPolicyGroupId, environmentId);
    }

    @Override
    public void redeploySharedPolicyGroup(ReactableSharedPolicyGroup reactableSharedPolicyGroup) {
        undeploySharedPolicyGroup(reactableSharedPolicyGroup.getId(), reactableSharedPolicyGroup.getEnvironmentId());
        deploySharedPolicyGroup(reactableSharedPolicyGroup);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * Ensures the organization has the minimal requirement to be run properly.
     * - add a default id ("organization-id") if not set
     * @param reactableOrganization to deploy
     */
    public void ensureMinimalRequirementForOrganization(ReactableOrganization reactableOrganization) {
        if (!StringUtils.hasText(reactableOrganization.getId())) {
            reactableOrganization.getDefinition().setId("DEFAULT");
        }
    }

    /**
     * Ensures the shared policy group has the minimal requirement to be run properly.
     * - add a default id ("environment-id") if not set
     * @param reactableSharedPolicyGroup to deploy
     */
    public void ensureMinimalRequirementForOrganization(ReactableSharedPolicyGroup reactableSharedPolicyGroup) {
        if (!StringUtils.hasText(reactableSharedPolicyGroup.getEnvironmentId())) {
            reactableSharedPolicyGroup.getDefinition().setEnvironmentId("DEFAULT");
        }
    }

    public Map<String, ReactableApi<?>> getDeployedForTestClass() {
        return Collections.unmodifiableMap(deployedForTestClass);
    }

    /**
     * Called by the {@link GatewayTestingExtension} when apis wanted for the test class are deployed.
     * @param deployedForTestClass is the list of deployed apis.
     */
    public void setDeployedClassApis(Map<String, ReactableApi<?>> deployedForTestClass) {
        this.deployedForTestClass = deployedForTestClass;
    }

    public int gatewayPort() {
        if (gatewayPort == -1) {
            gatewayPort = getAvailablePort();
        }
        return gatewayPort;
    }

    public int tcpPort() {
        if (tcpPort == -1) {
            tcpPort = getAvailablePort();
        }
        return tcpPort;
    }

    public int technicalApiPort() {
        if (technicalApiPort == -1) {
            technicalApiPort = getAvailablePort();
        }
        return technicalApiPort;
    }

    /**
     * Override api endpoints port on demand
     * @param api is the api on which apply the new port
     * @param port is the port to reach.
     */
    protected void updateEndpointsPort(Api api, int port) {
        updateEndpoints(api, endpoint -> endpoint.setTarget(exchangePort(endpoint.getTarget(), port)));
    }

    /**
     * Override api endpoints
     * @param api is the api on which the endponts will be transformed
     * @param endpointConsumer is the consumer used to transform the endpoints
     */
    protected void updateEndpoints(Api api, Consumer<Endpoint> endpointConsumer) {
        if (api.getProxy() != null && api.getProxy().getGroups() != null) {
            for (Endpoint endpoint : api.getProxy().getGroups().iterator().next().getEndpoints()) {
                endpointConsumer.accept(endpoint);
            }
        }
    }

    /**
     * Override api endpoints port on demand
     * @param api is the api on which apply the new port
     * @param port is the port to reach.
     */
    protected void updateEndpointsPort(io.gravitee.definition.model.v4.Api api, int port) {
        updateEndpoints(
            api,
            endpoint -> {
                try {
                    ObjectNode configuration = (ObjectNode) objectMapper.readTree(endpoint.getConfiguration());
                    JsonNode targetNode = configuration.get("target");
                    if (targetNode != null) {
                        if (targetNode.isTextual()) {
                            String target = targetNode.asText();
                            String exchangePort = exchangePort(target, port);
                            configuration.put("target", exchangePort);
                        } else {
                            ((ObjectNode) targetNode).put("port", port);
                        }
                    }
                    endpoint.setConfiguration(configuration);
                } catch (Exception e) {
                    log.error("Unable to parse endpoint configuration", e);
                }
            }
        );
    }

    /**
     * Override api endpoints
     * @param api is the api on which the endponts will be transformed
     * @param endpointConsumer is the consumer used to transform the endpoints
     */
    protected void updateEndpoints(
        io.gravitee.definition.model.v4.Api api,
        Consumer<io.gravitee.definition.model.v4.endpointgroup.Endpoint> endpointConsumer
    ) {
        if (api.getEndpointGroups() != null) {
            api.getEndpointGroups().stream().flatMap(endpointGroup -> endpointGroup.getEndpoints().stream()).forEach(endpointConsumer);
        }
    }

    /**
     * Update the latest deployed organization (if it exists) and redeploy it.
     * Useful to add a policy for a specific test and avoid rewriting a json file.
     * @param organizationConsumer a consumer modifying the current deployed organization.
     */
    protected final void updateOrganization(Consumer<ReactableOrganization> organizationConsumer) {
        updateOrganization("DEFAULT", organizationConsumer);
    }

    /**
     * Update a deployed organization (if it exists) and redeploy it.
     * Useful to add a policy for a specific test and avoid rewriting a json file.
     * @param organizationId the id of the organization to update
     * @param organizationConsumer a consumer modifying the current deployed organization.
     */
    protected final void updateOrganization(String organizationId, Consumer<ReactableOrganization> organizationConsumer) {
        // Get deployed organization and create a new one from it
        final OrganizationManager organizationManager = applicationContext.getBean(OrganizationManager.class);

        final ReactableOrganization reactableOrganization = organizationManager.getOrganization(organizationId);

        if (reactableOrganization == null) {
            throw new PreconditionViolationException(
                String.format("No organization '%s' deployed , you cannot use this method", organizationId)
            );
        }

        ReactableOrganization updatingReactableOrganization = new ReactableOrganization(reactableOrganization.getDefinition());

        // Apply developer transformation on this organization
        organizationConsumer.accept(updatingReactableOrganization);

        // redeploy new organization
        updatingReactableOrganization.setDeployedAt(new Date());
        organizationManager.register(updatingReactableOrganization);
    }

    protected int getAvailablePort() {
        return TestSocketUtils.findAvailableTcpPort();
    }

    /**
     * Check if API is legacy (v3 engine) according to its definition class
     * @param definitionClass The definition class to check
     * @return true if it's a legacy api
     */
    protected boolean isLegacyApi(Class<?> definitionClass) {
        return Api.class.isAssignableFrom(definitionClass);
    }

    /**
     * Check if API is V4 according to its definition class
     * @param definitionClass The definition class to check
     * @return true if it's a V4 api
     */
    protected boolean isV4Api(Class<?> definitionClass) {
        return io.gravitee.definition.model.v4.Api.class.isAssignableFrom(definitionClass);
    }
}
