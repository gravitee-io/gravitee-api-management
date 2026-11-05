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
package io.gravitee.gateway.services.sync.process.distributed.service;

import io.gravitee.gateway.services.sync.process.repository.synchronizer.accesspoint.AccessPointDeployable;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.api.ApiReactorDeployable;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.apikey.SingleApiKeyDeployable;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.apiproduct.ApiProductReactorDeployable;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.authz.AuthzEntityReactorDeployable;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.authz.AuthzPolicyReactorDeployable;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.credential.CredentialDeployable;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.dictionary.DictionaryDeployable;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.license.LicenseDeployable;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.node.NodeMetadataDeployable;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.organization.OrganizationDeployable;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.sharedpolicygroup.SharedPolicyGroupReactorDeployable;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.subscription.SingleSubscriptionDeployable;
import io.gravitee.repository.distributedsync.model.DistributedSyncState;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import java.util.Optional;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface DistributedSyncService {
    void validate();

    boolean isEnabled();

    boolean isPrimaryNode();

    Completable ready();

    Maybe<DistributedSyncState> state();

    Completable storeState(final long fromTime, final long toTime);

    /**
     * Samples the node role once for a sync cycle and pins it until {@link #endCycle()}, so every
     * {@link #isPrimaryNode()} call made during the cycle sees the same role even if the cluster elects a new
     * primary meanwhile.
     *
     * @return whether this node acts as primary for the cycle
     */
    default boolean beginCycle() {
        return isPrimaryNode();
    }

    default void endCycle() {}

    /**
     * Whether the current cycle must re-distribute entities that are already deployed locally (the
     * {@code ActionOnApi.NONE} case): set when a previous distribution failed and its window is replayed, or
     * when this node was just promoted and must re-publish what the previous primary may have missed.
     */
    default boolean republishRequired() {
        return false;
    }

    /**
     * Requests that the current (or next) primary cycle re-distributes the already-deployed entities found in
     * its window.
     */
    default void requestReplay() {}

    /**
     * Forgets any pending replay: called when this node stops acting as primary.
     */
    default void onDemotion() {}

    default Optional<DistributedSyncMetrics> metrics() {
        return Optional.empty();
    }

    /**
     * The state published by the primary, read whatever the role of this node.
     */
    default Maybe<DistributedSyncState> sharedState() {
        return Maybe.empty();
    }

    /**
     * Whether every distributed event is visible to searches: a node synchronizing from scratch must not trust a
     * search structure still being rebuilt.
     */
    default Single<Boolean> catalogFullyIndexed() {
        return Single.just(true);
    }

    /**
     * Requests a rebuild of the whole distributed catalog by the primary: full republish, then removal of the
     * entries that are no longer live.
     */
    default void requestResync(final String reason) {}

    default boolean resyncInProgress() {
        return false;
    }

    /**
     * Called at the beginning of a primary cycle.
     *
     * @return whether this cycle rebuilds the catalog, i.e. must run as a full synchronization
     */
    default Single<Boolean> preparePrimaryCycle() {
        return Single.just(false);
    }

    /**
     * An entity could not be deployed locally by the primary: a catalog rebuild running in the same cycle must not
     * remove its previous entry.
     */
    default void recordLocalDeployFailure() {}

    /**
     * Records that the primary deployed an entity locally, so the cycle can compare what was deployed with what
     * was actually distributed (see {@link DistributedSyncMetrics#localDeploys()}). No-op when distributed sync is
     * disabled: nothing must be distributed.
     */
    default void recordLocalDeploy() {}

    Completable distributeIfNeeded(final ApiReactorDeployable deployable);

    Completable distributeIfNeeded(final SingleSubscriptionDeployable deployable);

    Completable distributeIfNeeded(final SingleApiKeyDeployable deployable);

    Completable distributeIfNeeded(final OrganizationDeployable deployable);

    Completable distributeIfNeeded(final DictionaryDeployable deployable);

    Completable distributeIfNeeded(final LicenseDeployable deployable);

    Completable distributeIfNeeded(final AccessPointDeployable deployable);

    Completable distributeIfNeeded(final SharedPolicyGroupReactorDeployable deployable);

    Completable distributeIfNeeded(final ApiProductReactorDeployable deployable);

    Completable distributeIfNeeded(final NodeMetadataDeployable deployable);

    Completable distributeIfNeeded(final AuthzEntityReactorDeployable deployable);

    Completable distributeIfNeeded(final AuthzPolicyReactorDeployable deployable);

    Completable distributeIfNeeded(final CredentialDeployable deployable);
}
