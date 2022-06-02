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
import io.gravitee.gateway.dictionary.DictionaryManager;
import io.gravitee.gateway.dictionary.model.Dictionary;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import java.util.*;
import java.util.concurrent.Executors;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class DictionarySynchronizerTest extends TestCase {

    @InjectMocks
    private DictionarySynchronizer dictionarySynchronizer = new DictionarySynchronizer();

    @Mock
    private EventRepository eventRepository;

    @Mock
    private DictionaryManager dictionaryManager;

    @Mock
    private ObjectMapper objectMapper;

    static final List<String> ENVIRONMENTS = Arrays.asList("DEFAULT", "OTHER_ENV");

    @Before
    public void setUp() {
        dictionarySynchronizer.setExecutor(Executors.newFixedThreadPool(1));
    }

    @Test
    public void initialSynchronize() throws Exception {
        Dictionary dictionary = new Dictionary();
        dictionary.setId("dictionary-test");

        final Event mockEvent = mockEvent(dictionary, EventType.PUBLISH_DICTIONARY);
        when(
            eventRepository.searchLatest(
                argThat(
                    criteria ->
                        criteria != null &&
                        criteria.getTypes().containsAll(Arrays.asList(EventType.PUBLISH_DICTIONARY, EventType.UNPUBLISH_DICTIONARY)) &&
                        criteria.getEnvironments().containsAll(ENVIRONMENTS)
                ),
                eq(Event.EventProperties.DICTIONARY_ID),
                anyLong(),
                anyLong()
            )
        )
            .thenReturn(singletonList(mockEvent));

        dictionarySynchronizer.synchronize(-1L, System.currentTimeMillis(), ENVIRONMENTS);

        verify(dictionaryManager).deploy((dictionary));
        verify(dictionaryManager, never()).undeploy(any(String.class));
    }

    @Test
    public void publish() throws Exception {
        Dictionary dictionary = new Dictionary();
        dictionary.setId("dictionary-test");

        final Event mockEvent = mockEvent(dictionary, EventType.PUBLISH_DICTIONARY);
        when(
            eventRepository.searchLatest(
                argThat(
                    criteria ->
                        criteria != null &&
                        criteria.getTypes().containsAll(Arrays.asList(EventType.PUBLISH_DICTIONARY, EventType.UNPUBLISH_DICTIONARY)) &&
                        criteria.getEnvironments().containsAll(ENVIRONMENTS)
                ),
                eq(Event.EventProperties.DICTIONARY_ID),
                anyLong(),
                anyLong()
            )
        )
            .thenReturn(singletonList(mockEvent));

        dictionarySynchronizer.synchronize(System.currentTimeMillis() - 5000, System.currentTimeMillis(), ENVIRONMENTS);

        verify(dictionaryManager).deploy(dictionary);
        verify(dictionaryManager, never()).undeploy(any(String.class));
    }

    @Test
    public void publishWithPagination() throws Exception {
        Dictionary dictionary = new Dictionary();
        dictionary.setId("dictionary-test");

        Dictionary dictionary2 = new Dictionary();
        dictionary2.setId("dictionary2-test");

        // Force bulk size to 1.
        dictionarySynchronizer.bulkItems = 1;

        final Event mockEvent = mockEvent(dictionary, EventType.PUBLISH_DICTIONARY);
        final Event mockEvent2 = mockEvent(dictionary2, EventType.PUBLISH_DICTIONARY);
        when(
            eventRepository.searchLatest(
                argThat(
                    criteria ->
                        criteria != null &&
                        criteria.getTypes().containsAll(Arrays.asList(EventType.PUBLISH_DICTIONARY, EventType.UNPUBLISH_DICTIONARY)) &&
                        criteria.getEnvironments().containsAll(ENVIRONMENTS)
                ),
                eq(Event.EventProperties.DICTIONARY_ID),
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
                        criteria.getTypes().containsAll(Arrays.asList(EventType.PUBLISH_DICTIONARY, EventType.UNPUBLISH_DICTIONARY)) &&
                        criteria.getEnvironments().containsAll(ENVIRONMENTS)
                ),
                eq(Event.EventProperties.DICTIONARY_ID),
                eq(1L),
                eq(1L)
            )
        )
            .thenReturn(singletonList(mockEvent2));

        dictionarySynchronizer.synchronize(System.currentTimeMillis() - 5000, System.currentTimeMillis(), ENVIRONMENTS);

        verify(dictionaryManager, times(1)).deploy(dictionary);
        verify(dictionaryManager, times(1)).deploy(dictionary2);
        verify(dictionaryManager, never()).undeploy(any(String.class));
    }

    @Test
    public void unpublish() throws Exception {
        Dictionary dictionary = new Dictionary();
        dictionary.setId("dictionary-test");

        final Event mockEvent = mockEvent(dictionary, EventType.UNPUBLISH_DICTIONARY);
        when(
            eventRepository.searchLatest(
                argThat(
                    criteria ->
                        criteria != null &&
                        criteria.getTypes().containsAll(Arrays.asList(EventType.PUBLISH_DICTIONARY, EventType.UNPUBLISH_DICTIONARY)) &&
                        criteria.getEnvironments().containsAll(ENVIRONMENTS)
                ),
                eq(Event.EventProperties.DICTIONARY_ID),
                anyLong(),
                anyLong()
            )
        )
            .thenReturn(singletonList(mockEvent));

        dictionarySynchronizer.synchronize(System.currentTimeMillis() - 5000, System.currentTimeMillis(), ENVIRONMENTS);

        verify(dictionaryManager, never()).deploy(dictionary);
        verify(dictionaryManager).undeploy(dictionary.getId());
    }

    @Test
    public void unpublishWithPagination() throws Exception {
        Dictionary dictionary = new Dictionary();
        dictionary.setId("dictionary-test");

        Dictionary dictionary2 = new Dictionary();
        dictionary2.setId("dictionary2-test");

        // Force bulk size to 1.
        dictionarySynchronizer.bulkItems = 1;

        final Event mockEvent = mockEvent(dictionary, EventType.UNPUBLISH_DICTIONARY);
        final Event mockEvent2 = mockEvent(dictionary2, EventType.UNPUBLISH_DICTIONARY);
        when(
            eventRepository.searchLatest(
                argThat(
                    criteria ->
                        criteria != null &&
                        criteria.getTypes().containsAll(Arrays.asList(EventType.PUBLISH_DICTIONARY, EventType.UNPUBLISH_DICTIONARY)) &&
                        criteria.getEnvironments().containsAll(ENVIRONMENTS)
                ),
                eq(Event.EventProperties.DICTIONARY_ID),
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
                        criteria.getTypes().containsAll(Arrays.asList(EventType.PUBLISH_DICTIONARY, EventType.UNPUBLISH_DICTIONARY)) &&
                        criteria.getEnvironments().containsAll(ENVIRONMENTS)
                ),
                eq(Event.EventProperties.DICTIONARY_ID),
                eq(1L),
                eq(1L)
            )
        )
            .thenReturn(singletonList(mockEvent2));

        dictionarySynchronizer.synchronize(System.currentTimeMillis() - 5000, System.currentTimeMillis(), ENVIRONMENTS);

        verify(dictionaryManager, never()).deploy(dictionary);
        verify(dictionaryManager).undeploy(dictionary.getId());
        verify(dictionaryManager).undeploy(dictionary2.getId());
    }

    @Test
    public void synchronizeWithLotOfDictionaryEvents() throws Exception {
        long page = 0;
        List<Event> eventAccumulator = new ArrayList<>(100);

        for (int i = 1; i <= 500; i++) {
            Dictionary dictionary = new Dictionary();
            dictionary.setId("dictionary" + i + "-test");

            if (i % 2 == 0) {
                eventAccumulator.add(mockEvent(dictionary, EventType.PUBLISH_DICTIONARY));
            } else {
                eventAccumulator.add(mockEvent(dictionary, EventType.UNPUBLISH_DICTIONARY));
            }

            if (i % 100 == 0) {
                when(
                    eventRepository.searchLatest(
                        argThat(
                            criteria ->
                                criteria != null &&
                                criteria
                                    .getTypes()
                                    .containsAll(Arrays.asList(EventType.PUBLISH_DICTIONARY, EventType.UNPUBLISH_DICTIONARY)) &&
                                criteria.getEnvironments().containsAll(ENVIRONMENTS)
                        ),
                        eq(Event.EventProperties.DICTIONARY_ID),
                        eq(page),
                        eq(100L)
                    )
                )
                    .thenReturn(eventAccumulator);

                page++;
                eventAccumulator = new ArrayList<>();
            }
        }

        dictionarySynchronizer.synchronize(System.currentTimeMillis() - 5000, System.currentTimeMillis(), ENVIRONMENTS);

        verify(dictionaryManager, times(250)).deploy(any(Dictionary.class));
        verify(dictionaryManager, times(250)).undeploy(anyString());
    }

    @Test
    public void shouldNotDeployIfProblemWhileReadingFromEvent() throws Exception {
        Dictionary dictionary = new Dictionary();
        dictionary.setId("dictionary-test");

        Event mockEvent = mockEvent(dictionary, EventType.PUBLISH_DICTIONARY);
        when(objectMapper.readValue(mockEvent.getPayload(), Dictionary.class)).thenThrow(new NullPointerException());
        when(
            eventRepository.searchLatest(
                argThat(
                    criteria ->
                        criteria != null &&
                        criteria.getTypes().containsAll(Arrays.asList(EventType.PUBLISH_DICTIONARY, EventType.UNPUBLISH_DICTIONARY))
                ),
                eq(Event.EventProperties.DICTIONARY_ID),
                anyLong(),
                anyLong()
            )
        )
            .thenReturn(singletonList(mockEvent));

        dictionarySynchronizer.synchronize(System.currentTimeMillis() - 5000, System.currentTimeMillis(), ENVIRONMENTS);

        verify(dictionaryManager, never()).deploy((dictionary));
    }

    private Event mockEvent(final Dictionary dictionary, EventType eventType) throws Exception {
        Map<String, String> properties = new HashMap<>();
        properties.put(Event.EventProperties.DICTIONARY_ID.getValue(), dictionary.getId());

        Event event = new Event();
        event.setType(eventType);
        event.setCreatedAt(new Date());
        event.setProperties(properties);
        event.setPayload(dictionary.getId());

        when(objectMapper.readValue(event.getPayload(), Dictionary.class)).thenReturn(dictionary);

        return event;
    }
}
