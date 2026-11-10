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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.common.event.EventManager;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.v4.plan.Plan;
import io.gravitee.gateway.handlers.api.ReactableApiProduct;
import io.gravitee.gateway.handlers.api.manager.ApiManager;
import io.gravitee.gateway.handlers.api.manager.ApiProductManager;
import io.gravitee.gateway.handlers.api.registry.ApiProductRegistry;
import io.gravitee.gateway.handlers.api.services.ApiKeyCacheService;
import io.gravitee.gateway.handlers.api.services.SubscriptionCacheService;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.gateway.services.sync.SyncConfiguration;
import io.gravitee.gateway.services.sync.process.common.deployer.ApiProductDeployer;
import io.gravitee.gateway.services.sync.process.common.deployer.ApiProductSubscriptionRefresher;
import io.gravitee.gateway.services.sync.process.common.mapper.SubscriptionMapper;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import io.gravitee.gateway.services.sync.process.distributed.service.NoopDistributedSyncService;
import io.gravitee.gateway.services.sync.process.repository.mapper.ApiKeyMapper;
import io.gravitee.gateway.services.sync.process.repository.service.PlanService;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.api.ApiSynchronizer;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.api.RepositoryApiMemberResyncTrigger;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.apiproduct.ApiProductReactorDeployable;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.reactivex.rxjava3.core.Completable;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationContext;

/**
 * Deploys API Products the way the synchronizer does: plans registered, then their subscriptions and API keys loaded from
 * the repositories (mocks a test can stub), then the product published. Deploying a product already deployed is an
 * update, so only what changed is redone.
 *
 * <p>Member APIs are resynced as on a gateway, by the synchronizer's own trigger, asynchronously. Reloading an API from
 * the repository is replaced by registering again the definition the test deployed.</p>
 *
 * @author GraviteeSource Team
 */
class ApiProductSyncDeployer {

    private final ApiProductManager apiProductManager;
    private final ApiProductDeployer deployer;
    private final ThreadPoolExecutor resyncExecutor;
    private final RepositoryApiMemberResyncTrigger memberResyncTrigger;

    ApiProductSyncDeployer(ApplicationContext applicationContext, Function<String, Optional<ReactableApi<?>>> deployedApiDefinition) {
        this.apiProductManager = applicationContext.getBean(ApiProductManager.class);
        ApiProductSubscriptionRefresher subscriptionRefresher = new ApiProductSubscriptionRefresher(
            applicationContext.getBean(SubscriptionRepository.class),
            applicationContext.getBean(ApiKeyRepository.class),
            new SubscriptionMapper(new GraviteeMapper(), applicationContext.getBean(ApiProductRegistry.class)),
            new ApiKeyMapper(),
            applicationContext.getBean(SubscriptionCacheService.class),
            applicationContext.getBean(ApiKeyCacheService.class),
            SyncConfiguration.DEFAULT_APIKEY_BULK_ITEMS,
            SyncConfiguration.DEFAULT_APIKEY_SUBSCRIPTIONS_CHUNK_SIZE
        );
        this.deployer = new ApiProductDeployer(
            apiProductManager,
            new PlanService(),
            new NoopDistributedSyncService(),
            subscriptionRefresher
        );
        ApiManager apiManager = applicationContext.getBean(ApiManager.class);
        this.resyncExecutor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
        this.memberResyncTrigger = new RepositoryApiMemberResyncTrigger(
            redeployingApiSynchronizer(apiManager, deployedApiDefinition),
            apiManager,
            resyncExecutor,
            applicationContext.getBean(EventManager.class),
            () -> true
        );
    }

    void close() {
        memberResyncTrigger.dispose();
        resyncExecutor.shutdownNow();
    }

    void deploy(ReactableApiProduct apiProduct) {
        apiProduct.setDeployedAt(deployedAtAfterDeployedVersion(apiProduct));
        deployer.deploy(deployable(apiProduct, SyncAction.DEPLOY)).blockingAwait();
    }

    void undeploy(ReactableApiProduct apiProduct) {
        deployer.undeploy(deployable(apiProduct, SyncAction.UNDEPLOY)).blockingAwait();
    }

    /**
     * The manager ignores a version not strictly newer than the deployed one, and two deploys can fall in the same
     * millisecond.
     */
    private Date deployedAtAfterDeployedVersion(ReactableApiProduct apiProduct) {
        ReactableApiProduct deployed = apiProductManager.get(apiProduct.getId());
        long earliest = deployed == null || deployed.getDeployedAt() == null ? 0 : deployed.getDeployedAt().getTime() + 1;
        if (apiProduct.getDeployedAt() != null && apiProduct.getDeployedAt().getTime() >= earliest) {
            return apiProduct.getDeployedAt();
        }
        return new Date(Math.max(System.currentTimeMillis(), earliest));
    }

    private static ApiSynchronizer redeployingApiSynchronizer(
        ApiManager apiManager,
        Function<String, Optional<ReactableApi<?>>> deployedApiDefinition
    ) {
        ApiSynchronizer apiSynchronizer = mock(ApiSynchronizer.class);
        when(apiSynchronizer.resyncMemberApis(any(), any())).thenAnswer(invocation -> {
            Set<String> apiIds = invocation.getArgument(0);
            return Completable.fromRunnable(() ->
                apiIds.forEach(apiId -> deployedApiDefinition.apply(apiId).ifPresent(apiManager::register))
            );
        });
        return apiSynchronizer;
    }

    private static ApiProductReactorDeployable deployable(ReactableApiProduct apiProduct, SyncAction syncAction) {
        List<Plan> plans = apiProduct.getPlans() != null ? apiProduct.getPlans() : List.of();
        Set<String> subscribablePlans = plans.stream().map(Plan::getId).collect(Collectors.toSet());
        return ApiProductReactorDeployable.builder()
            .apiProductId(apiProduct.getId())
            .reactableApiProduct(apiProduct)
            .syncAction(syncAction)
            .subscribablePlans(subscribablePlans)
            .build();
    }
}
