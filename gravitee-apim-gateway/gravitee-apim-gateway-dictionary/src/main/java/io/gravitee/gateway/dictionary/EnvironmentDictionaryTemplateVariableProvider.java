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
import java.util.Map;

@TemplateVariable(scopes = { TemplateVariableScope.API, TemplateVariableScope.HEALTH_CHECK })
public class EnvironmentDictionaryTemplateVariableProvider implements TemplateVariableProvider {

    private final String environmentId;
    private final DictionaryManager dictionaryManager;

    public EnvironmentDictionaryTemplateVariableProvider(String environmentId, DictionaryManager dictionaryManager) {
        this.environmentId = environmentId;
        this.dictionaryManager = dictionaryManager;
    }

    @Override
    public void provide(TemplateContext templateContext) {
        Map<String, Map<String, String>> dictionary = dictionaryManager.getDictionaries(environmentId);
        templateContext.setVariable("dictionaries", dictionary);
    }
}
