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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.api;

import static java.util.stream.Collectors.groupingBy;

import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.handlers.api.services.basicauth.BasicAuthCredential;
import io.gravitee.gateway.services.sync.process.common.model.SyncException;
import io.gravitee.gateway.services.sync.process.repository.mapper.BasicAuthCredentialsMapper;
import io.gravitee.repository.management.api.BasicAuthCredentialsRepository;
import io.gravitee.repository.management.api.search.BasicAuthCredentialsCriteria;
import io.gravitee.repository.management.model.BasicAuthCredentials;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

@CustomLog
@RequiredArgsConstructor
public class BasicAuthAppender {

    private final BasicAuthCredentialsRepository basicAuthCredentialsRepository;
    private final BasicAuthCredentialsMapper basicAuthCredentialsMapper;

    public List<ApiReactorDeployable> appends(
        final boolean initialSync,
        final List<ApiReactorDeployable> deployables,
        final Set<String> environments
    ) {
        final Map<String, ApiReactorDeployable> deployableByApi = deployables
            .stream()
            .collect(Collectors.toMap(ApiReactorDeployable::apiId, d -> d));
        List<Subscription> allBasicAuthSubscriptions = deployableByApi
            .values()
            .stream()
            .filter(deployable -> deployable.subscriptions() != null && deployable.basicAuthPlans() != null)
            .flatMap(deployable ->
                deployable
                    .subscriptions()
                    .stream()
                    .filter(subscription -> subscription.getPlan() != null && deployable.basicAuthPlans().contains(subscription.getPlan()))
            )
            .collect(Collectors.toList());
        if (!allBasicAuthSubscriptions.isEmpty()) {
            Map<String, List<BasicAuthCredential>> credentialsByApi = loadBasicAuthCredentials(
                initialSync,
                allBasicAuthSubscriptions,
                environments
            );
            credentialsByApi.forEach((api, credentials) -> {
                ApiReactorDeployable deployable = deployableByApi.get(api);
                deployable.basicAuthCredentials(credentials);
            });
        }
        return deployables;
    }

    private Map<String, List<BasicAuthCredential>> loadBasicAuthCredentials(
        final boolean initialSync,
        final List<Subscription> subscriptions,
        final Set<String> environments
    ) {
        try {
            Map<String, List<Subscription>> subscriptionsByIdMulti = subscriptions
                .stream()
                .collect(Collectors.groupingBy(Subscription::getId));

            BasicAuthCredentialsCriteria.BasicAuthCredentialsCriteriaBuilder criteriaBuilder = BasicAuthCredentialsCriteria.builder()
                .subscriptions(subscriptionsByIdMulti.keySet())
                .environments(environments);
            if (initialSync) {
                criteriaBuilder.includeRevoked(false);
            } else {
                criteriaBuilder.includeRevoked(true);
            }
            List<BasicAuthCredentials> bySubscriptions = basicAuthCredentialsRepository.findByCriteria(criteriaBuilder.build());
            return bySubscriptions
                .stream()
                .flatMap(cred ->
                    cred
                        .getSubscriptions()
                        .stream()
                        .flatMap(subscriptionId -> {
                            List<Subscription> subsForId = subscriptionsByIdMulti.get(subscriptionId);
                            if (subsForId == null || subsForId.isEmpty()) {
                                return java.util.stream.Stream.empty();
                            }
                            return subsForId.stream().map(subscription -> basicAuthCredentialsMapper.to(cred, subscription));
                        })
                )
                .collect(groupingBy(BasicAuthCredential::getApi));
        } catch (Exception ex) {
            throw new SyncException("Error occurred when retrieving Basic Auth credentials", ex);
        }
    }
}
