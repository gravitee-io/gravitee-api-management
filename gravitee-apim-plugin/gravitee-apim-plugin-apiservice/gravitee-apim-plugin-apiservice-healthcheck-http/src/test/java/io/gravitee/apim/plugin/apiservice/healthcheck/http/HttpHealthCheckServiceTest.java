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
package io.gravitee.apim.plugin.apiservice.healthcheck.http;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static io.gravitee.apim.plugin.apiservice.healthcheck.http.HttpHealthCheckService.HTTP_HEALTH_CHECK_TYPE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.endpointgroup.service.EndpointGroupServices;
import io.gravitee.definition.model.v4.endpointgroup.service.EndpointServices;
import io.gravitee.definition.model.v4.service.Service;
import io.gravitee.el.TemplateEngine;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.reactive.api.connector.endpoint.BaseEndpointConnector;
import io.gravitee.gateway.reactive.api.context.DeploymentContext;
import io.gravitee.gateway.reactive.api.context.ExecutionContext;
import io.gravitee.gateway.reactive.api.helper.PluginConfigurationHelper;
import io.gravitee.gateway.reactive.core.v4.endpoint.DefaultManagedEndpoint;
import io.gravitee.gateway.reactive.core.v4.endpoint.EndpointManager;
import io.gravitee.gateway.reactive.core.v4.endpoint.ManagedEndpointGroup;
import io.gravitee.gateway.reactive.handlers.api.v4.Api;
import io.gravitee.gateway.report.ReporterService;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.plugin.alert.AlertEventProducer;
import io.gravitee.reporter.api.health.EndpointStatus;
import io.reactivex.rxjava3.core.Completable;
import io.vertx.rxjava3.core.Vertx;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
public class HttpHealthCheckServiceTest {

    public static final String ENDPOINT_NAME = "ENDPOINT_NAME";
    public static final String API_ID = "API_ID";

    @Mock
    private PluginConfigurationHelper pluginConfigurationHelper;

    @Mock
    private EndpointManager endpointManager;

    @Mock
    private GatewayConfiguration gatewayConfig;

    @Mock
    private DeploymentContext deploymentContext;

    @Mock
    private Node node;

    @Mock
    private ReporterService reporterService;

    @Mock
    private AlertEventProducer alertEventProducer;

    @Mock
    private BaseEndpointConnector endpointConnector;

    @Mock
    private ManagedEndpointGroup managedEndpointGroup;

    @Mock
    private Endpoint endpoint;

    private final HttpHealthCheckServiceConfiguration hcConfig = new HttpHealthCheckServiceConfiguration();
    private final io.gravitee.definition.model.v4.Api apiDefinition = new io.gravitee.definition.model.v4.Api();
    private final Api api = new Api(apiDefinition);

    private final TemplateEngine templateEngine = TemplateEngine.templateEngine();

    @BeforeEach
    public void setup() {
        when(deploymentContext.getComponent(EndpointManager.class)).thenReturn(endpointManager);
        when(deploymentContext.getComponent(PluginConfigurationHelper.class)).thenReturn(pluginConfigurationHelper);

        apiDefinition.setId(API_ID);
    }

    @Test
    public void should_not_start_endpoint_health_check_when_api_has_no_endpoint() {
        final var cut = new HttpHealthCheckService(new Api(new io.gravitee.definition.model.v4.Api()), deploymentContext, gatewayConfig);
        cut.start();

        Assertions.assertThat(cut.getJobs()).isNotNull().isEmpty();

        cut.stop();
    }

    @Nested
    class HttpBasedEndpointConnector {

        private EndpointServices services;

