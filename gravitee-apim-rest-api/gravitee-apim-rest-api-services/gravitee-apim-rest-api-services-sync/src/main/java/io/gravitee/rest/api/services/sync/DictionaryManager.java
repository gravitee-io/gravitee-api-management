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

import io.gravitee.common.event.EventManager;
import io.gravitee.rest.api.model.configuration.dictionary.DictionaryEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.configuration.dictionary.DictionaryService;
import io.gravitee.rest.api.service.event.DictionaryEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DictionaryManager {

    private final Logger logger = LoggerFactory.getLogger(DictionaryManager.class);

    @Autowired
    private DictionaryService dictionaryService;

    @Autowired
    private EventManager eventManager;

    private final Map<String, DictionaryEntity> dictionaries = new HashMap<>();

    /**
     * Starts or restarts a dictionary if it has been updated since last start.
     *
     * @param dictionaryId the id of the dictionary to start.
     */
    public void start(String dictionaryId) {
        try {
            // FIXME Adding a fake execution context to match the new signature. It's not ideal but for the moment we have to fix quickly.
            final DictionaryEntity dictionary = dictionaryService.findById(new ExecutionContext(), dictionaryId);
            final DictionaryEntity startedDictionary = dictionaries.get(dictionary.getId());

            if (startedDictionary == null) {
                // The dictionary is not yet started, start it.
                eventManager.publishEvent(DictionaryEvent.START, dictionary);
            } else if (needRestart(dictionary, startedDictionary)) {
                // The dictionary is already started but has been updated, force a restart.
                eventManager.publishEvent(DictionaryEvent.RESTART, dictionary);
            }

            dictionaries.put(dictionary.getId(), dictionary);
        } catch (Exception e) {
            logger.error("Error occurred when trying to start the dictionary [{}].", dictionaryId, e);
        }
    }

    /**
     * Stop the dictionary.
     * @param dictionaryId the id of the dictionary.
     */
    public void stop(String dictionaryId) {
        try {
            DictionaryEntity dictionary = DictionaryEntity.builder().id(dictionaryId).build();
            eventManager.publishEvent(DictionaryEvent.STOP, dictionary);

            dictionaries.remove(dictionaryId);
        } catch (Exception e) {
            logger.error("Error occurred when trying to stop the dictionary [{}].", dictionaryId, e);
        }
    }

    private boolean needRestart(DictionaryEntity dictionary, DictionaryEntity startedDictionary) {
        // A dictionary needs to be restarted if its configuration or trigger has changed.
        return (
            (dictionary.getUpdatedAt() != null && dictionary.getUpdatedAt().after(startedDictionary.getUpdatedAt())) &&
            (
                !Objects.equals(dictionary.getProvider().getConfiguration(), startedDictionary.getProvider().getConfiguration()) ||
                !Objects.equals(dictionary.getTrigger(), startedDictionary.getTrigger())
            )
        );
    }
}
