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
package io.gravitee.gateway.dictionary;

import io.gravitee.el.TemplateContext;
import io.gravitee.el.TemplateVariableProvider;
import io.gravitee.el.TemplateVariableScope;
import io.gravitee.el.annotations.TemplateVariable;
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
@TemplateVariable(scopes = { TemplateVariableScope.API, TemplateVariableScope.HEALTH_CHECK })
public class DictionaryTemplateProvider implements DictionaryManager, TemplateVariableProvider {

    private final Logger LOGGER = LoggerFactory.getLogger(DictionaryTemplateProvider.class);

    private final Map<String, Dictionary> dictionaries = new HashMap<>();
    private final Map<String, Map<String, String>> values = new HashMap<>();

    @Override
    public void provide(TemplateContext context) {
        context.setVariable("dictionaries", values);
    }

    @Override
    public void deploy(Dictionary dictionary) {
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

    @Override
    public void undeploy(String dictionaryId) {
        Dictionary dictionary = dictionaries.remove(dictionaryId);
        if (dictionary != null) {
            values.remove(dictionaryId);
            LOGGER.info("A dictionary has been undeployed: {}", dictionaryId);
        }
    }
}