        @BeforeEach
        public void setup() throws Exception {
            services = new EndpointServices();

            when(deploymentContext.getComponent(Api.class)).thenReturn(api);
            when(deploymentContext.getComponent(Node.class)).thenReturn(node);
            when(deploymentContext.getComponent(ReporterService.class)).thenReturn(reporterService);
            when(deploymentContext.getComponent(AlertEventProducer.class)).thenReturn(alertEventProducer);

            hcConfig.setSchedule("* * * * * *");
            hcConfig.setTarget("/health");
            hcConfig.setAssertion("{#response.status == 200}");
            hcConfig.setFailureThreshold(2);
            hcConfig.setSuccessThreshold(2);
            when(pluginConfigurationHelper.readConfiguration(any(), any())).thenReturn(hcConfig);

            when(endpoint.getType()).thenReturn(HTTP_HEALTH_CHECK_TYPE);
            when(endpoint.getServices()).thenReturn(services);
            when(endpoint.getName()).thenReturn(ENDPOINT_NAME);

            final var managedEndpoint = new DefaultManagedEndpoint(endpoint, managedEndpointGroup, endpointConnector);
            when(endpointManager.all()).thenReturn(List.of(managedEndpoint));
        }

        @Test
        public void should_start_endpoint_health_check_when_api_has_one_HttpProxy_endpoint() throws Exception {
            final Service healthCheck = new Service();
            healthCheck.setEnabled(true);
            healthCheck.setOverrideConfiguration(true);
            healthCheck.setType(HTTP_HEALTH_CHECK_TYPE);
            services.setHealthCheck(healthCheck);

            when(managedEndpointGroup.getDefinition()).thenReturn(new EndpointGroup());

            CountDownLatch countDownLatch = new CountDownLatch(hcConfig.getSuccessThreshold());

            doAnswer(invoker -> {
                    countDownLatch.countDown();
                    return null;
                })
                .when(reporterService)
                .report(any());

            when(endpointConnector.connect(any()))
                .thenAnswer(invokable -> {
                    ExecutionContext ctx = invokable.getArgument(0);
                    ctx.response().status(200);
                    ctx.metrics().setEndpoint(hcConfig.getTarget());
                    return Completable.complete();
                });

            startHealthCheckAndValidate(countDownLatch, hcConfig.getSuccessThreshold(), 0, 0);
        }

        @Test
        public void should_start_endpoint_health_check_with_groupConfig_when_api_has_one_HttpProxy_endpoint() throws Exception {
            final Service healthCheck = new Service();
            healthCheck.setEnabled(false);
            services.setHealthCheck(healthCheck);

            final EndpointGroupServices groupServices = new EndpointGroupServices();
            final Service grpHealthCheck = new Service();
            grpHealthCheck.setEnabled(true);
            grpHealthCheck.setType(HTTP_HEALTH_CHECK_TYPE);
            groupServices.setHealthCheck(grpHealthCheck);

            final EndpointGroup endpointGroup = new EndpointGroup();
            endpointGroup.setServices(groupServices);
            when(managedEndpointGroup.getDefinition()).thenReturn(endpointGroup);

            CountDownLatch countDownLatch = new CountDownLatch(hcConfig.getSuccessThreshold());

            doAnswer(invoker -> {
                    countDownLatch.countDown();
                    return null;
                })
                .when(reporterService)
                .report(any());

            when(endpointConnector.connect(any()))
                .thenAnswer(invokable -> {
                    ExecutionContext ctx = invokable.getArgument(0);
                    ctx.response().status(200);
                    ctx.metrics().setEndpoint(hcConfig.getTarget());
                    return Completable.complete();
                });

            startHealthCheckAndValidate(countDownLatch, hcConfig.getSuccessThreshold(), 0, 0);
        }

