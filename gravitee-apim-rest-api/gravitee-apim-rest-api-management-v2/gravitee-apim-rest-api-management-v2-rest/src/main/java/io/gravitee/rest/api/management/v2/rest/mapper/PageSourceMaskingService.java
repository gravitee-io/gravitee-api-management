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
package io.gravitee.rest.api.management.v2.rest.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.documentation.model.PageSource;
import io.gravitee.fetcher.api.Fetcher;
import io.gravitee.fetcher.api.FetcherConfiguration;
import io.gravitee.fetcher.api.FetcherException;
import io.gravitee.fetcher.api.Sensitive;
import io.gravitee.plugin.core.api.PluginManager;
import io.gravitee.plugin.fetcher.FetcherPlugin;
import io.gravitee.rest.api.fetcher.FetcherConfigurationFactory;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import jakarta.annotation.PostConstruct;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

/**
 * Service to mask sensitive data in page source configurations using the @Sensitive annotation,
 * matching the behavior in PageServiceImpl.removeSensitiveData()
 *
 * @author GraviteeSource Team
 */
@Service
@Slf4j
public class PageSourceMaskingService implements ApplicationContextAware {

    private static final String SENSITIVE_DATA_REPLACEMENT = "********";
    private static volatile ApplicationContext context;
    private static final Object contextLock = new Object();

    private final PluginManager<FetcherPlugin<?>> fetcherPluginManager;
    private final FetcherConfigurationFactory fetcherConfigurationFactory;
    private final ApplicationContext applicationContext;
    private final ObjectMapper objectMapper;

    @Autowired
    public PageSourceMaskingService(
        PluginManager<FetcherPlugin<?>> fetcherPluginManager,
        FetcherConfigurationFactory fetcherConfigurationFactory,
        ApplicationContext applicationContext
    ) {
        this.fetcherPluginManager = fetcherPluginManager;
        this.fetcherConfigurationFactory = fetcherConfigurationFactory;
        this.applicationContext = applicationContext;
        this.objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        synchronized (contextLock) {
            log.debug("Setting ApplicationContext for PageSourceMaskingService via ApplicationContextAware");
            PageSourceMaskingService.context = applicationContext;
        }
    }

    @PostConstruct
    public void init() {
        synchronized (contextLock) {
            // Ensure context is set after bean initialization
            if (context == null && applicationContext != null) {
                log.info("Initializing PageSourceMaskingService context from injected applicationContext in @PostConstruct");
                PageSourceMaskingService.context = applicationContext;
            }
            log.info("PageSourceMaskingService initialized. Context available: {}", context != null);
            if (context != null) {
                try {
                    // Verify we can get our own bean
                    PageSourceMaskingService self = context.getBean(PageSourceMaskingService.class);
                    if (self != null) {
                        log.info("PageSourceMaskingService successfully verified - can retrieve its own bean from context");
                    }
                } catch (Exception e) {
                    log.warn("Could not retrieve PageSourceMaskingService bean from context", e);
                }
            }
        }
    }

    /**
     * Get the service instance from Spring context
     * This allows MapStruct mappers to access the service statically
     */
    public static PageSourceMaskingService getInstance() {
        ApplicationContext currentContext = context;

        if (currentContext == null) {
            // Double-check locking pattern to ensure thread safety
            synchronized (contextLock) {
                currentContext = context;
                if (currentContext == null) {
                    log.warn(
                        "PageSourceMaskingService context is not initialized. " +
                            "This may indicate that the bean has not been initialized yet. " +
                            "Check that PageSourceMaskingService is properly configured as a Spring @Service bean."
                    );
                    return null;
                }
            }
        }

        try {
            PageSourceMaskingService service = currentContext.getBean(PageSourceMaskingService.class);
            log.debug("Successfully retrieved PageSourceMaskingService instance from context");
            return service;
        } catch (Exception e) {
            log.error("Failed to get PageSourceMaskingService from context", e);
            return null;
        }
    }

