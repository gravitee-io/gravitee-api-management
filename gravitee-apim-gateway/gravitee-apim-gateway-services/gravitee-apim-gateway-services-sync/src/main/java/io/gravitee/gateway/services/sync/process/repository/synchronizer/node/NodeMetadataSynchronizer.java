/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)&#10;&#10;Licensed under the Apache License, Version 2.0 (the &quot;License&quot;);&#10;you may not use this file except in compliance with the License.&#10;You may obtain a copy of the License at&#10;&#10;        http://www.apache.org/licenses/LICENSE-2.0&#10;&#10;Unless required by applicable law or agreed to in writing, software&#10;distributed under the License is distributed on an &quot;AS IS&quot; BASIS,&#10;WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.&#10;See the License for the specific language governing permissions and&#10;limitations under the License.
 */

package io.gravitee.gateway.services.sync.process.repository.synchronizer.node;

import io.gravitee.gateway.services.sync.process.common.deployer.DeployerFactory;
import io.gravitee.gateway.services.sync.process.common.deployer.NodeMetadataDeployer;
import io.gravitee.gateway.services.sync.process.common.synchronizer.Order;
import io.gravitee.gateway.services.sync.process.repository.RepositorySynchronizer;
import io.gravitee.gateway.services.sync.process.repository.fetcher.InstallationIdFetcher;
import io.gravitee.gateway.services.sync.process.repository.fetcher.OrganizationIdsFetcher;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
@Slf4j
public class NodeMetadataSynchronizer implements RepositorySynchronizer {

    private final OrganizationIdsFetcher organizationIdsFetcher;
    private final InstallationIdFetcher installationIdFetcher;
    private final DeployerFactory deployerFactory;
    private final ThreadPoolExecutor syncFetcherExecutor;
    private final ThreadPoolExecutor syncDeployerExecutor;

    @Override
    public Completable synchronize(final Long from, final Long to, final Set<String> environments) {
        if (from == -1) {
            AtomicLong launchTime = new AtomicLong();
            return Single
                .just(NodeMetadataDeployable.builder())
                .flatMapMaybe(nodeMetadataDeployableBuilder ->
                    organizationIdsFetcher
                        .fetch(environments)
                        .map(nodeMetadataDeployableBuilder::organizationIds)
                        .switchIfEmpty(Maybe.just(nodeMetadataDeployableBuilder))
                )
                .flatMap(nodeMetadataDeployableBuilder ->
                    installationIdFetcher
                        .fetch()
                        .map(nodeMetadataDeployableBuilder::installationId)
                        .switchIfEmpty(Maybe.just(nodeMetadataDeployableBuilder))
                )
                .map(NodeMetadataDeployable.NodeMetadataDeployableBuilder::build)
                .subscribeOn(Schedulers.from(syncFetcherExecutor))
                .compose(upstream -> {
                    NodeMetadataDeployer nodeMetadataDeployer = deployerFactory.createNodeMetadataDeployer();
                    return upstream
                        .subscribeOn(Schedulers.from(syncDeployerExecutor))
                        .flatMap(deployable -> deploy(nodeMetadataDeployer, deployable));
                })
                .ignoreElement()
                .doOnSubscribe(disposable -> launchTime.set(Instant.now().toEpochMilli()))
                .doOnComplete(() -> {
                    log.info("Node metadata synchronized in {}ms", (System.currentTimeMillis() - launchTime.get()));
                });
        } else {
            return Completable.complete();
        }
    }

    private static Maybe<NodeMetadataDeployable> deploy(
        final NodeMetadataDeployer nodeMetadataDeployer,
        final NodeMetadataDeployable deployable
    ) {
        return nodeMetadataDeployer
            .deploy(deployable)
            .andThen(nodeMetadataDeployer.doAfterDeployment(deployable))
            .andThen(Maybe.just(deployable))
            .onErrorResumeNext(throwable -> {
                log.error("An error occurred during node metadata deployment", throwable);
                return Maybe.empty();
            });
    }

    @Override
    public int order() {
        return Order.NODE_METADATA.index();
    }
}
