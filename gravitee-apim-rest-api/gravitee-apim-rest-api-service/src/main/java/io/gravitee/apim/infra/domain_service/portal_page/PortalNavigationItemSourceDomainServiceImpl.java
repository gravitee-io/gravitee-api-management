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
package io.gravitee.apim.infra.domain_service.portal_page;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemSourceDomainService;
import io.gravitee.apim.core.portal_page.exception.InvalidPortalNavigationItemSourceException;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemSource;
import io.gravitee.fetcher.api.Fetcher;
import io.gravitee.fetcher.api.FetcherConfiguration;
import io.gravitee.fetcher.api.FetcherException;
import io.gravitee.fetcher.api.Sensitive;
import io.gravitee.plugin.core.api.PluginManager;
import io.gravitee.plugin.fetcher.FetcherPlugin;
import io.gravitee.rest.api.fetcher.FetcherConfigurationFactory;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@CustomLog
public class PortalNavigationItemSourceDomainServiceImpl implements PortalNavigationItemSourceDomainService {

    public static final String SENSITIVE_DATA_REPLACEMENT = "********";
    public static final String BLANKED_CONFIGURATION = "{}";

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    private final FetcherConfigurationFactory fetcherConfigurationFactory;
    private final PluginManager<FetcherPlugin<?>> pluginManager;
    private final ApplicationContext applicationContext;

    private final Map<String, Set<String>> sensitiveKeysBySourceType = new ConcurrentHashMap<>();

    @Override
    public String fetchContent(PortalNavigationItemSource source) {
        var fetcher = loadFetcher(source).orElseThrow(() ->
            InvalidPortalNavigationItemSourceException.unknownSourceType(source.getSourceType())
        );
        return readContent(fetcher, source);
    }

    /** Never throws: if the sensitive keys cannot be resolved, the configuration is blanked from the response. */
    @Override
    public void removeSensitiveData(PortalNavigationItemSource source) {
        try {
            var sensitiveKeys = sensitiveConfigurationKeys(source.getSourceType());
            if (sensitiveKeys == null) {
                source.setSourceConfiguration(BLANKED_CONFIGURATION);
                return;
            }
            var configuration = (ObjectNode) JSON_MAPPER.readTree(source.getSourceConfiguration());
            boolean masked = false;
            for (String key : sensitiveKeys) {
                var value = configuration.get(key);
                if (value != null && !value.isNull()) {
                    configuration.put(key, SENSITIVE_DATA_REPLACEMENT);
                    masked = true;
                }
            }
            if (masked) {
                source.setSourceConfiguration(JSON_MAPPER.writeValueAsString(configuration));
            }
        } catch (Exception e) {
            log.warn(
                "Unable to mask sensitive data of portal page source [type={}], blanking its configuration",
                source.getSourceType(),
                e
            );
            source.setSourceConfiguration(BLANKED_CONFIGURATION);
        }
    }

    /**
     * Restores stored secrets only within the same source type: a placeholder sent against another
     * fetcher would otherwise hand that fetcher the previous one's credentials. Across a type change
     * the placeholder is left unresolved, and validation rejects it.
     */
    @Override
    public void mergeSensitiveData(PortalNavigationItemSource oldSource, PortalNavigationItemSource newSource) {
        if (oldSource == null || newSource == null || !Objects.equals(oldSource.getSourceType(), newSource.getSourceType())) {
            return;
        }
        try {
            var sensitiveKeys = sensitiveConfigurationKeys(newSource.getSourceType());
            if (sensitiveKeys == null || sensitiveKeys.isEmpty()) {
                return;
            }
            var updated = (ObjectNode) JSON_MAPPER.readTree(newSource.getSourceConfiguration());
            var original = (ObjectNode) JSON_MAPPER.readTree(oldSource.getSourceConfiguration());
            boolean merged = false;
            for (String key : sensitiveKeys) {
                var value = updated.get(key);
                if (value != null && value.isTextual() && SENSITIVE_DATA_REPLACEMENT.equals(value.textValue())) {
                    var originalValue = original.get(key);
                    if (originalValue != null) {
                        updated.set(key, originalValue);
                    } else {
                        updated.remove(key);
                    }
                    merged = true;
                }
            }
            if (merged) {
                newSource.setSourceConfiguration(JSON_MAPPER.writeValueAsString(updated));
            }
        } catch (Exception e) {
            log.warn("Unable to merge sensitive data of portal page source [type={}]", newSource.getSourceType(), e);
        }
    }

