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

import io.gravitee.el.TemplateContext;
import io.gravitee.el.TemplateVariableProvider;
import io.gravitee.el.TemplateVariableScope;
import io.gravitee.el.annotations.TemplateVariable;
import io.gravitee.gateway.dictionary.model.Dictionary;
import io.gravitee.gateway.reactive.api.context.ContextAttributes;
import io.gravitee.gateway.reactive.api.context.HttpExecutionContext;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@TemplateVariable(scopes = { TemplateVariableScope.API, TemplateVariableScope.HEALTH_CHECK })
public class DictionaryTemplateProvider implements DictionaryManager, TemplateVariableProvider {

    private final Logger LOGGER = LoggerFactory.getLogger(DictionaryTemplateProvider.class);

    private final Map<String, Map<String, Dictionary>> dictionaries = new HashMap<>();
    private final Map<String, Map<String, Map<String, String>>> values = new HashMap<>();

    @Override
    public <T extends HttpExecutionContext> void provide(T ctx) {
        String environmentId = ctx.getAttribute(ContextAttributes.ATTR_ENVIRONMENT);
        TemplateContext templateContext = ctx.getTemplateEngine().getTemplateContext();
        templateContext.setVariable("dictionaries", values.get(environmentId));
    }

    @Override
    public void deploy(Dictionary dictionary) {
        String environmentId = dictionary.getEnvironmentId();
        //fallback on legacy events
        String key = dictionary.getKey() == null ? dictionary.getId() : dictionary.getKey();

        dictionaries.putIfAbsent(environmentId, new HashMap<>());
        values.putIfAbsent(environmentId, new HashMap<>());

        Dictionary existing = dictionaries.get(environmentId).get(key);
        if (existing == null || dictionary.getDeployedAt().after(existing.getDeployedAt())) {
            if (dictionary.getProperties() == null) {
                dictionary.setProperties(Collections.emptyMap());
            }

            LOGGER.info("Dictionary {} has been deployed with {} properties", dictionary, dictionary.getProperties().size());
            dictionaries.get(environmentId).put(key, dictionary);
            values.get(environmentId).put(key, dictionary.getProperties());
        }
    }

    @Override
    public void undeploy(Dictionary dictionary) {
        String environmentId = dictionary.getEnvironmentId();
        //fallback on legacy events
        String key = dictionary.getKey() == null ? dictionary.getId() : dictionary.getKey();
        Map<String, Dictionary> envDictionaries = dictionaries.get(environmentId);
        if (envDictionaries != null) {
            Dictionary removed = envDictionaries.remove(key);

            if (envDictionaries.isEmpty()) {
                dictionaries.remove(environmentId);
            }

            if (removed != null) {
                Map<String, Map<String, String>> envValues = values.get(environmentId);
                if (envValues != null) {
                    envValues.remove(key);
                    if (envValues.isEmpty()) {
                        values.remove(environmentId);
                    }
                }

                LOGGER.info("A dictionary has been undeployed: {}", removed);
            }
        }
    }

    @Override
    public void provide(TemplateContext templateContext) {
        //overridden default method to enable http context access
    }
}
