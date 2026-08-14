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
package io.gravitee.gateway.dictionary;

import io.gravitee.gateway.dictionary.model.Dictionary;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import lombok.CustomLog;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
public class MultiEnvironmentDictionaryManager implements DictionaryManager {

    private final Map<String, Map<String, Dictionary>> dictionaries = new HashMap<>();
    private final Map<String, Map<String, Map<String, String>>> values = new HashMap<>();

    @Override
    public void deploy(Dictionary dictionary) {
        String environmentId = dictionary.getEnvironmentId();
        String key = runtimeKey(dictionary);

        dictionaries.putIfAbsent(environmentId, new HashMap<>());
        values.putIfAbsent(environmentId, new HashMap<>());

        Dictionary existing = dictionaries.get(environmentId).get(key);
        if (existing != null && !Objects.equals(existing.getId(), dictionary.getId())) {
            log.error(
                "Dictionary {} (name={}) collides on runtime key '{}' in environment {} with existing dictionary {} (name={}). Incoming deploy is rejected; occupant is kept",
                dictionary.getId(),
                dictionary.getName(),
                key,
                environmentId,
                existing.getId(),
                existing.getName()
            );
            return;
        }
        if (existing == null || dictionary.getDeployedAt().after(existing.getDeployedAt())) {
            if (dictionary.getProperties() == null) {
                dictionary.setProperties(Collections.emptyMap());
            }

            log.info("Dictionary {} has been deployed with {} properties", dictionary, dictionary.getProperties().size());
            dictionaries.get(environmentId).put(key, dictionary);
            values.get(environmentId).put(key, dictionary.getProperties());
        }
    }

    @Override
    public void undeploy(Dictionary dictionary) {
        String environmentId = dictionary.getEnvironmentId();
        String key = runtimeKey(dictionary);
        Map<String, Dictionary> envDictionaries = dictionaries.get(environmentId);
        if (envDictionaries != null) {
            Dictionary occupant = envDictionaries.get(key);
            if (occupant == null) {
                return;
            }
            if (!Objects.equals(occupant.getId(), dictionary.getId())) {
                log.debug(
                    "Dictionary {} was requested for undeploy but runtime key '{}' is occupied by dictionary {} — ignoring",
                    dictionary.getId(),
                    key,
                    occupant.getId()
                );
                return;
            }

            envDictionaries.remove(key);
            if (envDictionaries.isEmpty()) {
                dictionaries.remove(environmentId);
            }

            Map<String, Map<String, String>> envValues = values.get(environmentId);
            if (envValues != null) {
                envValues.remove(key);
                if (envValues.isEmpty()) {
                    values.remove(environmentId);
                }
            }

            log.info("A dictionary has been undeployed: {}", dictionary);
        }
    }

    /**
     * Runtime map key: prefer the dictionary {@code key} when present, otherwise fall back to {@code id}
     * for legacy events that predate the key field.
     */
    private static String runtimeKey(Dictionary dictionary) {
        String key = dictionary.getKey();
        return key == null || key.isBlank() ? dictionary.getId() : key;
    }

    @Override
    public EnvironmentDictionaryTemplateVariableProvider createTemplateVariableProvider(String environmentId) {
        return new EnvironmentDictionaryTemplateVariableProvider(environmentId, this);
    }

    @Override
    public Map<String, Map<String, String>> getDictionaries(String environmentId) {
        return values.get(environmentId);
    }
}