        @Test
        public void should_have_state_transition() throws Exception {
            when(alertEventProducer.isEmpty()).thenReturn(false);

            final Service healthCheck = new Service();
            healthCheck.setEnabled(false);
            services.setHealthCheck(healthCheck);

            final EndpointGroupServices groupServices = new EndpointGroupServices();
            final Service grpHealthCheck = new Service();
            grpHealthCheck.setEnabled(true);
            grpHealthCheck.setType(HTTP_HEALTH_CHECK_TYPE);
            groupServices.setHealthCheck(grpHealthCheck);

            final EndpointGroup endpointGroup = new EndpointGroup();
            endpointGroup.setServices(groupServices);
            when(managedEndpointGroup.getDefinition()).thenReturn(endpointGroup);

            CountDownLatch countDownLatch = new CountDownLatch(hcConfig.getFailureThreshold() + hcConfig.getSuccessThreshold() + 1);

            doAnswer(invoker -> {
                    EndpointStatus status = invoker.getArgument(0);
                    if (!status.isTransition()) {
                        // we do not count down if there is a transition because we want to be
                        // sure that the alert will be sent
                        countDownLatch.countDown();
                    }
                    return null;
                })
                .when(reporterService)
                .report(any());

            doAnswer(invoker -> {
                    countDownLatch.countDown();
                    return null;
                })
                .when(alertEventProducer)
                .send(any());

            when(endpointConnector.connect(any()))
                .thenAnswer(invokable -> {
                    ExecutionContext ctx = invokable.getArgument(0);
                    ctx.response().status(500); // UP to TRANSITIONALLY_DOWN
                    ctx.metrics().setEndpoint(hcConfig.getTarget());
                    return Completable.complete();
                })
                .thenAnswer(invokable -> {
                    ExecutionContext ctx = invokable.getArgument(0);
                    ctx.response().status(500); // TRANSITIONALLY_DOWN to DOWN
                    ctx.metrics().setEndpoint(hcConfig.getTarget());
                    return Completable.complete();
                })
                .thenAnswer(invokable -> {
                    ExecutionContext ctx = invokable.getArgument(0);
                    ctx.response().status(500); // No transition (DOWN)
                    ctx.metrics().setEndpoint(hcConfig.getTarget());
                    return Completable.complete();
                })
                .thenAnswer(invokable -> {
                    ExecutionContext ctx = invokable.getArgument(0);
                    ctx.response().status(200); // // DOWN to TRANSITIONALLY_UP
                    ctx.metrics().setEndpoint(hcConfig.getTarget());
                    return Completable.complete();
                })
                .thenAnswer(invokable -> {
                    ExecutionContext ctx = invokable.getArgument(0);
                    ctx.response().status(200); // // TRANSITIONALLY_UP to UP
                    ctx.metrics().setEndpoint(hcConfig.getTarget());
                    return Completable.complete();
                });

            final int expectedTransition = 4;
            startHealthCheckAndValidate(
                countDownLatch,
                hcConfig.getSuccessThreshold(),
                hcConfig.getFailureThreshold() + 1,
                expectedTransition
            );
        }

        @Test
        public void should_have_state_transition_on_connection_error() throws Exception {
            when(alertEventProducer.isEmpty()).thenReturn(false);

            final Service healthCheck = new Service();
            healthCheck.setEnabled(false);
            services.setHealthCheck(healthCheck);

            final EndpointGroupServices groupServices = new EndpointGroupServices();
            final Service grpHealthCheck = new Service();
            grpHealthCheck.setEnabled(true);
            grpHealthCheck.setType(HTTP_HEALTH_CHECK_TYPE);
            groupServices.setHealthCheck(grpHealthCheck);

            final EndpointGroup endpointGroup = new EndpointGroup();
            endpointGroup.setServices(groupServices);
            when(managedEndpointGroup.getDefinition()).thenReturn(endpointGroup);

            CountDownLatch countDownLatch = new CountDownLatch(hcConfig.getFailureThreshold() + hcConfig.getSuccessThreshold() + 1);

            doAnswer(invoker -> {
                    EndpointStatus status = invoker.getArgument(0);
                    if (!status.isTransition()) {
                        // we do not count down if there is a transition because we want to be
                        // sure that the alert will be sent
                        countDownLatch.countDown();
                    }
                    return null;
                })
                .when(reporterService)
                .report(any());

            doAnswer(invoker -> {
                    countDownLatch.countDown();
                    return null;
                })
                .when(alertEventProducer)
                .send(any());

            when(endpointConnector.connect(any()))
                .thenAnswer(invokable -> Completable.error(new UnknownHostException())) // UP to TRANSITIONALLY_DOWN
                .thenAnswer(invokable -> Completable.error(new SocketException())) // TRANSITIONALLY_DOWN to DOWN
                .thenAnswer(invokable -> {
                    ExecutionContext ctx = invokable.getArgument(0);
                    ctx.response().status(500); // No transition (DOWN)
                    ctx.metrics().setEndpoint(hcConfig.getTarget());
                    return Completable.complete();
                })
                .thenAnswer(invokable -> {
                    ExecutionContext ctx = invokable.getArgument(0);
                    ctx.response().status(200); // // DOWN to TRANSITIONALLY_UP
                    ctx.metrics().setEndpoint(hcConfig.getTarget());
                    return Completable.complete();
                })
                .thenAnswer(invokable -> {
                    ExecutionContext ctx = invokable.getArgument(0);
                    ctx.response().status(200); // // TRANSITIONALLY_UP to UP
                    ctx.metrics().setEndpoint(hcConfig.getTarget());
                    return Completable.complete();
                });

            final int expectedTransition = 4;
            startHealthCheckAndValidate(
                countDownLatch,
                hcConfig.getSuccessThreshold(),
                hcConfig.getFailureThreshold() + 1,
                expectedTransition
            );
        }

