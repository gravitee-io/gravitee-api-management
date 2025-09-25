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
package io.gravitee.rest.api.services.sync;

import static java.util.stream.Collectors.toMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.data.domain.Page;
import io.gravitee.node.api.license.InvalidLicenseException;
import io.gravitee.node.api.license.LicenseFactory;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.node.api.license.MalformedLicenseException;
import io.gravitee.node.license.DefaultLicenseManager;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EventLatestRepository;
import io.gravitee.repository.management.api.LicenseRepository;
import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.api.search.LicenseCriteria;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import io.gravitee.repository.management.model.License;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.converter.ApiConverter;
import io.gravitee.rest.api.service.v4.PrimaryOwnerService;
import io.gravitee.rest.api.service.v4.mapper.ApiMapper;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class SyncManager {

    private static final int TIMEFRAME_BEFORE_DELAY = 10 * 60 * 1000;
    private static final int TIMEFRAME_AFTER_DELAY = 1 * 60 * 1000;
    private final AtomicLong counter = new AtomicLong(0);

    @Autowired
    private DictionaryManager dictionaryManager;

    @Lazy
    @Autowired
    private EventLatestRepository eventLatestRepository;

    @Lazy
    @Autowired
    private LicenseRepository licenseRepository;

    @Autowired
    private ApiManager apiManager;

    @Autowired
    private ApiConverter apiConverter;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    private PrimaryOwnerService primaryOwnerService;

    @Autowired
    private ApiMapper apiMapper;

    @Autowired
    private LicenseManager licenseManager;

    @Autowired
    private LicenseFactory licenseFactory;

    private long lastRefreshAt = -1;

    public void refresh() {
        log.debug("Synchronization #{} started at {}", counter.incrementAndGet(), Instant.now());
        log.debug("Refreshing state...");

        long nextLastRefreshAt = System.currentTimeMillis();

        try {
            synchronizeLicenses(nextLastRefreshAt);
        } catch (Exception ex) {
            log.error("An error occurs while synchronizing licenses", ex);
        }

        try {
            synchronizeApis(nextLastRefreshAt);
        } catch (Exception ex) {
            log.error("An error occurs while synchronizing APIs", ex);
        }

        try {
            synchronizeDictionaries(nextLastRefreshAt);
        } catch (Exception ex) {
            log.error("An error occurs while synchronizing dictionaries", ex);
        }

        lastRefreshAt = nextLastRefreshAt;
        log.debug("Synchronization #{} ended at {}", counter.get(), Instant.now());
    }

    private void synchronizeApis(long nextLastRefreshAt) {
        final EventCriteria eventCriteria = EventCriteria.builder()
            .types(Set.of(EventType.PUBLISH_API, EventType.UNPUBLISH_API, EventType.START_API, EventType.STOP_API))
            .from(lastRefreshAt - TIMEFRAME_BEFORE_DELAY)
            .to(nextLastRefreshAt + TIMEFRAME_AFTER_DELAY)
            .build();

        Map<String, Event> apiEvents = eventLatestRepository
            .search(eventCriteria, Event.EventProperties.API_ID, null, null)
            .stream()
            .collect(toMap(event -> event.getProperties().get(Event.EventProperties.API_ID.getValue()), event -> event));

        // Then, compute events
        computeApiEvents(apiEvents);
    }

    private void synchronizeDictionaries(long nextLastRefreshAt) throws Exception {
        final EventCriteria eventCriteria = EventCriteria.builder()
            .types(Set.of(EventType.START_DICTIONARY, EventType.STOP_DICTIONARY))
            .from(lastRefreshAt - TIMEFRAME_BEFORE_DELAY)
            .to(nextLastRefreshAt + TIMEFRAME_AFTER_DELAY)
            .build();

        Map<String, Event> dictionaryEvents = eventLatestRepository
            .search(eventCriteria, Event.EventProperties.DICTIONARY_ID, null, null)
            .stream()
            .collect(toMap(event -> event.getProperties().get(Event.EventProperties.DICTIONARY_ID.getValue()), event -> event));

        computeDictionaryEvents(dictionaryEvents);
    }

    private void synchronizeLicenses(long nextLastRefreshAt) throws TechnicalException {
        final LicenseCriteria licenseCriteria = LicenseCriteria.builder()
            .referenceType(License.ReferenceType.ORGANIZATION)
            .from(lastRefreshAt - TIMEFRAME_BEFORE_DELAY)
            .to(nextLastRefreshAt + TIMEFRAME_AFTER_DELAY)
            .build();

        // Then, compute events
        Page<License> licenses = licenseRepository.findByCriteria(licenseCriteria, null);
        licenses
            .getContent()
            .forEach(license -> {
                try {
                    var orgLicense = licenseFactory.create("ORGANIZATION", license.getReferenceId(), license.getLicense());
                    licenseManager.registerOrganizationLicense(license.getReferenceId(), orgLicense);
                } catch (Exception e) {
                    log.warn("Organization license cannot be registered for [{}].", license.getReferenceId(), e);
                }
            });
    }

    private void computeDictionaryEvents(Map<String, Event> dictionaryEvents) {
        dictionaryEvents.forEach((id, event) -> {
            switch (event.getType()) {
                case START_DICTIONARY:
                    dictionaryManager.start(id);
                    break;
                case STOP_DICTIONARY:
                    dictionaryManager.stop(id);
                    break;
                default:
                    break;
            }
        });
    }

    private void computeApiEvents(Map<String, Event> apiEvents) {
        final int parallelism = Runtime.getRuntime().availableProcessors() * 2;

        if (apiEvents.size() > parallelism) {
            final ForkJoinPool.ForkJoinWorkerThreadFactory factory = pool -> {
                final ForkJoinWorkerThread worker = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
                worker.setName("gio.sync-" + worker.getPoolIndex());
                return worker;
            };

            ForkJoinPool customThreadPool = new ForkJoinPool(parallelism, factory, null, false);
            customThreadPool.submit(() ->
                apiEvents
                    .entrySet()
                    .parallelStream()
                    .forEach(e -> processApiEvent(e.getKey(), e.getValue()))
            );
            customThreadPool.shutdown();
        } else {
            apiEvents.forEach(this::processApiEvent);
        }
    }

    protected void processApiEvent(String apiId, Event apiEvent) {
        switch (apiEvent.getType()) {
            case UNPUBLISH_API, STOP_API: {
                apiManager.undeploy(apiId);
                break;
            }
            case START_API, PUBLISH_API:
                try {
                    // Read API definition from event
                    Api apiToDeploy = objectMapper.readValue(apiEvent.getPayload(), Api.class);

                    if (apiToDeploy != null) {
                        // Get deployed API
                        Api deployedApi = apiManager.get(apiToDeploy.getId());

                        // API is not yet deployed, so let's do it !
                        if (deployedApi == null) {
                            apiManager.deploy(apiToDeploy);
                        } else {
                            if (deployedApi.getDeployedAt().before(apiToDeploy.getDeployedAt())) {
                                apiManager.update(apiToDeploy);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("Unable to handle event [" + apiEvent.getType() + "]  for API [" + apiId + "]", e);
                }
                break;
            default:
                break;
        }
    }
}
