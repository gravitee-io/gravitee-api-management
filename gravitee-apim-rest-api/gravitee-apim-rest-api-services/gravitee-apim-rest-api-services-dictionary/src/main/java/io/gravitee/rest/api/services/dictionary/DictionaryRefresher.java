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
package io.gravitee.rest.api.services.dictionary;

import io.gravitee.definition.model.Property;
import io.gravitee.rest.api.model.configuration.dictionary.DictionaryEntity;
import io.gravitee.rest.api.model.configuration.dictionary.UpdateDictionaryEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.impl.configuration.dictionary.DictionaryNotFoundException;
import io.gravitee.rest.api.services.dictionary.model.DynamicProperty;
import io.gravitee.rest.api.services.dictionary.provider.Provider;
import io.vertx.core.Handler;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class DictionaryRefresher implements Handler<Long> {

    private DictionaryEntity dictionary;
    private Provider provider;
    private io.gravitee.rest.api.service.configuration.dictionary.DictionaryService dictionaryService;

    public DictionaryRefresher(final DictionaryEntity dictionary) {
        this.dictionary = dictionary;
    }

    @Override
    public void handle(Long event) {
        log.debug("Running dictionary refresher task for {}", dictionary);

        provider
            .get()
            .whenComplete((dynamicProperties, throwable) -> {
                if (throwable != null) {
                    log.error(
                        "[{}] Unexpected error while getting dictionary's properties from provider: {}",
                        dictionary.getId(),
                        provider.name(),
                        throwable
                    );
                } else if (dynamicProperties != null) {
                    updateDictionary(dynamicProperties);
                }
            });
    }

    private void updateDictionary(Collection<DynamicProperty> dynProperties) {
        Map<String, String> properties = dynProperties
            .stream()
            .collect(
                Collectors.toMap(
                    Property::getKey,
                    dynamicProperty -> (dynamicProperty.getValue() == null) ? "" : dynamicProperty.getValue(),
                    (oldValue, newValue) -> {
                        log.error(
                            "Duplicate key found (attempted merging values {} and {}) for dictionary {}",
                            oldValue,
                            newValue,
                            dictionary.getName()
                        );
                        throw new IllegalStateException(
                            String.format(
                                "Duplicate key found (attempted merging values {} and {}) for dictionary {}",
                                oldValue,
                                newValue,
                                dictionary.getName()
                            )
                        );
                    }
                )
            );

        // Compare properties with latest values
        if (!properties.equals(dictionary.getProperties())) {
            try {
                // Get a fresh version of the dictionary before updating its properties.
                dictionary = dictionaryService.updateProperties(GraviteeContext.getExecutionContext(), dictionary.getId(), properties);
            } catch (DictionaryNotFoundException e) {
                log.debug("Trying to update a deleted dictionary {} - nothing to do...", dictionary.getId());
            } catch (Exception ex) {
                log.error("Unexpected error while updating and deploying the dictionary {}", dictionary.getId(), ex);
            }
        }
    }

    public void setDictionaryService(io.gravitee.rest.api.service.configuration.dictionary.DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

    public void setProvider(Provider provider) {
        this.provider = provider;
    }
}