        private void startHealthCheckAndValidate(
            CountDownLatch countDownLatch,
            int expectedSuccess,
            int expectedFailure,
            int expectedTransitions
        ) throws InterruptedException {
            final var cut = new HttpHealthCheckService(api, deploymentContext, gatewayConfig);
            cut.start();

            Assertions.assertThat(cut.getJobs()).isNotNull().hasSize(1);

            countDownLatch.await();
            cut.stop();

            Assertions.assertThat(cut.getJobs()).isNotNull().hasSize(0);

            verify(alertEventProducer, times(expectedTransitions)).send(any());

            verify(reporterService, times(expectedSuccess))
                .report(
                    argThat((EndpointStatus reportable) ->
                        reportable.getEndpoint().equals(ENDPOINT_NAME) &&
                        reportable.getSteps().get(0).getRequest().getUri() != null &&
                        reportable.getSteps().get(0).getRequest().getUri().endsWith(hcConfig.getTarget()) &&
                        reportable.isSuccess()
                    )
                );

            verify(reporterService, times(expectedFailure))
                .report(
                    argThat((EndpointStatus reportable) ->
                        reportable.getEndpoint().equals(ENDPOINT_NAME) &&
                        (
                            reportable.getSteps().get(0).getRequest().getUri() == null ||
                            reportable.getSteps().get(0).getRequest().getUri().endsWith(hcConfig.getTarget())
                        ) &&
                        !reportable.isSuccess()
                    )
                );

            verify(reporterService, times(expectedTransitions)).report(argThat((EndpointStatus reportable) -> reportable.isTransition()));
        }
    }

    @Nested
    class NonHttpBasedEndpointConnector {

        @RegisterExtension
        private WireMockExtension wiremock = WireMockExtension
            .newInstance()
            .options(WireMockConfiguration.wireMockConfig().dynamicPort().dynamicHttpsPort())
            .build();

        private static final String ENDPOINT_TYPE = "kafka";

        @Mock
        private Configuration configuration;

        @Test
        public void should_call_target_using_instance_of_httpClient() throws Exception {
            when(deploymentContext.getComponent(Api.class)).thenReturn(api);
            when(deploymentContext.getComponent(Node.class)).thenReturn(node);
            when(deploymentContext.getComponent(ReporterService.class)).thenReturn(reporterService);
            when(deploymentContext.getComponent(AlertEventProducer.class)).thenReturn(alertEventProducer);
            when(deploymentContext.getComponent(Vertx.class)).thenReturn(Vertx.vertx());
            when(deploymentContext.getComponent(Configuration.class)).thenReturn(configuration);

            wiremock.stubFor(get("/health").willReturn(ok()));

            hcConfig.setTarget("http://localhost:" + wiremock.getPort() + "/health");
            hcConfig.setSchedule("* * * * * *");
            hcConfig.setAssertion("{#response.status == 200}");
            hcConfig.setMethod(HttpMethod.GET);
            hcConfig.setSuccessThreshold(2);

            when(pluginConfigurationHelper.readConfiguration(any(), any())).thenReturn(hcConfig);

            final var services = new EndpointServices();
            when(endpoint.getServices()).thenReturn(services);
            when(endpoint.getType()).thenReturn(ENDPOINT_TYPE);
            when(endpoint.getName()).thenReturn(ENDPOINT_NAME);

            final var managedEndpoint = new DefaultManagedEndpoint(endpoint, managedEndpointGroup, endpointConnector);
            when(endpointManager.all()).thenReturn(List.of(managedEndpoint));

            final Service healthCheck = new Service();
            healthCheck.setEnabled(true);
            healthCheck.setOverrideConfiguration(true);
            healthCheck.setType(HTTP_HEALTH_CHECK_TYPE);
            services.setHealthCheck(healthCheck);

            when(managedEndpointGroup.getDefinition()).thenReturn(new EndpointGroup());
            CountDownLatch countDownLatch = new CountDownLatch(hcConfig.getSuccessThreshold());

            doAnswer(invoker -> {
                    countDownLatch.countDown();
                    return null;
                })
                .when(reporterService)
                .report(any());

            final var cut = new HttpHealthCheckService(api, deploymentContext, gatewayConfig);
            cut.start();

            Assertions.assertThat(cut.getJobs()).isNotNull().hasSize(1);

            countDownLatch.await();
            cut.stop();

            Assertions.assertThat(cut.getJobs()).isNotNull().hasSize(0);

            verify(alertEventProducer, never()).send(any());

            verify(reporterService, times(hcConfig.getSuccessThreshold()))
                .report(
                    argThat((EndpointStatus reportable) ->
                        reportable.getEndpoint().equals(ENDPOINT_NAME) &&
                        reportable.getSteps().get(0).getRequest().getUri().endsWith(hcConfig.getTarget()) &&
                        reportable.isSuccess()
                    )
                );
        }