    /**
     * Masks sensitive data in the page source configuration and returns it as a Map
     * @param pageSource the page source from core model
     * @return the configuration map with sensitive fields masked, or null if no source
     */
    public Map<String, Object> maskSensitiveConfiguration(PageSource pageSource) {
        if (pageSource == null || pageSource.getConfiguration() == null || pageSource.getConfiguration().isEmpty()) {
            return null;
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> configMap = objectMapper.readValue(pageSource.getConfiguration(), LinkedHashMap.class);

            // Try to get the fetcher to identify sensitive fields via @Sensitive annotation
            Fetcher fetcher = getFetcher(pageSource);
            if (fetcher != null) {
                try {
                    FetcherConfiguration fetcherConfiguration = fetcher.getConfiguration();
                    removeSensitiveData(fetcherConfiguration);

                    @SuppressWarnings("unchecked")
                    Map<String, Object> result = objectMapper.convertValue(fetcherConfiguration, LinkedHashMap.class);
                    log.debug("Successfully masked sensitive data using fetcher plugin for type: {}", pageSource.getType());
                    return result;
                } catch (Exception e) {
                    log.error("Failed to mask sensitive data using fetcher plugin for type: {}", pageSource.getType(), e);
                    throw new TechnicalManagementException("Failed to mask sensitive data for fetcher type: " + pageSource.getType(), e);
                }
            } else {
                throw new TechnicalManagementException(
                    "Unable to mask sensitive data: no fetcher plugin available for type: " + pageSource.getType()
                );
            }
        } catch (Exception e) {
            log.error("Error while masking sensitive data in page source configuration", e);
            throw new TechnicalManagementException("An error occurred while masking page source configuration", e);
        }
    }

    private Fetcher getFetcher(PageSource pageSource) throws FetcherException {
        if (pageSource == null || pageSource.getConfiguration() == null || pageSource.getConfiguration().isEmpty()) {
            return null;
        }

        try {
            FetcherPlugin<?> fetcherPlugin = fetcherPluginManager.get(pageSource.getType());
            if (fetcherPlugin == null) {
                return null;
            }

            ClassLoader fetcherCL = fetcherPlugin.fetcher().getClassLoader();
            @SuppressWarnings("unchecked")
            Class<? extends FetcherConfiguration> fetcherConfigurationClass = (Class<? extends FetcherConfiguration>) fetcherCL.loadClass(
                fetcherPlugin.configuration().getName()
            );
            @SuppressWarnings("unchecked")
            Class<? extends Fetcher> fetcherClass = (Class<? extends Fetcher>) fetcherCL.loadClass(fetcherPlugin.clazz());

            FetcherConfiguration fetcherConfigurationInstance = fetcherConfigurationFactory.create(
                fetcherConfigurationClass,
                pageSource.getConfiguration()
            );
            Fetcher fetcher = fetcherClass.getConstructor(fetcherConfigurationClass).newInstance(fetcherConfigurationInstance);
            applicationContext.getAutowireCapableBeanFactory().autowireBean(fetcher);

            return fetcher;
        } catch (Exception e) {
            log.error("Error creating fetcher instance: {}", e.getMessage(), e);
            throw new FetcherException("Unable to create fetcher instance", e);
        }
    }

    private void removeSensitiveData(FetcherConfiguration fetcherConfiguration) {
        if (fetcherConfiguration == null) {
            return;
        }

        Field[] fields = fetcherConfiguration.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(Sensitive.class)) {
                boolean accessible = field.canAccess(fetcherConfiguration);
                field.setAccessible(true);
                try {
                    field.set(fetcherConfiguration, SENSITIVE_DATA_REPLACEMENT);
                } catch (IllegalAccessException e) {
                    log.error("Error while removing fetcher sensitive data", e);
                }
                if (!accessible) {
                    field.setAccessible(false);
                }
            }
        }
    }
}
