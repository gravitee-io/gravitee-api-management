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
package io.gravitee.rest.api.service.impl.upgrade;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.DictionaryRepository;
import io.gravitee.repository.management.api.EventLatestRepository;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.api.OrganizationRepository;
import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.model.Dictionary;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.Organization;
import io.gravitee.rest.api.service.InstallationService;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
@Slf4j
public class EventsLatestUpgrader extends OneShotUpgrader {

    private static final int BULK_SIZE = 2;
    private final ApiRepository apiRepository;
    private final DictionaryRepository dictionaryRepository;
    private final OrganizationRepository organizationRepository;
    private final EventRepository eventRepository;
    private final EventLatestRepository eventLatestRepository;

    @Autowired
    public EventsLatestUpgrader(
        @Lazy ApiRepository apiRepository,
        @Lazy DictionaryRepository dictionaryRepository,
        @Lazy OrganizationRepository organizationRepository,
        @Lazy EventRepository eventRepository,
        @Lazy EventLatestRepository eventLatestRepository
    ) {
        super(InstallationService.EVENTS_LATEST_UPGRADER_STATUS);
        this.apiRepository = apiRepository;
        this.dictionaryRepository = dictionaryRepository;
        this.organizationRepository = organizationRepository;
        this.eventRepository = eventRepository;
        this.eventLatestRepository = eventLatestRepository;
    }

    @Override
    public int getOrder() {
        return 502;
    }

    @Override
    protected void processOneShotUpgrade() throws TechnicalException {
        migrateApiEvents();
        migrateDictionaryEvents();
        migrateOrganizationEvents();
    }

    private void migrateApiEvents() throws TechnicalException {
        log.info("Starting migrating latest event for APIs");
        Page<String> firstPage =
            this.apiRepository.searchIds(List.of(), new PageableBuilder().pageNumber(0).pageSize(BULK_SIZE).build(), null);
        migrateEvents(Event.EventProperties.API_ID.getValue(), firstPage.getContent());

        // Cover others pages if required
        if (firstPage.getTotalElements() > 0) {
            long pageNumber = (int) Math.ceil((double) firstPage.getTotalElements() / BULK_SIZE);
            for (int pageIdx = 0; pageIdx < pageNumber; pageIdx++) {
                Page<String> nextPage =
                    this.apiRepository.searchIds(List.of(), new PageableBuilder().pageNumber(pageIdx).pageSize(BULK_SIZE).build(), null);
                migrateEvents(Event.EventProperties.API_ID.getValue(), nextPage.getContent());
            }
        }
    }

    private void migrateDictionaryEvents() throws TechnicalException {
        log.info("Starting migrating latest event for dictionaries");
        Set<Dictionary> dictionaries = this.dictionaryRepository.findAll();
        List<String> ids = dictionaries.stream().map(Dictionary::getId).collect(Collectors.toList());
        migrateEvents(Event.EventProperties.DICTIONARY_ID.getValue(), ids);
    }

    private void migrateOrganizationEvents() throws TechnicalException {
        log.info("Starting migrating latest event for organizations");
        Set<Organization> organizations = this.organizationRepository.findAll();
        List<String> ids = organizations.stream().map(Organization::getId).collect(Collectors.toList());
        migrateEvents(Event.EventProperties.ORGANIZATION_ID.getValue(), ids);
    }

    private void migrateEvents(final String propertyId, final List<String> ids) throws TechnicalException {
        for (String id : ids) {
            Page<Event> search =
                this.eventRepository.search(
                        new EventCriteria.Builder().property(propertyId, id).build(),
                        new PageableBuilder().pageNumber(0).pageSize(1).build()
                    );
            if (search.getPageElements() > 0) {
                Event event = search.getContent().get(0);
                event.getProperties().put(Event.EventProperties.ID.getValue(), event.getId());
                event.setId(id);
                this.eventLatestRepository.createOrPatch(event);
            }
        }
    }
}
