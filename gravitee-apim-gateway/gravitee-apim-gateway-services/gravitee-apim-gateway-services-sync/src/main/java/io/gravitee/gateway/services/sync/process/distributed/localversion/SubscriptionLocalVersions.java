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
package io.gravitee.gateway.services.sync.process.distributed.localversion;

import io.gravitee.repository.management.model.Subscription;
import io.reactivex.rxjava3.core.Single;
import java.util.List;

/**
 * The subscription local versions of one load, read on the first page that needs them and reused for the next pages.
 *
 * @author GraviteeSource Team
 */
@FunctionalInterface
public interface SubscriptionLocalVersions {
    /**
     * Drops the control plane subscriptions that have a local version, unless the control plane changed them after the local change.
     * Those replace the local version.
     */
    Single<List<Subscription>> resolve(List<Subscription> subscriptions);
}
