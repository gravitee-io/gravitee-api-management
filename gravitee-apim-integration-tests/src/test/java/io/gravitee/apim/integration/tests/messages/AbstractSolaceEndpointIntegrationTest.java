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
package io.gravitee.apim.integration.tests.messages;

import com.graviteesource.endpoint.solace.SolaceEndpointConnectorFactory;
import com.graviteesource.reactor.message.MessageApiReactorFactory;
import com.solace.messaging.MessagingService;
import com.solace.messaging.config.SolaceConstants;
import com.solace.messaging.config.SolaceProperties;
import com.solace.messaging.config.profile.ConfigurationProfile;
import com.solace.messaging.publisher.DirectMessagePublisher;
import com.solace.messaging.publisher.OutboundMessage;
import com.solace.messaging.receiver.DirectMessageReceiver;
import com.solace.messaging.receiver.InboundMessage;
import com.solace.messaging.resources.Topic;
import com.solace.messaging.resources.TopicSubscription;
import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.reactor.ReactorBuilder;
import io.gravitee.apim.plugin.reactor.ReactorPlugin;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.gateway.reactive.reactor.v4.reactor.ReactorFactory;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.solace.Service;
import org.testcontainers.solace.SolaceContainer;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Testcontainers
@Slf4j
public abstract class AbstractSolaceEndpointIntegrationTest extends AbstractGatewayTest {

    protected static final String USER = "admin";
    protected static final String PASSWORD = "admin";
    protected static final String VPN = "default";
    protected String topic;

    @Container
    protected static final SolaceContainer solaceContainer = new SolaceContainer("solace/solace-pubsub-standard:10.3")
        .withCredentials(USER, PASSWORD)
        .withVpn(VPN)
        .withExposedPorts(Service.SMF.getPort())
        .withEnv("username_admin_globalaccesslevel", "admin")
        .withEnv("username_admin_password", "admin");

    protected MessagingService messagingService;

    @Override
    public void configureReactors(Set<ReactorPlugin<? extends ReactorFactory<?>>> reactors) {
        reactors.add(ReactorBuilder.build(MessageApiReactorFactory.class));
    }

    @Override
    public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
        endpoints.putIfAbsent("solace", EndpointBuilder.build("solace", SolaceEndpointConnectorFactory.class));
    }

    @Override
    public void configureApi(ReactableApi<?> api, Class<?> definitionClass) {
        topic = UUID.randomUUID().toString();
        if (definitionClass.isAssignableFrom(Api.class)) {
            Api apiDefinition = (Api) api.getDefinition();
            apiDefinition
                .getEndpointGroups()
                .stream()
                .flatMap(eg -> eg.getEndpoints().stream())
                .filter(endpoint -> endpoint.getType().equals("solace"))
                .forEach(endpoint -> {
                    endpoint.setConfiguration(endpoint.getConfiguration().replace("solace-url", solaceContainer.getOrigin(Service.SMF)));
                    endpoint.setSharedConfigurationOverride(endpoint.getSharedConfigurationOverride().replace("test-topic", topic));
                });
        }
    }

    @BeforeEach
    public void prepareSolaceClient() {
        MessagingService createdMessageService = createMessageService();
        messagingService = createdMessageService.connect();
        log.info("solace connected");
    }

    @AfterEach
    public void closeSolaceClient() {
        if (messagingService.isConnected()) {
            messagingService.disconnect();
            log.info("solace disconnected");
        }
    }

    protected Flowable<InboundMessage> subscribeToSolace(final String topic) {
        final DirectMessageReceiver receiver = messagingService
            .createDirectMessageReceiverBuilder()
            .withSubscriptions(TopicSubscription.of(topic))
            .build();

        return Completable.fromCompletionStage(receiver.startAsync()).andThen(
            Flowable.create(emitter -> receiver.receiveAsync(emitter::onNext), BackpressureStrategy.DROP)
        );
    }

    protected Completable publishToSolace(final String topic, List<OutboundMessage> outboundMessages) {
        return publishToSolace(topic, outboundMessages, 0);
    }

    protected Completable publishToSolace(final String topic, List<OutboundMessage> outboundMessages, final long delay) {
        final DirectMessagePublisher publisher = messagingService.createDirectMessagePublisherBuilder().build();
        return Completable.fromCompletionStage(publisher.startAsync())
            .delay(delay, TimeUnit.MILLISECONDS)
            .andThen(
                Completable.fromRunnable(() -> {
                    Topic topic1 = Topic.of(topic);
                    outboundMessages.forEach(outboundMessage -> publisher.publish(outboundMessage, topic1));
                })
            );
    }

    private MessagingService createMessageService() {
        final Properties serviceConfiguration = new Properties();
        serviceConfiguration.setProperty(SolaceProperties.TransportLayerProperties.HOST, solaceContainer.getOrigin(Service.SMF));
        serviceConfiguration.setProperty(SolaceProperties.ServiceProperties.VPN_NAME, VPN);
        // use basic auth
        serviceConfiguration.setProperty(
            SolaceProperties.AuthenticationProperties.SCHEME,
            SolaceConstants.AuthenticationConstants.AUTHENTICATION_SCHEME_BASIC
        );
        serviceConfiguration.setProperty(SolaceProperties.AuthenticationProperties.SCHEME_BASIC_USER_NAME, USER);
        serviceConfiguration.setProperty(SolaceProperties.AuthenticationProperties.SCHEME_BASIC_PASSWORD, PASSWORD);
        return MessagingService.builder(ConfigurationProfile.V1).fromProperties(serviceConfiguration).build();
    }
}
