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
import io.gravitee.gateway.services.sync.process.repository.synchronizer.dictionary.DictionaryDeployable;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.license.LicenseDeployable;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.organization.OrganizationDeployable;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.sharedpolicygroup.SharedPolicyGroupReactorDeployable;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.subscription.SingleSubscriptionDeployable;
import io.gravitee.repository.distributedsync.model.DistributedSyncState;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;

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

    Completable distributeIfNeeded(final ApiReactorDeployable deployable);

    Completable distributeIfNeeded(final SingleSubscriptionDeployable deployable);

    Completable distributeIfNeeded(final SingleApiKeyDeployable deployable);

    Completable distributeIfNeeded(final OrganizationDeployable deployable);

    Completable distributeIfNeeded(final DictionaryDeployable deployable);

    Completable distributeIfNeeded(final LicenseDeployable deployable);

    Completable distributeIfNeeded(final AccessPointDeployable deployable);

    Completable distributeIfNeeded(final SharedPolicyGroupReactorDeployable deployable);
}