        @Test
        public void should_consider_service_unhealthy_on_connection_error() throws Exception {
            when(deploymentContext.getComponent(Api.class)).thenReturn(api);
            when(deploymentContext.getComponent(Node.class)).thenReturn(node);
            when(deploymentContext.getComponent(ReporterService.class)).thenReturn(reporterService);
            when(deploymentContext.getComponent(AlertEventProducer.class)).thenReturn(alertEventProducer);
            when(deploymentContext.getComponent(Vertx.class)).thenReturn(Vertx.vertx());
            when(deploymentContext.getComponent(Configuration.class)).thenReturn(configuration);

            hcConfig.setTarget("http://localhost:" + wiremock.getPort() + "/health");
            hcConfig.setSchedule("* * * * * *");
            hcConfig.setAssertion("{#response.status == 200}");
            hcConfig.setMethod(HttpMethod.GET);
            hcConfig.setSuccessThreshold(2);

            wiremock.shutdownServer();

            when(pluginConfigurationHelper.readConfiguration(any(), any())).thenReturn(hcConfig);

            final var services = new EndpointServices();
            when(endpoint.getServices()).thenReturn(services);
            when(endpoint.getType()).thenReturn(ENDPOINT_TYPE);
            when(endpoint.getName()).thenReturn(ENDPOINT_NAME);

            final var managedEndpoint = new DefaultManagedEndpoint(endpoint, managedEndpointGroup, endpointConnector);
            when(endpointManager.all()).thenReturn(List.of(managedEndpoint));

            final Service healthCheck = new Service();
            healthCheck.setEnabled(true);
            healthCheck.setOverrideConfiguration(true);
            healthCheck.setType(HTTP_HEALTH_CHECK_TYPE);
            services.setHealthCheck(healthCheck);

            when(managedEndpointGroup.getDefinition()).thenReturn(new EndpointGroup());
            CountDownLatch countDownLatch = new CountDownLatch(hcConfig.getFailureThreshold());

            doAnswer(invoker -> {
                    countDownLatch.countDown();
                    return null;
                })
                .when(reporterService)
                .report(any());

            final var cut = new HttpHealthCheckService(api, deploymentContext, gatewayConfig);
            cut.start();

            Assertions.assertThat(cut.getJobs()).isNotNull().hasSize(1);

            countDownLatch.await();
            cut.stop();

            Assertions.assertThat(cut.getJobs()).isNotNull().hasSize(0);

            verify(alertEventProducer, times(2)).send(any());

            verify(reporterService, times(hcConfig.getFailureThreshold()))
                .report(
                    argThat((EndpointStatus reportable) ->
                        reportable.getEndpoint().equals(ENDPOINT_NAME) &&
                        reportable.getSteps().get(0).getRequest().getUri().endsWith(hcConfig.getTarget()) &&
                        !reportable.isSuccess()
                    )
                );
        }
    }
}
