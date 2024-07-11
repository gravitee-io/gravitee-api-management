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
package inmemory;

import io.gravitee.apim.core.integration.exception.IntegrationIngestionException;
import io.gravitee.apim.core.integration.model.IngestStarted;
import io.gravitee.apim.core.integration.model.Integration;
import io.gravitee.apim.core.integration.model.IntegrationApi;
import io.gravitee.apim.core.integration.model.IntegrationSubscription;
import io.gravitee.apim.core.integration.service_provider.IntegrationAgent;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.definition.model.federation.FederatedApi;
import io.gravitee.definition.model.federation.SubscriptionParameter;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IntegrationAgentInMemory implements IntegrationAgent, InMemoryAlternative<IntegrationApi> {

    List<IntegrationApi> storage = new ArrayList<>();
    Map<String, Long> apisNumberToIngest = new HashMap<>();
    Map<String, Status> statuses = new HashMap<>();
    Map<String, List<String>> subscriptions = new HashMap<>();
    Map<String, List<SubscriptionEntity>> closedSubscriptions = new HashMap<>();

    @Override
    public Single<Status> getAgentStatusFor(String integrationId) {
        return Single.just(statuses.getOrDefault(integrationId, Status.CONNECTED));
    }

    @Override
    public Single<IngestStarted> startIngest(String integrationId, String ingestJobId) {
        var total = apisNumberToIngest.getOrDefault(integrationId, null);

        if (total != null) {
            return Single.just(new IngestStarted(ingestJobId, total));
        }
        return Single.error(new RuntimeException("job fail to start"));
    }

    @Override
    public Flowable<IntegrationApi> fetchAllApis(Integration integration) {
        return Flowable.fromIterable(storage).filter(asset -> asset.integrationId().equals(integration.getId()));
    }

    @Override
    public Single<IntegrationSubscription> subscribe(
        String integrationId,
        FederatedApi api,
        SubscriptionParameter subscriptionParameter,
        String subscriptionId,
        BaseApplicationEntity application
    ) {
        subscriptions.compute(
            integrationId,
            (integration, subscriptionIds) -> {
                if (subscriptionIds == null) {
                    subscriptionIds = new ArrayList<>();
                }
                subscriptionIds.add(subscriptionId);
                return subscriptionIds;
            }
        );
        IntegrationSubscription.Type type;
        if (subscriptionParameter instanceof SubscriptionParameter.ApiKey) {
            type = IntegrationSubscription.Type.API_KEY;
        } else if (subscriptionParameter instanceof SubscriptionParameter.OAuth) {
            type = IntegrationSubscription.Type.OAUTH2;
        } else {
            throw new IntegrationIngestionException("Unsupported subscription parameter: " + subscriptionParameter);
        }
        return Single.just(
            new IntegrationSubscription(
                integrationId,
                type,
                String.join("-", type.name(), subscriptionId, application.getId(), application.getName()),
                Map.of("key", "value")
            )
        );
    }

    @Override
    public Completable unsubscribe(String integrationId, FederatedApi api, SubscriptionEntity subscription) {
        return Completable.fromRunnable(() -> {
            var subscriptions = closedSubscriptions.computeIfAbsent(integrationId, value -> new ArrayList<>());
            subscriptions.add(subscription);
        });
    }

    @Override
    public Flowable<IntegrationApi> discoverApis(String integrationId) {
        return Flowable.fromIterable(storage).filter(asset -> asset.integrationId().equals(integrationId));
    }

    @Override
    public void initWith(List<IntegrationApi> items) {
        this.storage.addAll(items);
    }

    @Override
    public void reset() {
        this.storage.clear();
        this.statuses.clear();
    }

    @Override
    public List<IntegrationApi> storage() {
        return Collections.unmodifiableList(storage);
    }

    public List<SubscriptionEntity> closedSubscriptions(String integrationId) {
        return closedSubscriptions.get(integrationId);
    }

    public void configureAgentFor(String integrationId, Status status) {
        statuses.put(integrationId, status);
    }

    public void configureApisNumberToIngest(String integrationId, Long total) {
        apisNumberToIngest.put(integrationId, total);
    }
}
