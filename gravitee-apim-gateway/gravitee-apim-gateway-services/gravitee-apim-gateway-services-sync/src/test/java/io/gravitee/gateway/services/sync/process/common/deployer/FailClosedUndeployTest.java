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

import static org.assertj.core.api.Assertions.assertThat;

import io.reactivex.rxjava3.core.Completable;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class FailClosedUndeployTest {

    @Test
    void should_distribute_after_a_successful_local_undeploy() {
        AtomicInteger distributed = new AtomicInteger();

        FailClosedUndeploy.undeployThenDistribute(Completable.complete(), Completable.fromRunnable(distributed::incrementAndGet))
            .test()
            .assertComplete();

        assertThat(distributed).hasValue(1);
    }

    @Test
    void should_distribute_and_surface_the_local_failure_when_the_local_undeploy_fails() {
        AtomicInteger distributed = new AtomicInteger();
        RuntimeException localFailure = new RuntimeException("unregister failed");

        FailClosedUndeploy.undeployThenDistribute(Completable.error(localFailure), Completable.fromRunnable(distributed::incrementAndGet))
            .test()
            .assertError(localFailure);

        assertThat(distributed).hasValue(1);
    }

    @Test
    void should_surface_the_distribution_failure_with_the_local_failure_suppressed_when_both_fail() {
        RuntimeException localFailure = new RuntimeException("unregister failed");
        RuntimeException distributionFailure = new RuntimeException("redis down");

        FailClosedUndeploy.undeployThenDistribute(Completable.error(localFailure), Completable.error(distributionFailure))
            .test()
            .assertError(distributionFailure);

        // The distribution failure (transient) must be the surfaced one so the synchronizers classify the
        // undeploy as retryable; the local failure is attached for observability.
        assertThat(distributionFailure.getSuppressed()).containsExactly(localFailure);
    }

    @Test
    void should_surface_the_distribution_failure_after_a_successful_local_undeploy() {
        RuntimeException distributionFailure = new RuntimeException("redis down");

        FailClosedUndeploy.undeployThenDistribute(Completable.complete(), Completable.error(distributionFailure))
            .test()
            .assertError(distributionFailure);
    }
}
