/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.services.sync.synchronizer;

import static io.gravitee.gateway.services.sync.spring.SyncConfiguration.PARALLELISM;
import static io.gravitee.repository.management.model.Event.EventProperties.ORGANIZATION_ID;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.Organization;
import io.gravitee.definition.model.flow.Consumer;
import io.gravitee.definition.model.flow.ConsumerType;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.gateway.dictionary.DictionaryManager;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.platform.manager.OrganizationManager;
import io.gravitee.repository.management.api.OrganizationRepository;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.annotations.NonNull;
import io.reactivex.schedulers.Schedulers;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class OrganizationSynchronizer extends AbstractSynchronizer {

    private final Logger logger = LoggerFactory.getLogger(OrganizationSynchronizer.class);

    @Autowired
    private OrganizationManager organizationManager;

    @Autowired
    private GatewayConfiguration gatewayConfiguration;

    @Autowired
    private ObjectMapper objectMapper;

    public void synchronize(Long lastRefreshAt, Long nextLastRefreshAt, List<String> environments) {
        final long start = System.currentTimeMillis();
        final Long count;

        Map<String, Event> organizationEvents;
        if (lastRefreshAt == -1) {
            count = initialSynchronizeOrganizations(nextLastRefreshAt, environments);
        } else {
            count =
                this.searchLatestEvents(
                        lastRefreshAt,
                        nextLastRefreshAt,
                        true,
                        ORGANIZATION_ID,
                        environments,
                        EventType.PUBLISH_ORGANIZATION
                    )
                    .compose(this::processOrganizationEvents)
                    .count()
                    .blockingGet();
        }

        if (lastRefreshAt == -1) {
            logger.info("{} organization(s) synchronized in {}ms.", count, (System.currentTimeMillis() - start));
        } else {
            logger.debug("{} organization(s) synchronized in {}ms.", count, (System.currentTimeMillis() - start));
        }
    }

    private long initialSynchronizeOrganizations(long nextLastRefreshAt, List<String> environments) {
        return this.searchLatestEvents(null, nextLastRefreshAt, true, ORGANIZATION_ID, environments, EventType.PUBLISH_ORGANIZATION)
            .compose(this::processOrganizationEvents)
            .count()
            .blockingGet();
    }

    private Flowable<String> processOrganizationEvents(Flowable<Event> upstream) {
        return upstream.flatMapMaybe(this::toOrganization).compose(this::deployOrganization);
    }

    private Maybe<io.gravitee.definition.model.Organization> toOrganization(Event event) {
        try {
            // Read organization definition from event
            final Organization organization = objectMapper.readValue(event.getPayload(), Organization.class);
            organization.setUpdatedAt(event.getUpdatedAt());
            return Maybe.just(organization);
        } catch (IOException ioe) {
            logger.error("Error while determining deployed organization into events payload", ioe);
        }

        return Maybe.empty();
    }

    @NonNull
    private Flowable<String> deployOrganization(Flowable<io.gravitee.definition.model.Organization> upstream) {
        return upstream
            .parallel(PARALLELISM)
            .runOn(Schedulers.from(executor))
            .doOnNext(
                organization -> {
                    try {
                        List<String> shardingTags = gatewayConfiguration.shardingTags().orElse(null);
                        if (shardingTags != null && !shardingTags.isEmpty()) {
                            List<Flow> filteredFlows = organization
                                .getFlows()
                                .stream()
                                .filter(
                                    flow -> {
                                        List<Consumer> consumers = flow.getConsumers();
                                        if (consumers != null && !consumers.isEmpty()) {
                                            Set<String> flowTags = consumers
                                                .stream()
                                                .filter((consumer -> consumer.getConsumerType().equals(ConsumerType.TAG)))
                                                .map(consumer -> consumer.getConsumerId())
                                                .collect(Collectors.toSet());
                                            return gatewayConfiguration.hasMatchingTags(flowTags);
                                        }
                                        return true;
                                    }
                                )
                                .collect(Collectors.toList());

                            organization.setFlows(filteredFlows);
                        }

                        // Update definition with required information for deployment phase
                        final io.gravitee.gateway.platform.Organization organizationPlatform = new io.gravitee.gateway.platform.Organization(
                            organization
                        );
                        organizationPlatform.setUpdatedAt(organization.getUpdatedAt());
                        organizationManager.register(organizationPlatform);
                    } catch (Exception e) {
                        logger.error(
                            "An error occurred when trying to deploy organization {} [{}].",
                            organization.getName(),
                            organization.getId()
                        );
                    }
                }
            )
            .sequential()
            .map(io.gravitee.definition.model.Organization::getId);
    }
}
