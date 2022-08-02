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
package io.gravitee.rest.api.services.sync;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.gravitee.common.event.EventManager;
import io.gravitee.rest.api.model.configuration.dictionary.DictionaryEntity;
import io.gravitee.rest.api.model.configuration.dictionary.DictionaryProviderEntity;
import io.gravitee.rest.api.model.configuration.dictionary.DictionaryTriggerEntity;
import io.gravitee.rest.api.service.configuration.dictionary.DictionaryService;
import io.gravitee.rest.api.service.event.DictionaryEvent;
import io.gravitee.rest.api.services.sync.DictionaryManager;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class DictionaryManagerTest {

    private static final String DICTIONARY_ID = "dictionary#1";

    @Mock
    private DictionaryService dictionaryService;

    @Mock
    private EventManager eventManager;

    @InjectMocks
    private DictionaryManager cut;

    @Before
    public void before() {
        cut = new DictionaryManager();
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldStart() {
        final DictionaryEntity dictionary = new DictionaryEntity();
        dictionary.setId(DICTIONARY_ID);

        when(dictionaryService.findById(DICTIONARY_ID)).thenReturn(dictionary);
        cut.start(DICTIONARY_ID);

        verify(eventManager, times(1)).publishEvent(eq(DictionaryEvent.START), eq(dictionary));
    }

    @Test
    public void shouldNotStartAlreadyStarted() {
        final DictionaryProviderEntity provider = new DictionaryProviderEntity();
        provider.setConfiguration(JsonNodeFactory.instance.nullNode());

        final DictionaryEntity dictionary = new DictionaryEntity();
        dictionary.setUpdatedAt(new Date());
        dictionary.setProvider(provider);
        dictionary.setId(DICTIONARY_ID);

        when(dictionaryService.findById(DICTIONARY_ID)).thenReturn(dictionary);

        cut.start(DICTIONARY_ID);
        verify(eventManager, times(1)).publishEvent(eq(DictionaryEvent.START), eq(dictionary));

        cut.start(DICTIONARY_ID);
        verifyNoMoreInteractions(eventManager);
    }

    @Test
    public void shouldRestartTriggerChanged() {
        final DictionaryProviderEntity provider = new DictionaryProviderEntity();
        provider.setConfiguration(JsonNodeFactory.instance.nullNode());

        final DictionaryTriggerEntity trigger = new DictionaryTriggerEntity();
        trigger.setRate(1);
        trigger.setUnit(TimeUnit.SECONDS);

        final DictionaryEntity dictionary = new DictionaryEntity();
        dictionary.setUpdatedAt(new Date());
        dictionary.setProvider(provider);
        dictionary.setId(DICTIONARY_ID);
        dictionary.setTrigger(trigger);

        final DictionaryTriggerEntity triggerUpdated = new DictionaryTriggerEntity();
        triggerUpdated.setRate(1);
        triggerUpdated.setUnit(TimeUnit.MINUTES);

        final DictionaryEntity dictionaryUpdated = new DictionaryEntity();
        dictionaryUpdated.setUpdatedAt(new Date(ZonedDateTime.now().plusSeconds(60).toInstant().toEpochMilli()));
        dictionaryUpdated.setProvider(provider);
        dictionaryUpdated.setId(DICTIONARY_ID);
        dictionaryUpdated.setTrigger(triggerUpdated);

        when(dictionaryService.findById(DICTIONARY_ID)).thenReturn(dictionary).thenReturn(dictionaryUpdated);

        cut.start(DICTIONARY_ID);
        cut.start(DICTIONARY_ID);

        verify(eventManager, times(1)).publishEvent(eq(DictionaryEvent.START), eq(dictionary));
        verify(eventManager, times(1)).publishEvent(eq(DictionaryEvent.RESTART), eq(dictionary));
        verifyNoMoreInteractions(eventManager);
    }

    @Test
    public void shouldRestartProviderConfigChanged() {
        final DictionaryProviderEntity provider = new DictionaryProviderEntity();
        provider.setConfiguration(JsonNodeFactory.instance.nullNode());

        final DictionaryTriggerEntity trigger = new DictionaryTriggerEntity();
        trigger.setRate(1);
        trigger.setUnit(TimeUnit.SECONDS);

        final DictionaryEntity dictionary = new DictionaryEntity();
        dictionary.setUpdatedAt(new Date());
        dictionary.setProvider(provider);
        dictionary.setId(DICTIONARY_ID);
        dictionary.setTrigger(trigger);

        final DictionaryProviderEntity providerUpdated = new DictionaryProviderEntity();
        providerUpdated.setConfiguration(JsonNodeFactory.instance.arrayNode());

        final DictionaryEntity dictionaryUpdated = new DictionaryEntity();
        dictionaryUpdated.setUpdatedAt(new Date(ZonedDateTime.now().plusSeconds(60).toInstant().toEpochMilli()));
        dictionaryUpdated.setProvider(providerUpdated);
        dictionaryUpdated.setId(DICTIONARY_ID);
        dictionaryUpdated.setTrigger(trigger);

        when(dictionaryService.findById(DICTIONARY_ID)).thenReturn(dictionary).thenReturn(dictionaryUpdated);

        cut.start(DICTIONARY_ID);
        cut.start(DICTIONARY_ID);

        verify(eventManager, times(1)).publishEvent(eq(DictionaryEvent.START), eq(dictionary));
        verify(eventManager, times(1)).publishEvent(eq(DictionaryEvent.RESTART), eq(dictionary));
        verifyNoMoreInteractions(eventManager);
    }

    @Test
    public void shouldStop() {
        cut.stop(DICTIONARY_ID);
        verify(eventManager, times(1))
            .publishEvent(eq(DictionaryEvent.STOP), argThat(dictionary -> (((DictionaryEntity) dictionary).getId().equals(DICTIONARY_ID))));
    }
}
