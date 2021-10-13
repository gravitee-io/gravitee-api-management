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
package io.gravitee.gateway.reactor.handler.context;

import io.gravitee.el.TemplateVariableProvider;
import io.gravitee.el.TemplateVariableScope;
import io.gravitee.el.annotations.TemplateVariable;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.util.ClassUtils;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class TemplateVariableProviderFactory {

    private final Logger logger = LoggerFactory.getLogger(TemplateVariableProviderFactory.class);

    @Autowired
    private ApplicationContext applicationContext;

    private List<TemplateVariableProvider> providers;

    public List<TemplateVariableProvider> getTemplateVariableProviders() {
        if (providers == null) {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            providers =
                loadFactories(classLoader)
                    .stream()
                    .map(
                        name -> {
                            try {
                                Class<TemplateVariableProvider> instance = (Class<TemplateVariableProvider>) ClassUtils.forName(
                                    name,
                                    classLoader
                                );
                                return applicationContext.getBean(instance);
                            } catch (ClassNotFoundException e) {
                                logger.warn("TemplateVariableProvider class not found : {}", e.getMessage());
                                return null;
                            } catch (NoSuchBeanDefinitionException e) {
                                logger.debug("No TemplateVariableProvider of type {} is defined", name);
                                return null;
                            }
                        }
                    )
                    .filter(
                        provider -> {
                            if (provider != null) {
                                TemplateVariable annotation = provider.getClass().getAnnotation(TemplateVariable.class);
                                return annotation != null && Arrays.asList(annotation.scopes()).contains(getTemplateVariableScope());
                            }
                            return false;
                        }
                    )
                    .collect(Collectors.toList());
        }

        return providers;
    }

    public Set<String> loadFactories(ClassLoader classLoader) {
        return new LinkedHashSet<>(SpringFactoriesLoader.loadFactoryNames(TemplateVariableProvider.class, classLoader));
    }

    protected abstract TemplateVariableScope getTemplateVariableScope();
}
