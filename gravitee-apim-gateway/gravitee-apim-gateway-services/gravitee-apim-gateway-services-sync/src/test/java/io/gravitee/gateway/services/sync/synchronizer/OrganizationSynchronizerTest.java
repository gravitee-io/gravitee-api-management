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

import static java.util.Collections.singletonList;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.Organization;
import io.gravitee.gateway.dictionary.DictionaryManager;
import io.gravitee.gateway.dictionary.model.Dictionary;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.platform.manager.OrganizationManager;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class OrganizationSynchronizerTest extends TestCase {

    @InjectMocks
    private OrganizationSynchronizer organizationSynchronizer = new OrganizationSynchronizer();

    @Mock
    private EventRepository eventRepository;

    @Mock
    private OrganizationManager organizationManager;

    @Mock
    private GatewayConfiguration gatewayConfiguration;

    @Mock
    private ObjectMapper objectMapper;

    static final List<String> ENVIRONMENTS = Arrays.asList("DEFAULT", "OTHER_ENV");
    static final String ORGANISATION_TEST = "organisation-test";
    static final String ORGANISATION_TEST_2 = "organisation-test_2";

    @Before
    public void setUp() {
        organizationSynchronizer.setExecutor(Executors.newFixedThreadPool(1));
    }

    @Test
    public void initialSynchronize() throws Exception {
        Organization organization = new Organization();
        organization.setId(ORGANISATION_TEST);

        final Event mockEvent = mockEvent(organization, EventType.PUBLISH_ORGANIZATION);
        when(
            eventRepository.searchLatest(
                argThat(
                    criteria ->
                        criteria != null &&
                        criteria.getTypes().containsAll(Arrays.asList(EventType.PUBLISH_ORGANIZATION)) &&
                        criteria.getEnvironments().containsAll(ENVIRONMENTS)
                ),
                eq(Event.EventProperties.ORGANIZATION_ID),
                anyLong(),
                anyLong()
            )
        )
            .thenReturn(singletonList(mockEvent));

        organizationSynchronizer.synchronize(-1L, System.currentTimeMillis(), ENVIRONMENTS);

        verify(organizationManager).register(argThat(organizationPlatform -> organizationPlatform.getId().equals(ORGANISATION_TEST)));
        verify(organizationManager, never()).unregister(anyString());
    }

    @Test
    public void publish() throws Exception {
        Organization organization = new Organization();
        organization.setId(ORGANISATION_TEST);

        final Event mockEvent = mockEvent(organization, EventType.PUBLISH_ORGANIZATION);
        when(
            eventRepository.searchLatest(
                argThat(
                    criteria ->
                        criteria != null &&
                        criteria.getTypes().containsAll(Arrays.asList(EventType.PUBLISH_ORGANIZATION)) &&
                        criteria.getEnvironments().containsAll(ENVIRONMENTS)
                ),
                eq(Event.EventProperties.ORGANIZATION_ID),
                anyLong(),
                anyLong()
            )
        )
            .thenReturn(singletonList(mockEvent));

        organizationSynchronizer.synchronize(System.currentTimeMillis() - 5000, System.currentTimeMillis(), ENVIRONMENTS);

        verify(organizationManager).register(argThat(organizationPlatform -> organizationPlatform.getId().equals(ORGANISATION_TEST)));
        verify(organizationManager, never()).unregister(anyString());
    }

    @Test
    public void publishWithPagination() throws Exception {
        Organization organization = new Organization();
        organization.setId(ORGANISATION_TEST);

        Organization organization2 = new Organization();
        organization2.setId(ORGANISATION_TEST_2);

        // Force bulk size to 1.
        organizationSynchronizer.bulkItems = 1;

        final Event mockEvent = mockEvent(organization, EventType.PUBLISH_ORGANIZATION);
        final Event mockEvent2 = mockEvent(organization2, EventType.PUBLISH_ORGANIZATION);
        when(
            eventRepository.searchLatest(
                argThat(
                    criteria ->
                        criteria != null &&
                        criteria.getTypes().containsAll(Arrays.asList(EventType.PUBLISH_ORGANIZATION)) &&
                        criteria.getEnvironments().containsAll(ENVIRONMENTS)
                ),
                eq(Event.EventProperties.ORGANIZATION_ID),
                eq(0L),
                eq(1L)
            )
        )
            .thenReturn(singletonList(mockEvent));

        when(
            eventRepository.searchLatest(
                argThat(
                    criteria ->
                        criteria != null &&
                        criteria.getTypes().containsAll(Arrays.asList(EventType.PUBLISH_ORGANIZATION)) &&
                        criteria.getEnvironments().containsAll(ENVIRONMENTS)
                ),
                eq(Event.EventProperties.ORGANIZATION_ID),
                eq(1L),
                eq(1L)
            )
        )
            .thenReturn(singletonList(mockEvent2));

        organizationSynchronizer.synchronize(System.currentTimeMillis() - 5000, System.currentTimeMillis(), ENVIRONMENTS);

        verify(organizationManager, times(1))
            .register(argThat(organizationPlatform -> organizationPlatform.getId().equals(ORGANISATION_TEST)));
        verify(organizationManager, times(1))
            .register(argThat(organizationPlatform -> organizationPlatform.getId().equals(ORGANISATION_TEST_2)));
        verify(organizationManager, never()).unregister(anyString());
    }

    @Test
    public void synchronizeWithLotOfOrganizationEvents() throws Exception {
        long page = 0;
        List<Event> eventAccumulator = new ArrayList<>(100);

        for (int i = 1; i <= 500; i++) {
            Organization organization = new Organization();
            organization.setId("dictionary" + i + "-test");

            eventAccumulator.add(mockEvent(organization, EventType.PUBLISH_ORGANIZATION));

            if (i % 100 == 0) {
                when(
                    eventRepository.searchLatest(
                        argThat(
                            criteria ->
                                criteria != null &&
                                criteria.getTypes().containsAll(Arrays.asList(EventType.PUBLISH_ORGANIZATION)) &&
                                criteria.getEnvironments().containsAll(ENVIRONMENTS)
                        ),
                        eq(Event.EventProperties.ORGANIZATION_ID),
                        eq(page),
                        eq(100L)
                    )
                )
                    .thenReturn(eventAccumulator);

                page++;
                eventAccumulator = new ArrayList<>();
            }
        }

        organizationSynchronizer.synchronize(System.currentTimeMillis() - 5000, System.currentTimeMillis(), ENVIRONMENTS);

        verify(organizationManager, times(500)).register(any(io.gravitee.gateway.platform.Organization.class));
        verify(organizationManager, never()).unregister(anyString());
    }

    private Event mockEvent(final Organization organization, EventType eventType) throws Exception {
        Map<String, String> properties = new HashMap<>();
        properties.put(Event.EventProperties.ORGANIZATION_ID.getValue(), organization.getId());

        Event event = new Event();
        event.setType(eventType);
        event.setCreatedAt(new Date());
        event.setProperties(properties);
        event.setPayload(organization.getId());

        when(objectMapper.readValue(event.getPayload(), Organization.class)).thenReturn(organization);

        return event;
    }
}
