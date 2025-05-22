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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class StandaloneDictionaryManager implements DictionaryManager {

    private final Logger LOGGER = LoggerFactory.getLogger(StandaloneDictionaryManager.class);

    private final Map<String, Dictionary> dictionaries = new HashMap<>();
    private final Map<String, Map<String, String>> values = new HashMap<>();

    @Override
    public void deploy(Dictionary dictionary) {
        if (dictionary.getKey() == null) {
            Dictionary oldDictionary = dictionaries.get(dictionary.getId());
            if (oldDictionary == null || dictionary.getDeployedAt().after(oldDictionary.getDeployedAt())) {
                if (dictionary.getProperties() == null) {
                    dictionary.setProperties(Collections.emptyMap());
                }

                LOGGER.info("Dictionary {} has been deployed with {} properties", dictionary, dictionary.getProperties().size());
                dictionaries.put(dictionary.getId(), dictionary);
                values.put(dictionary.getId(), dictionary.getProperties());
            }
        }
    }

    @Override
    public void undeploy(Dictionary dictionary) {
        String dictionaryId = dictionary.getId();
        Dictionary removed = dictionaries.remove(dictionaryId);
        if (removed != null) {
            values.remove(dictionaryId);
            LOGGER.info("A dictionary has been undeployed: {}", dictionaryId);
        }
    }

    @Override
    public EnvironmentDictionaryTemplateVariableProvider createTemplateVariableProvider(String environmentId) {
        return new EnvironmentDictionaryTemplateVariableProvider(environmentId, this);
    }

    @Override
    public Map<String, Map<String, String>> getDictionaries(String environmentId) {
        return values;
    }
}
