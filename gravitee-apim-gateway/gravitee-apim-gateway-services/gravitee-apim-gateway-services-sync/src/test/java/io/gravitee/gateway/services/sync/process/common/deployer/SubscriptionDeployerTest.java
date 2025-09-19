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
package io.gravitee.gateway.services.sync.process.common.deployer;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.api.service.SubscriptionService;
import io.gravitee.gateway.reactive.reactor.v4.subscription.SubscriptionDispatcher;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.gateway.services.sync.process.common.model.SyncException;
import io.gravitee.gateway.services.sync.process.distributed.service.NoopDistributedSyncService;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.api.ApiReactorDeployable;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.subscription.SingleSubscriptionDeployable;
import io.gravitee.node.api.Node;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.CommandRepository;
import io.reactivex.rxjava3.core.Completable;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SubscriptionDeployerTest {

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private SubscriptionDispatcher subscriptionDispatcher;

    @Mock
    private CommandRepository commandRepository;

    @Mock
    private Node node;

    @Mock
    private ObjectMapper objectMapper;

    private SubscriptionDeployer cut;

    @BeforeEach
    public void beforeEach() {
        cut = new SubscriptionDeployer(
            subscriptionService,
            subscriptionDispatcher,
            commandRepository,
            node,
            objectMapper,
            new NoopDistributedSyncService()
        );
    }

    @Nested
    class DeployTest {

        @Test
        void should_deploy_subscriptions_with_subscribable_plan() {
            Subscription subscription1 = Subscription.builder().plan("plan").id("subscription1").build();
            Subscription subscription2 = Subscription.builder().plan("unsubscribablePlan").id("subscription2").build();
            ApiReactorDeployable apiReactorDeployable = ApiReactorDeployable.builder()
                .apiId("apiId")
                .reactableApi(mock(ReactableApi.class))
                .subscribablePlans(Set.of("plan"))
                .subscriptions(List.of(subscription1, subscription2))
                .build();
            cut.deploy(apiReactorDeployable).test().assertComplete();
            verify(subscriptionService).register(subscription1);
            verify(subscriptionService, never()).register(subscription2);
            verifyNoMoreInteractions(subscriptionService);
        }

        @Test
        void should_ignore_subscription_in_error() {
            Subscription subscription1 = Subscription.builder().plan("plan").id("subscription1").build();
            Subscription subscription2 = Subscription.builder().plan("plan").id("subscription2").build();
            ApiReactorDeployable apiReactorDeployable = ApiReactorDeployable.builder()
                .apiId("apiId")
                .reactableApi(mock(ReactableApi.class))
                .subscribablePlans(Set.of("plan"))
                .subscriptions(List.of(subscription1, subscription2))
                .build();
            doThrow(new SyncException("error")).when(subscriptionService).register(subscription1);
            cut.deploy(apiReactorDeployable).test().assertComplete();
            verify(subscriptionService).register(subscription1);
            verify(subscriptionService).register(subscription2);
            verifyNoMoreInteractions(subscriptionService);
        }

        @Test
        void should_dispatch_when_subscription_registered_to_be_dispatched() {
            Subscription subscriptionToDispatch = Subscription.builder()
                .api("apiId")
                .plan("plan")
                .type(Subscription.Type.PUSH)
                .id("subscription1")
                .build();
            ApiReactorDeployable apiReactorDeployable = ApiReactorDeployable.builder()
                .apiId("apiId")
                .reactableApi(mock(ReactableApi.class))
                .subscribablePlans(Set.of("plan"))
                .subscriptions(List.of(subscriptionToDispatch))
                .build();
            cut.deploy(apiReactorDeployable).test().assertComplete();
            verify(subscriptionService).register(subscriptionToDispatch);
            verifyNoMoreInteractions(subscriptionService);

            when(subscriptionDispatcher.dispatch(subscriptionToDispatch)).thenReturn(Completable.complete());
            cut.doAfterDeployment(apiReactorDeployable).test().assertComplete();
            verify(subscriptionDispatcher).dispatch(subscriptionToDispatch);
        }

        @Test
        void should_do_nothing_when_no_subscription_registered_to_be_dispatched() {
            Subscription subscription = Subscription.builder().api("apiId").plan("plan").id("subscription").build();
            ApiReactorDeployable apiReactorDeployable = ApiReactorDeployable.builder()
                .apiId("apiId")
                .reactableApi(mock(ReactableApi.class))
                .subscribablePlans(Set.of("plan"))
                .subscriptions(List.of(subscription))
                .build();
            cut.deploy(apiReactorDeployable).test().assertComplete();
            verify(subscriptionService).register(subscription);
            verifyNoMoreInteractions(subscriptionService);

            cut.doAfterDeployment(apiReactorDeployable).test().assertComplete();
            verifyNoInteractions(subscriptionDispatcher);
        }

        @Test
        void should_ignore_when_dispatching_in_error_when_subscription_registered_to_be_dispatched()
            throws InterruptedException, TechnicalException {
            Subscription subscriptionToDispatch1 = Subscription.builder()
                .api("apiId")
                .plan("plan")
                .type(Subscription.Type.PUSH)
                .id("subscription1")
                .build();
            Subscription subscriptionToDispatch2 = Subscription.builder()
                .api("apiId")
                .plan("plan")
                .type(Subscription.Type.PUSH)
                .id("subscription2")
                .build();
            ApiReactorDeployable apiReactorDeployable = ApiReactorDeployable.builder()
                .apiId("apiId")
                .reactableApi(mock(ReactableApi.class))
                .subscribablePlans(Set.of("plan"))
                .subscriptions(List.of(subscriptionToDispatch1, subscriptionToDispatch2))
                .build();
            cut.deploy(apiReactorDeployable).test().assertComplete();
            verify(subscriptionService).register(subscriptionToDispatch1);
            verify(subscriptionService).register(subscriptionToDispatch2);
            verifyNoMoreInteractions(subscriptionService);

            when(subscriptionDispatcher.dispatch(subscriptionToDispatch1)).thenReturn(Completable.error(new SyncException("error")));
            when(subscriptionDispatcher.dispatch(subscriptionToDispatch2)).thenReturn(Completable.complete());
            cut.doAfterDeployment(apiReactorDeployable).test().await().onComplete();

            await().untilAsserted(() -> {
                verify(subscriptionDispatcher).dispatch(subscriptionToDispatch1);
                verify(commandRepository).create(any());
                verify(subscriptionDispatcher).dispatch(subscriptionToDispatch2);
            });
        }
    }

    @Nested
    class UndeployTest {

        @Test
        void should_undeploy_subscriptions_from_api_id() {
            ApiReactorDeployable apiReactorDeployable = ApiReactorDeployable.builder().apiId("apiId").build();
            cut.undeploy(apiReactorDeployable).test().assertComplete();
            verify(subscriptionService).unregisterByApiId("apiId");
            verifyNoMoreInteractions(subscriptionService);
        }

        @Test
        void should_ignore_undeploy_subscriptions_from_api_in_error() {
            ApiReactorDeployable apiReactorDeployable = ApiReactorDeployable.builder().apiId("apiId").build();
            doThrow(new SyncException("error")).when(subscriptionService).unregisterByApiId("apiId");
            cut.undeploy(apiReactorDeployable).test().assertComplete();
            verify(subscriptionService).unregisterByApiId("apiId");
            verifyNoMoreInteractions(subscriptionService);
        }

        @Test
        void should_undeploy_from_subscription() {
            Subscription subscription = Subscription.builder().api("apiId").plan("plan").id("subscription").build();
            SingleSubscriptionDeployable subscriptionDeployable = SingleSubscriptionDeployable.builder().subscription(subscription).build();
            cut.undeploy(subscriptionDeployable).test().assertComplete();
            verify(subscriptionService).unregister(subscription);
            verifyNoMoreInteractions(subscriptionService);
        }

        @Test
        void should_ignore_undeploy_from_subscription_in_error() {
            Subscription subscription = Subscription.builder().api("apiId").plan("plan").id("subscription").build();
            SingleSubscriptionDeployable subscriptionDeployable = SingleSubscriptionDeployable.builder().subscription(subscription).build();
            doThrow(new SyncException("error")).when(subscriptionService).unregister(subscription);
            cut.undeploy(subscriptionDeployable).test().assertComplete();
            verify(subscriptionService).unregister(subscription);
            verifyNoMoreInteractions(subscriptionService);
        }

        @Test
        void should_dispatch_when_subscription_undeployed() {
            Subscription subscriptionToDispatch = Subscription.builder()
                .api("apiId")
                .plan("plan")
                .type(Subscription.Type.PUSH)
                .id("subscription1")
                .build();
            SingleSubscriptionDeployable apiReactorDeployable = SingleSubscriptionDeployable.builder()
                .subscription(subscriptionToDispatch)
                .build();
            when(subscriptionDispatcher.dispatch(subscriptionToDispatch)).thenReturn(Completable.complete());
            cut.undeploy(apiReactorDeployable).test().assertComplete();
            verify(subscriptionService).unregister(subscriptionToDispatch);
            verifyNoMoreInteractions(subscriptionService);
            verify(subscriptionDispatcher).dispatch(subscriptionToDispatch);
        }
    }
}
