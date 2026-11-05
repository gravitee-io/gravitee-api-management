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

import io.reactivex.rxjava3.core.Completable;

/**
 * Chains a local undeployment with its distribution so that the distribution always happens.
 *
 * <p>When the primary node fails to undeploy an entity locally, the other gateways must still stop serving it:
 * skipping the distribution would leave a stopped API, a closed subscription or a revoked API key live on every
 * secondary until the entity changes again. The local failure is surfaced once the distribution ran.
 * <p>
 * When the distribution fails (transient, the sync window must be retried) it is surfaced even if the local step
 * also failed, so the error classification treats the whole undeploy as retryable and the secondary-side message
 * is guaranteed: the local failure is attached as suppressed for observability.
 */
public final class FailClosedUndeploy {

    private FailClosedUndeploy() {}

    public static Completable undeployThenDistribute(final Completable localUndeploy, final Completable distribution) {
        return localUndeploy
            .onErrorResumeNext(localFailure ->
                distribution
                    .onErrorResumeNext(distributionFailure -> {
                        distributionFailure.addSuppressed(localFailure);
                        return Completable.error(distributionFailure);
                    })
                    .andThen(Completable.error(localFailure))
            )
            .andThen(distribution);
    }
}
