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
package io.gravitee.gateway.handlers.api.http.client.spring;

import io.gravitee.gateway.api.Connector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.DeferredImportSelector;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * @author David BRASSELY (david at gravitee.io)
 * @author GraviteeSource Team
 */
public class HttpClientConfigurationImportSelector implements DeferredImportSelector {

    private final Logger LOGGER = LoggerFactory.getLogger(HttpClientConfigurationImportSelector.class);

    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        LOGGER.debug("Looking for an HTTP Client implementation");
        List<String> configurations = getCandidateConfigurations();
        if (configurations.isEmpty()) {
            LOGGER.error("No HTTP client implementation can be found !");
            throw new IllegalStateException("No HTTP client implementation can be found !");
        }

        LOGGER.debug("\tFound {} {} implementation(s)", configurations.size(), Connector.class.getSimpleName());

        configurations = removeDuplicates(configurations);
        return configurations.toArray(new String[configurations.size()]);
    }

    /**
     * Return the HTTP client class names that should be considered. By default
     * this method will load candidates using {@link SpringFactoriesLoader} with
     * {@link #getSpringFactoriesLoaderFactoryClass()}.
     * attributes}
     * @return a list of candidate configurations
     */
    protected List<String> getCandidateConfigurations() {
        List<String> configurations = SpringFactoriesLoader.loadFactoryNames(
                getSpringFactoriesLoaderFactoryClass(), this.getClass().getClassLoader());
        Assert.notEmpty(configurations,
                "No HTTP client classes found in META-INF/spring.factories. If you" +
                        "are using a custom packaging, make sure that file is correct.");
        return configurations;
    }

    /**
     * Return the class used by {@link org.springframework.core.io.support.SpringFactoriesLoader} to
     * load configuration candidates.
     * @return the factory class
     */
    protected Class<?> getSpringFactoriesLoaderFactoryClass() {
        return Connector.class;
    }

    protected final <T> List<T> removeDuplicates(List<T> list) {
        return new ArrayList<>(new LinkedHashSet<>(list));
    }
}
