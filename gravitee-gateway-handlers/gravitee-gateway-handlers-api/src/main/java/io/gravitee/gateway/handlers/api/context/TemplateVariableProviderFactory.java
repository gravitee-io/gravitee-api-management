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
package io.gravitee.gateway.handlers.api.context;

import io.gravitee.gateway.api.expression.TemplateVariableProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.util.ClassUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class TemplateVariableProviderFactory {

    @Autowired
    private ApplicationContext applicationContext;

    List<TemplateVariableProvider> getTemplateVariableProviders() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Set<String> factories = new LinkedHashSet<>(SpringFactoriesLoader.loadFactoryNames(
                TemplateVariableProvider.class, Thread.currentThread().getContextClassLoader()));

        return factories.stream()
                .map(new Function<String, TemplateVariableProvider>() {
                    @Override
                    public TemplateVariableProvider apply(String name) {
                        try {
                            Class<TemplateVariableProvider> instanceClass =
                                    (Class<TemplateVariableProvider>) ClassUtils.forName(name, classLoader);
                            return applicationContext.getBean(instanceClass);
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                            return null;
                        }
                    }
                }).collect(Collectors.toList());
    }
}
