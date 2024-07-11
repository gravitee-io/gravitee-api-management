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
package io.gravitee.apim.core.integration.service_provider;

import io.gravitee.apim.core.integration.model.IngestStarted;
import io.gravitee.apim.core.integration.model.Integration;
import io.gravitee.apim.core.integration.model.IntegrationApi;
import io.gravitee.apim.core.integration.model.IntegrationSubscription;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.definition.model.federation.FederatedApi;
import io.gravitee.definition.model.federation.SubscriptionParameter;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

public interface IntegrationAgent {
    /**
     * Returns the status of the Agent for an integration
     * @param integrationId The integration id
     * @return {@link Status} the Agent's status of the integration
     */
    Single<Status> getAgentStatusFor(String integrationId);

    Single<IngestStarted> startIngest(String integrationId, String ingestJobId);

    Flowable<IntegrationApi> fetchAllApis(Integration integration);

    /**
     * Send Subscription command to Agent.
     * @param integrationId The Integration id.
     * @param api The Federated API to subscribe.
     * @param subscriptionParameter The Federated Plan to subscribe.
     * @param subscriptionId The Subscription id.
     * @param application The Application that subscribes to the API.
     * @return {String} The API Key created
     */
    Single<IntegrationSubscription> subscribe(
        String integrationId,
        FederatedApi api,
        SubscriptionParameter subscriptionParameter,
        String subscriptionId,
        BaseApplicationEntity application
    );

    /**
     * Send Unsubscribe command
     *
     * @param integrationId The integartion id.
     * @param api           The Federated API.
     * @param subscription  The subscription to close.
     * @return {Completable}
     */
    Completable unsubscribe(String integrationId, FederatedApi api, SubscriptionEntity subscription);

    Flowable<IntegrationApi> discoverApis(String integrationId);

    enum Status {
        CONNECTED,
        DISCONNECTED,
    }
}