    /** Only successful resolutions are cached, so a plugin loaded later is not stuck on a missed lookup. */
    private Set<String> sensitiveConfigurationKeys(String sourceType) throws ClassNotFoundException {
        var cached = sensitiveKeysBySourceType.get(sourceType);
        if (cached != null) {
            return cached;
        }
        var plugin = pluginManager.get(sourceType);
        if (plugin == null) {
            return null;
        }
        var configurationClass = plugin.fetcher().getClassLoader().loadClass(plugin.configuration().getName());
        var sensitiveKeys = Arrays.stream(configurationClass.getDeclaredFields())
            .filter(field -> field.isAnnotationPresent(Sensitive.class))
            .map(Field::getName)
            .collect(Collectors.toUnmodifiableSet());
        sensitiveKeysBySourceType.put(sourceType, sensitiveKeys);
        return sensitiveKeys;
    }

    @Override
    public void validateSourceConfiguration(PortalNavigationItemSource source) {
        var plugin = loadPlugin(source).orElseThrow(() ->
            InvalidPortalNavigationItemSourceException.unknownSourceType(source.getSourceType())
        );
        rejectUnresolvedPlaceholders(source);
        try {
            buildFetcher(plugin, source);
        } catch (Exception e) {
            throw InvalidPortalNavigationItemSourceException.invalidSourceConfiguration(source.getSourceType(), e);
        }
        if (source.isUseAutoFetch() && source.getFetchCron() == null) {
            throw InvalidPortalNavigationItemSourceException.cronRequiredForAutoFetch();
        }
        if (source.getFetchCron() != null && !CronExpression.isValidExpression(source.getFetchCron())) {
            throw InvalidPortalNavigationItemSourceException.invalidCronExpression(source.getFetchCron());
        }
    }

    /**
     * Runs after the merge: a placeholder still there had no stored value to restore, and would be
     * persisted as the secret itself.
     */
    private void rejectUnresolvedPlaceholders(PortalNavigationItemSource source) {
        try {
            var sensitiveKeys = sensitiveConfigurationKeys(source.getSourceType());
            if (sensitiveKeys == null || sensitiveKeys.isEmpty()) {
                return;
            }
            var configuration = (ObjectNode) JSON_MAPPER.readTree(source.getSourceConfiguration());
            for (String key : sensitiveKeys) {
                var value = configuration.get(key);
                if (value != null && value.isTextual() && SENSITIVE_DATA_REPLACEMENT.equals(value.textValue())) {
                    throw InvalidPortalNavigationItemSourceException.unresolvedSensitivePlaceholder(key);
                }
            }
        } catch (InvalidPortalNavigationItemSourceException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Unable to check sensitive placeholders of portal page source [type={}]", source.getSourceType(), e);
        }
    }

    private String readContent(Fetcher fetcher, PortalNavigationItemSource source) {
        try {
            var resource = fetcher.fetch();
            try (var content = resource.getContent()) {
                return new String(content.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (FetcherException | IOException e) {
            throw new TechnicalDomainException("unable to fetch content from source type " + source.getSourceType(), e);
        }
    }

    private Optional<Fetcher> loadFetcher(PortalNavigationItemSource source) {
        return loadPlugin(source).map(plugin -> buildFetcher(plugin, source));
    }

    private Optional<FetcherPlugin<?>> loadPlugin(PortalNavigationItemSource source) {
        return Optional.ofNullable(pluginManager.get(source.getSourceType()));
    }

    @SuppressWarnings("unchecked")
    private Fetcher buildFetcher(FetcherPlugin<?> plugin, PortalNavigationItemSource source) {
        try {
            var classLoader = plugin.fetcher().getClassLoader();
            var configClass = (Class<? extends FetcherConfiguration>) classLoader.loadClass(plugin.configuration().getName());
            var config = fetcherConfigurationFactory.create(configClass, source.getSourceConfiguration());
            if (config == null) {
                throw new TechnicalDomainException("configuration cannot be read by fetcher type " + source.getSourceType());
            }
            var fetcherClass = (Class<? extends Fetcher>) classLoader.loadClass(plugin.clazz());
            var fetcher = fetcherClass.getConstructor(configClass).newInstance(config);
            applicationContext.getAutowireCapableBeanFactory().autowireBean(fetcher);
            return fetcher;
        } catch (Exception e) {
            throw new TechnicalDomainException("unable to build fetcher instance", e);
        }
    }
}
