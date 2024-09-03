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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.license;

import io.gravitee.gateway.services.sync.process.common.deployer.DeployerFactory;
import io.gravitee.gateway.services.sync.process.common.deployer.LicenseDeployer;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import io.gravitee.gateway.services.sync.process.repository.RepositorySynchronizer;
import io.gravitee.gateway.services.sync.process.repository.fetcher.LicenseFetcher;
import io.gravitee.node.api.Node;
import io.gravitee.repository.management.model.License;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class LicenseSynchronizer implements RepositorySynchronizer {

    private final Node node;
    private final LicenseFetcher licenseFetcher;
    private final DeployerFactory deployerFactory;
    private final ThreadPoolExecutor syncFetcherExecutor;
    private final ThreadPoolExecutor syncDeployerExecutor;

    @Override
    public Completable synchronize(final Long from, final Long to, final Set<String> environments) {
        AtomicLong launchTime = new AtomicLong();
        return licenseFetcher
            .fetchLatest(from, to, (Set<String>) node.metadata().get(Node.META_ORGANIZATIONS))
            .subscribeOn(Schedulers.from(syncFetcherExecutor))
            .rebatchRequests(syncFetcherExecutor.getMaximumPoolSize())
            .flatMap(licenses -> Flowable.fromStream(licenses.stream().map(this::prepareForDeployment)))
            // per deployable
            .compose(upstream -> {
                LicenseDeployer licenseDeployer = deployerFactory.createLicenseDeployer();
                return upstream
                    .parallel(syncDeployerExecutor.getMaximumPoolSize())
                    .runOn(Schedulers.from(syncDeployerExecutor))
                    .flatMap(deployable -> deploy(licenseDeployer, deployable))
                    .sequential();
            })
            .count()
            .doOnSubscribe(disposable -> launchTime.set(Instant.now().toEpochMilli()))
            .doOnSuccess(count -> {
                String logMsg = String.format("%s licenses synchronized in %sms", count, (System.currentTimeMillis() - launchTime.get()));
                if (from == -1) {
                    log.info(logMsg);
                } else {
                    log.debug(logMsg);
                }
            })
            .ignoreElement();
    }

    private LicenseDeployable prepareForDeployment(final License organizationLicense) {
        return LicenseDeployable
            .builder()
            .id(organizationLicense.getReferenceId())
            .license(organizationLicense.getLicense())
            .syncAction(SyncAction.DEPLOY)
            .build();
    }

    private static Flowable<LicenseDeployable> deploy(final LicenseDeployer licenseDeployer, final LicenseDeployable deployable) {
        return licenseDeployer
            .deploy(deployable)
            .andThen(licenseDeployer.doAfterDeployment(deployable))
            .andThen(Flowable.just(deployable))
            .onErrorResumeNext(throwable -> {
                log.error(throwable.getMessage(), throwable);
                return Flowable.empty();
            });
    }

    @Override
    public int order() {
        return 0;
    }
}
