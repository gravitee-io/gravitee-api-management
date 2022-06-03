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

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.gravitee.apim.gateway.tests.sdk.utils.URLUtils.exchangePort;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayConfigurationBuilder;
import io.gravitee.apim.gateway.tests.sdk.plugin.PluginRegister;
import io.gravitee.apim.gateway.tests.sdk.runner.ApiConfigurer;
import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.Endpoint;
import io.gravitee.definition.model.EndpointGroup;
import io.gravitee.definition.model.Plan;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Slf4JLoggerFactory;
import io.reactiverse.junit5.web.WebClientOptionsInject;
import io.reactivex.observers.TestObserver;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractGatewayTest implements PluginRegister, ApiConfigurer, ApplicationContextAware {

    private int wiremockHttpsPort;
    private int wiremockPort;

    /**
     * Map of deployed apis for the current test method thanks to {@link io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi}
     */
    protected Map<String, Api> deployedApis;
    private int gatewayPort = -1;
    private int technicalApiPort = -1;
    private Map<String, Api> deployedForTestClass;
    private boolean areClassApisPrepared = false;
    private ApplicationContext applicationContext;

    /**
     * The wiremock used by the deployed apis as a backend.
     */
    protected WireMockServer wiremock;

    /**
     * Override this method to modify the configuration of the wiremock server which acts as a backend for the deployed apis.
     * @param configuration the {@link WireMockConfiguration} to modify accordingly to your need.
     */
    protected void configureWireMock(WireMockConfiguration configuration) {}

    /**
     * Override this method if you want to pass some specific configuration to the gateway.
     * WARNING: since the gateway is run only once for the test class, this configuration will impact all your test cases.
     * To know the options, please check the documentation <a href="https://docs.gravitee.io/apim/3.x/apim_installguide_gateway_configuration.html#default_configuration">here</a>.
     * @param gatewayConfigurationBuilder is the configuration builder. Just use it with {@link GatewayConfigurationBuilder#set(String, Object)}.
     *                                    For example, to configure tags for the gateway, just use <code>gatewayConfigurationBuilder.set("tags", "my-tag")</code>
     */
    protected void configureGateway(GatewayConfigurationBuilder gatewayConfigurationBuilder) {}

    /**
     * Proxy for {@link TestObserver#awaitTerminalEvent(long, TimeUnit)} with a default of 30 seconds.
     * It awaits 30 seconds or until this TestObserver/TestSubscriber receives an onError or onComplete events, whichever happens first.
     * @param obs is the observer to await
     * @return the observer after wait
     */
    protected <T> TestObserver<T> awaitTerminalEvent(TestObserver<T> obs) {
        obs.awaitTerminalEvent(30, TimeUnit.SECONDS);
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
     * Default configuration of a WebClient to be able to call the gateway
     * To use a custom Webclient for a test class, just redeclare this with the same name and the appropriate options.
     */
    @WebClientOptionsInject
    public WebClientOptions options = new WebClientOptions().setDefaultHost("localhost").setDefaultPort(gatewayPort());

    protected AbstractGatewayTest() {}

    @BeforeAll
    public static void init() {
        InternalLoggerFactory.setDefaultFactory(Slf4JLoggerFactory.INSTANCE);
    }

    /**
     * Here we inject Vertx parameter even if it's not use:
     * when implementing the test cases, the developer will need a {@link io.vertx.ext.web.client.WebClient}, which is automatically resolved as a parameter if Vertx has already been resolved.
     * Injecting it in the BeforeEach at abstract class level allows to automatically inject it to ease the life of the developer.
     *
     * Ensure the testContext is completed before starting a test, see <a href="io.gravitee.gateway.standalone.flow>Vertx documentation</a>"
     * Update endpoints for each apis deployed for the whole test class, see {@link AbstractGatewayTest#updateEndpointsOnDeployedApisForClassIfNeeded()}
     * @param vertx this parameter is only used to let the VertxExtension initialize it. It will allow to use WebClient directly.
     * @param testContext
     */
    @BeforeEach
    public void setUp(Vertx vertx, VertxTestContext testContext) throws Exception {
        resetAllMocks();
        prepareWireMock();
        updateEndpointsOnDeployedApisForClassIfNeeded();
        updateEndpointsOnDeployedApisForTestIfNeeded();
        // Prepare something on a Vert.x event-loop thread
        // The thread changes with each test instance
        testContext.completeNow();
    }

    private void resetAllMocks() throws Exception {
        for (String name : applicationContext.getBeanDefinitionNames()) {
            Object bean = applicationContext.getBean(name);
            if (AopUtils.isAopProxy(bean) && bean instanceof Advised) {
                bean = ((Advised) bean).getTargetSource().getTarget();
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
        if (wiremockPort != -1) {
            wireMockConfiguration.port(wiremockPort);
        }
        if (wiremockHttpsPort != -1) {
            wireMockConfiguration.httpsPort(wiremockHttpsPort);
        }
        wiremock = new WireMockServer(wireMockConfiguration);
        wiremock.start();
        wiremockPort = wiremock.port();
        wiremockHttpsPort = wiremock.httpsPort();
    }

    /**
     * Ensure the testContext is completed after a test, see <a href="io.gravitee.gateway.standalone.flow>Vertx documentation</a>"
     * @param testContext
     */
    @AfterEach
    void cleanUp(VertxTestContext testContext) {
        // Clean things up on the same Vert.x event-loop thread
        // that called prepare and foo
        testContext.completeNow();
        wiremock.stop();
    }

    /**
     * HACK: To ease the developer life, we propose to configure {@link WireMockExtension} thanks to {@link RegisterExtension}.
     * Currently, there is no way to indicate to junit5 that {@link RegisterExtension} should be registered before a {@link org.junit.jupiter.api.extension.ExtendWith} one.
     * That say, our {@link GatewayTestingExtension} is registered before the wiremock server is configured, and apis for class levels are already deployed, but without the right wiremock port.
     * Doing that only once during the first {@link BeforeEach}, we are able to update the endpoints of apis declared at class level with {@link DeployApi}
     */
    private void updateEndpointsOnDeployedApisForClassIfNeeded() {
        if (!areClassApisPrepared && !deployedForTestClass.isEmpty()) {
            deployedForTestClass.forEach((k, v) -> updateEndpoints(v));
        }
        this.areClassApisPrepared = true;
    }

    private void updateEndpointsOnDeployedApisForTestIfNeeded() {
        if (!deployedApis.isEmpty()) {
            deployedApis.forEach((k, v) -> updateEndpoints(v));
        }
    }

    @Override
    public void configureApi(Api api) {}

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * Ensures the api has the minimal requirement to be run properly.
     * - Add a default Keyless plan if api has no plan.
     * - Add a default Endpoint group if api has none.
     * @param api
     */
    public void ensureMinimalRequirementForApi(Api api) {
        this.addDefaultKeylessPlanIfNeeded(api);
        this.addDefaultEndpointGroupIfNeeded(api);
    }

    /**
     * Called by the {@link GatewayTestingExtension} when apis wanted for the test class are deployed.
     * @param deployedForTestClass is the list of deployed apis.
     */
    public void setDeployedClassApis(Map<String, Api> deployedForTestClass) {
        this.deployedForTestClass = deployedForTestClass;
    }

    public int gatewayPort() {
        if (gatewayPort == -1) {
            gatewayPort = getAvailablePort();
        }
        return gatewayPort;
    }

    public int technicalApiPort() {
        if (technicalApiPort == -1) {
            technicalApiPort = getAvailablePort();
        }
        return technicalApiPort;
    }

    /**
     * Override api plans to create a default Keyless plan
     * @param api is the api to override
     */
    protected void addDefaultKeylessPlanIfNeeded(Api api) {
        if (api.getPlans() == null || api.getPlans().isEmpty()) {
            // By default, add a keyless plan to the API
            Plan plan = new Plan();
            plan.setId("default_plan");
            plan.setName("Default plan");
            plan.setSecurity("key_less");

            api.setPlans(Collections.singletonList(plan));
        }
    }

    /**
     * Add a default endpoint group to the api
     */
    private void addDefaultEndpointGroupIfNeeded(Api api) {
        if (api.getProxy().getGroups() == null || api.getProxy().getGroups().isEmpty()) {
            // Create a default endpoint group
            EndpointGroup group = new EndpointGroup();
            group.setName("default");
            group.setEndpoints(Collections.emptySet());
            api.getProxy().setGroups(Collections.singleton(group));
        }
    }

    /**
     * Override api endpoints to replace port by the configured wiremock port
     * @param api is the api to override
     */
    private void updateEndpoints(Api api) {
        // Define dynamically endpoint port
        for (Endpoint endpoint : api.getProxy().getGroups().iterator().next().getEndpoints()) {
            final int port = endpoint.getTarget().startsWith("https") ? wiremockHttpsPort : wiremockPort;
            endpoint.setTarget(exchangePort(endpoint.getTarget(), port));
        }
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
     * @param api is the api on which the endpoints will be transformed
     * @param endpointConsumer is the consumer used to transform the endpoints
     */
    protected void updateEndpoints(Api api, Consumer<Endpoint> endpointConsumer) {
        for (Endpoint endpoint : api.getProxy().getGroups().iterator().next().getEndpoints()) {
            endpointConsumer.accept(endpoint);
        }
    }

    private int getAvailablePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }
}
